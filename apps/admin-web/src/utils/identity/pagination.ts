import { ApiError } from '../../api/ApiError';

export type SafePagination = { kind: 'safe'; total: number; lastPage: number; maximumPage: number } | { kind: 'limited'; reason: string };
export function safePagination(total: string, page: number, pageSize: number): SafePagination {
  if (!/^(0|[1-9][0-9]*)$/.test(total) || !Number.isInteger(page) || page < 1 || page > 2147483647
    || !Number.isInteger(pageSize) || pageSize < 1 || pageSize > 100) throw new ApiError('PROTOCOL', '分页响应格式异常，请重试');
  const exact = BigInt(total), size = BigInt(pageSize);
  if (exact > BigInt(Number.MAX_SAFE_INTEGER)) return { kind: 'limited', reason: '精确总数超出当前分页组件的安全整数范围，无法继续使用页码分页。' };
  const last = exact === 0n ? 1n : (exact + size - 1n) / size;
  // 同时约束公开 int32 页号和 JPA int offset；绝不将不安全 total 传给组件。
  const maximum = (2147483647n / size) + 1n;
  return { kind: 'safe', total: Number(exact), lastPage: Number(last), maximumPage: Number(maximum < 2147483647n ? maximum : 2147483647n) };
}
/** 每个用户导航最多自动纠正一次；并发变化再次越界留给显式刷新。 */
export function correctedPage(pagination: SafePagination, page: number, corrected: boolean): number | undefined {
  if (corrected || pagination.kind !== 'safe' || page <= pagination.lastPage) return;
  return Math.min(pagination.lastPage, pagination.maximumPage);
}
