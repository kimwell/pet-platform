import { Result } from 'antd';
import { Link } from '@tanstack/react-router';

export function SystemNotFound() {
  return <Result status="404" title="页面不存在" subTitle="请检查访问地址。" extra={<Link to="/">返回系统入口</Link>} />;
}
