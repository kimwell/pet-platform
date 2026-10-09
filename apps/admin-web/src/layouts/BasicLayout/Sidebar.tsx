import { Drawer, Layout, Menu } from 'antd';
import { Link, useLocation } from '@tanstack/react-router';
import { useLayoutState } from '../../stores/app.store';
import type { AuthSpace, WebIdentity } from '../../utils/auth/spaces';
import { navigationFor } from '../../router/pageAccess';
const iconPaths: Record<string, string> = {
  users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M17 4a4 4 0 0 1 0 8M22 21v-2a4 4 0 0 0-3-3.9',
  roles: 'M12 3l8 4v5c0 5-8 9-8 9s-8-4-8-9V7zM8 12l3 3 5-6',
  organizations: 'M9 2h6v5H9zM2 17h6v5H2zM16 17h6v5h-6zM9 17h6v5H9zM12 7v10M5 17v-5h14v5',
  tenants: 'M4 21V3h12v18M16 9h4v12M8 7h4M8 11h4M8 15h4M2 21h20',
  accounts: 'M4 3h16v18H4zM9 7h6M8 16c0-4 8-4 8 0M12 12a2 2 0 1 0 0-4 2 2 0 0 0 0 4',
  security: 'M12 3l8 4v5c0 5-8 9-8 9s-8-4-8-9V7zM12 9v6M12 18h.01',
  home: 'M3 11l9-8 9 8M5 9v12h14V9M9 21v-7h6v7',
};
function NavigationIcon({ path }: { path: string }) {
  return <svg className="navigation-icon" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><path d={iconPaths[path.split('/').pop() ?? 'home'] ?? iconPaths.home} /></svg>;
}
export function Sidebar({ space, identity, mobile }: { space: AuthSpace; identity: WebIdentity; mobile: boolean }) {
  const location = useLocation(), collapsed = useLayoutState(state => state.collapsed[space]);
  const open = useLayoutState(state => state.mobileOpen[space]), close = useLayoutState(state => state.closeMobile);
  const items = navigationFor(space, identity);
  const selected = [...items].sort((a, b) => b.path.length - a.path.length).find(item => location.pathname === item.path || location.pathname.startsWith(item.path + '/'));
  const menu = <nav className="sidebar-navigation" aria-label="主导航"><Menu mode="inline" selectedKeys={selected ? [selected.path] : []} onClick={() => close(space)} items={items.map(item => ({ key: item.path, icon: <NavigationIcon path={item.path} />, label: <Link to={item.path}>{item.title}</Link> }))} /></nav>;
  return mobile ? <Drawer open={open} onClose={() => close(space)} placement="left" title={space === 'STAFF' ? '员工导航' : '平台导航'} size="min(88vw, 320px)" styles={{ body: { padding: 0 } }}>{menu}</Drawer> : <Layout.Sider className="basic-sidebar" theme="light" collapsed={collapsed} width={240} collapsedWidth={64} trigger={null}>{menu}</Layout.Sider>;
}
