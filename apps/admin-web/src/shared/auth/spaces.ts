import type { components } from '@pet/api-contracts';

export type AuthSpace = 'STAFF' | 'PLATFORM';
export type WebIdentity = components['schemas']['CurrentIdentity'] | components['schemas']['PlatformCurrentIdentity'];
export type StaffLogin = components['schemas']['LoginInput'];
export type PlatformLogin = components['schemas']['PlatformLoginInput'];
export const spaces = {
  STAFF: { path: '/admin', security: '/admin/security', login: '/admin/login', api: '/admin', title: '员工工作台', label: '员工' },
  PLATFORM: { path: '/platform', security: '/platform/security', login: '/platform/login', api: '/platform', title: '平台控制台', label: '平台管理员' },
} as const;

// 对齐 Character.isWhitespace / String.strip，避免 JS trim 额外剥离 NBSP 等字符。
export function normalizeName(value: string): string {
  return value.replace(/^[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]+|[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]+$/g, '')
    .replace(/[A-Z]/g, char => char.toLowerCase());
}
