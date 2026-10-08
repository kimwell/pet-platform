# 本地基础设施

本目录只用于开发和技术验证，不是生产部署配置。镜像标签与多架构 digest 沿用 [版本矩阵](../../docs/development/VERSION-MATRIX.md) 及 P01 镜像证据；三个服务都使用 named volume。P03-02 后端已接入 PostgreSQL/JPA/Flyway；Redis 或 AMQP 客户端仍未接入。容器健康与应用 /actuator/health/readiness 分别验证；数据源进程输入见 [本地开发](../../docs/development/LOCAL-DEVELOPMENT.md)。

## 初始化与命令

复制 `.env.example` 为同目录 `.env`，在本机填写三项本地专用密码；不要在聊天、日志或 Git 中提交值，不复用生产值。环境文件采用一行一个变量；值包含 `$` 时用 Compose 支持的单引号避免插值。Redis 密码不要使用换行或控制字符。`.env` 被忽略，示例空密码会阻止启动。普通 Java 进程不自动读取此文件。

从项目根执行：

```sh
pnpm check:infra
pnpm dev:infra
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml ps
pnpm stop:infra
```

`check:infra` 使用 `config --quiet`，避免输出解析后的凭据；缺少文件明确报错，端口格式错误通过 Compose 自身解析定位到具体变量。`dev:infra` 等待三项真实健康检查；`stop:infra` 只停止本项目服务并核对各容器状态/退出码/OOM，异常返回非零，不删除卷。支持 `pnpm <命令> --env-file <临时文件> --project <本项目验证名称>`；省略时使用本目录.env与固定项目名。

直接Compose命令为 `docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml config --quiet`、`up -d --wait --wait-timeout 180`、`stop`；无需Node，但直接stop命令的0只表示操作完成，仍须inspect核对容器退出码。

默认只绑定 `127.0.0.1`：PostgreSQL 的 `POSTGRES_PORT`、Redis 的 `REDIS_PORT`、AMQP 的 `RABBITMQ_PORT`、管理 UI 的 `RABBITMQ_MANAGEMENT_PORT` 均可配置；默认端口见示例。RabbitMQ 管理页为 `http://127.0.0.1:<RABBITMQ_MANAGEMENT_PORT>`，使用本地填写的用户和密码。后端可选消息能力仍未安装，启动 RabbitMQ 不自动开启消息业务。

## 数据、认证与冲突

PostgreSQL 通过 pg_isready 检查服务就绪，Redis 使用认证后的 PING，RabbitMQ 通过 diagnostics 检查节点运行。PostgreSQL 初始化用户属于本地技术环境管理员，不代表后续应用运行/迁移受限角色已实现。Redis 保持 protected-mode 并要求密码，外部端口仅回环可访问。

默认项目名固定 `pet-platform-local`（验证时可用明确的 --project 覆盖），卷名由项目名与 `postgres-data`、`redis-data`、`rabbitmq-data` 组成。停止、重新启动、重建容器均保留卷。RabbitMQ固定hostname=rabbitmq，避免新容器ID改变节点数据目录；停止宽限显式30秒。P02旧验证卷内的旧节点目录保留未删除；本轮只有技术环境，不证明旧节点消息迁移，已有真实数据改变节点名须单独迁移验证。已有卷的初始化账号/密码不会因修改 `.env` 自动更新；按相应服务的账号管理操作处理，不能通过删除已有卷掩盖问题。

端口冲突时只调整本项目 `.env`，不要停止、删除其他容器。需要自行清空本项目数据时，先核对项目名和备份，再显式运行 `docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml down --volumes`；该命令会不可逆删除本项目 named volumes，本轮未执行。

P02 验证使用独立 `pet-platform-p02-validation` 项目、公开技术夹具凭据和另一组回环端口；这些不是正式业务账号或生产秘密。执行状态、结束时容器状态及命令见 [验证报告](../../docs/testing/P02-01-VERIFICATION.md)。不要把验证夹具直接当日常或生产环境配置。

P02-01 原始停止证据不改写：普通Compose stop返回0，但RabbitMQ主进程137、OOMKilled=false；节点shutdown后主进程0，容器内exec客户端137仍为FAIL，见 [历史报告](../../docs/testing/P02-01-VERIFICATION.md)。

P02-02复现时inspect显示原容器StopTimeout=1，Docker事件为SIGTERM→约1秒SIGKILL→主进程137，无OOM事件；不能仅凭137判断OOM。设置30秒宽限并重建本项目RabbitMQ（同一卷）后，正常停止主进程0，无SIGKILL。新stop脚本也实际拒绝了原137状态，不掩盖失败。详细事件、认证连接、hostname重建、结束状态见 [P02-02](../../docs/testing/P02-02-VERIFICATION.md)。未使用docker kill作为正常停止、未删除任何卷、未提高资源要求。
