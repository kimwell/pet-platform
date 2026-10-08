import { ApiError, errorText } from '../../shared/api/ApiError';

export type PasswordForm = { currentPassword: string; newPassword: string; confirmation: string };
export function passwordError(value: string | undefined): string | undefined {
  if (!value) return '请输入密码';
  const characters = [...value];
  if (value.length > 256 || characters.length < 12 || characters.length > 128) return '密码须为12至128个有效字符';
  if (characters.some(char => char.length === 1 && /[\uD800-\uDFFF]/.test(char))) return '密码包含非法字符编码';
}
export function confirmationError(password: string | undefined, confirmation: string | undefined): string | undefined {
  return confirmation === password ? undefined : '两次输入的新密码不一致';
}
export const securityFields = ['currentPassword', 'newPassword'] as const;
export function securityFieldErrors(error: unknown) {
  if (!(error instanceof ApiError) || error.status !== 422) return [];
  // 精确白名单，禁止动态路径解析/对象属性赋值。
  return (error.fieldErrors ?? []).filter(field => securityFields.some(name => name === field.field))
    .map(field => ({ name: field.field as 'currentPassword' | 'newPassword', errors: [field.message] }));
}
export function securityUnmappedErrors(error: unknown): string[] {
  return error instanceof ApiError ? (error.fieldErrors ?? []).filter(field => !securityFields.some(name => name === field.field)).map(field => field.message) : [];
}
export function securityErrorText(error: unknown): string {
  if (!(error instanceof ApiError) || ['NETWORK', 'TIMEOUT', 'PROTOCOL', 'CANCELLED'].includes(error.kind) || (error.status ?? 0) >= 500) {
    return '请求结果未确认，请重新确认当前身份或重新登录；请勿直接重复提交';
  }
  if (error.code === 'SECURITY_CONFIRMATION_FAILED') return '当前密码不正确，请重新输入';
  if (error.code === 'PASSWORD_CHANGE_REQUIRED') return '当前需要修改密码后才能继续';
  if (error.status === 409) return '当前状态已变化，请重新确认身份后再操作';
  return errorText(error);
}
