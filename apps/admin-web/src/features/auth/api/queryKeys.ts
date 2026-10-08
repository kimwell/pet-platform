import type { QueryKey } from '@tanstack/react-query';
import type { AuthSpace, WebIdentity } from '../../../shared/auth/spaces';

export const authKeys = {
  me: (space: AuthSpace, epoch: number) => [{ principalType: space, sessionEpoch: epoch }, 'auth', 'me', 'current'] as const,
  protected: (identity: WebIdentity, epoch: number, module: string, resource: string, kind: string, params: object = {}) => [
    { principalType: identity.principalType, tenantId: identity.tenantId, principalId: identity.principalId,
      sessionId: identity.sessionId, authorizationVersion: identity.authorizationVersion, sessionEpoch: epoch,
      authorizedStoreIds: [...identity.authorizedStoreIds].sort(), dataScope: identity.dataScope }, module, resource, kind, params,
  ] as const,
  belongsTo: (key: QueryKey, space: AuthSpace) => typeof key[0] === 'object' && key[0] !== null
    && 'principalType' in key[0] && key[0].principalType === space,
};
