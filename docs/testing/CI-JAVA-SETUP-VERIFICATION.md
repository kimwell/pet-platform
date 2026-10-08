# CI Java 安装失败排查与修复

日期：2026-10-08（Asia/Shanghai）。本轮仅修复 CI Java 安装配置并验证当前工作区；不推进开发阶段。精确 JDK 和 Action 版本以 [VERSION-MATRIX](../development/VERSION-MATRIX.md#2026-10-08-ci-java-版本输入修复) 为唯一文档事实源。

## 原因与修复

用户截图显示 frontend 成功、backend 在 setup-java 步骤失败，Maven 尚未执行。错误是原 `java-version` 使用四段 Java 运行版本号，Action 直接用 SemVer 解析，无法接受该字符串。截图中的 v4 弃用提示和 Node `punycode` 提示是警告，根因是版本解析错误。

依据 [原 Action 的解析逻辑](https://github.com/actions/setup-java/blob/cf277c60eb25467037889841efdb72551f06f6c3/src/distributions/base-installer.ts)、[新 Action 的 Temurin 匹配逻辑](https://github.com/actions/setup-java/blob/b6effb05e454b25005698d916606bdc6ffcbf961/src/distributions/temurin/installer.ts) 和本次取得的 [Adoptium 官方响应](evidence/CI-JAVA-SETUP-2026-10-08/adoptium-release.json)，CI 输入改为该发布的 `version_data.semver`。它仍对应矩阵冻结的 JDK 发布及其 Linux x64 资产。没有改为宽泛的大版本范围，也没有降低 Java 基线。

[workflow](../../.github/workflows/check.yml) 同时将弃用的 setup-java 更新到矩阵中的固定官方 SHA，并在安装后断言真实 `java.runtime.version`。新 Action 自身使用 Node24；应用 Node 仍由既有 `.nvmrc` 和 setup-node 配置管理。Maven 缓存路径及其他构建命令保持原有配置。

## 验证与边界

完整工作目录、命令、退出码、摘要与日志见 [检查记录](evidence/CI-JAVA-SETUP-2026-10-08/checks-final.json)。本地工具均使用已有隔离目录，未修改全局配置。解析探针直接加载官方 Action 的实际分发包，只改写临时探针中的启动入口以读取真实解析器；安装器的网络读取由本次真实官方响应快照提供，没有执行 Linux JDK 安装。

| 检查 | 结果与等级 | 证据 |
| --- | --- | --- |
| 官方发布与固定 Action SHA | DOCUMENTED / RESOLVED；本轮首次 Git 查询成功，随后网络复查失败或超时保留 | [官方 refs](evidence/CI-JAVA-SETUP-2026-10-08/resolved-official-refs.json)、[源码校验摘要](evidence/CI-JAVA-SETUP-2026-10-08/official-sources.json) |
| Action 的实际解析及资产匹配 | 退出码 0；RUNTIME_VERIFIED，仅解析及元数据匹配。原输入在新版仍失败；修复输入匹配同一发布，其他构建号不匹配 | [探针](evidence/CI-JAVA-SETUP-2026-10-08/probe.cjs)、[日志](evidence/CI-JAVA-SETUP-2026-10-08/action-version-probe-r3.log) |
| workflow YAML 与关键参数 | 退出码 0；DOCUMENTED，触发器、SHA、版本输入与缓存路径一致 | [记录](evidence/CI-JAVA-SETUP-2026-10-08/workflow-structure.json) |
| workflow 中的运行版本断言 | 退出码 0；RUNTIME_VERIFIED，仅当前 macOS arm64 已安装的冻结 JDK | [日志](evidence/CI-JAVA-SETUP-2026-10-08/runtime-version-step-r2.log) |
| 仓库检查 | 退出码 0，单锁、固定依赖、入口与秘密特征检查通过 | [日志](evidence/CI-JAVA-SETUP-2026-10-08/repository-check-r2.log) |
| 后端 `./mvnw --batch-mode clean verify` | 退出码 0；COMPILED / RUNTIME_VERIFIED，90 项单元测试与 23 项集成测试，0 失败/错误/跳过；含真实 PostgreSQL/Testcontainers 技术集成测试 | [日志](evidence/CI-JAVA-SETUP-2026-10-08/backend-verify-r2.log)、[统计](evidence/CI-JAVA-SETUP-2026-10-08/test-summary.json) |
| `pnpm contracts:check` | 退出码 0；COMPILED / RUNTIME_VERIFIED，真实后端导出、生成产物一致性及类型检查通过 | [日志](evidence/CI-JAVA-SETUP-2026-10-08/contracts-check.log) |
| `git diff --check` | 退出码 0，补丁格式检查通过 | [记录](evidence/CI-JAVA-SETUP-2026-10-08/diff-check.json) |
| 修复后的 GitHub 托管 CI | NOT_EXECUTED / NOT_VERIFIED；没有提交、推送或触发远程执行 | 当前修复只存在本地工作区 |

首次本地 Maven 检查捕获到独立的异步租户 MVC 测试中文响应编码失败，退出码 1，见 [原始日志](evidence/CI-JAVA-SETUP-2026-10-08/backend-verify.log)。检查期间工作区出现并行租户开发改动，随后该测试已有 UTF-8 修正；本轮没有编辑这些后端源文件或测试文件。上表后端结果是保留并行改动后对当前工作区的重新验证，不能归因为 Java 安装配置修复了该测试。

最初解析探针的隔离加载方式缺少 Node 全局对象而失败，随后调整探针加载方式后通过；该失败与实际 CI 问题不同，原始记录保留。网络复查的 HTTP/2 错误和超时不写为 PASS。没有执行新的前端构建、微信设备验证或业务验收，也不将已有截图中的 frontend 成功推广到本次远程修复结果。

修复提交并推送后才能触发采用新配置的 CI；直接重跑截图中的旧提交仍使用原 workflow。
