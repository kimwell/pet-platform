import type { ReactNode } from 'react';
import type { AuthSpace } from './spaces';
import { usePermission } from './permissions';

export function PermissionBoundary({ space, code, children }: { space: AuthSpace; code: string; children: ReactNode }) {
  return usePermission(space, code) ? children : null;
}
