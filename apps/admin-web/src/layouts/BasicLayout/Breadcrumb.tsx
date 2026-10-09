import { Breadcrumb } from 'antd';
import { Link } from '@tanstack/react-router';
import { spaces } from '../../utils/auth/spaces';
import type { AuthSpace } from '../../utils/auth/spaces';
import { pageFor } from '../../router/pageAccess';
export function AppBreadcrumb({ space, path }: { space: AuthSpace; path: string }) {
  const page = pageFor(space, path), home = path.replace(/\/$/, '') === spaces[space].path;
  return <div className="basic-breadcrumb"><Breadcrumb items={[{ title: <Link to={spaces[space].path}>首页</Link> }, ...(!home ? [{ title: page.title }] : [])]} /></div>;
}
