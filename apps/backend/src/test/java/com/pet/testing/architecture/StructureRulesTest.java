package com.pet.testing.architecture;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.*;

import static org.junit.jupiter.api.Assertions.*;

/** 编译字节码结构门禁；反例由ASM生成，不需要制造生产业务类。 */
class StructureRulesTest {
    private static final String ROOT = "com/pet/platform/";
    record Symbols(String name, Set<String> dependencies, Set<String> calls) { }

    static Symbols symbols(byte[] bytes) {
        var reader = new ClassReader(bytes);
        var refs = new TreeSet<String>();
        var calls = new TreeSet<String>();
        // 所有符号类常量覆盖指令owner、class literal、bootstrap handle等；不搜索源码字符串。
        var buffer = new char[reader.getMaxStringLength()];
        for (int i = 1; i < reader.getItemCount(); i++) {
            int item = reader.getItem(i);
            if (item == 0) continue;
            int tag = reader.readByte(item - 1);
            if (tag == 7) addType(reader.readUTF8(item, buffer), refs);
            if (tag == 12) descriptor(reader.readUTF8(item + 2, buffer), refs);
            if (tag == 16) descriptor(reader.readUTF8(item, buffer), refs);
        }
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            void signature(String signature) {
                if (signature == null) return;
                var visitor = new SignatureVisitor(Opcodes.ASM9) {
                    String outer;
                    @Override public void visitClassType(String name) { refs.add(name); outer = name; }
                    @Override public void visitInnerClassType(String name) { outer += "$" + name; refs.add(outer); }
                };
                new SignatureReader(signature).accept(visitor);
            }
            AnnotationVisitor annotation(String desc) {
                descriptor(desc, refs);
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override public void visit(String name, Object value) { if (value instanceof Type t) descriptor(t.getDescriptor(), refs); }
                    @Override public void visitEnum(String name, String desc, String value) { descriptor(desc, refs); }
                    @Override public AnnotationVisitor visitAnnotation(String name, String desc) { return annotation(desc); }
                    @Override public AnnotationVisitor visitArray(String name) { return this; }
                };
            }
            @Override public void visit(int version, int access, String name, String signature, String parent, String[] interfaces) {
                signature(signature);
            }
            @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) { return annotation(desc); }
            @Override public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                descriptor(desc, refs); signature(signature);
                return new FieldVisitor(Opcodes.ASM9) {
                    @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) { return annotation(desc); }
                };
            }
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                descriptor(desc, refs); signature(signature);
                return new MethodVisitor(Opcodes.ASM9) {
                    private void call(String owner, String name, String descriptor) {
                        calls.add(owner + "#" + name);
                        if (owner.equals("jakarta/persistence/EntityManager") && name.equals("createQuery") && descriptor.startsWith("(Ljava/lang/String;")) calls.add(owner + "#string-query");
                    }
                    private void bootstrap(Object value) {
                        if (value instanceof Handle h) call(h.getOwner(), h.getName(), h.getDesc());
                        if (value instanceof ConstantDynamic c) {
                            bootstrap(c.getBootstrapMethod());
                            for (int i = 0; i < c.getBootstrapMethodArgumentCount(); i++) bootstrap(c.getBootstrapMethodArgument(i));
                        }
                    }
                    @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) { call(owner, name, descriptor); }
                    @Override public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... arguments) {
                        bootstrap(handle); for (Object argument : arguments) bootstrap(argument);
                    }
                    @Override public void visitLdcInsn(Object value) { bootstrap(value); }
                    @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) { calls.add(owner + "#" + name); }
                    @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) { return annotation(desc); }
                    @Override public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) { return annotation(desc); }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        refs.remove(reader.getClassName());
        return new Symbols(reader.getClassName(), refs, calls);
    }
    private static void addType(String name, Set<String> refs) {
        if (name.startsWith("[")) descriptor(name, refs); else refs.add(name);
    }
    private static void descriptor(String desc, Set<String> refs) {
        if (desc == null) return;
        if (desc.startsWith("(")) {
            for (Type t : Type.getArgumentTypes(desc)) descriptor(t.getDescriptor(), refs);
            descriptor(Type.getReturnType(desc).getDescriptor(), refs);
        } else {
            Type t = Type.getType(desc);
            if (t.getSort() == Type.ARRAY) descriptor(t.getElementType().getDescriptor(), refs);
            if (t.getSort() == Type.OBJECT) refs.add(t.getInternalName());
        }
    }
    private static String module(String name) {
        if (!name.startsWith(ROOT)) return "";
        String[] parts = name.substring(ROOT.length()).split("/");
        return parts[0].equals("modules") && parts.length > 1 ? "modules/" + parts[1] : parts[0];
    }
    private static boolean repository(String target, Set<String> seen) {
        if (!seen.add(target)) return false;
        if (target.equals("org/springframework/data/repository/Repository")) return true;
        try (var stream = StructureRulesTest.class.getClassLoader().getResourceAsStream(target + ".class")) {
            if (stream == null) return false;
            var reader = new ClassReader(stream);
            for (String parent : reader.getInterfaces()) if (repository(parent, seen)) return true;
            return reader.getSuperName() != null && repository(reader.getSuperName(), seen);
        } catch (IOException exception) { throw new IllegalStateException("无法分析Repository继承", exception); }
    }
    private static boolean allowed(String from, String to) {
        if (to.equals("shared") || from.equals(to)) return true;
        if (from.startsWith("modules/")) return Set.of("platform", "identity", "customeridentity", "attachment", "audit", "notification").contains(to);
        return switch (from) {
            case "platform", "attachment", "notification" -> to.equals("audit");
            case "identity", "customeridentity" -> Set.of("platform", "audit").contains(to);
            default -> false;
        };
    }
    static List<String> violations(Symbols symbols, Set<String> testClasses) {
        var errors = new ArrayList<String>();
        String owner = module(symbols.name());
        for (String target : symbols.dependencies()) {
            String other = module(target);
            String rule = null;
            boolean contextModel = Set.of(ROOT + "shared/tenancy/TenantContext", ROOT + "shared/tenancy/DataScope",
                    ROOT + "shared/tenancy/ScopeGrant", ROOT + "shared/security/CurrentPrincipal").contains(symbols.name());
            boolean businessLayer = symbols.name().contains("/api/") || symbols.name().contains("/application/") || symbols.name().contains("/domain/");
            boolean rawPersistence = target.equals("jakarta/persistence/EntityManager") || target.equals("jakarta/persistence/EntityManagerFactory")
                    || target.equals("jakarta/persistence/Query") || target.equals("jakarta/persistence/TypedQuery")
                    || target.startsWith("org/hibernate/Session") || target.startsWith("org/springframework/jdbc/")
                    || target.startsWith("java/sql/") || target.equals("javax/sql/DataSource");
            boolean redisImplementation = Set.of(ROOT + "shared/redis/RedisValueStore", ROOT + "shared/redis/RedisConfiguration").contains(symbols.name());
            boolean rawRedis = target.startsWith("org/springframework/data/redis/") || target.startsWith("io/lettuce/") || target.startsWith("redis/clients/");
            boolean asyncImplementation = symbols.name().equals(ROOT + "shared/tenancy/TenantTaskExecutor") || symbols.name().startsWith(ROOT + "shared/tenancy/TenantTaskExecutor$");
            boolean rawAsync = target.startsWith("java/util/concurrent/Executor") || target.equals("java/util/concurrent/ThreadPoolExecutor")
                    || target.startsWith("java/util/concurrent/ThreadPoolExecutor$") || target.equals("java/util/concurrent/ForkJoinPool")
                    || target.equals("java/util/concurrent/ScheduledExecutorService") || target.equals("java/util/concurrent/ScheduledThreadPoolExecutor")
                    || target.equals("java/util/Timer") || target.equals("org/springframework/scheduling/annotation/Async")
                    || target.startsWith("org/springframework/core/task/") || target.startsWith("org/springframework/scheduling/concurrent/");
            if (rawRedis && !redisImplementation) rule = "绕过受控Redis访问";
            else if (target.startsWith("org/springframework/cache/")) rule = "当前禁止授权结果通用缓存";
            else if (rawAsync && !asyncImplementation) rule = "绕过批准的租户异步入口";
            else if (target.equals(ROOT + "shared/redis/RedisValueStore") && !symbols.name().startsWith(ROOT + "shared/redis/")) rule = "绕过受控Redis驱动";
            else if ((symbols.name().contains("/api/") || symbols.name().contains("/application/")) && target.startsWith(ROOT + "shared/persistence/ScopedPersistence")) rule = "业务层直连受控基础实现";
            else if (businessLayer && rawPersistence) rule = "业务层直连数据库";
            else if (!owner.isEmpty() && !owner.equals("shared") && repository(target, new HashSet<>())
                    && !target.startsWith(ROOT)) rule = "业务模块继承裸Repository";
            else if (target.equals("java/lang/InheritableThreadLocal")) rule = "禁止隐式继承身份";
            else if (contextModel && (target.startsWith("org/springframework/web/") || target.startsWith("jakarta/servlet/")
                    || target.contains("/api/") || target.startsWith("cn/binarywang/wx/") || target.startsWith("me/chanjar/weixin/"))) rule = "上下文模型依赖HTTP或SDK";
            else if (symbols.name().contains("/domain/") && target.startsWith(ROOT + "shared/tenancy/")) rule = "domain依赖线程范围";
            else if (target.startsWith("com/pet/testing/") || testClasses.contains(target)) rule = "生产引用测试夹具";
            else if (owner.equals("shared") && !other.isEmpty() && !other.equals("shared")) rule = "shared依赖具体模块";
            else if (symbols.name().contains("/domain/") && (target.contains("/api/")
                    || target.startsWith("org/springframework/web/") || target.startsWith("jakarta/servlet/")
                    || target.contains("/infrastructure/") || target.equals(ROOT + "shared/observability/TraceContext"))) rule = "domain依赖HTTP/api";
            else if (symbols.name().contains("/domain/") && (target.startsWith("cn/binarywang/wx/")
                    || target.startsWith("me/chanjar/weixin/"))) rule = "domain依赖WxJava";
            else if (!owner.isEmpty() && !other.isEmpty() && !owner.equals(other) && repository(target, new HashSet<>())) rule = "跨模块Repository";
            else if (!owner.isEmpty() && !other.isEmpty() && !allowed(owner, other)) rule = "模块依赖未登记";
            else if (!owner.isEmpty() && !other.isEmpty() && !owner.equals(other) && !other.equals("shared")
                    && !target.startsWith(ROOT + other + "/application/")) rule = "跨模块依赖非application（含Repository）";
            else if (symbols.name().contains("/api/") && (target.contains("/infrastructure/")
                    || repository(target, new HashSet<>())
                    || target.startsWith("org/springframework/data/repository/")
                    || target.startsWith("org/springframework/data/jpa/repository/"))) rule = "api直连持久化";
            if (rule != null) errors.add(rule + "：" + symbols.name() + " -> " + target);
        }
        for (String call : symbols.calls()) {
            String calledOwner = call.substring(0, call.indexOf('#'));
            String calledMethod = call.substring(call.indexOf('#') + 1);
            boolean controlledImplementation = Set.of(ROOT + "shared/persistence/ScopedPersistence", ROOT + "shared/persistence/ScopedTransaction").contains(symbols.name());
            boolean databaseCall = calledOwner.equals("jakarta/persistence/EntityManager") || calledOwner.equals("jakarta/persistence/Query")
                    || calledOwner.equals("jakarta/persistence/TypedQuery") || calledOwner.startsWith("org/hibernate/Session")
                    || calledOwner.startsWith("org/springframework/jdbc/") || calledOwner.startsWith("java/sql/")
                    || calledOwner.equals("javax/sql/DataSource");
            if (databaseCall && !controlledImplementation) errors.add("未登记底层持久化调用：" + symbols.name() + " -> " + call);
            if (calledOwner.equals("jakarta/persistence/EntityManager") && Set.of("merge", "find", "getReference", "createNativeQuery", "string-query").contains(calledMethod)) {
                errors.add("禁止无范围实体或SQL入口：" + symbols.name() + " -> " + call);
            }
            if (Set.of(ROOT + "shared/tenancy/TenantContextHolder#frame", ROOT + "shared/tenancy/TenantContextHolder#replace",
                    ROOT + "shared/tenancy/TenantContextHolder#CURRENT").contains(call)
                    && !Set.of(ROOT + "shared/tenancy/TenantContextHolder", ROOT + "shared/tenancy/TenantExecutionScope").contains(symbols.name())) {
                errors.add("直接操作上下文底层存储：" + symbols.name() + " -> " + call);
            }
            if (call.equals(ROOT + "shared/tenancy/TenantExecutionScope#openIdentity")
                    && !Set.of(ROOT + "shared/tenancy/TenantContextFilter", ROOT + "shared/tenancy/TrustedTenantExecutor").contains(symbols.name())) {
                errors.add("绕过可信身份入口：" + symbols.name());
            }
            boolean taskExecutor = symbols.name().equals(ROOT + "shared/tenancy/TenantTaskExecutor") || symbols.name().startsWith(ROOT + "shared/tenancy/TenantTaskExecutor$");
            if (calledOwner.equals(ROOT + "shared/tenancy/TenantExecutionScope") && Set.of("openTask", "captureTaskDeadline", "hasWorkerContext").contains(calledMethod)
                    && !taskExecutor && !(calledMethod.equals("hasWorkerContext") && symbols.name().equals(ROOT + "shared/tenancy/TenantExecutionScope"))) {
                errors.add("绕过可信异步快照入口：" + symbols.name());
            }
            if (calledOwner.equals(ROOT + "shared/redis/RedisKey") && calledMethod.equals("<init>")
                    && !Set.of(ROOT + "shared/redis/RedisKeyBuilder", ROOT + "shared/redis/PlatformRedisAccess").contains(symbols.name())) {
                errors.add("伪造受控Redis地址：" + symbols.name());
            }
            if (calledOwner.equals(ROOT + "shared/tenancy/TenantTaskExecutor") && calledMethod.equals("submitUnscoped") && !symbols.name().startsWith(ROOT + "shared/")) {
                errors.add("业务不能使用无身份任务入口：" + symbols.name());
            }
            if (calledOwner.equals(ROOT + "shared/tenancy/TenantTaskExecutor") && calledMethod.equals("<init>")
                    && !Set.of(ROOT + "shared/tenancy/TenantTaskExecutor", ROOT + "shared/tenancy/AsyncExecutionConfiguration").contains(symbols.name())) {
                errors.add("业务不能创建未登记执行器：" + symbols.name());
            }
            if (Set.of(ROOT + "shared/tenancy/TrustedTenantExecutor", ROOT + "shared/security/PlatformScopeGuard").contains(calledOwner)
                    && calledMethod.equals("<init>") && !symbols.name().equals(ROOT + "shared/tenancy/TenancyConfiguration")) {
                errors.add("业务不能创建未登记可信身份入口：" + symbols.name());
            }
            if ((calledOwner.equals("java/util/concurrent/CompletableFuture") && Set.of("runAsync", "supplyAsync").contains(calledMethod))
                    || (calledOwner.equals("java/lang/Thread") && Set.of("<init>", "start", "startVirtualThread").contains(calledMethod) && !taskExecutor)) {
                errors.add("绕过批准的租户异步入口：" + symbols.name());
            }
        }
        return errors;
    }
    static Set<String> classes(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return new HashSet<>(paths.filter(p -> p.toString().endsWith(".class"))
                    .map(p -> root.relativize(p).toString().replace('\\', '/').replaceFirst("\\.class$", "")).toList());
        }
    }
    @Test void actualProductionBytecodeObeysRules() throws Exception {
        var testClasses = classes(Path.of("target/test-classes"));
        var errors = new ArrayList<String>();
        try (var paths = Files.walk(Path.of("target/classes"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                errors.addAll(violations(symbols(Files.readAllBytes(path)), testClasses));
            }
        }
        assertEquals(List.of(), errors);
    }
    static byte[] fixture(String from, String to, boolean generic) {
        var writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, from, null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PRIVATE, "dependency", generic ? "Ljava/util/List;" : "L" + to + ";",
                generic ? "Ljava/util/List<L" + to + ";>;" : null, null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
    @Test void rejectsEveryRequiredNegativeFixtureIncludingGenericOnlyReferences() {
        for (var pair : List.of(
                List.of(ROOT + "shared/Bad", ROOT + "modules/alpha/domain/Rule"),
                List.of(ROOT + "modules/alpha/domain/Bad", ROOT + "modules/alpha/api/Controller"),
                List.of(ROOT + "modules/alpha/domain/Bad", "org/springframework/web/bind/annotation/RestController"),
                List.of(ROOT + "modules/alpha/domain/Bad", ROOT + "modules/alpha/infrastructure/Adapter"),
                List.of(ROOT + "modules/alpha/domain/Bad", "cn/binarywang/wx/miniapp/api/WxMaService"),
                List.of(ROOT + "modules/alpha/application/Bad", ROOT + "modules/beta/infrastructure/Repository"),
                List.of(ROOT + "identity/api/Bad", ROOT + "identity/infrastructure/Repository"),
                List.of(ROOT + "audit/application/Bad", ROOT + "identity/application/Directory"),
                List.of(ROOT + "shared/Bad", "com/pet/testing/Fixture"),
                List.of(ROOT + "shared/tenancy/TenantContext", "jakarta/servlet/http/HttpServletRequest"),
                List.of(ROOT + "shared/tenancy/TenantContext", "me/chanjar/weixin/common/api/WxService"),
                List.of(ROOT + "modules/alpha/domain/Bad", ROOT + "shared/tenancy/TenantContextHolder"),
                List.of(ROOT + "modules/alpha/application/Bad", "java/lang/InheritableThreadLocal"),
                List.of(ROOT + "shared/Bad", ROOT + "protocol/ProtocolFixtures"))) {
            for (boolean generic : List.of(false, true)) assertFalse(violations(symbols(fixture(pair.get(0), pair.get(1), generic)),
                    Set.of(ROOT + "protocol/ProtocolFixtures")).isEmpty(), pair.toString());
        }
        assertTrue(violations(symbols(fixture(ROOT + "identity/application/Good", ROOT + "platform/application/Directory", true)), Set.of()).isEmpty());
    }
    @Test void rejectsBytecodeStorageMutationAndUnauthorizedIdentityOpening() {
        var writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, ROOT + "modules/alpha/application/Bad", null, "java/lang/Object", null);
        var method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "bypass", "()V", null, null);
        method.visitMethodInsn(Opcodes.INVOKESTATIC, ROOT + "shared/tenancy/TenantContextHolder", "replace", "(L" + ROOT + "shared/tenancy/TenantExecutionScope;)V", false);
        method.visitMethodInsn(Opcodes.INVOKESTATIC, ROOT + "shared/tenancy/TenantExecutionScope", "openIdentity", "(L" + ROOT + "shared/security/CurrentPrincipal;)L" + ROOT + "shared/tenancy/TenantExecutionScope;", false);
        method.visitInsn(Opcodes.RETURN); method.visitMaxs(1, 0); method.visitEnd(); writer.visitEnd();
        var errors = violations(symbols(writer.toByteArray()), Set.of());
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("直接操作上下文底层存储")));
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("绕过可信身份入口")));
    }
    @Test void contextStorageAndIdentityOpeningHaveNoPublicMutators() {
        for (var method : com.pet.platform.shared.tenancy.TenantContextHolder.class.getDeclaredMethods()) {
            if (Set.of("frame", "replace").contains(method.getName())) assertFalse(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
        }
        for (var method : com.pet.platform.shared.tenancy.TenantExecutionScope.class.getDeclaredMethods()) {
            if (Set.of("openIdentity", "finishBoundary", "withVerifiedStore", "openTask", "captureTaskDeadline", "hasWorkerContext").contains(method.getName())) assertFalse(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
        }
    }
    @Test void detectsRawPersistenceInBusinessLayersAndRepositoryInheritance() {
        for (String layer : List.of("api", "application", "domain")) {
            for (String target : List.of("jakarta/persistence/EntityManager", "org/springframework/jdbc/core/JdbcTemplate", "java/sql/Connection", "javax/sql/DataSource")) {
                assertTrue(violations(symbols(fixture(ROOT + "modules/alpha/" + layer + "/Bad", target, true)), Set.of()).stream().anyMatch(e -> e.startsWith("业务层直连数据库")));
            }
        }
        assertTrue(violations(symbols(fixture(ROOT + "modules/alpha/infrastructure/BadRepository", "org/springframework/data/jpa/repository/JpaRepository", true)), Set.of()).stream().anyMatch(e -> e.startsWith("业务模块继承裸Repository")));
        assertTrue(violations(symbols(fixture(ROOT + "modules/alpha/application/Bad", ROOT + "shared/persistence/ScopedPersistence", true)), Set.of()).stream().anyMatch(e -> e.startsWith("业务层直连受控基础实现")));
        assertTrue(violations(symbols(fixture(ROOT + "modules/alpha/infrastructure/Good", ROOT + "shared/persistence/ScopedPersistence", true)), Set.of()).isEmpty());
    }
    @Test void infrastructurePackageDoesNotAuthorizeNativeQueriesMergeOrBulkBypass() {
        for (String methodName : List.of("merge", "find", "getReference", "createNativeQuery", "createQuery")) {
            var writer = new ClassWriter(0);
            writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, ROOT + "modules/alpha/infrastructure/Bad", null, "java/lang/Object", null);
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "bypass", "()V", null, null);
            method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "jakarta/persistence/EntityManager", methodName, "(Ljava/lang/String;)Ljakarta/persistence/Query;", true);
            method.visitInsn(Opcodes.RETURN); method.visitMaxs(2, 1); method.visitEnd(); writer.visitEnd();
            var errors = violations(symbols(writer.toByteArray()), Set.of());
            assertTrue(errors.stream().anyMatch(e -> e.startsWith("未登记底层持久化调用")));
            assertTrue(errors.stream().anyMatch(e -> e.startsWith("禁止无范围实体或SQL入口")));
        }
    }
    @Test void nativeMethodReferenceIsAlsoDetectedInInfrastructure() {
        var writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, ROOT + "modules/alpha/infrastructure/Bad", null, "java/lang/Object", null);
        var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "bypass", "()V", null, null);
        method.visitInvokeDynamicInsn("apply", "()Ljava/util/function/Function;",
                new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "()V", false),
                Type.getMethodType("(Ljava/lang/String;)Ljakarta/persistence/Query;"),
                new Handle(Opcodes.H_INVOKEINTERFACE, "jakarta/persistence/EntityManager", "createNativeQuery", "(Ljava/lang/String;)Ljakarta/persistence/Query;", true));
        method.visitInsn(Opcodes.RETURN); method.visitMaxs(2, 1); method.visitEnd(); writer.visitEnd();
        assertTrue(violations(symbols(writer.toByteArray()), Set.of()).stream().anyMatch(e -> e.startsWith("禁止无范围实体或SQL入口")));
    }
    @Test void controlledBaseDoesNotExposeDetachedSaveOrDatabaseHandles() {
        var type = com.pet.platform.shared.persistence.ScopedPersistence.class;
        for (var method : type.getMethods()) {
            assertFalse(Set.of("save", "merge", "deleteById", "getReferenceById", "findById", "findAll", "deleteAllInBatch").contains(method.getName()));
            assertFalse(Set.of(jakarta.persistence.EntityManager.class, jakarta.persistence.Query.class, javax.sql.DataSource.class).contains(method.getReturnType()));
        }
        for (var typeWithOwnership : List.of(com.pet.platform.shared.persistence.TenantScopedEntity.class, com.pet.platform.shared.persistence.StoreScopedEntity.class)) {
            for(var method : typeWithOwnership.getMethods()) assertFalse(Set.of("setTenantId", "setStoreId").contains(method.getName()));
        }
    }
    static List<String> artifactViolations(Set<String> entries, Set<String> tests, Set<String> testResources) {
        return entries.stream().filter(entry -> tests.contains(entry.replaceFirst("^BOOT-INF/classes/", "").replaceFirst("\\.class$", ""))
                || testResources.contains(entry.replaceFirst("^BOOT-INF/classes/", ""))
                || entry.startsWith("BOOT-INF/classes/com/pet/testing/")
                || entry.contains("testcontainers-")).sorted().toList();
    }
    @Test void rejectsTestClassesAndMigrationInArtifactFixture() {
        assertEquals(2, artifactViolations(Set.of("BOOT-INF/classes/com/pet/testing/Controller.class", "BOOT-INF/classes/test-migrations/V1__probe.sql"),
                Set.of("com/pet/testing/Controller"), Set.of("test-migrations/V1__probe.sql")).size());
    }
    @Test void rejectsRawRedisAndCacheAbstractionsEvenInInfrastructureAndGenericSignatures() {
        for (String target : List.of("org/springframework/data/redis/core/RedisTemplate", "org/springframework/data/redis/connection/RedisConnectionFactory",
                "io/lettuce/core/RedisClient", "org/springframework/cache/annotation/Cacheable", ROOT + "shared/redis/RedisValueStore")) {
            assertFalse(violations(symbols(fixture(ROOT + "modules/alpha/infrastructure/Bad", target, true)), Set.of()).isEmpty());
        }
        assertTrue(violations(symbols(fixture(ROOT + "modules/alpha/infrastructure/Good", ROOT + "shared/redis/TenantRedisAccess", true)), Set.of()).isEmpty());
    }
    @Test void rejectsUnapprovedExecutorsAndAsyncAnnotationsOnlyInProjectClasses() {
        for (String target : List.of("java/util/concurrent/ExecutorService", "java/util/concurrent/ForkJoinPool", "java/util/concurrent/Executors",
                "org/springframework/scheduling/annotation/Async", "org/springframework/core/task/TaskExecutor")) {
            assertTrue(violations(symbols(fixture(ROOT + "modules/alpha/application/Bad", target, true)), Set.of()).stream().anyMatch(e -> e.startsWith("绕过批准的租户异步入口")));
        }
        assertTrue(violations(symbols(fixture(ROOT + "modules/alpha/application/Good", ROOT + "shared/tenancy/TenantTaskExecutor", true)), Set.of()).isEmpty());
    }
    @Test void rejectsKeyForgeryUnscopedTasksAndInternalSnapshotOpeningViaMethodHandles() {
        for (var call : List.of(List.of(ROOT + "shared/redis/RedisKey", "<init>"),
                List.of(ROOT + "shared/tenancy/TenantExecutionScope", "openTask"),
                List.of(ROOT + "shared/tenancy/TenantTaskExecutor", "submitUnscoped"),
                List.of(ROOT + "shared/tenancy/TenantTaskExecutor", "<init>"),
                List.of(ROOT + "shared/tenancy/TrustedTenantExecutor", "<init>"),
                List.of(ROOT + "shared/security/PlatformScopeGuard", "<init>"),
                List.of("java/util/concurrent/CompletableFuture", "supplyAsync"))) {
            var writer = new ClassWriter(0);
            writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, ROOT + "modules/alpha/application/Bad", null, "java/lang/Object", null);
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "bypass", "()V", null, null);
            method.visitLdcInsn(new Handle(Opcodes.H_INVOKESTATIC, call.get(0), call.get(1), "()V", false));
            method.visitInsn(Opcodes.POP); method.visitInsn(Opcodes.RETURN); method.visitMaxs(1, 1); method.visitEnd(); writer.visitEnd();
            assertFalse(violations(symbols(writer.toByteArray()), Set.of()).isEmpty(),call.toString());
        }
        for (var constructor : com.pet.platform.shared.redis.RedisKey.class.getDeclaredConstructors()) assertFalse(java.lang.reflect.Modifier.isPublic(constructor.getModifiers()));
    }
}
