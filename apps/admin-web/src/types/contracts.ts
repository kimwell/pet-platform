import type { components } from '@pet/api-contracts';

// 只导入类型，不产生请求、状态或业务页面代码。
export type ProtocolError = components['schemas']['Failure'];
export type FieldError = components['schemas']['FieldErrorDetail'];
export type ProtocolPage = components['schemas']['PageResponseFieldErrorDetail'];
export type NullSuccess = components['schemas']['SuccessVoid'];
type Assert<T extends true> = T;
export type WebContractChecks = [
  Assert<ProtocolPage['total'] extends string ? true : false>,
  Assert<NullSuccess['data'] extends null ? true : false>,
  Assert<ProtocolError['success'] extends false ? true : false>
];

// P05-03只消费生成的敏感操作类型，无请求或页面实现。
export type StaffPasswordChange = components['schemas']['ChangePasswordInput'];
export type StaffSecurityConfirmation = components['schemas']['ConfirmationInput'];
export type StaffPasswordReset = components['schemas']['ResetPasswordInput'];
export type StaffSessionRevocation = components['schemas']['RevokeSessionsInput'];
export type WebStaffSecurityChecks = [
  Assert<StaffPasswordReset['version'] extends string ? true : false>,
  Assert<components['schemas']['CurrentIdentity']['passwordChangeRequired'] extends boolean ? true : false>
];

// P05-04仅类型消费，平台/员工按principalType分支，不添加登录页面。
export type { AuthenticatedIdentity } from '@pet/api-contracts';
export type PlatformIdentity = components['schemas']['PlatformCurrentIdentity'];
export type WebPlatformChecks = [
  Assert<PlatformIdentity['tenantId'] extends null ? true : false>,
  Assert<components['schemas']['CurrentIdentity']['tenantId'] extends string ? true : false>
];
