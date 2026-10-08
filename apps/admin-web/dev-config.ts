/** 仅由 Vite 开发服务器读取，不注入客户端，也不提供生产 API 地址。 */
export function resolveDevBackendOrigin(value: string | undefined): string {
  if (value === undefined) return 'http://127.0.0.1:8080';
  const message = 'DEV_BACKEND_ORIGIN 必须是完整的 HTTP/HTTPS 来源，不包含凭据、路径、查询或片段，端口须为 1～65535';
  if (!/^https?:\/\//i.test(value)) throw new Error(message);
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    throw new Error(message);
  }
  if (!['http:', 'https:'].includes(url.protocol) || !url.hostname
      || url.username || url.password || url.pathname !== '/' || url.search || url.hash
      || (url.port && Number(url.port) < 1)) {
    throw new Error(message);
  }
  return url.origin;
}
