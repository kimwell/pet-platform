import { Alert, Space, Typography } from 'antd';

export function SystemEntry() {
  return (
    <Space orientation="vertical" size="large" className="system-entry">
      <Typography.Title level={1}>系统工程入口</Typography.Title>
      <Typography.Paragraph>三端工程骨架正在建立，当前页面用于确认 Web 应用入口。</Typography.Paragraph>
      <Alert type="info" title="尚未接入正式功能" description="认证、身份权限、租户隔离、附件和消息能力将在后续任务实现。" showIcon />
    </Space>
  );
}
