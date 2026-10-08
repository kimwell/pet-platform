import {readFile,writeFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import {join} from 'node:path';
import openapiTS,{astToString} from 'openapi-typescript';
import {normalizeOpenApi,miniDeclaration} from '../../../../scripts/contract-files.mjs';
const root=fileURLToPath(new URL('../../../../',import.meta.url));const dir=fileURLToPath(new URL('.',import.meta.url));const checks=[];
for(const [name,type] of [['backend','src/generated/api.d.ts'],['test-contract','test/generated/test-contract.d.ts']]){
 const schema=normalizeOpenApi(await readFile(join(dir,'verify-final-reports/openapi/'+name+'.openapi.json'),'utf8'));
 const types='/** 自动生成：后端OpenAPI → openapi-typescript；禁止手改。 */\n'+astToString(await openapiTS(JSON.parse(schema),{alphabetize:true}));
 for(const [path,value] of [['packages/api-contracts/openapi/'+name+'.openapi.json',schema],['packages/api-contracts/'+type,types],...(name==='backend'?[['apps/wechat-miniprogram/miniprogram/types/generated/api.d.ts',miniDeclaration(types)]]:[])]){
  checks.push({path,equal:await readFile(join(root,path),'utf8')===value});
 }
}
await writeFile(join(dir,'contracts-final-snapshot-result.json'),JSON.stringify(checks,null,2));console.log(JSON.stringify(checks,null,2));if(checks.some(c=>!c.equal))process.exitCode=1;
