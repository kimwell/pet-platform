import { useEffect, useRef, useSyncExternalStore } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Link, useRouter, useSearch, useRouteContext } from '@tanstack/react-router';
import { Alert, Button, Card, Form, Input, Space, Typography } from 'antd';
import { ApiError } from '../../api/ApiError';
import { ErrorNotice } from '../../components/ErrorNotice/index';
import { safeReturnTo } from '../../utils/auth/returnTo';
import { normalizeName, spaces } from '../../utils/auth/spaces';
import type { AuthSpace, StaffLogin } from '../../utils/auth/spaces';
import { SessionFailure } from '../../components/SessionFailure';
import { isRestricted } from '../../utils/auth/permissions';

function LoginPage({ space }: { space: AuthSpace }) {
  const router = useRouter();
  const { auth } = router.options.context!;
  const search = useSearch({ strict: false });
  const guardError = useRouteContext({ strict: false }).sessionError;
  const [form] = Form.useForm<StaffLogin>();
  const submitting = useRef(false);
  useSyncExternalStore(auth.runtime.subscribe, () => auth.runtime.snapshot(space), () => auth.runtime.snapshot(space));
  useEffect(() => () => { form.resetFields(); }, [form]);
  const mutation = useMutation({
    // Query的mutation变量始终undefined，密码只经表单短暂局部输入进入请求。
    mutationFn: async () => {
      try {
        await auth.login(space, form.getFieldsValue());
        form.resetFields();
        const identity = await auth.current(space);
        if (!identity) throw new ApiError('HTTP', '未能确认当前身份，请重新登录', 401, 'AUTH_REQUIRED');
        return isRestricted(identity);
      } finally { form.setFieldValue('password', undefined); }
    },
    onSuccess: async restricted => {
      router.history.replace(restricted ? spaces[space].security : safeReturnTo(space, 'returnTo' in search ? search.returnTo : undefined));
      await router.invalidate();
    },
    onError: error => {
      if (error instanceof ApiError && error.fieldErrors) {
        const fields = space === 'STAFF' ? ['tenantCode', 'loginName', 'password'] : ['loginName', 'password'];
        form.setFields(error.fieldErrors.filter(f => fields.includes(f.field)).map(f => ({ name: f.field as keyof StaffLogin, errors: [f.message] })));
      }
    },
  });
  const pending = mutation.isPending || auth.runtime.isBusy(space);
  const submit = async () => {
    if (submitting.current || pending) return;
    submitting.current = true;
    try { await mutation.mutateAsync(); } catch { /* 错误仅由当前表单展示。 */ }
    finally { submitting.current = false; }
  };
  const nameRule = (_: unknown, value: string | undefined) => !value || /^[a-z0-9][a-z0-9._-]{0,63}$/.test(normalizeName(value))
    ? Promise.resolve() : Promise.reject(new Error('账号须为 1～64 位字母、数字、点、下划线或连字符'));
  const notice = auth.runtime.notice(space);
  const allowedFields = space === 'STAFF' ? ['tenantCode', 'loginName', 'password'] : ['loginName', 'password'];
  const unmappedMessages = mutation.error instanceof ApiError ? mutation.error.fieldErrors?.filter(field => !allowedFields.includes(field.field)).map(field => field.message) : undefined;
  if (guardError) return <SessionFailure error={guardError} notice={notice} />;
  return <main className="login-page">
    <div className="login-brand"><Link to="/">企业应用</Link><span>{spaces[space].label}入口</span></div>
    <Card className="login-card">
      <Typography.Title level={1}>{space === 'STAFF' ? '员工登录' : '平台管理员登录'}</Typography.Title>
      <Typography.Paragraph type="secondary">{space === 'STAFF' ? '使用所属租户的员工账号登录' : '使用独立的平台管理员账号登录'}</Typography.Paragraph>
      <Space orientation="vertical" className="full-width" size="middle">
        {notice && <Alert type="info" showIcon title={notice} />}
        {mutation.isError && <ErrorNotice error={mutation.error} messages={unmappedMessages} />}
        <Form form={form} layout="vertical" scrollToFirstError={{ focus: true }} onFinish={() => void submit()} disabled={pending} clearOnDestroy requiredMark={false} autoComplete="on">
          {space === 'STAFF' && <Form.Item name="tenantCode" label="租户编码" rules={[
            { required: true, message: '请输入租户编码' },
            { validator: (_, value: string | undefined) => !value || /^[a-z0-9][a-z0-9-]{0,31}$/.test(normalizeName(value))
              ? Promise.resolve() : Promise.reject(new Error('租户编码须为 1～32 位字母、数字或连字符')) },
          ]}><Input autoComplete="organization" autoCapitalize="none" spellCheck={false} maxLength={64} /></Form.Item>}
          <Form.Item name="loginName" label="账号" rules={[{ required: true, message: '请输入账号' }, { validator: nameRule }]}>
            <Input autoComplete="username" autoCapitalize="none" spellCheck={false} maxLength={128} />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password autoComplete="current-password" maxLength={256} visibilityToggle={false} />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={pending}>登录</Button>
        </Form>
        <Link to={space === 'STAFF' ? '/platform/login' : '/admin/login'}>{space === 'STAFF' ? '前往平台管理员登录' : '前往员工登录'}</Link>
      </Space>
    </Card>
  </main>;
}
export function StaffLoginPage() { return <LoginPage space="STAFF" />; }
export function PlatformLoginPage() { return <LoginPage space="PLATFORM" />; }
