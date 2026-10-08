import { Alert, Button, Result } from 'antd';
import type { ErrorComponentProps } from '@tanstack/react-router';
import { Link, useRouter } from '@tanstack/react-router';
import { ErrorNotice } from './api/ErrorNotice';

export function SystemError({ error, reset }: ErrorComponentProps) {
  const router = useRouter();
  const path = router.state.location.pathname;
  const notice = router.options.context?.auth.runtime.notice(path.startsWith('/platform') ? 'PLATFORM' : 'STAFF');
  return <main className="system-content"><Result status="error" title="页面暂时无法加载" subTitle="当前身份未能确认，请重试。" extra={<><Button onClick={() => { reset(); void router.invalidate(); }}>重试</Button> <Link to="/">返回系统入口</Link></>} /><ErrorNotice error={error} />{notice && <Alert type="warning" title={notice} />}</main>;
}
