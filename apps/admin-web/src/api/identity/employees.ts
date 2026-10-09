import type { components, operations } from '@pet/api-contracts';
import type { AuthService } from '../auth/AuthService';
import { ApiError } from '../ApiError';
import { safePagination } from '../../utils/identity/pagination';
import { isEmployeeId } from '../../utils/identity/detailSearch';

export type Employee = components['schemas']['EmployeeView'];
export type EmployeePage = components['schemas']['PageResponseEmployeeView'];
export async function getEmployee(auth: AuthService, employeeId: operations['getEmployee']['parameters']['path']['employeeId'], signal: AbortSignal): Promise<Employee> {
  if (!isEmployeeId(employeeId)) throw new ApiError('PROTOCOL', '员工地址中的 ID 格式无效');
  let traceId: string | undefined;
  const value = await auth.runtime.request<components['schemas']['SuccessEmployeeView']['data']>('STAFF', `/admin/identity/users/${employeeId}`, { signal, onTrace: trace => { traceId = trace; } });
  const instant = (input: unknown) => typeof input === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(input) && Number.isFinite(Date.parse(input)) && new Date(input).toISOString() === input;
  if (!value || value.id !== employeeId || typeof value.loginName !== 'string' || typeof value.displayName !== 'string'
    || !['ACTIVE', 'DISABLED'].includes(value.status) || !instant(value.createdAt) || !instant(value.updatedAt)) {
    throw new ApiError('PROTOCOL', '员工详情响应格式异常，请重试', undefined, undefined, traceId);
  }
  return value;
}
export async function listEmployees(auth: AuthService, query: NonNullable<operations['listEmployees']['parameters']['query']>, signal: AbortSignal): Promise<EmployeePage> {
  const value = await auth.runtime.request<EmployeePage | null>('STAFF', '/admin/identity/users', { query, signal });
  if (!value || !Array.isArray(value.items) || value.page !== query.page || value.pageSize !== query.pageSize || typeof value.total !== 'string'
    || value.items.some(item => !item || typeof item.id !== 'string' || typeof item.loginName !== 'string'
      || typeof item.displayName !== 'string' || !['ACTIVE', 'DISABLED'].includes(item.status)
      || !Number.isFinite(Date.parse(item.createdAt)) || !Number.isFinite(Date.parse(item.updatedAt)))) throw new ApiError('PROTOCOL', '员工列表响应格式异常，请重试');
  safePagination(value.total, value.page, value.pageSize);
  return value;
}
