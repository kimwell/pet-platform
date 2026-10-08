# 异步任务与结果协议

冻结日期：2026-10-07，P01-02。本文拥有用户任务字段和接口；领取/重试/消息见 [异步事件](../architecture/ASYNC-EVENTS.md)。本轮未提供任务或导入导出功能。

任务由模块专用接口提交，例如未来 identity 用户导出；**没有**接受任意表名/SQL/字段的通用创建接口。该模块声明 taskType、所需权限、输入schema、最大量、文件类型、分片/部分失败规则与幂等键。基础只管理状态和调度。

| 方法与路径 | 语义 |
| --- | --- |
| POST 模块专用任务入口 | 当前域权限/范围校验，持久化输入及执行描述；202，data为Task |
| GET `<prefix>/tasks/{taskId}` | 对应身份/tenant/提交者或明确任务管理权限，返回Task |
| POST `<prefix>/tasks/{taskId}/cancel` | 所有者或task:job:cancel；Cookie需CSRF；200返回Task |
| GET `<prefix>/tasks/{taskId}/result` | 当前权限重新验证，成功返回结果摘要/附件ID；不是裸文件 |
| POST `<prefix>/tasks/{taskId}/result-access` | 重新授权后返回附件AccessGrant，Cookie需CSRF |

prefix分别platform/admin/customer，认证域不能混用。任务范围外统一404；结果未完成409 TASK_NOT_READY，过期409 TASK_RESULT_EXPIRED，能力关闭503。下载通过附件协议，不提供公开磁盘URL。

Task必填：taskId、taskType、status、createdAt、startedAt:null或UTC、finishedAt:null或UTC、progress（0～100整数或null）、result:null或Result、error:null或TaskError、resultExpiresAt:null或UTC。不回显原始敏感输入、执行租约或提交Token。Result含summary中文、attachmentIds:[]、counters（非负整数字符串），具体字段由模块schema扩展；TaskError含稳定code、中文message、traceId，无内部堆栈/SQL。

状态：QUEUED→RUNNING→SUCCEEDED/FAILED；重试进入RETRY_WAIT再RUNNING；取消QUEUED直接CANCELLED，RUNNING转CANCEL_REQUESTED，由handler安全检查点进入CANCELLED。结果提交已成功则取消409 TASK_ALREADY_FINISHED，不能撤销已提交业务。终态不自动改RUNNING，手工重试创建新taskId并关联previousTaskId（仅接口明确支持时）。租约超时只能在幂等handler保证下恢复。

提交保存tenant/主体/权限版本/数据范围快照和输入摘要，执行前/每分片检查**当前权限与提交范围交集**，权限提升不能扩大旧任务、撤销则FAILED TASK_AUTHORIZATION_REVOKED。系统任务使用登记purpose，不能自称提交人。下载再次按当前权限判断。

默认单任务最多3次暂时错误重试（10秒、60秒、300秒），业务/权限/校验错误不重试；执行超时默认30分钟，模块可明确覆盖并留证。默认结果有效期24小时，终态任务元数据至少30天，文件到期由附件清理；失败详情下载如含业务数据也需相同授权。任务轮询建议2秒起、最大10秒退避，页面离开停止；轮询不会跨设备自动续期登录。

小批业务导入部分失败由模块明确每行/分片事务与行错误文件；基础任务状态SUCCEEDED只代表handler按声明策略完成，Result必须区分成功/失败计数，不能默认“所有行成功”。
