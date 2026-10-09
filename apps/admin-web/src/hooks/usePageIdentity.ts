import { createContext, useContext } from 'react';
import type { WebIdentity } from '../utils/auth/spaces';
export const PageIdentityContext = createContext<WebIdentity | null>(null);
export function usePageIdentity() {
  const identity = useContext(PageIdentityContext);
  if (!identity) throw new Error('页面需要经过验证的身份');
  return identity;
}
