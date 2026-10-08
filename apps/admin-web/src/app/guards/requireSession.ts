import { redirect } from '@tanstack/react-router';
import type { AuthService } from '../../features/auth/api/AuthService';
import type { AuthSpace } from '../../shared/auth/spaces';
import { spaces } from '../../shared/auth/spaces';
import { safeReturnTo } from '../../shared/auth/returnTo';
import { ApiError } from '../../shared/api/ApiError';

export async function requireSession(auth: AuthService, space: AuthSpace, href: string) {
  let identity;
  try { identity = await auth.current(space); }
  catch (error) {
    // 预期服务/权限故障是页面状态，不抛入React渲染错误边界。
    if (error instanceof ApiError && error.kind !== 'CANCELLED') return { sessionError: error };
    throw error;
  }
  if (!identity) throw redirect({ to: spaces[space].login, search: { returnTo: safeReturnTo(space, href) }, replace: true });
  return { sessionError: undefined };
}
