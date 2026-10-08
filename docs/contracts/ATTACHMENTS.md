# 附件 HTTP 契约

冻结日期：2026-10-07，P01-02。本文拥有接口和DTO；存储/补偿/票据安全唯一在 [文件存储](../architecture/FILE-STORAGE.md)。所有端点均未实现。

prefix为 `/api/platform`、`/api/admin`、`/api/customer`，各自只接受对应身份；平台仅绑定控制面资源，不自动读租户业务。每个接口既查对应域代码权限（attachment:file:upload/read/delete）又调用业务访问策略；客户默认代码声明的本人能力不是员工授权。

| 方法与路径 | 输入 | 成功data / 状态 |
| --- | --- | --- |
| POST `<prefix>/attachments/temporary` | multipart，单file；不传tenant/磁盘路径 | Attachment，201 |
| GET `<prefix>/attachments/{id}` | ID | Attachment摘要，200 |
| PUT `<prefix>/attachments/{id}/binding` | resourceType/resourceId/resourceVersion/attachmentVersion | Attachment，200 |
| POST `<prefix>/attachments/{id}/access` | purpose=DOWNLOAD或MEDIA | AccessGrant，200 |
| GET `<prefix>/attachments/{id}/content` | 认证；可有受限Range | 授权字节200/206，成功不套JSON |
| DELETE `<prefix>/attachments/{id}` | 查询version | null，200；逻辑不可访问，不代表磁盘立即删除 |
| GET `/api/integrations/attachments/access/{ticket}` | 唯一票据，不携带其他域凭据兜底 | 授权字节，按用途；不可见/失效统一404 |

Binding端点分派到resourceType登记的**业务owner绑定用例**，在其事务中校验资源/范围/版本并调用附件应用接口；不能只凭请求ID改附件表。一个附件只绑定一个资源；重复同目标可幂等，改目标409 ATTACHMENT_ALREADY_BOUND。BOUND删除/解绑必须业务owner用例批准，不存在任意删除绑定文件的通用绕过。

Attachment字段全部required：id（UUID）、originalName（安全显示名）、mediaType（识别值）、size（有界字节整数）、status（TEMPORARY/BOUND/DELETING/DELETED）、temporaryExpiresAt（UTC，TEMPORARY必有，其他null）、version（整数字符串）、createdAt（UTC）。查询可含binding:null或 `{resourceType,resourceId}`，不得输出storageRoot、磁盘名/路径或摘要作为访问凭证。

AccessGrant：`{mode:"AUTHENTICATED_DOWNLOAD"|"TICKET",url:string,expiresAt:string|null,purpose:"DOWNLOAD"|"MEDIA"}`。认证模式url为同源当前域content相对路径、expiresAt=null；Ticket模式url为HTTPS短期专用地址、到期必填。Header可用的下载优先认证模式，小程序video使用MEDIA票据；不可依赖小程序image/video支持认证Header。

协议例（非上传功能）：

```json
{"success":true,"data":{"id":"123e4567-e89b-42d3-a456-426614174000","originalName":"示例.png","mediaType":"image/png","size":1024,"status":"TEMPORARY","temporaryExpiresAt":"2026-10-08T00:00:00.000Z","version":"0","createdAt":"2026-10-07T00:00:00.000Z","binding":null},"traceId":"0123456789abcdef0123456789abcdef"}
```

输入类型/大小非法422 ATTACHMENT_TYPE_INVALID/ATTACHMENT_SIZE_EXCEEDED；代理超过请求体上限413，进入应用时标准JSON，代理返回非JSON由客户端按HTTP识别。原文件名不是磁盘名；临时过期/不在范围统一404。文件不可用且已具业务访问权返回409 ATTACHMENT_UNAVAILABLE，绝不输出物理路径。

Cookie上传/绑定/票据/删除需CSRF；小程序wx.uploadFile按域Token，返回data字符串必须解析并检查statusCode/信封，不能只看success回调。中止上传不等同服务端事务回滚，临时意图由清理恢复。成功流以Content-Type/Content-Disposition识别，失败JSON不得保存为文件。
