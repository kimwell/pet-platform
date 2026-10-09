import { readFile, readdir, mkdir, lstat, unlink, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { createHash } from 'node:crypto';

export const marker = '// 自动生成：请勿手改；来源SHA256=';
export function normalizeOpenApi(raw) {
  const doc = JSON.parse(raw);
  if (doc.openapi !== '3.1.0') throw new Error('OpenAPI版本与冻结协议不一致');
  // 仅移除springdoc按请求地址生成的临时loopback server；保留显式配置的servers。
  if (doc.servers?.every(server => server.description === 'Generated server url' && /^http:\/\/(127\.0\.0\.1|localhost|\[::1\]):\d+$/.test(server.url))) delete doc.servers;
  const sorted = value => Array.isArray(value) ? value.map(sorted) : value !== null && typeof value === 'object'
    ? Object.fromEntries(Object.keys(value).sort().map(key => [key, sorted(value[key])])) : value;
  return JSON.stringify(sorted(doc), null, 2) + '\n';
}
export function miniDeclaration(declaration) {
  return marker + createHash('sha256').update(declaration).digest('hex') + '\n' + declaration;
}
export async function differences(root, artifacts) {
  const changed = [];
  for (const [path, expected] of artifacts) {
    let actual;
    try { actual = await readFile(join(root, path), 'utf8'); } catch (error) { if (error.code !== 'ENOENT') throw error; }
    if (actual !== expected) changed.push(path);
  }
  const mini = join(root, 'apps/wechat-miniprogram/types/generated');
  try {
    for (const file of await readdir(mini)) if (file !== 'api.d.ts') changed.push('apps/wechat-miniprogram/types/generated/' + file);
  } catch (error) { if (error.code !== 'ENOENT') throw error; }
  return changed.sort();
}
export async function syncMini(root, declaration) {
  // 删除范围是固定的类型生成目录；拒绝符号链接、子目录和未标记的手写文件。
  const directory = join(root, 'apps/wechat-miniprogram/types/generated');
  for (const ancestor of ['apps', 'apps/wechat-miniprogram', 'apps/wechat-miniprogram/types', 'apps/wechat-miniprogram/types/generated']) {
    try {
      if ((await lstat(join(root, ancestor))).isSymbolicLink()) throw new Error('小程序生成路径禁止符号链接');
    } catch (error) { if (error.code !== 'ENOENT') throw error; }
  }
  await mkdir(directory, { recursive: true });
  const files = await readdir(directory);
  for (const file of files) {
    const path = join(directory, file);
    const stat = await lstat(path);
    if (!stat.isFile() || !(await readFile(path, 'utf8')).startsWith(marker)) throw new Error('生成目录含未标记文件，拒绝覆盖：' + file);
  }
  for (const file of files) if (file !== 'api.d.ts') await unlink(join(directory, file));
  await writeFile(join(directory, 'api.d.ts'), miniDeclaration(declaration));
}
