import { useRef } from 'react';
import { Modal } from 'antd';
import { useBlocker } from '@tanstack/react-router';
import type { AuthSpace } from '../utils/auth/spaces';
import { useSession } from './useSession';

/** 只处理本模块未保存表单；安全代际改变时直接让出路由，不能阻挡身份清理。 */
export function useDraftGuard(dirty: () => boolean, space: AuthSpace = 'STAFF') {
  const { auth } = useSession(space, false);
  const epoch = useRef(auth.runtime.epoch(space));
  const [modal, contextHolder] = Modal.useModal();
  const current = () => epoch.current === auth.runtime.epoch(space);
  const mayDiscard = async () => !current() || !dirty() || await modal.confirm({
    title: '放弃未保存内容？', content: '离开后将清理本次表单草稿。',
    okText: '放弃内容', cancelText: '继续编辑',
  });
  useBlocker({ shouldBlockFn: async () => !await mayDiscard() && current(), enableBeforeUnload: () => current() && dirty() });
  return { mayDiscard, contextHolder };
}
