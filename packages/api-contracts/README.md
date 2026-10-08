# 纯类型接口契约

只导出生产OpenAPI生成的components/paths/operations，无运行时请求、浏览器或微信依赖。根命令 `pnpm contracts:generate` / `pnpm contracts:check` / `pnpm contracts:typecheck`。

OpenAPI和d.ts禁止手改；test/generated仅验证具体泛型，不在包出口。当前生产paths为空，公共模型由真实Java类型显式注册。Web以import type从@pet/api-contracts导入；小程序同步同份声明到固定生成目录并使用本地import type。

流程、标量、环境与限制见 [生成规范](../../docs/contracts/OPENAPI-GENERATION.md)，执行证据见 [P03-03](../../docs/testing/P03-03-VERIFICATION.md)。
