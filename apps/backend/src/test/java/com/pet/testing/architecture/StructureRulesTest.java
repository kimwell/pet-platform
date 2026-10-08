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
    record Symbols(String name, Set<String> dependencies) { }

    static Symbols symbols(byte[] bytes) {
        var reader = new ClassReader(bytes);
        var refs = new TreeSet<String>();
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
                    @Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) { return annotation(desc); }
                    @Override public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) { return annotation(desc); }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        refs.remove(reader.getClassName());
        return new Symbols(reader.getClassName(), refs);
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
            if (target.startsWith("com/pet/testing/") || testClasses.contains(target)) rule = "生产引用测试夹具";
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
                List.of(ROOT + "shared/Bad", ROOT + "protocol/ProtocolFixtures"))) {
            for (boolean generic : List.of(false, true)) assertFalse(violations(symbols(fixture(pair.get(0), pair.get(1), generic)),
                    Set.of(ROOT + "protocol/ProtocolFixtures")).isEmpty(), pair.toString());
        }
        assertTrue(violations(symbols(fixture(ROOT + "identity/application/Good", ROOT + "platform/application/Directory", true)), Set.of()).isEmpty());
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
}
