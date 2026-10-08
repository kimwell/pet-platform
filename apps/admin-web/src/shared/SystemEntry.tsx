import { Button, Card, Typography } from 'antd';
import { Link } from '@tanstack/react-router';

export function SystemEntry() {
  return (
    <main className="system-content system-entry">
      <Typography.Title level={1}>企业应用</Typography.Title>
      <Typography.Paragraph type="secondary">选择与你的账号对应的入口</Typography.Paragraph>
      <div className="entry-grid">
        <Card title="员工工作台"><Typography.Paragraph>使用所属租户的员工账号</Typography.Paragraph><Link to="/admin"><Button type="primary">进入员工工作台</Button></Link></Card>
        <Card title="平台控制台"><Typography.Paragraph>使用独立的平台管理员账号</Typography.Paragraph><Link to="/platform"><Button>进入平台控制台</Button></Link></Card>
      </div>
    </main>
  );
}
