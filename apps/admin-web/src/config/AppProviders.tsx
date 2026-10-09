import type { PropsWithChildren } from 'react';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { QueryClientProvider } from '@tanstack/react-query';
import type { Services } from '../api/auth/AuthService';

export function AppProviders({ children, services }: PropsWithChildren<{ services: Services }>) {
  // 页面通知与确认在各自contextHolder中展示，统一提供主题和Query上下文。
  return (
    <ConfigProvider locale={zhCN} theme={{ token: { borderRadius: 6, colorBgLayout: '#f4f6f9', colorPrimary: '#1677ff', colorText: '#172033', colorTextSecondary: '#667085' }, components: { Table: { headerBg: '#f7f8fa', headerColor: '#344054' } } }}>
      <QueryClientProvider client={services.queryClient}>{children}</QueryClientProvider>
    </ConfigProvider>
  );
}
