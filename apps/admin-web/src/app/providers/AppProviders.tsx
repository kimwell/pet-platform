import type { PropsWithChildren } from 'react';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { QueryClientProvider } from '@tanstack/react-query';
import type { Services } from '../../features/auth/api/AuthService';

export function AppProviders({ children, services }: PropsWithChildren<{ services: Services }>) {
  // 本轮只使用页面内Alert，无message/modal上下文调用，因此不添加无用途的App。
  return (
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={services.queryClient}>{children}</QueryClientProvider>
    </ConfigProvider>
  );
}
