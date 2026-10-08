import { Layout, Typography } from 'antd';
import { Outlet } from '@tanstack/react-router';

export function SystemLayout() {
  return (
    <Layout className="system-layout">
      <Layout.Header className="system-header"><Typography.Text strong>企业应用工程</Typography.Text></Layout.Header>
      <Layout.Content className="system-content"><Outlet /></Layout.Content>
    </Layout>
  );
}
