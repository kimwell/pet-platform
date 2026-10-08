import { create } from 'zustand';
import type { AuthSpace } from '../../shared/auth/spaces';

// 侧栏折叠需在布局重新加载/导航期间保留；不存服务端身份或任何凭据。
export const useLayoutState = create<{
  collapsed: Record<AuthSpace, boolean>;
  toggle: (space: AuthSpace) => void;
  reset: (space: AuthSpace) => void;
}>(set => ({
  collapsed: { STAFF: false, PLATFORM: false },
  toggle: space => set(state => ({ collapsed: { ...state.collapsed, [space]: !state.collapsed[space] } })),
  reset: space => set(state => ({ collapsed: { ...state.collapsed, [space]: false } })),
}));
