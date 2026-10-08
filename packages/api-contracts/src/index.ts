// 公共模型来自生产应用OpenAPI；测试契约不属于包的导出API。
export type { components, paths, operations } from './generated/api';

import type { components as Generated } from './generated/api';
// 三种实际生成模型组成判别身份；STAFF/CUSTOMER保留必填tenantId。
export type AuthenticatedIdentity = Generated['schemas']['CurrentIdentity'] | Generated['schemas']['PlatformCurrentIdentity'] | Generated['schemas']['CustomerCurrentIdentity'];
