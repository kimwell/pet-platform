import { useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Outlet, useRouter, useRouteContext, useLocation } from '@tanstack/react-router';
import { Alert, Grid, Layout, Spin, Typography } from 'antd';
import { ErrorNotice } from '../../components/ErrorNotice';
import { spaces } from '../../utils/auth/spaces';
import type { AuthSpace } from '../../utils/auth/spaces';
import { useSession } from '../../hooks/useSession';
import { useLayoutState } from '../../stores/app.store';
import { SessionFailure } from '../../components/SessionFailure';
import { isRestricted } from '../../utils/auth/permissions';
import { canAccessPage, pageFor } from '../../router/pageAccess';
import { AppHeader } from './Header';
import { Sidebar } from './Sidebar';
import { AppBreadcrumb } from './Breadcrumb';
import { UserDropdown } from './UserDropdown';
import { PageIdentityContext } from '../../hooks/usePageIdentity';
import { ResourceDialog } from '../../components/ResourceDialog';
function SessionLayout({ space }: { space: AuthSpace }) {
  const router = useRouter();
  const location = useLocation();
  const page = pageFor(space, location.pathname);
  const guardError = useRouteContext({ strict: false }).sessionError;
  const session = useSession(space, !guardError);
  const refetchSession = session.refetch;
  const runtime = session.auth.runtime;
  useEffect(() => {
    // Query既有可见性重验之外，覆盖窗口重新聚焦；只重验当前保护空间。
    const revalidate = () => { if (document.visibilityState === 'visible' && !runtime.isBusy(space)) void refetchSession(); };
    window.addEventListener('focus', revalidate);
    return () => window.removeEventListener('focus', revalidate);
  }, [space, runtime, refetchSession]);
  const collapsed = useLayoutState(state => state.collapsed[space]);
  const mobile = Grid.useBreakpoint().md === false;
  const logout = useMutation({
    mutationFn: () => session.auth.logout(space),
    onSuccess: async () => { await router.navigate({ to: spaces[space].login, search: { returnTo: undefined }, replace: true }); await router.invalidate(); },
  });
  useEffect(() => {
    if (!session.busy && (session.data === null || (session.data && !canAccessPage(page, session.data)))) void router.invalidate();
  }, [session.data, session.busy, page, router]);
  if (guardError || session.isError) return <SessionFailure error={guardError ?? session.error} notice={session.notice} retry={guardError ? undefined : () => void session.refetch()} />;
  if (session.isPending || session.data === null) return <div className="page-loading" role="status"><Spin /> 正在确认当前会话…</div>;
  const identity = session.data;
  if (!identity) return null;
  if (!canAccessPage(page, identity)) return <div className="page-loading" role="status"><Spin /> 正在确认页面访问条件…</div>;
  return <Layout className={`app-shell ${space.toLowerCase()}`}>
    <AppHeader space={space} mobile={mobile}><UserDropdown space={space} identity={identity} busy={logout.isPending || session.busy} logout={() => { if (!runtime.isBusy(space)) logout.mutate(); }} /></AppHeader>
    <Sidebar space={space} identity={identity} mobile={mobile} />
    <main className={`content-wrapper${collapsed ? ' content-wrapper-collapsed' : ''}${mobile ? ' content-wrapper-mobile' : ''}`}>
      <div className="content-scroll" id="main-content" tabIndex={-1}><div className="basic-content">
        <AppBreadcrumb space={space} path={location.pathname} />
        {location.state.legacyLinkNotice && <Alert showIcon type="warning" title={location.state.legacyLinkNotice} />}
        {session.notice && !logout.isError && <Alert showIcon type="warning" title={session.notice} />}
        {logout.isError && <ErrorNotice error={logout.error} title={session.notice ?? '退出未确认，请重试退出'} />}
        <PageIdentityContext.Provider value={identity}><ResourceDialog key={JSON.stringify([identity.principalId, runtime.epoch(space), identity.authorizationVersion, identity.sessionId])}><Outlet /></ResourceDialog></PageIdentityContext.Provider>
        {isRestricted(identity) && <Typography.Text type="secondary">请完成密码修改后继续使用工作台。</Typography.Text>}
      </div></div>
    </main>
  </Layout>;
}
export function StaffLayout() { return <SessionLayout space="STAFF" />; }
export function PlatformLayout() { return <SessionLayout space="PLATFORM" />; }
