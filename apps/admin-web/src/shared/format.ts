import type { components } from '@pet/api-contracts';

/** 当前身份未公开业务 zoneId，员工时间按契约默认时区展示，页面明确标注。 */
export const displayTimeZone = 'Asia/Shanghai';
export const employeeStatusText = (status: components['schemas']['EmployeeView']['status']) => status === 'ACTIVE' ? '启用' : '停用';
const timeFormat = new Intl.DateTimeFormat('zh-CN', { timeZone: displayTimeZone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' });
export function displayText(value: string | null | undefined): string { return value || '—'; }
export function displayInstant(value: string | null | undefined): string {
  if (!value || !Number.isFinite(Date.parse(value))) return '—';
  return timeFormat.format(new Date(value));
}
