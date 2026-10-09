import { queryOptions } from '@tanstack/react-query';
import type { WebIdentity } from '../../../../shared/auth/spaces';
import { hasPermission } from '../../../../shared/auth/permissions';
import { authKeys } from '../../../auth/api/queryKeys';
import type { AuthService } from '../../../auth/api/AuthService';
import { listEmployees } from '../api/employees';
import type { EmployeeSearch } from './search';

export function employeeListOptions(auth: AuthService, identity: WebIdentity, query: EmployeeSearch) {
  const epoch = auth.runtime.epoch('STAFF');
  return queryOptions({ queryKey: authKeys.protected(identity, epoch, 'identity', 'employees', 'list', query),
    enabled: hasPermission(identity, 'STAFF', 'identity:user:list') && !auth.runtime.isBusy('STAFF'),
    staleTime: 30_000, gcTime: 300_000, refetchOnWindowFocus: false,
    // 不使用 placeholderData；新条件或新身份不能沿用上一结果。
    queryFn: ({ signal }) => { auth.runtime.assertCurrent('STAFF', epoch); return listEmployees(auth, query, signal); },
  });
}
