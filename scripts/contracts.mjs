import { execFileSync } from 'node:child_process';
import { mkdtemp, readFile, writeFile, mkdir, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import openapiTS, { astToString } from 'openapi-typescript';
import { normalizeOpenApi, miniDeclaration, differences, syncMini } from './contract-files.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const mode = process.argv[2];
if (!['generate', 'check'].includes(mode)) throw new Error('用法：contracts.mjs generate|check');
const temporary = await mkdtemp(join(tmpdir(), 'pet-contracts-'));
try {
  const raw = join(temporary, 'raw');
  const args = ['--batch-mode', 'clean', '-Dtest=ContractExportTest,ProductionOpenApiExportTest', '-Dpet.contract.output=' + raw, 'test'];
  if (process.platform === 'win32') execFileSync('cmd.exe', ['/d', '/c', 'mvnw.cmd', ...args], { cwd: join(root, 'apps/backend'), stdio: 'inherit' });
  else execFileSync('./mvnw', args, { cwd: join(root, 'apps/backend'), stdio: 'inherit' });
  const artifacts = new Map();
  for (const [name, output] of [['backend', 'src/generated/api.d.ts'], ['test-contract', 'test/generated/test-contract.d.ts']]) {
    const schema = normalizeOpenApi(await readFile(join(raw, name + '.openapi.json'), 'utf8'));
    const types = '/** 自动生成：后端OpenAPI → openapi-typescript；禁止手改。 */\n' + astToString(await openapiTS(JSON.parse(schema), { alphabetize: true }));
    artifacts.set('packages/api-contracts/openapi/' + name + '.openapi.json', schema);
    artifacts.set('packages/api-contracts/' + output, types);
    if (name === 'backend') artifacts.set('apps/wechat-miniprogram/miniprogram/types/generated/api.d.ts', miniDeclaration(types));
  }
  // 写入或比较之前完整生成；check从不覆盖仓库文件。
  if (mode === 'check') {
    const changed = await differences(root, artifacts);
    if (changed.length) {
      console.error('契约产物不一致：\n' + changed.map(file => '  ' + file).join('\n'));
      process.exitCode = 1;
    } else console.log('后端schema、两份类型和小程序同步产物一致。此检查不等于完整破坏性变更分析。');
  } else {
    await syncMini(root, artifacts.get('packages/api-contracts/src/generated/api.d.ts'));
    for (const [path, content] of artifacts) {
      if (path.startsWith('apps/')) continue;
      await mkdir(dirname(join(root, path)), { recursive: true });
      await writeFile(join(root, path), content);
    }
    console.log('已生成生产公共契约和单独测试契约；测试路径不导出到Web/小程序公共包。');
  }
  // 生成类型也必须被严格检查；不靠skipLibCheck放过自产声明。
  execFileSync('pnpm', ['--filter', '@pet/api-contracts', 'typecheck'], { cwd: root, stdio: 'inherit' });
} finally {
  await rm(temporary, { recursive: true, force: true });
}
