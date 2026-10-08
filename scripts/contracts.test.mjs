import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, readFile, writeFile, rm, symlink } from 'node:fs/promises';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import { normalizeOpenApi, miniDeclaration, differences, syncMini } from './contract-files.mjs';

test('稳定化只移除动态loopback地址，保留required、null、枚举及认证语义', () => {
  const doc = { openapi: '3.1.0', paths: {}, servers: [{ url: 'http://127.0.0.1:12345', description: 'Generated server url' }],
    components: { schemas: { Data: { type: ['string', 'null'], enum: ['A', 'B'], required: ['id'] } }, securitySchemes: { token: { type: 'apiKey', in: 'header', name: 'Token' } } } };
  const output = JSON.parse(normalizeOpenApi(JSON.stringify(doc)));
  assert.equal(output.servers, undefined);
  assert.deepEqual(output.components, doc.components);
  doc.servers = [{ url: 'https://api.example.invalid' }];
  assert.deepEqual(JSON.parse(normalizeOpenApi(JSON.stringify(doc))).servers, doc.servers);
});
test('漂移报告包含真实变化且check不覆盖文件；同步只删除带标识的旧生成类型', async () => {
  const root = await mkdtemp(join(tmpdir(), 'pet-contract-files-'));
  try {
    const generated = join(root, 'apps/wechat-miniprogram/miniprogram/types/generated');
    await syncMini(root, 'type Total = string;\n');
    await writeFile(join(generated, 'old.d.ts'), miniDeclaration('旧生成文件'));
    assert.equal((await differences(root, new Map([['apps/wechat-miniprogram/miniprogram/types/generated/api.d.ts', miniDeclaration('type Total = number;\n')]]))).length, 2);
    assert.equal(await readFile(join(generated, 'api.d.ts'), 'utf8'), miniDeclaration('type Total = string;\n'));
    await syncMini(root, 'type Total = string;\n');
    await assert.rejects(readFile(join(generated, 'old.d.ts')));
    await writeFile(join(generated, 'handwritten.ts'), '保留手写源码');
    await assert.rejects(syncMini(root, '新产物'), /未标记文件/);
    assert.equal(await readFile(join(generated, 'handwritten.ts'), 'utf8'), '保留手写源码');
  } finally { await rm(root, { recursive: true, force: true }); }
});
test('同步拒绝符号链接目录', async () => {
  const root = await mkdtemp(join(tmpdir(), 'pet-contract-links-'));
  try {
    await mkdir(join(root, 'real'));
    await symlink(join(root, 'real'), join(root, 'apps'), 'dir');
    await assert.rejects(syncMini(root, 'test'), /符号链接/);
  } finally { await rm(root, { recursive: true, force: true }); }
});
