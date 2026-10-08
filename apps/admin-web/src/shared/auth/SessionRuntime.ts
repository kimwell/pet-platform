import type { components } from '@pet/api-contracts';
import { ApiError, cancelled } from '../api/ApiError';
import { RequestClient } from '../api/request';
import type { RequestOptions } from '../api/request';
import { spaces } from './spaces';
import type { AuthSpace } from './spaces';

type Csrf = components['schemas']['CsrfResult'];
type SpaceState = {
  epoch: number; invalidated: boolean; csrf?: Csrf; pendingCsrf?: Promise<Csrf>;
  controllers: Set<AbortController>; busy?: Promise<void>; notice?: string;
};
export class SessionRuntime {
  private readonly state: Record<AuthSpace, SpaceState> = {
    STAFF: { epoch: 0, invalidated: false, controllers: new Set() },
    PLATFORM: { epoch: 0, invalidated: false, controllers: new Set() },
  };
  private listeners = new Set<() => void>();
  constructor(readonly client: RequestClient, private readonly clearSpace: (space: AuthSpace) => void, private readonly revalidate?: (space: AuthSpace) => void) {}
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  private emit() { this.listeners.forEach(listener => listener()); }
  epoch(space: AuthSpace) { return this.state[space].epoch; }
  snapshot(space: AuthSpace) { return `${this.epoch(space)}:${this.isBusy(space) ? 1 : 0}`; }
  isBusy(space: AuthSpace) { return Boolean(this.state[space].busy); }
  notice(space: AuthSpace) { return this.state[space].notice; }
  setNotice(space: AuthSpace, value?: string) { this.state[space].notice = value; this.emit(); }
  async idle(space: AuthSpace) { await this.state[space].busy; }
  assertCurrent(space: AuthSpace, epoch: number) { if (epoch !== this.epoch(space)) throw cancelled(); }
  advance(space: AuthSpace, project?: (epoch: number) => void) {
    const state = this.state[space];
    state.epoch++; state.invalidated = false; state.csrf = undefined; state.pendingCsrf = undefined;
    state.controllers.forEach(c => c.abort()); state.controllers.clear();
    this.clearSpace(space); project?.(state.epoch); this.emit();
    return state.epoch;
  }
  completeSecurityOperation(space: AuthSpace, notice: string) {
    this.advance(space);
    // 成功通知不被随后确认旧会话的401覆盖；新登录 advance 才开启新代际。
    this.state[space].invalidated = true;
    this.setNotice(space, notice);
  }
  private expire(space: AuthSpace, epoch: number, error: ApiError) {
    if (epoch !== this.epoch(space) || this.state[space].invalidated) return;
    this.advance(space); this.state[space].invalidated = true;
    this.setNotice(space, error.code === 'AUTH_REQUIRED' ? undefined : '当前会话已失效，请重新登录');
  }
  /** 同一空间的登录/退出互斥；查询等待过渡完成。空间之间不互相等待。 */
  async transition<T>(space: AuthSpace, work: () => Promise<T>, project?: (epoch: number) => void): Promise<T> {
    if (this.isBusy(space)) throw new ApiError('HTTP', '当前操作正在处理，请稍候');
    let finish = () => {};
    this.state[space].busy = new Promise<void>(resolve => { finish = resolve; });
    this.advance(space, project);
    try { return await work(); }
    finally { this.state[space].busy = undefined; finish(); this.emit(); }
  }
  private async transport<T>(space: AuthSpace, epoch: number, path: string, options: RequestOptions, protectedRequest: boolean): Promise<T> {
    this.assertCurrent(space, epoch);
    if (!path.startsWith(`${spaces[space].api}/`)) throw new ApiError('PROTOCOL', '请求身份空间不匹配');
    const controller = new AbortController();
    const abort = () => controller.abort();
    if (options.signal?.aborted) controller.abort();
    options.signal?.addEventListener('abort', abort, { once: true });
    this.state[space].controllers.add(controller);
    try {
      const value = await this.client.request<T>(path, { ...options, signal: controller.signal });
      this.assertCurrent(space, epoch); return value;
    } catch (error) {
      this.assertCurrent(space, epoch);
      if (error instanceof ApiError && error.kind === 'HTTP') {
        if (protectedRequest && error.status === 401 && error.code !== 'LOGIN_FAILED') this.expire(space, epoch, error);
        // 只使对应凭据过期，原写入不重放。权限403保留CSRF。
        if (error.code === 'CSRF_INVALID') this.state[space].csrf = undefined;
        if (protectedRequest && !path.endsWith('/auth/me') && ['PERMISSION_DENIED', 'PASSWORD_CHANGE_REQUIRED'].includes(error.code ?? '')) this.revalidate?.(space);
      }
      throw error;
    } finally { this.state[space].controllers.delete(controller); options.signal?.removeEventListener('abort', abort); }
  }
  async csrf(space: AuthSpace): Promise<Csrf> {
    const state = this.state[space];
    if (state.csrf && Date.parse(state.csrf.expiresAt) > Date.now() + 5_000) return state.csrf;
    if (state.pendingCsrf) return state.pendingCsrf;
    const epoch = state.epoch;
    const promise = this.transport<Csrf | null>(space, epoch, `${spaces[space].api}/auth/csrf`, {}, true).then(value => {
      this.assertCurrent(space, epoch);
      if (!value || typeof value.csrfToken !== 'string' || !/^[A-Za-z0-9_-]{32,256}$/.test(value.csrfToken)
        || !Number.isFinite(Date.parse(value.expiresAt)) || Date.parse(value.expiresAt) <= Date.now()) {
        throw new ApiError('PROTOCOL', '安全凭据响应格式异常，请重试');
      }
      state.csrf = value; return value;
    }).finally(() => { if (state.pendingCsrf === promise) state.pendingCsrf = undefined; });
    state.pendingCsrf = promise; return promise;
  }
  async request<T>(space: AuthSpace, path: string, options: RequestOptions = {}, protectedRequest = true): Promise<T> {
    const epoch = this.epoch(space);
    // 在获取秘密之前验证路径，不能将认证头发送到另一空间或任意URL。
    this.client.url(path, options.query);
    if (!path.startsWith(`${spaces[space].api}/`)) throw new ApiError('PROTOCOL', '请求身份空间不匹配');
    if (options.signal?.aborted) throw cancelled();
    let csrfToken: string | undefined;
    if (options.method && options.method !== 'GET') {
      csrfToken = (await this.csrf(space)).csrfToken;
      this.assertCurrent(space, epoch);
      if (options.signal?.aborted) throw cancelled();
    }
    return this.transport<T>(space, epoch, path, { ...options, csrfToken }, protectedRequest);
  }
}
