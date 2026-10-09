import { QueryClient, queryOptions } from '@tanstack/react-query';
import type { components } from '@pet/api-contracts';
import { ApiError, queryRetry } from '../ApiError';
import { RequestClient } from '../request';
import { SessionRuntime } from '../../utils/auth/SessionRuntime';
import { normalizeName, spaces } from '../../utils/auth/spaces';
import type { AuthSpace, WebIdentity, StaffLogin, PlatformLogin } from '../../utils/auth/spaces';
import { authKeys } from './queryKeys';
import { useLayoutState } from '../../stores/app.store';

const authority = (identity: WebIdentity) => JSON.stringify([
  identity.principalType, identity.tenantId, identity.principalId, identity.sessionId,
  identity.authorizationVersion, [...identity.permissionCodes].sort(), [...identity.authorizedStoreIds].sort(), identity.dataScope,
  identity.principalType === 'STAFF' ? identity.passwordChangeRequired : false,
]);

export class AuthService {
  readonly runtime: SessionRuntime;
  constructor(readonly queryClient: QueryClient, client = new RequestClient()) {
    this.runtime = new SessionRuntime(client, space => {
      const predicate = (query: { queryKey: readonly unknown[] }) => authKeys.belongsTo(query.queryKey, space);
      void queryClient.cancelQueries({ predicate });
      queryClient.removeQueries({ predicate });
      useLayoutState.getState().reset(space);
    }, space => {
      void queryClient.invalidateQueries({ queryKey: authKeys.me(space, this.runtime.epoch(space)) });
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
            || (space === 'STAFF' && (typeof value.tenantId !== 'string' || !value.dataScope || !('passwordChangeRequired' in value) || typeof value.passwordChangeRequired !== 'boolean'))
            || (space === 'PLATFORM' && (value.tenantId !== null || value.dataScope !== null))) {
            throw new ApiError('PROTOCOL', '身份响应格式异常，请重试');
          }
          if (previous && authority(previous) !== authority(value)) {
            // 先交付新代际的 Query 身份，再通知路由/观察者，避免重验看见中间空态。
            this.runtime.advance(space, next => this.queryClient.setQueryData(authKeys.me(space, next), value));
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
  async secureSelf(space: AuthSpace, operation: 'password' | 'logout-all', input: components['schemas']['ChangePasswordInput'] | components['schemas']['ConfirmationInput']) {
    const identity = this.queryClient.getQueryData<WebIdentity | null>(authKeys.me(space, this.runtime.epoch(space)));
    try { return await this.runtime.transition(space, async () => {
      this.runtime.setNotice(space);
      // 取消旧请求后保留当前身份投影供禁用中的表单展示，不保存密码或权限副本。
      const body = operation === 'password'
        ? { currentPassword: input.currentPassword, newPassword: (input as components['schemas']['ChangePasswordInput']).newPassword }
        : { currentPassword: input.currentPassword };
      try {
        await this.runtime.request<null>(space, `${spaces[space].api}/auth/${operation}`, { method: operation === 'password' ? 'PUT' : 'POST', json: body });
        this.runtime.completeSecurityOperation(space, operation === 'password' ? '密码已修改，请重新登录' : '当前账号的全部设备已退出，请重新登录');
      } catch (error) {
        // 失效401只证明当前会话已失效，不证明本次改密/全撤销成功。
        if (!(error instanceof ApiError) || ['NETWORK', 'TIMEOUT', 'PROTOCOL', 'CANCELLED'].includes(error.kind) || (error.status ?? 0) >= 500) {
          this.runtime.setNotice(space, '请求结果未确认，请重新确认当前身份或重新登录；请勿直接重复提交');
        }
        throw error;
      } finally {
        // 只能清理应用引用；不能保证 JavaScript 内存被物理擦除。
        body.currentPassword = ''; if ('newPassword' in body) body.newPassword = '';
      }
    }, next => { if (identity) this.queryClient.setQueryData(authKeys.me(space, next), identity); }); } finally {
      input.currentPassword = ''; if ('newPassword' in input) input.newPassword = '';
    }
  }
}

export function createServices(client?: RequestClient) {
  const queryClient = new QueryClient({ defaultOptions: {
    // 显式写入即刻尝试并反馈失败；离线不得排队到恢复网络后自动提交。
    queries: { retry: queryRetry, retryDelay: count => (count + 1) * 1000 }, mutations: { retry: false, gcTime: 0, networkMode: 'always' },
  } });
  return { queryClient, auth: new AuthService(queryClient, client) };
}
export type Services = ReturnType<typeof createServices>;
