import { readFile,writeFile,cp,mkdir } from 'node:fs/promises';
import { join } from 'node:path';
import openapiTS,{astToString} from 'openapi-typescript';
import {normalizeOpenApi,miniDeclaration,differences} from '../../../../scripts/contract-files.mjs';
const root=process.cwd(), evidence=join(root,'docs/testing/evidence/P05-05');
const artifacts=new Map();
await mkdir(join(evidence,'verify-final-openapi'),{recursive:true});
for(const [name,out] of [['backend','src/generated/api.d.ts'],['test-contract','test/generated/test-contract.d.ts']]){
 const path=join(root,'apps/backend/target/openapi',name+'.openapi.json');
 await cp(path,join(evidence,'verify-final-openapi',name+'.openapi.json'));
 const schema=normalizeOpenApi(await readFile(path,'utf8'));
 const types='/** 自动生成：后端OpenAPI → openapi-typescript；禁止手改。 */\n'+astToString(await openapiTS(JSON.parse(schema),{alphabetize:true}));
 artifacts.set('packages/api-contracts/openapi/'+name+'.openapi.json',schema);
 artifacts.set('packages/api-contracts/'+out,types);
 if(name==='backend')artifacts.set('apps/wechat-miniprogram/miniprogram/types/generated/api.d.ts',miniDeclaration(types));
}
const changed=await differences(root,artifacts);
const result={status:changed.length?'FAIL':'PASS',changed,artifacts:[...artifacts.keys()],scope:'最终clean verify实际导出→重新生成并逐字比较；不覆盖仓库产物，不代替完整破坏性兼容分析'};
await writeFile(join(evidence,'final-contract-snapshot.json'),JSON.stringify(result,null,2)+'\n');
console.log(JSON.stringify(result));if(changed.length)process.exitCode=1;
