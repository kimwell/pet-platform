import { useEffect, useMemo, useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useLocation, useRouter } from '@tanstack/react-router';
import { Alert, Button, Checkbox, Descriptions, Form, Input, Modal, Select, Space, Spin, Table, Tag, Typography } from 'antd';
import type { components } from '@pet/api-contracts';
import type { WebIdentity } from '../../shared/auth/spaces';
import { useSession } from '../../shared/auth/useSession';
import { hasPermission } from '../../shared/auth/permissions';
import { ErrorNotice } from '../../shared/api/ErrorNotice';
import { managementFieldErrors, managementNameRule, requiresRecheck } from '../identity/management/api';
import { useDraftGuard } from '../identity/management/useDraftGuard';
import { passwordError } from '../account/securityForm';
import { safePagination, correctedPage } from '../identity/users/queries/pagination';
import { options, controlSearch, listHref, detailHref, returnHref, permissionNames } from './api';
import type { Account, AccountPage, Result } from './api';
const path = '/platform/accounts';
const basic = ['platform:session:manage', 'platform:credential:change'];
const passwordRule = { validator: (_: unknown, value: string) => { const error = passwordError(value); return error ? Promise.reject(new Error(error)) : Promise.resolve(); } };
export function AccountsPage({ identity }: { identity: WebIdentity }) {
  const { auth } = useSession('PLATFORM', false), client = useQueryClient(), router = useRouter(), location = useLocation();
  const id = location.pathname.slice(path.length + 1), detail = Boolean(id), q = useMemo(() => controlSearch(location.searchStr, true), [location.searchStr]);
  const allowed = (p: string) => hasPermission(identity, 'PLATFORM', 'platform:account:' + p);
  const list = useQuery({ ...options<AccountPage>(auth, identity, 'PLATFORM', 'accounts', 'platform:account:list', path, q), enabled: !detail && allowed('list') && !auth.runtime.isBusy(identity.principalType) });
  const selected = useQuery({ ...options<Account>(auth, identity, 'PLATFORM', 'accounts', 'platform:account:detail', path + '/' + id), enabled: detail && allowed('detail') && !auth.runtime.isBusy(identity.principalType) });
  const grants = useQuery(options<components['schemas']['ControlPermissionOption'][]>(auth, identity, 'PLATFORM', 'permissions', 'platform:account:grant', '/platform/permissions'));
  const [mode, setMode] = useState<'create' | 'edit' | 'status' | 'grants' | 'reset' | 'revoke'>(), [closing, setClosing] = useState(false), [recheck, setRecheck] = useState(false);
  const [create] = Form.useForm<components['schemas']['CreateControlAccount']>(), [edit] = Form.useForm<components['schemas']['EditControlAccount']>(), [permissionForm] = Form.useForm<components['schemas']['ControlAccountGrants']>(), [reset] = Form.useForm<components['schemas']['ControlAccountReset']>();
  const submitting = useRef(false), corrected = useRef(false);
  const clear = () => { create.resetFields(); edit.resetFields(); permissionForm.resetFields(); reset.resetFields(); };
  const draft = useDraftGuard(() => Boolean(mode && [create, edit, permissionForm, reset].some(f => f.isFieldsTouched())), 'PLATFORM');
  const mutation = useMutation({ retry: false, gcTime: 0, mutationFn: () => {
    if (mode === 'create') return auth.runtime.request<Result>('PLATFORM', path, { method: 'POST', json: create.getFieldsValue(true) });
    const account = selected.data; if (!account) throw new Error('请先读取平台账号');
    let suffix = '', body: unknown, method: 'PUT' | 'POST' = 'PUT';
    if (mode === 'edit') body = { version: account.version, displayName: edit.getFieldValue('displayName') } satisfies components['schemas']['EditControlAccount'];
    else if (mode === 'status') { suffix = '/status'; body = { version: account.version, status: account.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' } satisfies components['schemas']['ControlStatus']; }
    else if (mode === 'grants') { suffix = '/permissions'; body = { version: account.version, permissions: permissionForm.getFieldValue('permissions') } satisfies components['schemas']['ControlAccountGrants']; }
    else if (mode === 'reset') { suffix = '/password'; body = { version: account.version, newPassword: reset.getFieldValue('newPassword') } satisfies components['schemas']['ControlAccountReset']; }
    else { suffix = '/sessions/revoke'; method = 'POST'; body = { version: account.version } satisfies components['schemas']['ControlAccountRevoke']; }
    return auth.runtime.request<Result>('PLATFORM', path + '/' + account.id + suffix, { method, json: body });
  }, onSuccess: async result => { clear(); setClosing(true); setMode(undefined); await client.invalidateQueries({ predicate: x => x.queryKey[1] === 'control' || x.queryKey[1] === 'auth' }); await auth.current('PLATFORM'); if (!detail && allowed('detail')) await router.navigate({ href: detailHref(path, result.id, q) }); }, onError: error => { create.setFields(managementFieldErrors(error, ['loginName', 'displayName', 'initialPassword', 'permissions'])); edit.setFields(managementFieldErrors(error, ['displayName'])); permissionForm.setFields(managementFieldErrors(error, ['permissions'])); reset.setFields(managementFieldErrors(error, ['newPassword'])); setRecheck(requiresRecheck(error)); }, onSettled: () => { create.setFieldValue('initialPassword', undefined); reset.setFieldValue('newPassword', undefined); submitting.current = false; } });
  const close = async () => { if (mutation.isPending || !await draft.mayDiscard()) return; clear(); mutation.reset(); setClosing(true); setMode(undefined); };
  const open = (value: typeof mode) => { mutation.reset(); setRecheck(false); clear(); create.setFieldValue('permissions', basic.filter(code => grants.data?.some(p => p.code === code && p.grantable))); if (value === 'edit') edit.setFieldValue('displayName', selected.data?.displayName); if (value === 'grants') permissionForm.setFieldValue('permissions', selected.data?.permissions); setMode(value); };
  const submit = () => { if (submitting.current || mutation.isPending || recheck) return; submitting.current = true; mutation.mutate(); };
  const permissionItems = grants.data?.map(p => ({ label: permissionNames[p.code] ?? p.code, value: p.code, disabled: !p.grantable || basic.includes(p.code) }));
  const permissionsRule = { validator: (_: unknown, value: string[]) => basic.every(code => value?.includes(code)) ? Promise.resolve() : Promise.reject(new Error('必须保留本人会话与改密权限')) };
  const pagination = list.data ? safePagination(list.data.total, q.page, q.pageSize) : undefined;
  useEffect(() => { if (!detail && pagination) { const page = correctedPage(pagination, q.page, corrected.current); if (page) { corrected.current = true; void router.navigate({ href: listHref(path, { ...q, page }), replace: true }); } } }, [detail, pagination, q, router]);
  const [filter] = Form.useForm();
  useEffect(() => { filter.setFieldsValue(q); }, [filter, q]);const target = selected.data;const self = target?.id === identity.principalId;
  return <>{draft.contextHolder}<Typography.Title level={2}>{detail ? '平台账号详情' : '平台账号管理'}</Typography.Title>
    {!detail ? <>
      <Form form={filter} initialValues={q} layout="inline" className="control-filters" onFinish={values => { corrected.current = false; void router.navigate({ href: listHref(path, { ...q, keyword: values.keyword?.trim() || undefined, status: values.status, sortBy: values.sortBy, sortOrder: values.sortOrder, page: 1 }) }); }}>
        <Form.Item name="keyword" label="关键词"><Input maxLength={100} allowClear /></Form.Item><Form.Item name="status" label="状态"><Select style={{ width: 110 }} allowClear options={[{ value: 'ACTIVE', label: '启用' }, { value: 'DISABLED', label: '停用' }]} /></Form.Item><Form.Item name="sortBy" label="排序"><Select style={{ width: 120 }} options={[{ value: 'createdAt', label: '创建时间' }, { value: 'loginName', label: '账号' }, { value: 'displayName', label: '显示名称' }, { value: 'status', label: '状态' }]} /></Form.Item><Form.Item name="sortOrder" label="顺序"><Select style={{ width: 100 }} options={[{ value: 'asc', label: '升序' }, { value: 'desc', label: '降序' }]} /></Form.Item><Button htmlType="submit">查询</Button>
      </Form><Space wrap><Button onClick={() => void list.refetch()}>刷新列表</Button>{allowed('create') && allowed('grant') && <Button type="primary" disabled={closing || mutation.isPending || !grants.data || grants.isFetching} onClick={() => open('create')}>新建平台账号</Button>}</Space>
      {grants.isError && allowed('grant') && <ErrorNotice error={grants.error} />}
      {list.isError ? <ErrorNotice error={list.error} /> : list.isPending ? <Spin /> : <>{pagination?.kind === 'limited' && <Alert type="warning" title={pagination.reason} />}<Table<Account> rowKey="id" dataSource={list.data?.items} loading={list.isFetching} scroll={{ x: 550 }} columns={[{ title: '账号', dataIndex: 'loginName' }, { title: '显示名称', dataIndex: 'displayName' }, { title: '状态', render: (_, a) => <Tag>{a.status === 'ACTIVE' ? '启用' : '停用'}</Tag> }, { title: '操作', render: (_, a) => allowed('detail') && <Button onClick={() => void router.navigate({ href: detailHref(path, a.id, q) })}>查看详情</Button> }]} pagination={pagination?.kind === 'safe' ? { current: q.page, pageSize: q.pageSize, total: pagination.total, showSizeChanger: true, pageSizeOptions: [10, 20, 50, 100], onChange: (page, pageSize) => { corrected.current = false; void router.navigate({ href: listHref(path, { ...q, page, pageSize }) }); } } : false} /></>}
    </> : <><Button onClick={() => void router.navigate({ href: returnHref(location.searchStr, path, true) })}>返回平台账号列表</Button><Button onClick={() => void selected.refetch()}>重新读取详情</Button>
      {selected.isError ? <ErrorNotice error={selected.error} /> : selected.isPending ? <Spin /> : target && <><Descriptions bordered column={1} items={[{ key: 'loginName', label: '账号', children: target.loginName }, { key: 'displayName', label: '显示名称', children: target.displayName }, { key: 'status', label: '状态', children: target.status === 'ACTIVE' ? '启用' : '停用' }, { key: 'permissions', label: '正式权限', children: <Space wrap>{target.permissions.map(code => <Tag key={code}>{permissionNames[code] ?? code}</Tag>)}</Space> }]} />
        {self && <Alert type="info" title="这是当前账号。本人改密或退出全部设备请使用账号安全；此处不提供自我停用或撤权。" />}
        <Space wrap>{allowed('update') && <Button disabled={closing || mutation.isPending || selected.isFetching} onClick={() => open('edit')}>修改账号资料</Button>}{!self && <>
          {allowed(target.status === 'ACTIVE' ? 'disable' : 'enable') && <Button danger={target.status === 'ACTIVE'} disabled={closing || mutation.isPending || selected.isFetching} onClick={() => open('status')}>{target.status === 'ACTIVE' ? '停用平台账号' : '启用平台账号'}</Button>}
          {allowed('grant') && <Button disabled={closing || mutation.isPending || !grants.data || grants.isFetching || selected.isFetching} onClick={() => open('grants')}>配置平台权限</Button>}
          {allowed('reset-password') && <Button disabled={closing || mutation.isPending || selected.isFetching} onClick={() => open('reset')}>重置平台密码</Button>}
          {allowed('revoke-sessions') && <Button danger disabled={closing || mutation.isPending || selected.isFetching} onClick={() => open('revoke')}>撤销全部平台会话</Button>}
        </>}</Space>{grants.isError && allowed('grant') && <ErrorNotice error={grants.error} />}
      </>}
    </>}
    <Modal title={{ create: '新建平台账号', edit: '修改平台账号资料', grants: '配置平台权限', reset: '重置平台密码', revoke: '确认撤销平台会话', status: '确认平台账号状态' }[mode ?? 'create']} open={Boolean(mode)} destroyOnHidden maskClosable={false} footer={null} onCancel={close} afterClose={() => setClosing(false)}>
      {mutation.isError && <ErrorNotice error={mutation.error} title={recheck ? '结果未确认或授权、版本已变化，请关闭并重新读取后操作' : undefined} />}
      {mode === 'create' ? <><Alert type="info" title="请安全输入初始密码并通过安全渠道交付。普通详情不返回密码；账号应登录后使用本人改密入口。" /><Form form={create} layout="vertical" onFinish={submit} disabled={mutation.isPending || recheck}><Form.Item name="loginName" label="平台账号" rules={[{ required: true, pattern: /^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/, message: '请输入1至64位有效账号' }]}><Input autoComplete="off" /></Form.Item><Form.Item name="displayName" label="显示名称" rules={[managementNameRule]}><Input /></Form.Item><Form.Item name="initialPassword" label="初始密码" rules={[passwordRule]}><Input.Password autoComplete="new-password" /></Form.Item><Form.Item name="permissions" label="允许授予的权限" rules={[permissionsRule]}><Checkbox.Group className="control-permissions" options={permissionItems} /></Form.Item><Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={recheck}>创建平台账号</Button></Form></> : mode === 'edit' ? <Form form={edit} layout="vertical" onFinish={submit} disabled={mutation.isPending || recheck}><Form.Item name="displayName" label="显示名称" rules={[managementNameRule]}><Input /></Form.Item><Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={recheck}>保存账号资料</Button></Form> : mode === 'grants' ? <><Alert type="warning" title="权限变化会使此账号全部旧平台会话失效。只能授予自己具备的权限，且必须保留有效管理入口。" /><Form form={permissionForm} layout="vertical" disabled={mutation.isPending || recheck} onFinish={submit}><Form.Item name="permissions" label="平台权限" rules={[permissionsRule]}><Checkbox.Group className="control-permissions" options={permissionItems} /></Form.Item><Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={recheck}>确认保存权限</Button></Form></> : mode === 'reset' ? <><Alert type="warning" title="重置后全部旧平台会话失效。新密码不会出现在普通详情，请通过安全渠道交付。" /><Form form={reset} layout="vertical" disabled={mutation.isPending || recheck} onFinish={submit}><Form.Item name="newPassword" label="新密码" rules={[passwordRule]}><Input.Password autoComplete="new-password" /></Form.Item><Button danger type="primary" htmlType="submit" loading={mutation.isPending} disabled={recheck}>确认重置密码</Button></Form></> : <><Alert type="warning" title={mode === 'revoke' ? '此账号全部旧平台会话将失效，STAFF 会话不受影响。' : target?.status === 'ACTIVE' ? '停用会拒绝新登录，并使全部旧平台会话失效；不能移除最后一个有效管理入口。' : '启用后需重新登录，已经撤销的旧平台会话不会复活。'} /><Button danger type="primary" loading={mutation.isPending} disabled={recheck} onClick={submit}>确认执行</Button></>}
      <Button disabled={mutation.isPending} onClick={close}>取消</Button>
    </Modal>
  </>;
}
