import { Button, Card, Descriptions, Space, Typography } from 'antd';
import type { WebIdentity } from '../../shared/auth/spaces';

export function AccountHome({ identity, refreshing, refresh }: { identity: WebIdentity; refreshing: boolean; refresh: () => void }) {
  return <Space orientation="vertical" size="large" className="full-width">
    <div className="page-heading"><div>
      <Typography.Title level={1}>你好，{identity.displayName}</Typography.Title>
      <Typography.Paragraph type="secondary">{identity.principalType === 'STAFF' ? '当前已进入员工工作台' : '当前已进入平台控制台'}</Typography.Paragraph>
    </div><Button loading={refreshing} onClick={refresh}>刷新当前身份</Button></div>
    <Card title="当前身份">
      <Descriptions column={1} items={[
        { key: 'name', label: '姓名', children: identity.displayName },
        { key: 'space', label: '身份空间', children: identity.principalType === 'STAFF' ? '员工' : '平台管理员' },
        ...(identity.principalType === 'STAFF' ? [{ key: 'tenant', label: '当前租户标识', children: identity.tenantId }] : []),
      ]} />
    </Card>
  </Space>;
}
