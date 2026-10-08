import type { components } from "./contract.generated";
type Equal<A,B> = (<T>()=>T extends A?1:2) extends (<T>()=>T extends B?1:2) ? true : false;
type Assert<T extends true> = T;
type F = components["schemas"]["TypeFixture"];
type AmountString = Assert<Equal<F["amount"],string>>;
type IdString = Assert<Equal<F["id"],string>>;
type VersionString = Assert<Equal<F["version"],string>>;
type DateTimeString = Assert<Equal<F["occurredAt"],string>>;
type Nullable = Assert<Equal<F["nullableName"],string|null>>;
type Optional = Assert<Equal<F["optionalName"],string|undefined>>;
type Enum = Assert<Equal<F["status"],"TEMPORARY"|"BOUND">>;
function identityGuard(me:components["schemas"]["CurrentIdentity"]) {
 if(me.principalType==="PLATFORM") { const tenant:null=me.tenantId; const scope:null=me.dataScope; return [tenant,scope]; }
 const tenant:string=me.tenantId; return [tenant,me.dataScope.grants];
}
// @ts-expect-error 金额不得是JavaScript浮点number
const invalidAmount:F["amount"]=12.3;
// @ts-expect-error required nullable字段不能用undefined替代null
const invalidNull:F["nullableName"]=undefined;
export type Checks=[AmountString,IdString,VersionString,DateTimeString,Nullable,Optional,Enum];
export {identityGuard,invalidAmount,invalidNull};
