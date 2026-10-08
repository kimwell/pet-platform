import { useEffect, useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useRouter } from '@tanstack/react-router';
import { Alert, Button, Card, Descriptions, Form, Input, Space, Typography } from 'antd';
import { ApiError } from '../../shared/api/ApiError';
import { ErrorNotice } from '../../shared/api/ErrorNotice';
import { canManageSelf, isRestricted } from '../../shared/auth/permissions';
import type { WebIdentity } from '../../shared/auth/spaces';
import { spaces } from '../../shared/auth/spaces';
import { confirmationError, passwordError, securityErrorText, securityFieldErrors, securityUnmappedErrors } from './securityForm';
import type { PasswordForm } from './securityForm';

function SecurityForm({ identity, operation, disabled }: { identity: WebIdentity; operation: 'password' | 'logout-all'; disabled: boolean }) {
  const router = useRouter(); const { auth } = router.options.context!; const space = identity.principalType;
  const [form] = Form.useForm<PasswordForm>(); const submitting = useRef(false); const mounted = useRef(true);
  const [failure, setFailure] = useState<ApiError>();
  const mutation = useMutation({
    retry: false, gcTime: 0,
    // Mutation 变量始终 undefined；表单密码不进入长期 Mutation 缓存。
    mutationFn: async () => {
      const fields = form.getFieldsValue();
      try {
        await auth.secureSelf(space, operation, operation === 'password'
          ? { currentPassword: fields.currentPassword, newPassword: fields.newPassword }
          : { currentPassword: fields.currentPassword });
      } finally {
        fields.currentPassword = ''; fields.newPassword = ''; fields.confirmation = '';
        form.resetFields();
      }
    },
  });
  const resetMutation = mutation.reset;
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; form.resetFields(); resetMutation(); }; }, [form, resetMutation]);
  const submit = async () => {
    if (submitting.current || disabled || mutation.isPending || auth.runtime.isBusy(space)) return;
    submitting.current = true; setFailure(undefined);
    try {
      await mutation.mutateAsync();
      // 导航由 beforeLoad 统一重验身份；成功通知由 runtime 单一持有。
      await router.invalidate();
    } catch (error) {
      if (mounted.current) { setFailure(error instanceof ApiError ? error : new ApiError('PROTOCOL', '请求结果未确认')); form.setFields(securityFieldErrors(error)); }
    } finally { submitting.current = false; mutation.reset(); }
  };
  const passwordRule = { validator: (_: unknown, value: string | undefined) => { const error = passwordError(value); return error ? Promise.reject(new Error(error)) : Promise.resolve(); } };
  const uncertain = failure instanceof ApiError && (['NETWORK', 'TIMEOUT', 'PROTOCOL', 'CANCELLED'].includes(failure.kind) || (failure.status ?? 0) >= 500);
  return <Space orientation="vertical" className="full-width" size="middle">
    {failure && !uncertain && <ErrorNotice error={failure} title={securityErrorText(failure)} messages={securityUnmappedErrors(failure)} />}
    {uncertain && <Button onClick={() => { void auth.current(space).then(value => { if (value && mounted.current) setFailure(undefined); return router.invalidate(); }).catch(() => { /* 保持未确认状态。 */ }); }}>重新确认当前身份</Button>}
    <Form form={form} name={`${space.toLowerCase()}-${operation}`} layout="vertical" requiredMark={false} clearOnDestroy disabled={disabled || mutation.isPending} onFinish={() => void submit()}>
      <Form.Item name="currentPassword" label="当前密码" rules={[passwordRule]}><Input.Password autoComplete="current-password" visibilityToggle={false} maxLength={256} /></Form.Item>
      {operation === 'password' && <>
        <Form.Item name="newPassword" label="新密码" rules={[passwordRule]} extra="12至128个字符，保留空格和大小写"><Input.Password autoComplete="new-password" visibilityToggle={false} maxLength={256} /></Form.Item>
        <Form.Item name="confirmation" label="确认新密码" dependencies={['newPassword']} rules={[{ required: true, message: '请再次输入新密码' }, { validator: (_, value: string | undefined) => { const error = confirmationError(form.getFieldValue('newPassword'), value); return error ? Promise.reject(new Error(error)) : Promise.resolve(); } }]}><Input.Password autoComplete="new-password" visibilityToggle={false} maxLength={256} /></Form.Item>
      </>}
      <Button type={operation === 'password' ? 'primary' : 'default'} danger={operation === 'logout-all'} htmlType="submit" loading={mutation.isPending} disabled={uncertain}>{operation === 'password' ? '修改密码并重新登录' : '确认退出全部设备'}</Button>
    </Form>
  </Space>;
}
export function AccountSecurity({ identity, disabled, refreshing, refresh }: { identity: WebIdentity; disabled: boolean; refreshing: boolean; refresh: () => void }) {
  const space = identity.principalType;
  return <Space orientation="vertical" size="large" className="full-width security-page">
    <div className="page-heading"><div><Typography.Title level={1}>账号安全</Typography.Title><Typography.Paragraph type="secondary">管理本人密码与登录会话</Typography.Paragraph></div><Button disabled={disabled} loading={refreshing} onClick={refresh}>刷新当前身份</Button></div>
    {isRestricted(identity) && <Alert showIcon type="warning" title="当前需要修改密码后才能继续" description="修改成功后，当前账号的全部设备将退出，请使用新密码重新登录。" />}
    <Card title="本人身份"><Descriptions column={1} items={[{ key: 'name', label: '姓名', children: identity.displayName }, { key: 'space', label: '身份空间', children: spaces[space].label }]} /></Card>
    {canManageSelf(identity, space, 'password') && <Card title="修改密码"><SecurityForm identity={identity} operation="password" disabled={disabled} /></Card>}
    {canManageSelf(identity, space, 'logout-all') && <Card title="退出全部设备"><Typography.Paragraph>将退出当前账号的全部设备，包括当前设备。请再次输入当前密码确认。</Typography.Paragraph><SecurityForm identity={identity} operation="logout-all" disabled={disabled} /></Card>}
  </Space>;
}
