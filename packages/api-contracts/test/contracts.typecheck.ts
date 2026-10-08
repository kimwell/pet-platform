import type { components as Production } from '../src/generated/api';
import type { components as TestContract } from './generated/test-contract';

type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Assert<T extends true> = T;
type Scalar = NonNullable<TestContract['schemas']['SuccessScalarOutput']['data']>;
type Page = NonNullable<TestContract['schemas']['SuccessPageResponseScalarOutput']['data']>;
// 由tsc拒绝金额/total/version数值化、丢失泛型、必填可空与可省略混淆。
export type ContractAssertions = [
  Assert<Equal<Production['schemas']['PageResponseFieldErrorDetail']['total'], string>>,
  Assert<Equal<Production['schemas']['SuccessVoid']['data'], null>>,
  Assert<Equal<Production['schemas']['Failure']['success'], false>>,
  Assert<Equal<Scalar['amount'], string>>,
  Assert<Equal<Scalar['version'], string>>,
  Assert<Equal<Scalar['id'], string>>,
  Assert<Equal<Scalar['occurredAt'], string>>,
  Assert<Equal<Scalar['date'], string>>,
  Assert<Equal<Scalar['state'], 'OPEN' | 'CLOSED'>>,
  Assert<Equal<Scalar['requiredNullable'], string | null>>,
  Assert<Equal<Scalar['optional'], string | undefined>>,
  Assert<Equal<Scalar['enabled'], boolean>>,
  Assert<Equal<Scalar['ratio'], number>>,
  Assert<Equal<Scalar['progress'], number>>,
  Assert<Equal<Page['items'][number], Scalar>>,
  Assert<Equal<Page['total'], string>>
];
// @ts-expect-error 错误分支不拥有data字段。
export type FailureHasNoData = Production['schemas']['Failure']['data'];
