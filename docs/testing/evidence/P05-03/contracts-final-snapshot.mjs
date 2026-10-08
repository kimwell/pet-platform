// 使用最终clean verify真实导出比对五份产物；不清理或重写已验证JAR。
import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';
import { createHash } from 'node:crypto';
import openapiTS, { astToString } from 'openapi-typescript';
import { normalizeOpenApi, miniDeclaration } from '../../../../scripts/contract-files.mjs';

const root = fileURLToPath(new URL('../../../../', import.meta.url));
const comparisons = [];
for (const [name, output] of [['backend', 'src/generated/api.d.ts'], ['test-contract', 'test/generated/test-contract.d.ts']]) {
  const schema = normalizeOpenApi(await readFile(join(root, 'apps/backend/target/openapi/' + name + '.openapi.json'), 'utf8'));
  const types = '/** 自动生成：后端OpenAPI → openapi-typescript；禁止手改。 */\n' + astToString(await openapiTS(JSON.parse(schema), { alphabetize: true }));
  const pairs = [
    ['packages/api-contracts/openapi/' + name + '.openapi.json', schema],
    ['packages/api-contracts/' + output, types],
  ];
  if (name === 'backend') pairs.push(['apps/wechat-miniprogram/miniprogram/types/generated/api.d.ts', miniDeclaration(types)]);
  for (const [path, value] of pairs) {
    comparisons.push({ path, matches: value === await readFile(join(root, path), 'utf8'), sha256: createHash('sha256').update(value).digest('hex') });
  }
}
const result = { cwd: root, argv: ['node', 'docs/testing/evidence/P05-03/contracts-final-snapshot.mjs'], exitCode: comparisons.every(item => item.matches) ? 0 : 1, comparisons };
await writeFile(new URL('contracts-final-snapshot-result.json', import.meta.url), JSON.stringify(result, null, 2));
console.log(JSON.stringify(result, null, 2));
process.exitCode = result.exitCode;
