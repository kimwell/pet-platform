import { Button, Result } from 'antd';
import type { ErrorComponentProps } from '@tanstack/react-router';

export function SystemError({ reset }: ErrorComponentProps) {
  return <Result status="error" title="页面暂时无法加载" subTitle="请重试，若问题持续请联系维护人员。" extra={<Button onClick={reset}>重试</Button>} />;
}
