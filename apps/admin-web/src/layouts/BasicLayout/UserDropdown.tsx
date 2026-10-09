import { Avatar, Button, Dropdown } from 'antd';
import { Link } from '@tanstack/react-router';
import { spaces } from '../../utils/auth/spaces';
import type { AuthSpace, WebIdentity } from '../../utils/auth/spaces';
import { canManageSelf } from '../../utils/auth/permissions';
export function UserDropdown({ space, identity, busy, logout }: { space: AuthSpace; identity: WebIdentity; busy: boolean; logout: () => void }) {
  return <Dropdown autoFocus trigger={['click']} menu={{ items: [
    ...(canManageSelf(identity, space, 'logout-all') ? [{ key: 'security', label: <Link to={spaces[space].security}>账号安全</Link> }] : []),
    { key: 'entry', label: <Link to="/">系统入口</Link> },
    { key: 'space', label: <Link to={space === 'STAFF' ? '/platform' : '/admin'}>{space === 'STAFF' ? '平台控制台' : '员工工作台'}</Link> },
    ...(canManageSelf(identity, space, 'logout') ? [{ key: 'logout', label: '退出当前会话', disabled: busy }] : [])], onClick: ({ key }) => { if (key === 'logout' && !busy) logout(); } }}><Button className="user-dropdown-trigger" type="text" aria-label="当前账号菜单"><Avatar size={30}>{identity.displayName.slice(0, 1)}</Avatar><span className="user-dropdown-summary"><strong>{identity.displayName}</strong><small>{spaces[space].label}</small></span><span className="user-dropdown-caret" aria-hidden="true">⌄</span></Button></Dropdown>;
}
