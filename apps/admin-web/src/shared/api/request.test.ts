import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, errorText, queryRetry } from './ApiError';
import { RequestClient } from './request';
import { fail, reply, traceId } from '../../test/responses';

describe('统一fetch协议（受控网络替身）', () => {
  afterEach(() => vi.useRealTimers());
  it('成功data:null保持null并保留trace元数据', async () => {
    const onTrace = vi.fn(); const client = new RequestClient(vi.fn().mockResolvedValue(reply(null)));
    await expect(client.request('/admin/auth/logout', { onTrace })).resolves.toBeNull(); expect(onTrace).toHaveBeenCalledWith(traceId);
  });
  it('编码query并携带Cookie/signal/JSON，不创建Token Header', async () => {
    const fetcher = vi.fn().mockResolvedValue(reply({})); const client = new RequestClient(fetcher);
    await client.request('/admin/example', { method: 'POST', query: { q: 'a b&中', page: 1, ids: ['a', 'b'], empty: null }, json: { value: '中' }, csrfToken: 'csrf' });
    expect(fetcher.mock.calls[0][0]).toBe('/api/admin/example?q=a+b%26%E4%B8%AD&page=1&ids=a&ids=b');
    const options = fetcher.mock.calls[0][1]; expect(options.credentials).toBe('same-origin'); expect(options.redirect).toBe('error');
    expect(options.headers.get('Content-Type')).toBe('application/json'); expect(options.headers.get('Authorization')).toBeNull(); expect(options.signal).toBeInstanceOf(AbortSignal);
  });
  it('FormData原样透传且不设置boundary', async () => {
    const fetcher = vi.fn().mockResolvedValue(reply(null)); const formData = new FormData(); formData.set('name', '测试');
    await new RequestClient(fetcher).request('/admin/example', { method: 'POST', formData });
    expect(fetcher.mock.calls[0][1].body).toBe(formData); expect(fetcher.mock.calls[0][1].headers.has('Content-Type')).toBe(false);
  });
  it('标准错误、字段错误和429 Retry-After', async () => {
    const fields = [{ field: 'loginName', code: 'REQUIRED', message: '请输入账号' }];
    const client = new RequestClient(vi.fn().mockResolvedValue(fail(422, 'VALIDATION_FAILED', fields)));
    await expect(client.request('/admin/auth/login')).rejects.toMatchObject({ kind: 'HTTP', status: 422, code: 'VALIDATION_FAILED', fieldErrors: fields, traceId });
    try { await new RequestClient(vi.fn().mockResolvedValue(fail(429, 'RATE_LIMITED'))).request('/admin/auth/login'); } catch (error) { expect(errorText(error)).toContain('17 秒'); }
  });
  it.each([401, 502])('非JSON错误页%d不能冒充会话失效或显示HTML', async status => {
    const client = new RequestClient(vi.fn().mockResolvedValue(new Response('<html>内部配置</html>', { status, headers: { 'Content-Type': 'text/html' } })));
    await expect(client.request('/admin/auth/me')).rejects.toMatchObject({ kind: 'PROTOCOL', status, message: '服务响应格式异常，请稍后重试' });
  });
  it.each([{}, { success: true, traceId }, { success: true, data: null, traceId: 'secret' }, { success: false, error: {}, traceId }])('缺失/不合法信封拒绝', async value => {
    const client = new RequestClient(vi.fn().mockResolvedValue(new Response(JSON.stringify(value), { headers: { 'Content-Type': 'application/json' } })));
    await expect(client.request('/admin/auth/me')).rejects.toMatchObject({ kind: 'PROTOCOL' });
  });
  it('HTTP状态和信封分支不一致拒绝', async () => {
    await expect(new RequestClient(vi.fn().mockResolvedValue(reply(null, 401))).request('/admin/auth/me')).rejects.toMatchObject({ kind: 'PROTOCOL' });
  });
  it('网络错误不同于401', async () => {
    await expect(new RequestClient(vi.fn().mockRejectedValue(new TypeError('内部网络细节'))).request('/admin/auth/me')).rejects.toMatchObject({ kind: 'NETWORK', status: undefined });
  });
  it('取消包括fetch未及时响应abort，不等待旧响应', async () => {
    const controller = new AbortController(); const client = new RequestClient(vi.fn(() => new Promise<Response>(() => {})));
    const result = client.request('/admin/auth/me', { signal: controller.signal }); controller.abort();
    await expect(result).rejects.toMatchObject({ kind: 'CANCELLED' });
  });
  it('已取消请求不发送', async () => {
    const fetcher = vi.fn(); const controller = new AbortController(); controller.abort();
    await expect(new RequestClient(fetcher).request('/admin/auth/me', { signal: controller.signal })).rejects.toMatchObject({ kind: 'CANCELLED' }); expect(fetcher).not.toHaveBeenCalled();
  });
  it('超时包括响应Body不完成', async () => {
    vi.useFakeTimers(); const body = new ReadableStream({ start() {} });
    const client = new RequestClient(vi.fn().mockResolvedValue(new Response(body, { headers: { 'Content-Type': 'application/json' } })));
    const result = client.request('/admin/auth/me', { timeoutMs: 50 }); const assertion = expect(result).rejects.toMatchObject({ kind: 'TIMEOUT' });
    await vi.advanceTimersByTimeAsync(50); await assertion;
  });
  it('响应trace头与信封不一致拒绝', async () => {
    const response = reply(null); response.headers.set('X-Trace-Id', 'b'.repeat(32));
    await expect(new RequestClient(vi.fn().mockResolvedValue(response)).request('/admin/auth/me')).rejects.toMatchObject({ kind: 'PROTOCOL' });
  });
  it.each(['https://elsewhere.invalid/api/admin', '//elsewhere.invalid/api', '/admin/../platform/auth/me', '/admin/%2e%2e/me', '/admin\\auth', '/admin/auth/me?token=secret'])('同源路径拒绝%s', async path => {
    const fetcher = vi.fn(); await expect(new RequestClient(fetcher).request(path, { csrfToken: 'secret' })).rejects.toMatchObject({ kind: 'PROTOCOL' }); expect(fetcher).not.toHaveBeenCalled();
  });
  it('API根地址和query秘密拒绝', async () => {
    expect(() => new RequestClient(fetch, 'https://elsewhere.invalid')).toThrow(ApiError);
    await expect(new RequestClient().request('/admin/auth/me', { query: { token: 'secret' } })).rejects.toMatchObject({ kind: 'PROTOCOL' });
  });
  it('重试只覆盖有限网络/超时/502-504，不重放mutation', () => {
    for (const status of [401, 403, 422, 429]) expect(queryRetry(0, new ApiError('HTTP', '失败', status))).toBe(false);
    expect(queryRetry(0, new ApiError('HTTP', '失败', 503))).toBe(true); expect(queryRetry(2, new ApiError('NETWORK', '失败'))).toBe(false);
  });
});
