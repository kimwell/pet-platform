import { Alert, Button, Result } from 'antd';
import { Link, useRouter } from '@tanstack/react-router';
import { ErrorNotice } from '../api/ErrorNotice';

export function SessionFailure({ error, notice, retry }: { error: unknown; notice?: string; retry?: () => void }) {
  const router = useRouter();
  return <main className="system-content"><Result status="error" title="当前身份暂时无法确认" subTitle="会话未被视为退出，请重试。" extra={<><Button onClick={retry ?? (() => { void router.invalidate(); })}>重试</Button> <Link to="/">返回系统入口</Link></>} /><ErrorNotice error={error} />{notice && <Alert type="warning" title={notice} />}</main>;
}
