import { createContext, useCallback, useContext, useEffect, useId, useLayoutEffect, useRef, useState } from 'react';
import type { PropsWithChildren, ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { Modal } from 'antd';

type Panel = { id: string; title?: ReactNode; cancel: () => void; closed?: () => void };
type DialogContext = { container: HTMLDivElement | null; active?: string; register: (panel: Panel) => () => void; update: (id: string, title: ReactNode) => void };
const Context = createContext<DialogContext | null>(null);

/** 一个业务页面共用一层弹框；子操作替换可见内容，保留父详情及其查询生命周期。 */
export function ResourceDialog({ children }: PropsWithChildren) {
  const [panels, setPanels] = useState<Panel[]>([]);
  const [container, setContainer] = useState<HTMLDivElement | null>(null);
  const current = panels.at(-1);
  const register = useCallback((panel: Panel) => {
    setPanels(previous => [...previous.filter(item => item.id !== panel.id), panel]);
    return () => setPanels(previous => previous.filter(item => item.id !== panel.id));
  }, []);
  const update = useCallback((id: string, title: ReactNode) => setPanels(previous => previous.map(item => item.id === id && item.title !== title ? { ...item, title } : item)), []);
  useEffect(() => {
    if (!current || !container) return;
    const frame = requestAnimationFrame(() => {
      const panel = container.querySelector<HTMLElement>('[data-dialog-panel]:not([hidden])');
      const first = panel?.querySelector<HTMLElement>('input:not([disabled]), button:not([disabled]), [tabindex="0"]');
      (first ?? panel)?.focus();
    });
    return () => cancelAnimationFrame(frame);
    // 模式变化时恢复焦点，标题与字段更新不能打断输入。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current?.id, container]);
  return <Context.Provider value={{ container, active: current?.id, register, update }}>
    {children}
    <Modal open={Boolean(current)} title={current?.title} width={900} footer={null} maskClosable={false}
      onCancel={() => current?.cancel()} forceRender>
      <div ref={setContainer} className="resource-dialog-content" />
    </Modal>
  </Context.Provider>;
}

type DialogPanelProps = PropsWithChildren<{ open?: boolean; title?: ReactNode; onCancel: () => void; afterClose?: () => void; destroyOnHidden?: boolean; maskClosable?: boolean; footer?: null }>;
/** 注册业务模式及关闭守卫，内容通过Portal进入同一个官方Modal。 */
export function DialogPanel({ open, title, onCancel, afterClose, children }: DialogPanelProps) {
  const context = useContext(Context);
  const id = useId();
  const callbacks = useRef({ onCancel, afterClose });
  useLayoutEffect(() => { callbacks.current = { onCancel, afterClose }; });
  const register = context?.register, update = context?.update;
  useLayoutEffect(() => {
    if (!open || !register) return;
    const unregister = register({ id, title, cancel: () => callbacks.current.onCancel() });
    return () => { unregister(); callbacks.current.afterClose?.(); };
    // title变化通过下方更新，不能重新排列正在编辑的子模式。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, open, register]);
  useLayoutEffect(() => { if (open) update?.(id, title); }, [id, open, title, update]);
  if (!context) throw new Error('业务弹框必须置于ResourceDialog中');
  return open && context.container ? createPortal(<div data-dialog-panel={id} tabIndex={-1} hidden={context.active !== id}>{children}</div>, context.container) : null;
}
