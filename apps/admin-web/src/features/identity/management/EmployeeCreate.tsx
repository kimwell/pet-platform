import { useRef, useState } from 'react';
import { useDraftGuard } from './useDraftGuard';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, Modal, Alert } from 'antd';
import type { components } from '@pet/api-contracts';
import type { WebIdentity } from '../../../shared/auth/spaces';
import { hasPermission } from '../../../shared/auth/permissions';
import { useSession } from '../../../shared/auth/useSession';
import { ErrorNotice } from '../../../shared/api/ErrorNotice';
import { passwordError } from '../../account/securityForm';
import { managementErrorMessages, managementNameRule, managementFieldErrors, uncertain } from './api';
import type { MutationResult } from './api';
import { useRouter } from '@tanstack/react-router';
import { employeeDetailHref } from '../users/queries/detailSearch';
export function EmployeeCreate({ identity }: { identity: WebIdentity }) {
  const { auth } = useSession('STAFF', false), client = useQueryClient(), router = useRouter();
  const [closing, setClosing] = useState(false);
  const [open, setOpen] = useState(false), [unknown, setUnknown] = useState(false);
  const [form] = Form.useForm<components['schemas']['CreateEmployee']>();
  const submitting = useRef(false);
  const draftGuard = useDraftGuard(() => open && Object.values(form.getFieldsValue(true)).some(value => value !== undefined && value !== ''));
  const mutation = useMutation({ retry: false, gcTime: 0,
    mutationFn: () => auth.runtime.request<MutationResult>('STAFF', '/admin/identity/users', { method: 'POST', json: form.getFieldsValue(true) }),
    onSuccess: async result => { form.resetFields(); setClosing(true); setOpen(false); await client.invalidateQueries({ predicate: q => q.queryKey[1] === 'identity' });
      if (hasPermission(identity, 'STAFF', 'identity:user:detail')) await router.navigate({ href: employeeDetailHref(result.id) }); },
    onError: error => { form.setFieldValue('initialPassword', undefined); form.setFields(managementFieldErrors(error, ['loginName', 'displayName', 'initialPassword'])); setUnknown(uncertain(error)); },
    onSettled: () => { submitting.current = false; form.setFieldValue('initialPassword', undefined); },
  });
  if (!hasPermission(identity, 'STAFF', 'identity:user:create')) return null;
  const close = async () => { if (mutation.isPending || !await draftGuard.mayDiscard()) return; setClosing(true); form.resetFields(); mutation.reset(); setOpen(false); };
  return <>{draftGuard.contextHolder}<Button type="primary" disabled={closing || mutation.isPending} onClick={() => { form.resetFields(); mutation.reset(); setUnknown(false); setOpen(true); }}>新建员工</Button>
    <Modal title="新建员工" open={open} destroyOnHidden afterClose={() => setClosing(false)} maskClosable={false} onCancel={close} footer={null}>
      {mutation.isError && <ErrorNotice error={mutation.error} messages={managementErrorMessages(mutation.error)} title={unknown ? '创建结果未确认，请关闭并刷新列表核对账号后再操作' : '创建失败'} />}
      <Alert type="info" title="员工以启用状态创建，初次登录必须修改临时密码；请通过安全人工渠道交付临时密码。" />
      <Form form={form} layout="vertical" disabled={mutation.isPending || unknown} onFinish={() => { if (submitting.current || mutation.isPending || unknown) return; submitting.current = true; mutation.mutate(); }}>
        <Form.Item name="loginName" label="账号" rules={[{ required: true, message: '请输入账号' }, { pattern: /^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/, message: '账号须为1至64位字母、数字、点、下划线或连字符' }]}><Input autoComplete="off" /></Form.Item>
        <Form.Item name="displayName" label="姓名" rules={[managementNameRule]}><Input /></Form.Item>
        <Form.Item name="initialPassword" label="临时密码" rules={[{ validator: (_, value: string) => { const error = passwordError(value); return error ? Promise.reject(new Error(error)) : Promise.resolve(); } }]}><Input.Password autoComplete="new-password" /></Form.Item>
        <Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={mutation.isPending || unknown}>创建员工</Button>
      </Form><Button onClick={close} disabled={mutation.isPending}>取消</Button>
    </Modal></>;
}
