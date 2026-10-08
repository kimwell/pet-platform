# Redis 受控资源访问约定

实施日期：2026-10-08，P04-03。版本唯一见 [VERSION-MATRIX](../development/VERSION-MATRIX.md)，配置输入见 [本地开发](../development/LOCAL-DEVELOPMENT.md#p04-03-redis与进程内执行器)。当前仅单节点资源字符串的 GET、带 TTL 的 SET、单 Key DEL；没有正式业务缓存、会话 DAO、锁、脚本或消息。验收见 [P04-03](../testing/P04-03-VERIFICATION.md)。

## 命名空间与地址

冻结架构要求环境、身份域/租户与资源隔离，但此前没有冻结具体 Redis 字节格式。本轮将资源空间细化为以下格式，不改变未来独立 Sa-Token 身份空间：

```text
<prefix>:<environment>:v1:tenant:<可信UUID>:raw:<module>:b<base64url业务标识>
<prefix>:<environment>:v1:tenant:<可信UUID>:store:<已验证UUID>:raw:<module>:b<base64url业务标识>
<prefix>:<environment>:v1:platform:raw:<module>:b<base64url业务标识>
```

prefix 与 module 都匹配 `[a-z][a-z0-9-]{0,31}`；environment 只能 local/test/prod。tenant 只从当前 BUSINESS 取得，没有接收任意 tenantId 的业务 API。StoreScopeGuard 先验证内部归属事实、当前操作范围与身份门店上限；当前已绑定门店时不能另选不同门店。使用地址时再次核对事实，缺正式事实源默认 503，不能因缓存命中跳过。

业务标识是非敏感资源标识，1～128 UTF-8 字节、合法 Unicode、不含控制字符。不进行 trim、字符替换或 Unicode 归一化：逐字节 Base64 URL 编码且去 padding，所以冒号、通配符、花括号、组合字符不会变成结构分隔符或发生替换碰撞。Base64 **不是加密**，禁止输入 Token、密码、手机号、微信 code、完整请求、下载票据等秘密/个人信息；模块应优先使用公开资源 UUID 或静态非敏感技术 ID。完整地址上限 512 ASCII 字节，不暴露原始地址 getter，toString 不输出内容。

RedisKey 只有包内构造器，携带签发 builder 与当前不可变执行范围。TenantRedisAccess 每次读写删检查签发来源及完整 TenantContext 一致，包括租户、主体域/ID、当前 permission/dataScope、门店上限、当前门店等；在 A 建立的对象不能到 B、其他主体或收窄范围复用。普通业务自行 new 另一个 builder 也不能改变配置前缀/环境来访问现有端口。地址对象只在当前边界内使用，跨请求/异步重新经注入 builder 创建；不持久化或作为授权凭据序列化。

PlatformRedisAccess 独立校验 `platform:redis:operate`，必须是可信 PLATFORM Provider 且线程无租户范围，创建及操作均复核；该权限仅是当前基础设施控制面能力声明，没有真实账号、权限注册表或管理页面。平台地址不被租户端口接纳，租户地址不被平台端口接纳；null tenant 从不退化到平台。P05 提供真实平台授权后才有实际控制面使用者。

无 hash tag，不将整个租户集中到一个槽。当前配置拒绝 Cluster/Sentinel/URL 覆盖，只验证单节点单 Key 操作；没有跨槽、多 Key 原子能力或 Redis Cluster 支持承诺。

## 数据权限与两种缓存语义

| 内容 | 当前规则 |
| --- | --- |
| 租户共享的原始资源 | 允许技术存储；命中后 owner 必须重新按当前操作和资源类型执行 TENANT/STORES/SELF 数据范围检查，确认门店/主体域+owner ID 后才能返回或使用 |
| 已按权限过滤的列表、统计、allowedActions、授权结果 | 当前禁止缓存；没有范围结果 API、范围指纹或真实权限版本失效机制。禁止用 tenant Key 保存管理员列表后给 STORES/SELF 复用，也禁止 Spring Cache/@Cacheable 替代 |

范围绑定保护的是 **地址对象使用**；不同合法身份重新创建同一 raw 逻辑地址仍可取得相同原始 bytes，这不是授权成功。端口无法判断字符串是不是列表，调用方必须遵守内容分类；静态检查不证明数据语义。测试明确验证共享原始 A2 资源被 A1 的当前 Store Guard 拒绝消费，以及宽范围 Key 对象不能用于窄范围。没有宣称静态规则能辨认所有动态误用。

现有 authorizationVersion 是内部事实模型字段，不是本轮新增的权威版本源；没有伪造权限版本解决失效。未来需要范围结果缓存时，先设计 permissionCode、主体域+ID、规范化门店/范围、权威版本与撤销机制，再专项验收；仅范围摘要也不能解决撤销或过期。

## 合法用法与最小接口

模块 infrastructure 的固定资源适配器注入 RedisKeyBuilder/TenantRedisAccess；application 先选择本用例范围并调用固定模块方法，HTTP 不接收 Key/权限代码/tenant。以下为未来适配器调用方式，不是生产业务实现：

```java
var key = keys.tenantResource("identity", resourceId.toString());
var raw = redis.read(key);
if (raw.isPresent()) {
    // 在当前范围重新验证资源的归属和操作权限，再映射/返回。
    resourcePolicy.requireCurrentAccess(resourceId, raw.get());
}
redis.write(key, resourceText, Duration.ofMinutes(5));
redis.delete(key);
```

门店资源用 `keys.storeResource(storeId, module, businessKey)`，仍需资源自己的 SELF/业务规则；Guard 只证明整店能力。模块封装必须承载明确资源规则，不能将受控接口直接透传给客户端。业务层不能注入裸 RedisTemplate、RedisConnectionFactory、Lettuce、其他客户端或 RedisValueStore；配置与私有驱动是唯一低层白名单。

读取返回 Optional：缺失为 empty，空字符串为 `Optional.of("")`，不支持 null 值/空对象占位。写入必须提供整毫秒 TTL，1ms～24h；没有永久 SET。值为严格 UTF-8 文本，最大 64KiB（空字符串允许）；线格式为 ASCII `v1:` 加文本 bytes，读取检查版本、长度及 UTF-8。模块如需 JSON，使用固定 schema、明确 DTO 和版本，不传任意类名或 default typing。端口不做 Java 原生反序列化，私有 template 明确 string Key/byte[] value，禁默认 serializer、禁 Redis 事务排队。

Redis 连接、超时、错误类型、损坏值/未知线版本都返回既有 `DEPENDENCY_UNAVAILABLE` 基础设施错误，不能静默视为 miss；删除返回真实存在与否。未提供查询降级、自动重试、缓存填充或 DB/Redis 原子事务。以后是否允许特定只读查询降级由业务明确决定；会话、安全版本、CSRF 等安全业务另行实现，不用本端口假定已完成。

不开放任意 Lua、KEYS、SCAN/删前缀、FLUSHDB、批量删除或通用 RedisUtil。Redis 权限/网络/ACL 仍需部署验证，应用命名空间不能隔离持有服务器凭据的恶意直接客户端。非 Java 原生格式的依据见 [冻结 Spring Data Redis 官方说明](https://docs.spring.io/spring-data/redis/reference/4.0/redis/template.html)。

## P05-02 认证专属存储例外（2026-10-08）

认证会话只由identity/infrastructure/session/AuthenticationRedis适配冻结官方DAO，不经过普通租户资源Redis API；业务原TTL/大小/类型限制未扩大。认证空间pet:<env>:<domain>:<domain>:，辅助空间pet:<env>:auth:staff:<kind>:<安全摘要>；独立应用pet与身份域隔离。认证TTL最多7天、值最多128KiB，不允许永久存储/SCAN；固定SessionWire及String数据/有限终端线模型、无多态反序列化或Java原生序列化。原始Token只存在必要服务端索引/会话值，禁止日志。频控/预会话/账号锁方法固定，业务不得直接依赖该实现；结构门禁仅批准认证适配器和装配配置使用原始Redis类型。真实失败均503，不能降为未登录或调用内存备份。详见[认证](../architecture/AUTHENTICATION.md#p05-02-staff-实施2026-10-08)。
