# 数据类型、时间与金额

冻结日期：2026-10-07，P01-02。本文拥有公共标量、空值和时间/金额语义；OpenAPI映射见 [类型生成](OPENAPI-GENERATION.md)。

| 类型 | JSON / 后端 / 校验 |
| --- | --- |
| ID | string；服务端生成小写标准UUID v4、DB uuid；非空UUID，不使用手机号/数据库序号 |
| 大整数 | 非负十进制string，正则 `^(0|[1-9][0-9]*)$`；total/version及无界Long计数，不经JS Number传递 |
| 分页/大小/进度 | 明确受界整数number：page/pageSize、文件字节≤当前上传上限、progress0～100；schema限制，不允许无界Long裸number |
| 金额 | 规范十进制string，BigDecimal/NUMERIC；不接受number、指数、千分位、+号或隐式舍入 |
| 时间点 | 输入RFC3339必须Z或明确offset；输出UTC `YYYY-MM-DDTHH:mm:ss.SSSZ`；DB timestamptz、Java Instant，毫秒精度 |
| 纯日期 | string `YYYY-MM-DD`，真实日历日期、Java LocalDate；不转UTC |
| 枚举 | 稳定大写代码string，TS字面量union；中文标签另映射，未知输入拒绝 |
| 布尔 | JSON true/false；不接受0/1或字符串 |
| 集合/空值 | 集合永远[]而非null；DTO必填可空字段显式null，缺失只按schema允许；PATCH独有缺失语义见API |

UUID生成使用JDK UUID，无新增ID框架。sessionId/eventId/taskId亦为非凭据UUID，票据/Token为认证文档的opaque秘密，不混为普通ID。普通响应不因字段值为空就隐式省略required字段。

## 业务时区与自然日

Tenant配置IANA zoneId，默认 Asia/Shanghai；Store可覆写其业务zoneId，具体接口明确按Tenant或所选Store解释。用户设备时区只影响显示，不改业务日期。平台时间报告以显式配置时区，默认Asia/Shanghai。多门店跨时区自然日查询默认逐Store解释后取各自区间；若接口无法支持则拒绝组合，不假设全世界固定+08。

时间点范围 `[startAt,endAt)`；自然日筛选使用startDate（含）和endDateExclusive（不含），后端按业务zone的当地午夜转Instant。UI选择“截至某日”时转换为下一自然日，不加固定24小时；夏令时可能23/25小时。开始≥结束422 DATE_RANGE_INVALID。例：Asia/Shanghai筛选2026-10-07，UTC为 `[2026-10-06T16:00:00.000Z,2026-10-07T16:00:00.000Z)`。

## 金额

金额与currency（ISO4217稳定代码）成组，不裸金额猜币种。模板默认支持CNY，minor unit=2，输入/响应规范如 `"12.30"`；数据库NUMERIC(19,2)，最多17整数位，负值仅具体接口明确允许（如调整），禁止负零。当前未定义其他币种业务；扩展时先登记币种精度/范围，不能将2位当所有币种通则。

超精度422 AMOUNT_INVALID，不能静默round。计算过程中BigDecimal保留足够精度，具体模块定义最终舍入点；没有更特定规则时最终币种精度采用HALF_UP。总额/单项税费等业务舍入必须由模块明确，通用基础不能擅自规定。Web/小程序金额输入与传输使用字符串，展示使用纯字符串格式工具或经验证的精度实现，不先转浮点再发后端。

枚举新增值须考虑旧端显示未知代码的中文兜底；删除/重命名或字段缺失/nullable变化按协议兼容性检查，不手改生成类型掩盖。

## P03-03 实际编解码（2026-10-08）

实现位于 `shared.serialization`，应用使用 Jackson 3；[验证报告](../testing/P03-03-VERIFICATION.md) 记录真实 HTTP、OpenAPI 及 PostgreSQL 往返。没有改变 Entity/JPA 映射，也没有全局 Long/BigDecimal 字符串化。

| 字段语义 | 当前实际规则与错误 |
| --- | --- |
| UUID ID | UUID 全局 HTTP 编解码：小写标准 v4、RFC variant；非法/空串/非字符串400。无整数ID转换 |
| total | 继续由 PageResponse 的 String 保存规范非负十进制；Long 工厂保真，不经 Number。已有协议不变 |
| 其他Long计数/version | `@DecimalCounter` 仅作用于被标注的Long字段；非负规范字符串，最大Long值；非字符串/格式/越界400。未标注Long保持原number，owner只能用于明确受界整数 |
| CNY金额 | BigDecimal 字段使用 `@CnyAmount(input=true)`（请求）或 `@CnyAmount`（响应），输入规范非负十进制、最多17整数位、可省略小数/接受1～2位；输出固定两位，例如12→"12.00"、"12.3"→"12.30"。超过2位（含额外尾零）或范围422 AMOUNT_INVALID；number、指数、千分位、+/-、负零、前导零、空串400 BAD_REQUEST。负金额尚无具体允许接口，当前基础注解不接受；扩展负值须由owner单独定义 |
| 非金额BigDecimal | 不应用金额注解；原数值协议保持，真实HTTP技术比例字段仍为number |
| Instant | 全局HTTP时间点输入必须Z或明确offset，允许0～3位小数；无时区/无效日历/>3位小数/非字符串400。输出UTC且固定3位毫秒；超毫秒精度输出属于内部错误，禁止默默截断 |
| LocalDate | 全局HTTP输入严格四位年YYYY-MM-DD、真实日期；非法/空串/日期时间400。输出不转换时区；四位年份范围之外的输出拒绝 |
| 枚举/Boolean/受界整数 | 枚举稳定代码，未知/数字400；Boolean只接受true/false；受界整数number，不接受小数、字符串或Java整数溢出。合法类型但违反Bean Validation边界为422 |
| null/缺失/空串/集合 | 不做全局空值省略或null→[]转换。必填可空用 `@JsonProperty(required=true)` 与schema null联合：显式null保留，缺失400；`@NotNull` 的null/缺失422。可省略字段明确声明，输出NON_NULL时null/缺失省略，空串保留；是否允许输入null由输入schema明确。列表DTO拒绝null，空列表[]；PageResponse构造器已保证这一点 |

当前输入金额省略尾零、时间输入小数最多3位、四位年份及未定义负金额的最小处理已在本节明确；不引入币种业务、隐式舍入或负值业务。该方案补齐原冻结协议细节，原分页total/信封不变。输出时间示例 `"2026-10-08T02:20:30.120Z"`；带+08:00输入转换到同一Instant，纯日期 `"2026-10-08"` 保持不变。

JSON字段类型/语法400，合法类型的约束错误422；只识别金额精度/范围的固定异常为AMOUNT_INVALID，不把普通内部异常当客户端输入错误。不回显原值/cause。JDK DST技术反例和PG标量往返通过；租户业务自然日查询、PATCH presence应用、并发version控制没有在本轮实现。
