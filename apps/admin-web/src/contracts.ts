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
