import type { AuthSpace, WebIdentity } from './spaces';
import { useSession } from '../../hooks/useSession';

export function isRestricted(identity: WebIdentity | null | undefined): boolean {
  return identity?.principalType === 'STAFF' && identity.passwordChangeRequired;
}
/** 只判断明确代码，数据范围仍由后端按每权限 grant 授权，不取跨权限最大范围。 */
export function hasPermission(identity: WebIdentity | null | undefined, space: AuthSpace, code: string): boolean {
  return Boolean(identity?.principalType === space && !isRestricted(identity) && code !== '*' && identity.permissionCodes.includes(code));
}
export type SelfAction = 'password' | 'logout-all' | 'logout';
export function canManageSelf(identity: WebIdentity | null | undefined, space: AuthSpace, action: SelfAction): boolean {
  if (!identity || identity.principalType !== space) return false;
  if (space === 'STAFF') return true;
  return hasPermission(identity, space, action === 'password' ? 'platform:credential:change' : 'platform:session:manage');
}
export function usePermission(space: AuthSpace, code: string): boolean {
  // 观察现有 Query 投影；本 hook 不另行发送 me，不建立权限镜像。
  const session = useSession(space, false);
  return !session.busy && !session.isPending && !session.isError && hasPermission(session.data, space, code);
}
