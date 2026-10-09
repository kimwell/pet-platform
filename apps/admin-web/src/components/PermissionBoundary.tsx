import type { ReactNode } from 'react';
import type { AuthSpace } from '../utils/auth/spaces';
import { usePermission } from '../utils/auth/permissions';

export function PermissionBoundary({ space, code, children }: { space: AuthSpace; code: string; children: ReactNode }) {
  return usePermission(space, code) ? children : null;
}
