import type { components, operations } from '@pet/api-contracts';
import type { AuthService } from '../../../auth/api/AuthService';
import { ApiError } from '../../../../shared/api/ApiError';
import { safePagination } from '../queries/pagination';

export type Employee = components['schemas']['EmployeeView'];
export type EmployeePage = components['schemas']['PageResponseEmployeeView'];
export async function listEmployees(auth: AuthService, query: NonNullable<operations['listEmployees']['parameters']['query']>, signal: AbortSignal): Promise<EmployeePage> {
  const value = await auth.runtime.request<EmployeePage | null>('STAFF', '/admin/identity/users', { query, signal });
  if (!value || !Array.isArray(value.items) || value.page !== query.page || value.pageSize !== query.pageSize || typeof value.total !== 'string'
    || value.items.some(item => !item || typeof item.id !== 'string' || typeof item.loginName !== 'string'
      || typeof item.displayName !== 'string' || !['ACTIVE', 'DISABLED'].includes(item.status)
      || !Number.isFinite(Date.parse(item.createdAt)) || !Number.isFinite(Date.parse(item.updatedAt)))) throw new ApiError('PROTOCOL', '员工列表响应格式异常，请重试');
  safePagination(value.total, value.page, value.pageSize);
  return value;
}
