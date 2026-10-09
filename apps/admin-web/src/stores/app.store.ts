import { create } from 'zustand';
import type { AuthSpace } from '../utils/auth/spaces';
// 仅保留各空间的客户端导航状态，不存身份、权限或凭据。
export const useLayoutState = create<{
  collapsed: Record<AuthSpace, boolean>; mobileOpen: Record<AuthSpace, boolean>;
  toggle: (space: AuthSpace) => void; openMobile: (space: AuthSpace) => void; closeMobile: (space: AuthSpace) => void; reset: (space: AuthSpace) => void;
}>(set => ({
  collapsed: { STAFF: false, PLATFORM: false }, mobileOpen: { STAFF: false, PLATFORM: false },
  toggle: space => set(state => ({ collapsed: { ...state.collapsed, [space]: !state.collapsed[space] } })),
  openMobile: space => set(state => ({ mobileOpen: { ...state.mobileOpen, [space]: true } })),
  closeMobile: space => set(state => ({ mobileOpen: { ...state.mobileOpen, [space]: false } })),
  reset: space => set(state => ({ collapsed: { ...state.collapsed, [space]: false }, mobileOpen: { ...state.mobileOpen, [space]: false } })),
}));
