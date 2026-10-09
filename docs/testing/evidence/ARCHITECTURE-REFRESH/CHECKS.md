# 架构改造检查命令与证据分级

工作根目录：`/Users/kimwell/work/pet-platform`。按实际执行顺序记录；早期 PASS 仅代表当时对应范围，最终结论以主验收报告的最终轮为准。

退出码非 0 全部保留为 FAIL；后续修复不改写原记录。COMPILED 的测试夹具不替代真实服务验收。视觉人工评分与真实布局指标分别标注。

原始记录：[commands.jsonl](commands.jsonl)。汇总：[主验收报告](../../ARCHITECTURE-REFRESH-VERIFICATION.md)。

| 轮次 | 工作目录 | 实际命令 | 退出码 | 证据等级 | 结果 / 日志 |
| --- | --- | --- | --- | --- | --- |
| web-typecheck-first | `/Users/kimwell/work/pet-platform` | `pnpm --filter @pet/admin-web typecheck` | 0 | COMPILED | PASS（编译或技术检查） [web-typecheck-first.log](web-typecheck-first.log) |
| web-typecheck-second | `/Users/kimwell/work/pet-platform` | `pnpm --filter @pet/admin-web typecheck` | 2 | NOT_VERIFIED | FAIL（保留原失败） [web-typecheck-second.log](web-typecheck-second.log) |
| web-check-first | `/Users/kimwell/work/pet-platform` | `pnpm check:web` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-check-first.log](web-check-first.log) |
| web-check-second | `/Users/kimwell/work/pet-platform` | `pnpm check:web` | 2 | NOT_VERIFIED | FAIL（保留原失败） [web-check-second.log](web-check-second.log) |
| web-check-third | `/Users/kimwell/work/pet-platform` | `pnpm check:web` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-check-third.log](web-check-third.log) |
| backend-compile-first | `/Users/kimwell/work/pet-platform/apps/backend` | `./mvnw --batch-mode -DskipTests compile` | 0 | COMPILED | PASS（编译或技术检查） [backend-compile-first.log](backend-compile-first.log) |
| backend-verify-first | `/Users/kimwell/work/pet-platform/apps/backend` | `./mvnw --batch-mode verify` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [backend-verify-first.log](backend-verify-first.log) |
| web-check-fourth | `/Users/kimwell/work/pet-platform` | `pnpm --filter @pet/admin-web check` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-check-fourth.log](web-check-fourth.log) |
| web-check-fifth | `/Users/kimwell/work/pet-platform` | `pnpm check:web` | 2 | NOT_VERIFIED | FAIL（保留原失败） [web-check-fifth.log](web-check-fifth.log) |
| web-check-sixth | `/Users/kimwell/work/pet-platform` | `pnpm check:web` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-check-sixth.log](web-check-sixth.log) |
| web-check-seventh | `/Users/kimwell/work/pet-platform` | `pnpm check:web` | 0 | COMPILED | PASS（编译或技术检查） [web-check-seventh.log](web-check-seventh.log) |
| mini-static-first | `/Users/kimwell/work/pet-platform` | `pnpm check:miniprogram` | 2 | NOT_VERIFIED | FAIL（保留原失败） [mini-static-first.log](mini-static-first.log) |
| mini-static-second | `/Users/kimwell/work/pet-platform` | `pnpm check:miniprogram` | 0 | COMPILED | PASS（编译或技术检查） [mini-static-second.log](mini-static-second.log) |
| browser-stage1-first | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-stage1.mjs` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [browser-stage1-first.log](browser-stage1-first.log) |
| mini-cli-auto-first | `/Users/kimwell/work/pet-platform` | `/Users/kimwell/Applications/WechatDevTools-2.02.2608080.app/Contents/MacOS/cli auto --project /Users/kimwell/work/pet-platform/apps/wechat-miniprogram --trust-project` | 0 | RESOLVED | PASS（打开自动化连接，不等于页面验证） [mini-cli-auto-first.log](mini-cli-auto-first.log) |
| web-protable-check | `/Users/kimwell/work/pet-platform` | `pnpm check:web` | 2 | NOT_VERIFIED | FAIL（保留原失败） [web-protable-check.log](web-protable-check.log) |
| mini-npm-build | `/Users/kimwell/work/pet-platform` | `/Users/kimwell/Applications/WechatDevTools-2.02.2608080.app/Contents/MacOS/cli build-npm --project /Users/kimwell/work/pet-platform/apps/wechat-miniprogram` | 0 | COMPILED | PASS（编译或技术检查） [mini-npm-build.log](mini-npm-build.log) |
| mini-automator-tool | `/Users/kimwell/work/pet-platform/.local-data/architecture/tools` | `pnpm add --save-exact miniprogram-automator@0.12.1 --ignore-workspace` | 0 | RESOLVED | PASS（依赖安装，不等于运行验收） [mini-automator-tool.log](mini-automator-tool.log) |
| web-protable-check-second | `/Users/kimwell/work/pet-platform` | `pnpm check:web` | 0 | COMPILED | PASS（编译或技术检查） [web-protable-check-second.log](web-protable-check-second.log) |
| web-build | `/Users/kimwell/work/pet-platform` | `pnpm build:web` | 0 | COMPILED | PASS（编译或技术检查） [web-build.log](web-build.log) |
| contracts-build | `/Users/kimwell/work/pet-platform` | `pnpm contracts:check` | 1 | NOT_VERIFIED | FAIL（保留原失败） [contracts-build.log](contracts-build.log) |
| mini-automator-isolated | `/Users/kimwell/work/pet-platform/.local-data/architecture/tools` | `pnpm add --save-exact miniprogram-automator@0.12.1 --ignore-workspace` | 0 | RESOLVED | PASS（依赖安装，不等于运行验收） [mini-automator-isolated.log](mini-automator-isolated.log) |
| mini-runtime-first | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs iphone12` | 1 | NOT_VERIFIED | FAIL（保留原失败） [mini-runtime-first.log](mini-runtime-first.log) |
| contracts-after-tag | `/Users/kimwell/work/pet-platform` | `pnpm contracts:check` | 0 | COMPILED | PASS（编译或技术检查） [contracts-after-tag.log](contracts-after-tag.log) |
| browser-supplement-first | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-supplement.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [browser-supplement-first.log](browser-supplement-first.log) |
| mini-runtime-second | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs iphone12` | 1 | NOT_VERIFIED | FAIL（保留原失败） [mini-runtime-second.log](mini-runtime-second.log) |
| repository-check | `/Users/kimwell/work/pet-platform` | `pnpm check:repo` | 0 | COMPILED | PASS（编译或技术检查） [repository-check.log](repository-check.log) |
| browser-manage-first | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-manage.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [browser-manage-first.log](browser-manage-first.log) |
| mini-runtime-third | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs iphone12` | 1 | NOT_VERIFIED | FAIL（保留原失败） [mini-runtime-third.log](mini-runtime-third.log) |
| browser-manage-second | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-manage.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [browser-manage-second.log](browser-manage-second.log) |
| mini-runtime-320 | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs iphone5` | 1 | NOT_VERIFIED | FAIL（保留原失败） [mini-runtime-320.log](mini-runtime-320.log) |
| browser-supplement-second | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-supplement.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [browser-supplement-second.log](browser-supplement-second.log) |
| browser-manage-third | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-manage.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [browser-manage-third.log](browser-manage-third.log) |
| mini-legacy-check | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-legacy.cjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [mini-legacy-check.log](mini-legacy-check.log) |
| mini-root-check | `/Users/kimwell/work/pet-platform` | `pnpm check:miniprogram` | 0 | COMPILED | PASS（编译或技术检查） [mini-root-check.log](mini-root-check.log) |
| backend-final-verify | `/Users/kimwell/work/pet-platform/apps/backend` | `./mvnw --batch-mode verify` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [backend-final-verify.log](backend-final-verify.log) |
| mini-root-npm | `/Users/kimwell/work/pet-platform` | `/Users/kimwell/Applications/WechatDevTools-2.02.2608080.app/Contents/MacOS/cli build-npm --project /Users/kimwell/work/pet-platform/apps/wechat-miniprogram` | 0 | COMPILED | PASS（编译或技术检查） [mini-root-npm.log](mini-root-npm.log) |
| web-layout-check | `/Users/kimwell/work/pet-platform` | `pnpm --filter @pet/admin-web typecheck` | 0 | COMPILED | PASS（编译或技术检查） [web-layout-check.log](web-layout-check.log) |
| mini-flat-320 | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs 320` | 1 | NOT_VERIFIED | FAIL（保留原失败） [mini-flat-320.log](mini-flat-320.log) |
| root-final-check | `/Users/kimwell/work/pet-platform` | `pnpm check` | 0 | COMPILED | PASS（编译或技术检查） [root-final-check.log](root-final-check.log) |
| web-manage-fifth | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-manage.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-manage-fifth.log](web-manage-fifth.log) |
| final-web-build | `/Users/kimwell/work/pet-platform` | `pnpm build:web` | 0 | COMPILED | PASS（编译或技术检查） [final-web-build.log](final-web-build.log) |
| web-supplement-third | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-supplement.mjs` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [web-supplement-third.log](web-supplement-third.log) |
| mini-flat-320-second | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs 320` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [mini-flat-320-second.log](mini-flat-320-second.log) |
| web-manage-sixth | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-manage.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-manage-sixth.log](web-manage-sixth.log) |
| mini-flat-375 | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs 375` | 1 | NOT_VERIFIED | FAIL（保留原失败） [mini-flat-375.log](mini-flat-375.log) |
| final-contracts-check | `/Users/kimwell/work/pet-platform` | `pnpm contracts:check` | 0 | COMPILED | PASS（编译或技术检查） [final-contracts-check.log](final-contracts-check.log) |
| web-manage-seventh | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-manage.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-manage-seventh.log](web-manage-seventh.log) |
| mini-flat-375-second | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs 375` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [mini-flat-375-second.log](mini-flat-375-second.log) |
| web-manage-eighth | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-manage.mjs` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [web-manage-eighth.log](web-manage-eighth.log) |
| final-contracts-tests | `/Users/kimwell/work/pet-platform` | `pnpm contracts:test` | 0 | COMPILED | PASS（编译或技术检查） [final-contracts-tests.log](final-contracts-tests.log) |
| final-backend-package | `/Users/kimwell/work/pet-platform/apps/backend` | `./mvnw --batch-mode -DskipTests package` | 0 | COMPILED | PASS（编译或技术检查） [final-backend-package.log](final-backend-package.log) |
| mini-flat-430 | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs 430` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [mini-flat-430.log](mini-flat-430.log) |
| final-runtime-restart | `/Users/kimwell/work/pet-platform` | `python3 docs/testing/evidence/ARCHITECTURE-REFRESH/replay/restart-final.py` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [final-runtime-restart.log](final-runtime-restart.log) |
| mini-final-430-metrics | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs 430` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [mini-final-430-metrics.log](mini-final-430-metrics.log) |
| web-final-runtime | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-final.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-final-runtime.log](web-final-runtime.log) |
| mini-navigation | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-navigation.cjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [mini-navigation.log](mini-navigation.log) |
| web-final-runtime-second | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-final.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-final-runtime-second.log](web-final-runtime-second.log) |
| web-final-runtime-third | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-final.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-final-runtime-third.log](web-final-runtime-third.log) |
| mini-unit | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-unit.cjs` | 0 | UNIT_FIXTURE | PASS（技术夹具） [mini-unit.log](mini-unit.log) |
| final-web-build-after-offline | `/Users/kimwell/work/pet-platform` | `pnpm build:web` | 0 | COMPILED | PASS（编译或技术检查） [final-web-build-after-offline.log](final-web-build-after-offline.log) |
| final-root-check-after-offline | `/Users/kimwell/work/pet-platform` | `pnpm check` | 0 | COMPILED | PASS（编译或技术检查） [final-root-check-after-offline.log](final-root-check-after-offline.log) |
| mini-navigation-real | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-navigation.cjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [mini-navigation-real.log](mini-navigation-real.log) |
| web-final-runtime-fourth | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-final.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-final-runtime-fourth.log](web-final-runtime-fourth.log) |
| web-final-runtime-fifth | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-final.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-final-runtime-fifth.log](web-final-runtime-fifth.log) |
| mini-navigation-real-second | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-navigation.cjs` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [mini-navigation-real-second.log](mini-navigation-real-second.log) |
| web-final-runtime-sixth | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-final.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-final-runtime-sixth.log](web-final-runtime-sixth.log) |
| mini-final-375-metrics | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs 375` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [mini-final-375-metrics.log](mini-final-375-metrics.log) |
| web-final-runtime-seventh | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-final.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-final-runtime-seventh.log](web-final-runtime-seventh.log) |
| web-final-runtime-eighth | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-final.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-final-runtime-eighth.log](web-final-runtime-eighth.log) |
| web-final-runtime-ninth | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-final.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-final-runtime-ninth.log](web-final-runtime-ninth.log) |
| mini-final-320-metrics | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs 320` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [mini-final-320-metrics.log](mini-final-320-metrics.log) |
| final-web-build-after-pagination | `/Users/kimwell/work/pet-platform` | `pnpm build:web` | 0 | COMPILED | PASS（编译或技术检查） [final-web-build-after-pagination.log](final-web-build-after-pagination.log) |
| final-root-check-after-pagination | `/Users/kimwell/work/pet-platform` | `pnpm check` | 0 | COMPILED | PASS（编译或技术检查） [final-root-check-after-pagination.log](final-root-check-after-pagination.log) |
| web-final-runtime-tenth | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-final.mjs` | 1 | NOT_VERIFIED | FAIL（保留原失败） [web-final-runtime-tenth.log](web-final-runtime-tenth.log) |
| visual-review | `/Users/kimwell/work/pet-platform` | `python3 docs/testing/evidence/ARCHITECTURE-REFRESH/replay/visual-review.py` | 0 | DOCUMENTED / RUNTIME_VERIFIED | PASS（人工评分 / 真实安全指标） [visual-review.log](visual-review.log) |
| web-final-runtime-eleventh | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/browser-final.mjs` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [web-final-runtime-eleventh.log](web-final-runtime-eleventh.log) |
| mini-final-320-visual | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs 320` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [mini-final-320-visual.log](mini-final-320-visual.log) |
| web-visual | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/web-visual.mjs` | 0 | RUNTIME_VERIFIED | PASS（真实公共框架布局） [web-visual.log](web-visual.log) |
| final-frozen-install | `/Users/kimwell/work/pet-platform` | `pnpm install --frozen-lockfile` | 0 | RESOLVED | PASS（依赖安装，不等于运行验收） [final-frozen-install.log](final-frozen-install.log) |
| mini-final-375-visual | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs 375` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [mini-final-375-visual.log](mini-final-375-visual.log) |
| final-whitespace-check | `/Users/kimwell/work/pet-platform` | `git diff --check` | 0 | DOCUMENTED | PASS（保全或文档检查） [final-whitespace-check.log](final-whitespace-check.log) |
| final-web-build-after-freeze | `/Users/kimwell/work/pet-platform` | `pnpm build:web` | 0 | COMPILED | PASS（编译或技术检查） [final-web-build-after-freeze.log](final-web-build-after-freeze.log) |
| final-root-check-after-freeze | `/Users/kimwell/work/pet-platform` | `pnpm check` | 0 | COMPILED | PASS（编译或技术检查） [final-root-check-after-freeze.log](final-root-check-after-freeze.log) |
| mini-final-430-visual | `/Users/kimwell/work/pet-platform` | `node docs/testing/evidence/ARCHITECTURE-REFRESH/replay/mini-capture.cjs 430` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [mini-final-430-visual.log](mini-final-430-visual.log) |
| final-visual-review | `/Users/kimwell/work/pet-platform` | `python3 docs/testing/evidence/ARCHITECTURE-REFRESH/replay/visual-review.py` | 0 | DOCUMENTED / RUNTIME_VERIFIED | PASS（人工评分 / 真实安全指标） [final-visual-review.log](final-visual-review.log) |
| final-source-audit | `/Users/kimwell/work/pet-platform` | `python3 docs/testing/evidence/ARCHITECTURE-REFRESH/replay/source-audit.py` | 0 | DOCUMENTED | PASS（保全或文档检查） [final-source-audit.log](final-source-audit.log) |
| final-runtime-cleanup | `/Users/kimwell/work/pet-platform` | `python3 docs/testing/evidence/ARCHITECTURE-REFRESH/replay/cleanup.py` | 1 | NOT_VERIFIED | FAIL（保留原失败） [final-runtime-cleanup.log](final-runtime-cleanup.log) |
| final-runtime-cleanup-second | `/Users/kimwell/work/pet-platform` | `python3 docs/testing/evidence/ARCHITECTURE-REFRESH/replay/cleanup.py` | 1 | NOT_VERIFIED | FAIL（保留原失败） [final-runtime-cleanup-second.log](final-runtime-cleanup-second.log) |
| final-runtime-cleanup-third | `/Users/kimwell/work/pet-platform` | `python3 docs/testing/evidence/ARCHITECTURE-REFRESH/replay/cleanup.py` | 1 | NOT_VERIFIED | FAIL（保留原失败） [final-runtime-cleanup-third.log](final-runtime-cleanup-third.log) |
| final-runtime-cleanup-fourth | `/Users/kimwell/work/pet-platform` | `python3 docs/testing/evidence/ARCHITECTURE-REFRESH/replay/cleanup.py` | 0 | RUNTIME_VERIFIED | PASS（对应范围） [final-runtime-cleanup-fourth.log](final-runtime-cleanup-fourth.log) |
| final-document-check | `/Users/kimwell/work/pet-platform` | `python3 docs/testing/evidence/ARCHITECTURE-REFRESH/replay/document-check.py` | 0 | DOCUMENTED | PASS（保全或文档检查） [final-document-check.log](final-document-check.log) |
| web-layout-completion-audit | `/Users/kimwell/work/pet-platform` | `python3 docs/testing/evidence/ARCHITECTURE-REFRESH/replay/web-completion-audit.py` | 0 | DOCUMENTED | PASS（保全或文档检查） [web-layout-completion-audit.log](web-layout-completion-audit.log) |
| final-document-check-after-layout-audit | `/Users/kimwell/work/pet-platform` | `python3 docs/testing/evidence/ARCHITECTURE-REFRESH/replay/document-check.py` | 0 | COMPILED | PASS（编译或技术检查） [final-document-check-after-layout-audit.log](final-document-check-after-layout-audit.log) |

## 其他原始证据

| 操作 | 工作目录 / 操作方式 | 退出码 | 等级 / 证据 |
| --- | --- | --- | --- |
| 临时正式后端与受限 PostgreSQL/Redis 初始化 | 根目录；各条实际命令在 JSON 中 | 逐条原值 | RUNTIME_VERIFIED；[runtime-setup.json](runtime-setup.json)，只用于隔离验收 |
| STAFF 验收租户正式初始化 | 根目录；正式初始化及 API 命令在 JSON 中 | 逐条原值 | RUNTIME_VERIFIED；[staff-runtime-bootstrap.json](staff-runtime-bootstrap.json)，不放宽门店权限政策 |
| 最终微信源码编译 | 冻结 GUI 普通编译，当前 iPhone 14 Pro Max；不是 shell 命令 | 不适用 | RUNTIME_VERIFIED；[AX](devtools-final-compile-ax.txt) / [截图](devtools-final-compile.png)，Errors=0；工具 Warnings=9 保留 |
| 依赖实际解析 | backend verify 和冻结 pnpm install | 对应日志原值 | RESOLVED / COMPILED；[版本与保全](preservation-final.json)，不改变版本事实源 |
| 未执行边界 | 本轮没有真机、生产跨源/TLS、多 OS、远程 CI、完整使用端正文或微信 code 交换 | NOT_EXECUTED | NOT_VERIFIED；原因及范围见主报告 |
| 本轮收尾 | 根目录；只停止归属已校验的本轮进程组与临时 tmpfs 容器 | 最终 0 | RUNTIME_VERIFIED；[cleanup.json](cleanup.json)，四个回环端口释放、既有三容器状态保全、凭据副本删除 |

清理早期失败来自 Docker 的 --rm 容器停止后自动移除，以及缺失对象错误文本大小写。原失败日志保留；按真实容器状态修正幂等检查后完成，未终止用户原有容器。

SDK 的 switchTab 故障注入属于工具故障边界。最终真实 API 导航已通过，失败回调/有栈返回由独立 UNIT_FIXTURE 验证；不将夹具描述为原生微信故障复现。
