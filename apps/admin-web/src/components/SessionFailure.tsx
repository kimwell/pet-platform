import { Alert, Button, Result } from 'antd';
import { Link, useRouter } from '@tanstack/react-router';
import { ErrorNotice } from './ErrorNotice/index';
import { ApiError } from '../api/ApiError';

export function SessionFailure({ error, notice, retry }: { error: unknown; notice?: string; retry?: () => void }) {
  const router = useRouter();
  if (error instanceof ApiError && error.code === 'PERMISSION_DENIED') return <main className="system-content"><Result status="403" title="当前账号没有访问权限" subTitle="请返回可访问的页面，或重新确认当前身份。" extra={<><Button onClick={retry ?? (() => { void router.invalidate(); })}>重新确认身份</Button> <Link to="/">返回系统入口</Link></>} /></main>;
  return <main className="system-content"><Result status="error" title="当前身份暂时无法确认" subTitle="会话未被视为退出，请重试。" extra={<><Button onClick={retry ?? (() => { void router.invalidate(); })}>重试</Button> <Link to="/">返回系统入口</Link></>} /><ErrorNotice error={error} />{notice && <Alert type="warning" title={notice} />}</main>;
}
