import { useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Link, useRouter, useRouteContext } from '@tanstack/react-router';
import { Alert, Avatar, Breadcrumb, Button, Dropdown, Layout, Menu, Space, Spin, Typography } from 'antd';
import { ErrorNotice } from '../../shared/api/ErrorNotice';
import { spaces } from '../../shared/auth/spaces';
import type { AuthSpace } from '../../shared/auth/spaces';
import { useSession } from '../../shared/auth/useSession';
import { AccountHome } from '../../features/account/AccountHome';
import { useLayoutState } from './layoutState';
import { SessionFailure } from '../../shared/auth/SessionFailure';

function SessionLayout({ space }: { space: AuthSpace }) {
  const router = useRouter();
  const guardError = useRouteContext({ strict: false }).sessionError;
  const session = useSession(space, !guardError);
  const collapsed = useLayoutState(state => state.collapsed[space]);
  const toggle = useLayoutState(state => state.toggle);
  const logout = useMutation({
    mutationFn: () => session.auth.logout(space),
    onSuccess: async () => { await router.navigate({ to: spaces[space].login, search: { returnTo: undefined }, replace: true }); await router.invalidate(); },
  });
  useEffect(() => {
    if (session.data === null && !session.busy) void router.invalidate();
  }, [session.data, session.busy, router]);
  if (guardError || session.isError) return <SessionFailure error={guardError ?? session.error} notice={session.notice} retry={guardError ? undefined : () => void session.refetch()} />;
  if (session.busy || session.isPending || session.data === null) return <div className="page-loading" role="status"><Spin /> 正在确认当前会话…</div>;
  const identity = session.data;
  if (!identity) return null;
  return <Layout className={`session-layout ${space.toLowerCase()}`}>
    <Layout.Sider collapsed={collapsed} collapsedWidth={72} width={220} className="session-sider" theme="light">
      <div className="shell-brand">{collapsed ? '企' : spaces[space].title}</div>
      <Menu mode="inline" selectedKeys={['home']} items={[{ key: 'home', label: <Link to={spaces[space].path}>当前身份</Link> }]} />
    </Layout.Sider>
    <Layout>
      <Layout.Header className="session-header">
        <Space><Button aria-label={collapsed ? '展开菜单' : '折叠菜单'} onClick={() => toggle(space)}>{collapsed ? '展开' : '收起'}</Button><Typography.Text>{spaces[space].label}空间</Typography.Text></Space>
        <Dropdown menu={{ items: [{ key: 'logout', label: '退出当前会话', disabled: logout.isPending }], onClick: () => { if (!logout.isPending && !session.auth.runtime.isBusy(space)) logout.mutate(); } }} trigger={['click']}>
          <Button type="text" aria-label="当前账号菜单"><Avatar size="small">{identity.displayName.slice(0, 1)}</Avatar> {identity.displayName}</Button>
        </Dropdown>
      </Layout.Header>
      <Layout.Content className="session-content">
        <Breadcrumb items={[{ title: spaces[space].title }, { title: '当前身份' }]} />
        {session.notice && <Alert showIcon type="warning" title={session.notice} />}
        {logout.isError && <ErrorNotice error={logout.error} title="退出未确认，请重试退出" />}
        <AccountHome identity={identity} refreshing={session.isFetching} refresh={() => void session.refetch()} />
        <div className="system-links"><Link to="/">系统入口</Link><Link to={space === 'STAFF' ? '/platform' : '/admin'}>{space === 'STAFF' ? '平台控制台' : '员工工作台'}</Link></div>
      </Layout.Content>
    </Layout>
  </Layout>;
}
export function StaffLayout() { return <SessionLayout space="STAFF" />; }
export function PlatformLayout() { return <SessionLayout space="PLATFORM" />; }
