import { queryOptions } from '@tanstack/react-query';
import type { WebIdentity } from '../../utils/auth/spaces';
import { hasPermission } from '../../utils/auth/permissions';
import { authKeys } from '../../api/auth/queryKeys';
import type { AuthService } from '../../api/auth/AuthService';
import { getEmployee, listEmployees } from '../../api/identity/employees';
import { isEmployeeId } from '../../utils/identity/detailSearch';
import { ApiError } from '../../api/ApiError';
import type { EmployeeSearch } from '../../utils/identity/search';

export function employeeListOptions(auth: AuthService, identity: WebIdentity, query: EmployeeSearch) {
  const epoch = auth.runtime.epoch('STAFF');
  return queryOptions({ queryKey: authKeys.protected(identity, epoch, 'identity', 'employees', 'list', query),
    enabled: hasPermission(identity, 'STAFF', 'identity:user:list') && !auth.runtime.isBusy('STAFF'),
    staleTime: 30_000, gcTime: 300_000, refetchOnWindowFocus: false,
    // 不使用 placeholderData；新条件或新身份不能沿用上一结果。
    queryFn: ({ signal }) => { auth.runtime.assertCurrent('STAFF', epoch); return listEmployees(auth, query, signal); },
  });
}

export function employeeDetailOptions(auth: AuthService, identity: WebIdentity, employeeId: string) {
  const epoch = auth.runtime.epoch('STAFF');
  const allowed = hasPermission(identity, 'STAFF', 'identity:user:detail');
  return queryOptions({ queryKey: authKeys.protected(identity, epoch, 'identity', 'employees', 'detail', { employeeId }),
    enabled: allowed && isEmployeeId(employeeId) && !auth.runtime.isBusy('STAFF'),
    // 明确尝试真实传输并展示网络错误，避免离线时暂停查询却仍显示成功状态。
    networkMode: 'always',
    staleTime: 30_000, gcTime: 300_000, refetchOnWindowFocus: false,
    queryFn: ({ signal }) => {
      auth.runtime.assertCurrent('STAFF', epoch);
      if (!allowed) throw new ApiError('HTTP', '当前账号没有查看员工详情的权限', 403, 'PERMISSION_DENIED');
      return getEmployee(auth, employeeId, signal);
    },
  });
}
