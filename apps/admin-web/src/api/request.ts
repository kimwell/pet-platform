import type { components } from '@pet/api-contracts';
import { apiConfig } from '../config/api';
import { ApiError, cancelled } from './ApiError';

type QueryValue = string | number | boolean | null | undefined;
export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  query?: Record<string, QueryValue | readonly QueryValue[]>;
  json?: unknown;
  formData?: FormData;
  signal?: AbortSignal;
  timeoutMs?: number;
  csrfToken?: string;
  onTrace?: (traceId: string) => void;
}
const object = (v: unknown): v is Record<string, unknown> => typeof v === 'object' && v !== null && !Array.isArray(v);
const trace = (v: unknown): v is string => typeof v === 'string' && /^[a-f0-9]{32}$/.test(v);
const safeMessage = (v: unknown): v is string => typeof v === 'string' && v.length > 0 && v.length <= 300 && !/[<>\r\n]/.test(v);
function failure(v: unknown): v is components['schemas']['ApiError'] {
  return object(v) && typeof v.code === 'string' && /^[A-Z][A-Z0-9_]{0,63}$/.test(v.code)
    && safeMessage(v.message) && (v.fieldErrors === undefined || (Array.isArray(v.fieldErrors) && v.fieldErrors.length > 0
      && v.fieldErrors.every(f => object(f) && typeof f.field === 'string'
        && /^[A-Za-z][A-Za-z0-9_]*(?:\[\d+\]|\.[A-Za-z][A-Za-z0-9_]*)*$/.test(f.field)
        && typeof f.code === 'string' && safeMessage(f.message))));
}

/** 原始字节响应须由独立下载客户端处理；本函数只接受标准 JSON 信封。 */
export class RequestClient {
  constructor(private readonly fetcher: typeof fetch = (...args) => fetch(...args), private readonly baseUrl: string = apiConfig.baseUrl) {
    if (!/^\/api(?:\/[a-z0-9-]+)*$/.test(baseUrl)) throw new ApiError('PROTOCOL', 'API 根路径配置无效');
  }
  url(path: string, query?: RequestOptions['query']): string {
    // 禁止绝对URL、协议相对URL、编码路径别名、反斜线、点段和内嵌query/hash。
    if (!/^\/[A-Za-z0-9/_-]+$/.test(path) || path.includes('//')) throw new ApiError('PROTOCOL', '请求地址无效');
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(query ?? {})) {
      if (/password|token|secret|csrf|credential/i.test(key)) throw new ApiError('PROTOCOL', '敏感信息不能放入请求地址');
      for (const item of Array.isArray(value) ? value : [value]) if (item !== null && item !== undefined) params.append(key, String(item));
    }
    return this.baseUrl + path + (params.size ? `?${params}` : '');
  }
  async request<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const url = this.url(path, options.query);
    if (options.json !== undefined && options.formData) throw new ApiError('PROTOCOL', '请求内容配置冲突');
    const timeoutMs = options.timeoutMs ?? apiConfig.timeoutMs;
    if (!Number.isFinite(timeoutMs) || timeoutMs <= 0) throw new ApiError('PROTOCOL', '请求超时配置无效');
    if (options.signal?.aborted) throw cancelled();
    const controller = new AbortController();
    let timedOut = false;
    const abort = () => controller.abort();
    options.signal?.addEventListener('abort', abort, { once: true });
    const timer = setTimeout(() => { timedOut = true; controller.abort(); }, timeoutMs);
    let removeAbort = () => {};
    const stopped = new Promise<never>((_, reject) => {
      const stop = () => reject(timedOut ? new ApiError('TIMEOUT', '请求超时，请稍后重试') : cancelled());
      controller.signal.addEventListener('abort', stop, { once: true });
      removeAbort = () => controller.signal.removeEventListener('abort', stop);
    });
    const run = async (): Promise<T> => {
      const headers = new Headers({ Accept: 'application/json' });
      if (options.csrfToken) headers.set('X-CSRF-Token', options.csrfToken);
      if (options.json !== undefined) headers.set('Content-Type', 'application/json');
      const response = await this.fetcher(url, {
        method: options.method ?? 'GET', credentials: 'same-origin', cache: 'no-store', redirect: 'error',
        headers, body: options.formData ?? (options.json !== undefined ? JSON.stringify(options.json) : undefined), signal: controller.signal,
      });
      const headerTrace = response.headers.get('X-Trace-Id');
      const validHeaderTrace = trace(headerTrace) ? headerTrace : undefined;
      const protocol = () => new ApiError('PROTOCOL', '服务响应格式异常，请稍后重试', response.status, undefined, validHeaderTrace);
      if (!/^application\/json(?:\s*;|$)/i.test(response.headers.get('Content-Type') ?? '')) throw protocol();
      let envelope: unknown;
      try { envelope = await response.json(); } catch { throw protocol(); }
      if (!object(envelope) || !trace(envelope.traceId) || (validHeaderTrace && envelope.traceId !== validHeaderTrace)) throw protocol();
      if (response.ok && envelope.success === true && 'data' in envelope && !('error' in envelope)) {
        if (controller.signal.aborted) throw cancelled();
        options.onTrace?.(envelope.traceId);
        return envelope.data as T;
      }
      if (!response.ok && envelope.success === false && !('data' in envelope) && failure(envelope.error)) {
        const retry = response.headers.get('Retry-After');
        const seconds = retry && /^[1-9][0-9]{0,7}$/.test(retry) ? Number(retry) : undefined;
        throw new ApiError('HTTP', envelope.error.message, response.status, envelope.error.code, envelope.traceId, envelope.error.fieldErrors, seconds);
      }
      throw protocol();
    };
    try { return await Promise.race([run(), stopped]); }
    catch (error) {
      if (controller.signal.aborted) throw timedOut ? new ApiError('TIMEOUT', '请求超时，请稍后重试') : cancelled();
      if (error instanceof ApiError) throw error;
      throw new ApiError('NETWORK', '网络连接失败，请检查网络后重试');
    } finally { clearTimeout(timer); removeAbort(); options.signal?.removeEventListener('abort', abort); }
  }
}
