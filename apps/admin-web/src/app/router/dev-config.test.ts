import { describe, expect, it } from 'vitest';
import { resolveDevBackendOrigin } from '../../../dev-config';

describe('开发代理配置', () => {
  it('仅未配置时使用本地默认值，支持合法 HTTP/HTTPS 来源', () => {
    expect(resolveDevBackendOrigin(undefined)).toBe('http://127.0.0.1:8080');
    expect(resolveDevBackendOrigin('https://example.com/')).toBe('https://example.com');
  });

  it.each(['', 'backend', 'http:localhost', 'https:/localhost', 'file:///tmp/backend', 'http://user:technical-fixture@localhost',
    'http://localhost/api', 'http://localhost?token=technical-fixture', 'http://localhost#fragment',
    'http://localhost:0', 'http://localhost:65536'])('拒绝错误配置并仅提示配置名称：%s', (value) => {
    expect(() => resolveDevBackendOrigin(value)).toThrow(/^DEV_BACKEND_ORIGIN 必须/);
    try { resolveDevBackendOrigin(value); } catch (error) {
      expect((error as Error).message).not.toContain('technical-fixture');
    }
  });
});
