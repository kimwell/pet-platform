import type { components } from '@pet/api-contracts';

export type ErrorKind = 'HTTP' | 'NETWORK' | 'CANCELLED' | 'TIMEOUT' | 'PROTOCOL';
export class ApiError extends Error {
  constructor(
    public readonly kind: ErrorKind,
    message: string,
    public readonly status?: number,
    public readonly code?: components['schemas']['ApiError']['code'],
    public readonly traceId?: string,
    public readonly fieldErrors?: components['schemas']['FieldErrorDetail'][],
    public readonly retryAfterSeconds?: number,
  ) { super(message); this.name = 'ApiError'; }
}

export const cancelled = () => new ApiError('CANCELLED', '请求已取消');
export function queryRetry(count: number, error: unknown): boolean {
  return error instanceof ApiError && count < 2
    && (error.kind === 'NETWORK' || error.kind === 'TIMEOUT'
      || (error.kind === 'HTTP' && [502, 503, 504].includes(error.status ?? 0)));
}
export function errorText(error: unknown): string {
  if (!(error instanceof ApiError)) return '页面暂时无法加载，请稍后重试';
  if (error.status === 429) return error.retryAfterSeconds
    ? `操作过于频繁，请在 ${error.retryAfterSeconds} 秒后重试`
    : '操作过于频繁，请稍后重试';
  if (error.code === 'CSRF_INVALID') return '安全凭据已失效，请重新操作';
  return error.message;
}
