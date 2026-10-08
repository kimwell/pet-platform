# 分页、排序与集合查询

冻结日期：2026-10-07，P01-02。本文是分页字段/非法参数/排序的唯一规范，P03-01 已实现参数/响应与白名单规则；P03-02 已实现真实持久化分页适配。

| 参数 | 约束 |
| --- | --- |
| page | 缺失1；十进制正整数，1～2147483647；不能空/负/小数/指数/重复 |
| pageSize | 缺失20；1～100，超限400 PAGINATION_INVALID，不静默截断 |
| sortBy | 缺失使用接口默认字段；必须字段白名单 |
| sortOrder | asc/desc；缺失使用默认方向；有sortOrder无sortBy为400 |

空参数不是缺失。默认单列排序，接口逐个登记白名单→JPA固定属性映射，绝不SQL拼接。标准列表默认 createdAt desc，加 id desc 唯一稳定条件；无createdAt的接口必须显式声明默认，例如code asc,id asc。nullable排序字段固定NULLS LAST。禁多值sortBy；游标分页只能作为特定业务扩展另行定义，不能混用本页码协议。

```json
{"success":true,"data":{"items":[],"page":1,"pageSize":20,"total":"0"},"traceId":"0123456789abcdef0123456789abcdef"}
```

items/page/pageSize/total全部必填，items为空为[]，total按 [大整数规则](DATA-TYPES.md) 为非负十进制字符串，JPA count也受相同范围。超最后页返回200、items=[]，page保持请求值、total为当前数量，不悄悄改page。JPA内部page-1；offset用安全long计算，无法支持的过深查询明确422 RESULT_TOO_LARGE，不溢出。

分页排序只保证同次查询排序确定，不保证并发插入/删除下跨页快照一致。列表重复ID去重不等同于完整性；需要稳定快照的大导出由专用任务/快照策略处理。

查询数组使用OpenAPI style=form、explode=true：`storeIds=<id1>&storeIds=<id2>`，不使用逗号/JSON/括号参数；客户端URLSearchParams逐个append。标量重复参数400；数组是否可空/最大长度由接口声明，ID数组默认去重后最大100，空集合意图不能编码成空字符串。

前端将total转换为Number前检查≤Number.MAX_SAFE_INTEGER；超限显示明确不支持当前页码列表，不能截断数值。字段总数对外仍保真，业务可采用游标扩展。

## P03-01 实际类型（2026-10-08）

`PageQuery.from(request.getParameterMap())` 直接验证原始 page/pageSize，不能先用绑定默认值吞掉空参数或重复参数。也可在已验证的调用链中构造 `new PageQuery(page, pageSize)`。缺失分别使用上表默认；仅接受十进制数字，可有前导零，按数值规范化，拒绝空白、正负号、小数、指数、重复、超界和超限。错误固定 400 PAGINATION_INVALID。`offset()` 用 long 精确乘法，最大合法参数偏移为 214748364600；当前只计算参数，不承诺数据库支持如此深的查询。未来适配器不能转成无界 int；无法承载时按冻结 422 RESULT_TOO_LARGE 明确拒绝。

接口静态声明排序配置，例如：

```java
var sorts = new SortWhitelist(
    Map.of("createdAt", "createdAt", "name", "displayName", "id", "id"),
    "createdAt", SortRule.Direction.DESC, "id");
var query = PageQuery.from(request.getParameterMap());
var rules = sorts.resolve(request.getParameterMap());
// 持久化入口使用 JpaPageAdapter.toPageable(query, sorts, request.getParameterMap())。
```

`sortBy=name&sortOrder=asc` 得到内部 `displayName ASC, id ASC`；`sortBy=name` 使用接口默认方向，得到 `displayName DESC, id DESC`。唯一补充字段方向跟随主排序，直接按 id 排序只返回一条规则。删除两个排序参数后恢复接口默认 `createdAt DESC, id DESC`。公开字段只支持白名单中的单个属性，公开→内部映射由代码拥有；配置默认字段/唯一字段必须登记。内部属性仅单层固定名，不接受任意实体路径。方向严格小写 asc/desc；空白/空串/大写、逗号/冒号/多分隔符、多列/重复参数、未知或仅内部字段均为 400 SORT_INVALID；只有 sortOrder 没有 sortBy 也拒绝。默认单列主排序加唯一条件不是客户端多列排序功能。

`PageResponse.of(items, query, actualCount)` 接受非负 long 实际计数并保真转为字符串；构造器也接受规范非负十进制字符串。items 非 null 且复制为不可变集合，不根据 items.size 推导总数；响应必有且只有 items/page/pageSize/total。空页传 `List.of()`，超末页保留 query 页码。分页响应自身的不合法构造属于编程错误，不能转换成客户端分页错误。

P03-01 当时没有 Spring Data/JPA 依赖或实际数据库查询；以下 P03-02 实现不改写其历史结论。P03-02 持久化任务需实现 page-1、受控排序属性、NULLS LAST、深查询能力限制和真实 count，再用真实 PostgreSQL 验证空页、超末页、排序、并发插删与精度。当前类型与 HTTP 夹具通过不表示 A03-02 的真实数据库场景通过，详情见 [验证报告](../testing/P03-01-VERIFICATION.md)。

## P03-02 持久化适配（2026-10-08）

`com.pet.platform.shared.persistence.JpaPageAdapter` 复用上述四个类型，不创建第二套模型。`toPageable(query, whitelist, rawParameters)` 内部调用白名单解析，只接收已登记的单层属性；`toSort` 使用受控 Spring Data Sort，每个 Order 显式 `nullsLast()`。冻结 JPA 实际支持 Jakarta Persistence 3.2 的空值次序，Criteria/Specification 已用真实 PostgreSQL 验证升序、降序及同值唯一排序；Hibernate 同时设置默认 NULLS LAST。原生 SQL 不由此配置自动保证，必须由 owner 显式实现。

外部 page-1 映射内部页号，offset 先按 PageQuery 的 long 计算。JPA `setFirstResult` 只支持 int，因此 offset>2147483647 明确 422 RESULT_TOO_LARGE，不截断；合法 page 范围维持 P03-01。pageSize=1 的最大外部页码仍可表示，深查询性能不是该边界的保证。

应用服务先将 Entity Page 映射 DTO Page，再 `JpaPageAdapter.fromPage(dtoPage)`；响应仍只有 items/page/pageSize/total。total 来自 Spring Data 数据库分页结果的 `getTotalElements()`，保真转字符串；例如技术大整数结果 `"total":"9007199254740993"`。PostgreSQL 测试已覆盖查询条件+分页、空结果、超末页保留请求页码、实际count、唯一排序不重复、NULLS LAST及后续删除导致的计数变化。没有证明跨页并发快照一致性，也没有新增生产列表接口。详细证据见 [P03-02](../testing/P03-02-VERIFICATION.md)。
