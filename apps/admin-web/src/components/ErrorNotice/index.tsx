import { Alert, Typography } from 'antd';
import { ApiError, errorText } from '../../api/ApiError';

export function ErrorNotice({ error, title, messages = [] }: { error: unknown; title?: string; messages?: string[] }) {
  if (error instanceof ApiError && error.kind === 'CANCELLED') return null;
  return <Alert showIcon type="error" title={title ?? errorText(error)} description={
    <>{messages.map((message, index) => <div key={index}>{message}</div>)}{error instanceof ApiError && error.traceId && <Typography.Text copyable={{ text: error.traceId }}>排错编号：{error.traceId}</Typography.Text>}</>
  } />;
}
