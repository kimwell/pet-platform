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
