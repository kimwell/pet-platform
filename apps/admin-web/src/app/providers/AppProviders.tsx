import { useState } from 'react';
import type { PropsWithChildren } from 'react';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

export function AppProviders({ children }: PropsWithChildren) {
  // 当前没有业务请求；缓存和身份策略在对应任务接入，不预设另一套规则。
  const [queryClient] = useState(() => new QueryClient());
  return (
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </ConfigProvider>
  );
}
