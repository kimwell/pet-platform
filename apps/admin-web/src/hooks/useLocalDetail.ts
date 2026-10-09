import { useEffect, useState } from 'react';
import { useLocation, useRouter } from '@tanstack/react-router';

/** 旧详情地址只消费一次跳转线索；弹框本身不持久化、不建立历史条目。 */
export function useLocalDetail(resource: string) {
  const router = useRouter(), location = useLocation();
  const [id, setId] = useState<string>(() => location.state.legacyModal?.resource === resource ? location.state.legacyModal.id : '');
  useEffect(() => {
    if (location.state.legacyModal) {
      // 消费路由系统传入的新旧链接线索；同页SPA跳转也应打开一次。
      // eslint-disable-next-line react-hooks/set-state-in-effect
      if (location.state.legacyModal.resource === resource) setId(location.state.legacyModal.id);
      router.history.replace(location.href, { ...location.state, legacyModal: undefined });
    }
  }, [location.href, location.state, resource, router]);
  return [id, setId] as const;
}
declare module '@tanstack/react-router' {
  interface HistoryState { legacyLinkNotice?: string; legacyModal?: { resource: string; id: string } }
}
