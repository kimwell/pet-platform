import type { components } from './generated/api';

// 与Web同源的生成声明，微信运行时无需解析workspace包。
export type ProtocolError = components['schemas']['Failure'];
export type FieldError = components['schemas']['FieldErrorDetail'];
export type ProtocolPage = components['schemas']['PageResponseFieldErrorDetail'];
export type NullSuccess = components['schemas']['SuccessVoid'];
type Assert<T extends true> = T;
export type MiniContractChecks = [
  Assert<ProtocolPage['total'] extends string ? true : false>,
  Assert<NullSuccess['data'] extends null ? true : false>,
  Assert<ProtocolError['success'] extends false ? true : false>
];

// P05-03只消费生成的敏感操作类型，无请求或页面实现。
export type StaffPasswordChange = components['schemas']['ChangePasswordInput'];
export type StaffSecurityConfirmation = components['schemas']['ConfirmationInput'];
export type StaffPasswordReset = components['schemas']['ResetPasswordInput'];
export type StaffSessionRevocation = components['schemas']['RevokeSessionsInput'];
export type MiniStaffSecurityChecks = [
  Assert<StaffPasswordReset['version'] extends string ? true : false>,
  Assert<components['schemas']['CurrentIdentity']['passwordChangeRequired'] extends boolean ? true : false>
];

// 同源生成模型保持跨端协议一致，不签发平台小程序Token。
export type PlatformIdentity = components['schemas']['PlatformCurrentIdentity'];
export type AuthenticatedIdentity = components['schemas']['CurrentIdentity'] | PlatformIdentity;
export type MiniPlatformChecks = [
  Assert<PlatformIdentity['tenantId'] extends null ? true : false>,
  Assert<components['schemas']['CurrentIdentity']['tenantId'] extends string ? true : false>
];
