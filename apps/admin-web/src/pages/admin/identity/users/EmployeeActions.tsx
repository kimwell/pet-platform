import { DialogPanel as Modal } from '../../../../components/ResourceDialog';
import { useRef, useState } from 'react';
import { useDraftGuard } from '../../../../hooks/useDraftGuard';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Form, Input, Select, Space, Spin, Typography } from 'antd';
import type { components } from '@pet/api-contracts';
import type { WebIdentity } from '../../../../utils/auth/spaces';
import { useSession } from '../../../../hooks/useSession';
import { hasPermission } from '../../../../utils/auth/permissions';
import { ErrorNotice } from '../../../../components/ErrorNotice/index';
import { ApiError } from '../../../../api/ApiError';
import { passwordError } from '../../../account/securityForm';
import { managementOptions, assignmentOptions, managementErrorMessages, managementNameRule, managementFieldErrors, requiresRecheck } from '../../../../api/identity/management';
import type { MutationResult } from '../../../../api/identity/management';
type Action = 'edit' | 'status' | 'roles' | 'stores' | 'password' | 'revoke-sessions';
type Draft = components['schemas']['EditEmployee'] & Partial<components['schemas']['EmployeeAssignments'] & components['schemas']['ChangeStatus'] & components['schemas']['ResetPasswordInput']>;
export function EmployeeActions({ identity, employeeId }: { identity: WebIdentity; employeeId: string }) {
  const { auth } = useSession('STAFF', false), client = useQueryClient();
  const management = useQuery(managementOptions(auth, identity, employeeId));
  const [closing, setClosing] = useState(false);
  const [action, setAction] = useState<Action>(), [unknown, setUnknown] = useState(false), [notice, setNotice] = useState<string>();
  const [form] = Form.useForm<Draft>(), submitting = useRef(false);
  const initial = useRef(''), versionAtOpen = useRef<string>(undefined);
  const draftGuard = useDraftGuard(() => Boolean(action) && JSON.stringify(form.getFieldsValue(true)) !== initial.current);
  const roles = useQuery({ ...assignmentOptions(auth, identity, false), enabled: action === 'roles' && hasPermission(identity, 'STAFF', 'identity:role:list') });
  const stores = useQuery({ ...assignmentOptions(auth, identity, true), enabled: action === 'stores' && hasPermission(identity, 'STAFF', 'platform:store:list') });
  const data = management.isError ? undefined : management.data;
  const mutation = useMutation({ retry: false, gcTime: 0, mutationFn: async () => {
    const input = form.getFieldsValue(true);
    const version = versionAtOpen.current;
    if (!version || !action) throw new ApiError('PROTOCOL', '请先重新读取员工版本');
    const suffix = action === 'edit' ? '' : action === 'revoke-sessions' ? '/revoke-sessions' : `/${action}`;
    const body = action === 'edit' ? { version, displayName: input.displayName } : action === 'status' ? { version, status: input.status }
      : ['roles', 'stores'].includes(action) ? { version, ids: input.ids ?? [] } : action === 'password' ? { version, currentPassword: input.currentPassword, newPassword: input.newPassword } : { version, currentPassword: input.currentPassword };
    return auth.runtime.request<MutationResult | null>('STAFF', `/admin/identity/users/${employeeId}${suffix}`, { method: action === 'revoke-sessions' ? 'POST' : 'PUT', json: body });
  }, onSuccess: async result => { setClosing(true); setAction(undefined); form.resetFields(); setNotice(result && !result.sessionCleanupComplete ? '操作成功，旧身份已失效，会话物理清理等待重试。' : '操作成功。'); await client.invalidateQueries({ predicate: q => q.queryKey[1] === 'identity' }); },
  onError: error => { form.setFields(managementFieldErrors(error, ['displayName', 'ids', 'status', 'currentPassword', 'newPassword'])); setUnknown(requiresRecheck(error)); },
  onSettled: () => { submitting.current = false; form.setFieldsValue({ currentPassword: undefined, newPassword: undefined }); }, });
  const busy = mutation.isPending || management.isFetching || closing;
  const can = (p: string) => hasPermission(identity, 'STAFF', p);
  const self = identity.principalId === employeeId;
  const begin = (next: Action) => { if (!data || busy) return; form.resetFields(); form.setFieldsValue({ displayName: data.displayName, ids: next === 'roles' ? data.roleIds : data.storeIds, status: data.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' }); initial.current = JSON.stringify(form.getFieldsValue(true)); versionAtOpen.current = data.version; mutation.reset(); setUnknown(false); setAction(next); };
  const close = async () => { if (mutation.isPending || !await draftGuard.mayDiscard()) return; setClosing(true); setAction(undefined); form.resetFields(); mutation.reset(); };
  const titles = { edit: '编辑员工资料', status: '变更员工状态', roles: '配置员工角色', stores: '配置员工门店', password: '重置临时密码', 'revoke-sessions': '撤销全部会话' };
  const options = action === 'roles' ? roles : stores;
  return <div className="identity-actions">{draftGuard.contextHolder}
    {notice && <Alert type="success" title={notice} closable onClose={() => setNotice(undefined)} />}
    {management.isPending && <Spin size="small" />}
    {management.isError && <><ErrorNotice error={management.error} messages={managementErrorMessages(management.error)} title="管理资料不可用" /><Button onClick={() => void management.refetch()}>重新读取管理资料</Button></>}
    {data && <><Typography.Paragraph type="secondary">资源版本：{data.version}；{data.passwordChangeRequired ? '需要首次改密' : '已完成凭据初始化'}{data.protectedAccount ? '；受保护管理员' : ''}</Typography.Paragraph><Space wrap>
      {can('identity:user:update') && <Button disabled={busy} onClick={() => begin('edit')}>编辑资料</Button>}
      {!data.protectedAccount && !self && <>
        {can(data.status === 'ACTIVE' ? 'identity:user:disable' : 'identity:user:enable') && <Button disabled={busy} danger={data.status === 'ACTIVE'} onClick={() => begin('status')}>{data.status === 'ACTIVE' ? '停用员工' : '启用员工'}</Button>}
        {can('identity:user:roles') && can('identity:role:list') && <Button disabled={busy} onClick={() => begin('roles')}>配置角色</Button>}
        {can('identity:user:stores') && can('platform:store:list') && <Button disabled={busy} onClick={() => begin('stores')}>配置门店</Button>}
        {data.status === 'ACTIVE' && can('identity:user:reset-password') && <Button disabled={busy} onClick={() => begin('password')}>重置密码</Button>}
        {data.status === 'ACTIVE' && can('identity:user:revoke-sessions') && <Button disabled={busy} onClick={() => begin('revoke-sessions')}>撤销全部会话</Button>}
      </>}</Space></>}
    <Modal title={action ? titles[action] : ''} open={Boolean(action)} destroyOnHidden afterClose={() => setClosing(false)} maskClosable={false} onCancel={close} footer={null}>
      {mutation.isError && <ErrorNotice error={mutation.error} messages={managementErrorMessages(mutation.error)} title={unknown ? '授权受限、版本冲突或操作结果未确认，请重新读取并核对' : '操作失败'} />}
      {unknown && <Button onClick={async () => { const result = await management.refetch(); if (!result.isError && result.data) { versionAtOpen.current = result.data.version; setUnknown(false); mutation.reset(); } }}>重新读取版本并保留非敏感草稿</Button>}
      <Form form={form} layout="vertical" disabled={mutation.isPending || unknown || management.isFetching} onFinish={() => { if (submitting.current || unknown || busy || (['roles', 'stores'].includes(action ?? '') && (options.isPending || options.isError))) return; submitting.current = true; mutation.mutate(); }}>
        {action === 'edit' && <Form.Item name="displayName" label="姓名" rules={[managementNameRule]}><Input /></Form.Item>}
        {action === 'status' && <><Alert type="warning" title="状态变化将使员工旧身份失效。" /><Form.Item name="status" label="目标状态" rules={[{ required: true }]}><Select options={[{ value: 'ACTIVE', label: '启用' }, { value: 'DISABLED', label: '停用' }]} /></Form.Item></>}
        {(action === 'roles' || action === 'stores') && <>
          {options.isError && <ErrorNotice error={options.error} messages={managementErrorMessages(options.error)} title="授权选项加载失败" />}
          <Form.Item name="ids" label={action === 'roles' ? '角色' : '门店'}><Select mode="multiple" loading={options.isFetching} disabled={options.isPending || options.isError || mutation.isPending || unknown} optionFilterProp="label" options={(options.data ?? []).map(item => ({ value: item.id, label: `${item.name}（${item.code}）`, disabled: 'protectedRole' in item && (item.protectedRole || item.status !== 'ACTIVE') }))} /></Form.Item>
          <Typography.Paragraph type="secondary">完整替换所选关联；清空会撤销全部关联。保存后员工须重新登录。</Typography.Paragraph>
        </>}
        {(action === 'password' || action === 'revoke-sessions') && <><Alert type="warning" title="需要操作者当前密码确认，员工全部旧会话将失效。" /><Form.Item name="currentPassword" label="你的当前密码" rules={[{ required: true, message: '请输入当前密码' }]}><Input.Password autoComplete="current-password" /></Form.Item></>}
        {action === 'password' && <Form.Item name="newPassword" label="员工临时密码" rules={[{ validator: (_, value: string) => { const error = passwordError(value); return error ? Promise.reject(new Error(error)) : Promise.resolve(); } }]}><Input.Password autoComplete="new-password" /></Form.Item>}
        <Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={unknown || busy || (['roles', 'stores'].includes(action ?? '') && (options.isPending || options.isError))}>保存</Button>
      </Form><Button disabled={mutation.isPending} onClick={close}>取消</Button>
    </Modal>
  </div>;
}
