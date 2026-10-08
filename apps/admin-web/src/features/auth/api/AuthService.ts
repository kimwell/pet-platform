import { QueryClient, queryOptions } from '@tanstack/react-query';
import { ApiError, queryRetry } from '../../../shared/api/ApiError';
import { RequestClient } from '../../../shared/api/request';
import { SessionRuntime } from '../../../shared/auth/SessionRuntime';
import { normalizeName, spaces } from '../../../shared/auth/spaces';
import type { AuthSpace, WebIdentity, StaffLogin, PlatformLogin } from '../../../shared/auth/spaces';
import { authKeys } from './queryKeys';
import { useLayoutState } from '../../../app/layout/layoutState';

const authority = (identity: WebIdentity) => JSON.stringify([
  identity.principalType, identity.tenantId, identity.principalId, identity.sessionId,
  identity.authorizationVersion, [...identity.permissionCodes].sort(), [...identity.authorizedStoreIds].sort(), identity.dataScope,
]);

export class AuthService {
  readonly runtime: SessionRuntime;
  constructor(readonly queryClient: QueryClient, client = new RequestClient()) {
    this.runtime = new SessionRuntime(client, space => {
      const predicate = (query: { queryKey: readonly unknown[] }) => authKeys.belongsTo(query.queryKey, space);
      void queryClient.cancelQueries({ predicate });
      queryClient.removeQueries({ predicate });
      useLayoutState.getState().reset(space);
    });
  }
  meOptions(space: AuthSpace) {
    const epoch = this.runtime.epoch(space);
    return queryOptions({
      queryKey: authKeys.me(space, epoch), staleTime: 0, gcTime: 0, retry: false,
      queryFn: async ({ signal }) => {
        await this.runtime.idle(space);
        this.runtime.assertCurrent(space, epoch);
        const previous = this.queryClient.getQueryData<WebIdentity | null>(authKeys.me(space, epoch));
        try {
          const value = await this.runtime.request<WebIdentity | null>(space, `${spaces[space].api}/auth/me`, { signal });
          if (!value || value.principalType !== space || typeof value.displayName !== 'string'
            || typeof value.sessionId !== 'string' || !Array.isArray(value.permissionCodes)
            || !Array.isArray(value.authorizedStoreIds) || typeof value.authorizationVersion !== 'string'
            || (space === 'STAFF' && (typeof value.tenantId !== 'string' || !value.dataScope))
            || (space === 'PLATFORM' && (value.tenantId !== null || value.dataScope !== null))) {
            throw new ApiError('PROTOCOL', '身份响应格式异常，请重试');
          }
          if (previous && authority(previous) !== authority(value)) {
            const next = this.runtime.advance(space);
            this.queryClient.setQueryData(authKeys.me(space, next), value);
          }
          return value;
        } catch (error) {
          if (error instanceof ApiError && error.kind === 'HTTP' && error.status === 401 && error.code !== 'LOGIN_FAILED') return null;
          throw error;
        }
      },
    });
  }
  async current(space: AuthSpace): Promise<WebIdentity | null> {
    await this.runtime.idle(space);
    const epoch = this.runtime.epoch(space);
    try { return await this.queryClient.fetchQuery(this.meOptions(space)); }
    catch (error) {
      // 当前me确定失效/授权变化会清旧Query；守卫再读取新代际，取消不是未登录。
      if (epoch !== this.runtime.epoch(space)) return this.current(space);
      throw error;
    }
  }
  async login(space: AuthSpace, input: StaffLogin | PlatformLogin) {
    return this.runtime.transition(space, async () => {
      this.runtime.setNotice(space);
      // 明确选字段，不发送未知tenantId、角色或UI状态；不改变密码。
      const body = space === 'STAFF' ? {
        tenantCode: normalizeName((input as StaffLogin).tenantCode), loginName: normalizeName(input.loginName), password: input.password,
      } : { loginName: normalizeName(input.loginName), password: input.password };
      await this.runtime.request(space, `${spaces[space].api}/auth/login`, { method: 'POST', json: body }, false);
      // 登录响应不作为用户事实；释放过渡后由current重新查me。
      this.runtime.advance(space);
      await this.runtime.csrf(space);
    });
  }
  async logout(space: AuthSpace) {
    return this.runtime.transition(space, async () => {
      try {
        await this.runtime.request<null>(space, `${spaces[space].api}/auth/logout`, { method: 'POST' });
        this.runtime.advance(space); this.runtime.setNotice(space);
      } catch (error) {
        if (error instanceof ApiError && error.kind === 'HTTP' && error.status === 401
          && ['AUTH_REQUIRED', 'SESSION_EXPIRED', 'SESSION_REVOKED'].includes(error.code ?? '')) {
          this.runtime.setNotice(space, '当前会话已失效，请重新登录'); return;
        }
        this.runtime.setNotice(space, '退出未确认，服务端会话可能仍有效，请重试退出'); throw error;
      }
    });
  }
}

export function createServices(client?: RequestClient) {
  const queryClient = new QueryClient({ defaultOptions: {
    queries: { retry: queryRetry, retryDelay: count => (count + 1) * 1000 }, mutations: { retry: false, gcTime: 0 },
  } });
  return { queryClient, auth: new AuthService(queryClient, client) };
}
export type Services = ReturnType<typeof createServices>;
