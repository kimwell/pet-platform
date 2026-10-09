import type { operations } from '@pet/api-contracts';
import type { HistoryState } from '@tanstack/react-router';

export type EmployeeQuery = NonNullable<operations['listEmployees']['parameters']['query']>;
/** URL 只保存已提交条件；门店选择事实尚未提供，本页不开放 storeId。 */
export type EmployeeSearch = Required<Pick<EmployeeQuery, 'page' | 'pageSize' | 'sortBy' | 'sortOrder'>> & Pick<EmployeeQuery, 'keyword' | 'status'>;
export const employeePath = '/admin/identity/users';
export const defaultSearch: EmployeeSearch = { page: 1, pageSize: 20, sortBy: 'createdAt', sortOrder: 'desc' };
export const sortFields = ['id', 'loginName', 'displayName', 'status', 'createdAt', 'updatedAt'] as const satisfies readonly EmployeeSearch['sortBy'][];
const fields = new Set(['keyword', 'status', 'page', 'pageSize', 'sortBy', 'sortOrder']);

/** 保留字符串和重复参数，避免 JSON search 解析改变字面量关键词。 */
export function parseWebSearch(raw: string): Record<string, unknown> {
  const result: Record<string, unknown> = Object.create(null);
  for (const [key, value] of new URLSearchParams(raw)) {
    if (Object.hasOwn(result, key)) result[key] = [...(Array.isArray(result[key]) ? result[key] : [result[key]]), value];
    else result[key] = value;
  }
  return result;
}
export function stringifyWebSearch(search: Record<string, unknown>): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(search)) {
    if (value !== undefined && value !== null) for (const item of Array.isArray(value) ? value : [value]) params.append(key, String(item));
  }
  return params.size ? `?${params}` : '';
}
// Java isBlank 使用 Character.isWhitespace：不把 NBSP 等不换行空格误判为可 trim 空白。
export function keywordError(value: string): string | undefined {
  if (!value || /^[\u0009-\u000d\u001c-\u0020\u1680\u2000-\u2006\u2008-\u200a\u2028\u2029\u205f\u3000]+$/u.test(value)) return '请输入非空白关键词';
  if ([...value].length > 100 || /[\uD800-\uDBFF](?![\uDC00-\uDFFF])|(?<![\uD800-\uDBFF])[\uDC00-\uDFFF]/u.test(value)) return '关键词最多 100 个 Unicode 字符';
}
function integer(value: unknown, fallback: number, maximum: number): number | undefined {
  if (value === undefined) return fallback;
  if ((typeof value !== 'string' && typeof value !== 'number') || !/^[0-9]+$/.test(String(value))) return;
  const exact = BigInt(value);
  if (exact < 1n || exact > BigInt(maximum)) return;
  return Number(exact);
}
export function normalizeSearch(input: Record<string, unknown>): { query: EmployeeSearch; notice?: string } {
  const page = integer(input.page, 1, 2147483647), pageSize = integer(input.pageSize, 20, 100);
  const keyword = input.keyword, status = input.status, sortBy = input.sortBy ?? 'createdAt', sortOrder = input.sortOrder ?? 'desc';
  const invalid = [...fields].some(key => Array.isArray(input[key])) || page === undefined || pageSize === undefined
    || (keyword !== undefined && (typeof keyword !== 'string' || Boolean(keywordError(keyword))))
    || (status !== undefined && status !== 'ACTIVE' && status !== 'DISABLED')
    || !sortFields.includes(sortBy as EmployeeSearch['sortBy']) || !['asc', 'desc'].includes(String(sortOrder))
    || (input.sortOrder !== undefined && input.sortBy === undefined);
  if (invalid) return { query: { ...defaultSearch }, notice: '查询地址包含无效或重复参数，已恢复默认条件。' };
  return { query: { page, pageSize, sortBy, sortOrder, ...(keyword !== undefined ? { keyword } : {}), ...(status !== undefined ? { status } : {}) } as EmployeeSearch,
    notice: Object.keys(input).some(key => !fields.has(key)) ? '查询地址包含本页不支持的参数，已移除。' : undefined };
}
export function employeeSearchValues(query: EmployeeSearch) {
  return { keyword: query.keyword, status: query.status,
    page: query.page === 1 ? undefined : query.page, pageSize: query.pageSize === 20 ? undefined : query.pageSize,
    sortBy: query.sortBy === 'createdAt' && query.sortOrder === 'desc' ? undefined : query.sortBy,
    sortOrder: query.sortBy === 'createdAt' && query.sortOrder === 'desc' ? undefined : query.sortOrder };
}
export function employeeHref(query: EmployeeSearch): string {
  return employeePath + stringifyWebSearch(employeeSearchValues(query));
}
/** Router 在初始 beforeLoad 之前规范化 search，先保留校验提示到当前历史条目。 */
export function initialEmployeeHistoryState(href: string, state: HistoryState): HistoryState {
  const url = new URL(href, 'https://local.invalid');
  if (url.pathname !== employeePath) return state;
  const { notice } = normalizeSearch(parseWebSearch(url.search));
  return notice ? { ...state, employeeUrlNotice: notice } : state;
}
export function submitFilters(query: EmployeeSearch, draft: Pick<EmployeeSearch, 'keyword' | 'status'>): EmployeeSearch {
  // 空输入表示不筛选；非空输入保持后端字面量语义，不 trim 或改大小写。
  return { ...query, page: 1, keyword: draft.keyword === '' ? undefined : draft.keyword, status: draft.status };
}
export function tableSort(field: unknown, order: unknown): Pick<EmployeeSearch, 'sortBy' | 'sortOrder'> {
  if (!sortFields.includes(field as EmployeeSearch['sortBy']) || !['ascend', 'descend'].includes(String(order))) return { sortBy: 'createdAt', sortOrder: 'desc' };
  return { sortBy: field as EmployeeSearch['sortBy'], sortOrder: order === 'ascend' ? 'asc' : 'desc' };
}
