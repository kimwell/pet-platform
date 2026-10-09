import { Button } from 'antd';
import { Link } from '@tanstack/react-router';
import type { ReactNode } from 'react';
import { useLayoutState } from '../../stores/app.store';
import { spaces } from '../../utils/auth/spaces';
import type { AuthSpace } from '../../utils/auth/spaces';
export function AppHeader({ space, mobile, children }: { space: AuthSpace; mobile: boolean; children: ReactNode }) {
  const toggle = useLayoutState(state => state.toggle), openMobile = useLayoutState(state => state.openMobile);
  return <header className="basic-header"><div className="header-leading"><Button className="sidebar-toggle" type="text" aria-label={mobile ? '打开导航菜单' : '切换侧边栏'} onClick={() => mobile ? openMobile(space) : toggle(space)}><span className="menu-toggle-icon" aria-hidden="true"><i /><i /><i /></span></Button><Link className="app-logo" to={spaces[space].path} aria-label="爪伴管理平台首页"><span className="app-logo-mark" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M5 10V5l7-3 7 3v5M4 11l8-4 8 4v9l-8 3-8-3zM12 7v16" /></svg></span><span className="app-logo-copy"><strong>爪伴管理平台</strong><small>{spaces[space].title}</small></span></Link></div><div className="header-center" />{children}</header>;
}
