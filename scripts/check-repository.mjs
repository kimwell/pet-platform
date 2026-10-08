import { readdirSync, readFileSync, existsSync, statSync } from 'node:fs';
import { resolve, relative } from 'node:path';
import { execFileSync } from 'node:child_process';
import assert from 'node:assert/strict';

const root = resolve('.');
const manifest = JSON.parse(readFileSync('package.json', 'utf8'));
const versionMatrix = readFileSync('docs/development/VERSION-MATRIX.md', 'utf8');
assert.equal(manifest.packageManager, 'pnpm@10.34.6');
assert.equal(process.versions.node, manifest.engines.node, '请使用 .nvmrc 的冻结 Node.js');
assert.equal(readFileSync('.nvmrc', 'utf8').trim(), manifest.engines.node);
const skipped = new Set(['.git', 'node_modules', 'dist', 'target', 'coverage', '.local-data', 'miniprogram_npm', '.cache', '.vite']);
const sources = [];
function visit(dir) {
  for (const item of readdirSync(dir, { withFileTypes: true })) {
    if (skipped.has(item.name) || item.isSymbolicLink()) continue;
    const path = resolve(dir, item.name);
    const rel = relative(root, path);
    if (item.isDirectory()) visit(path);
    else if (item.isFile()) sources.push({ path, rel });
  }
}
visit(root);
const locks = sources.filter(({ rel }) => /(^|\/)(pnpm-lock\.yaml|package-lock\.json|yarn\.lock|npm-shrinkwrap\.json)$/.test(rel));
assert.deepEqual(locks.map(({ rel }) => rel), ['pnpm-lock.yaml'], '前端必须只有根 pnpm 锁文件');
for (const path of ['package.json', 'apps/admin-web/package.json', 'apps/wechat-miniprogram/package.json', 'packages/api-contracts/package.json']) {
  const pkg = JSON.parse(readFileSync(path, 'utf8'));
  for (const [name, version] of Object.entries({ ...pkg.dependencies, ...pkg.devDependencies })) {
    if (name === '@pet/api-contracts') {
      assert.equal(version, 'workspace:*');
      assert.equal(path, 'apps/admin-web/package.json');
      continue;
    }
    assert.match(version, /^\d+\.\d+\.\d+(-[\w.-]+)?$/, `${path} 依赖 ${name} 必须固定精确版本`);
    const row = versionMatrix.split('\n').find((line) => line.startsWith(`| ${name} /`));
    assert.ok(row, `${name} 缺少唯一版本矩阵记录`);
    assert.equal(row.split('|')[2].trim(), version, `${name} 与冻结版本矩阵不一致`);
  }
}
assert.ok(existsSync('apps/backend/mvnw'));
if (process.platform !== 'win32') assert.ok(statSync('apps/backend/mvnw').mode & 0o111, 'Wrapper 缺少执行权限');
assert.match(readFileSync('apps/backend/src/main/java/com/pet/platform/Application.java', 'utf8'), /package com\.pet\.platform;/);
const findings = [];
const secretPatterns = [
  /-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----/,
  /\bAKIA[0-9A-Z]{16}\b/,
  /\bgh[pousr]_[A-Za-z0-9]{30,}\b/,
  /\bsk-(?:proj-)?[A-Za-z0-9_-]{30,}\b/,
  /(?:appsecret|api[_-]?key|access[_-]?token|password|secret)\s*[:=]\s*["'](?:[A-Za-z0-9+/=_-]{24,})["']/i
];
for (const { path, rel } of sources) {
  // 已有视觉资料与历史证据不归新 CI 扫描；本轮另外保全、人工核对。
  if (rel.startsWith('ui/') || rel.startsWith('docs/testing/evidence/') || rel === 'scripts/check-repository.mjs') continue;
  if (/\.(pem|key|p12|pfx|jks|keystore|crt|cer)$/.test(rel) || rel.endsWith('project.private.config.json') || /(^|\/)\.env($|\.)/.test(rel) && !rel.endsWith('.example')) {
    const tracked = execFileSync('git', ['ls-files', '--', rel], { encoding: 'utf8' }).trim();
    if (tracked) findings.push(`${rel}：本地配置或密钥文件进入版本控制`);
    continue;
  }
  if (statSync(path).size > 512_000) continue;
  const text = readFileSync(path, 'utf8');
  if (secretPatterns.some((pattern) => pattern.test(text))) findings.push(`${rel}：发现秘密特征，请在本机核查`);
}
assert.deepEqual(findings, [], findings.join('\n'));
console.log('单锁、精确直接依赖、工具链、固定后端入口及秘密特征检查通过。秘密检查不输出匹配值。');
