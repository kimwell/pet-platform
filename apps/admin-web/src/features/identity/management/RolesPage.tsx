import { useEffect, useRef, useState } from 'react';
import { useDraftGuard } from './useDraftGuard';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useLocation, useRouter, useSearch } from '@tanstack/react-router';
import { Alert, Button, Form, Input, Modal, Pagination, Result, Select, Space, Table, Tag, Typography } from 'antd';
import type { components } from '@pet/api-contracts';
import type { WebIdentity } from '../../../shared/auth/spaces';
import { useSession } from '../../../shared/auth/useSession';
import { hasPermission } from '../../../shared/auth/permissions';
import { ErrorNotice } from '../../../shared/api/ErrorNotice';
import { rolePageOptions, roleOptions, permissionOptions, managementErrorMessages, managementNameRule, managementFieldErrors, roleGrantFieldErrors, requiresRecheck } from './api';
import type { MutationResult, RoleDetail } from './api';
import { correctedPage, safePagination } from '../users/queries/pagination';
import { isEmployeeId } from '../users/queries/detailSearch';
const scopes = { TENANT: '租户全部', STORES: '授权门店', SELF: '本人' };
export function RolesPage({ identity }: { identity: WebIdentity }) {
  const { auth } = useSession('STAFF', false), router = useRouter(), location = useLocation();
  const search = useSearch({ strict: false }) as { page?: number };
  const page = search.page ?? 1;
  const id = location.pathname.split('/')[4];
  const listing = !id;
  const list = useQuery({ ...rolePageOptions(auth, identity, page), enabled: listing && hasPermission(identity, 'STAFF', 'identity:role:list') });
  const detail = useQuery({ ...roleOptions(auth, identity, id ?? ''), enabled: !listing && isEmployeeId(id ?? '') && hasPermission(identity, 'STAFF', 'identity:role:detail') });
  const active = listing ? list : detail;
  const can = (p: string) => hasPermission(identity, 'STAFF', p);
  const pagination = list.data ? safePagination(list.data.total, page, 20) : undefined;
  useEffect(() => {
    const canonical = `?page=${page}`;
    if (listing && location.searchStr !== canonical) { void router.navigate({ href: `/admin/identity/roles${canonical}`, replace: true, state: { rolePageCorrected: location.state.rolePageCorrected, roleUrlNotice: location.searchStr ? '查询参数已规范化为有效页码。' : location.state.roleUrlNotice } }); }
  }, [listing, location.searchStr, location.state, page, router]);
  const correction = pagination && list.data?.page === page ? correctedPage(pagination, page, Boolean(location.state.rolePageCorrected)) : undefined;
  useEffect(() => {
    if (listing && !list.isFetching && !list.isError && correction !== undefined) { void router.navigate({ href: `/admin/identity/roles?page=${correction}`, replace: true, state: { rolePageCorrected: true, roleUrlNotice: '请求页码超过末页，已返回最后一页。' } }); }
  }, [listing, list.isFetching, list.isError, correction, router]);
  return <section aria-labelledby="roles-heading"><div className="page-heading"><Typography.Title level={1} id="roles-heading" tabIndex={-1}>{listing ? '角色管理' : '角色详情'}</Typography.Title>
    <Space wrap>{listing && can('identity:role:create') && <RoleCreate identity={identity} />}{!listing && can('identity:role:list') && <Button onClick={() => void router.navigate({ href: '/admin/identity/roles' })}>返回角色列表</Button>}</Space></div>
    {location.state.roleUrlNotice && <Alert type="info" title={location.state.roleUrlNotice} />}
    {active.isError && <><ErrorNotice error={active.error} messages={managementErrorMessages(active.error)} title="角色读取失败" /><Button onClick={() => void (listing ? list.refetch() : detail.refetch())}>重试角色读取</Button></>}
    {listing ? <>{list.data && !list.isError && <Typography.Paragraph>共 {list.data.total} 个角色</Typography.Paragraph>}{!list.isError && pagination?.kind === 'limited' && <Alert type="warning" title={pagination.reason} />}{!list.isError && <Table rowKey="id" loading={list.isPending} dataSource={list.data?.items ?? []} pagination={false} scroll={{ x: 680 }} columns={[
      { title: '编码', dataIndex: 'code' }, { title: '名称', dataIndex: 'name' }, { title: '状态', dataIndex: 'status', render: status => <Tag>{status === 'ACTIVE' ? '启用' : '停用'}</Tag> },
      ...(can('identity:role:detail') ? [{ title: '操作', render: (_: unknown, row: components['schemas']['RoleSummary']) => <Button type="link" onClick={() => void router.navigate({ href: `/admin/identity/roles/${row.id}` })}>查看角色 {row.name}</Button> }] : []),
    ]} />}{!list.isError && pagination?.kind === 'safe' && <Pagination current={page} pageSize={20} total={pagination.total} showSizeChanger={false} onChange={value => void router.navigate({ href: `/admin/identity/roles?page=${value}`, state: { rolePageCorrected: false } })} />}</>
      : !isEmployeeId(id ?? '') ? <Result status="warning" title="角色 ID 格式无效" /> : detail.data && !detail.isError && <RoleEditor key={id} identity={identity} data={detail.data} refresh={async () => { const result = await detail.refetch(); return result.isError ? undefined : result.data?.role.version; }} />}
  </section>;
}
function RoleCreate({ identity }: { identity: WebIdentity }) {
  const { auth } = useSession('STAFF', false), client = useQueryClient(), router = useRouter();
  const [closing, setClosing] = useState(false);
  const [open, setOpen] = useState(false), [unknown, setUnknown] = useState(false), [form] = Form.useForm<components['schemas']['CreateRole']>();
  const submitting = useRef(false);
  const draftGuard = useDraftGuard(() => open && Object.values(form.getFieldsValue(true)).some(value => value !== undefined && value !== ''));
  const mutation = useMutation({ retry: false, gcTime: 0, mutationFn: (input: components['schemas']['CreateRole']) => auth.runtime.request<MutationResult>('STAFF', '/admin/identity/roles', { method: 'POST', json: input }),
    onSuccess: async result => { form.resetFields(); setClosing(true); setOpen(false); await client.invalidateQueries({ predicate: q => q.queryKey[1] === 'identity' }); if (hasPermission(identity, 'STAFF', 'identity:role:detail')) await router.navigate({ href: `/admin/identity/roles/${result.id}` }); },
    onError: error => { form.setFields(managementFieldErrors(error, ['code', 'name'])); setUnknown(requiresRecheck(error)); }, onSettled: () => { submitting.current = false; } });
  return <>{draftGuard.contextHolder}<Button type="primary" disabled={closing || mutation.isPending} onClick={() => { form.resetFields(); mutation.reset(); setUnknown(false); setOpen(true); }}>新建角色</Button><Modal title="新建角色" open={open} destroyOnHidden afterClose={() => setClosing(false)} footer={null} maskClosable={false} onCancel={async () => { if (!mutation.isPending && await draftGuard.mayDiscard()) { setClosing(true); setOpen(false); form.resetFields(); mutation.reset(); } }}>
    {mutation.isError && <ErrorNotice error={mutation.error} messages={managementErrorMessages(mutation.error)} title={unknown ? '创建结果未确认，请返回列表核对角色编码' : '创建角色失败'} />}
    <Form form={form} layout="vertical" disabled={mutation.isPending || unknown} onFinish={input => { if (submitting.current || mutation.isPending || unknown) return; submitting.current = true; mutation.mutate(input); }}>
      <Form.Item name="code" label="角色编码" rules={[{ required: true, message: '请输入角色编码' }, { pattern: /^[a-z0-9][a-z0-9-]{0,31}$/, message: '编码须为1至32位小写字母、数字或连字符' }]}><Input /></Form.Item>
      <Form.Item name="name" label="角色名称" rules={[managementNameRule]}><Input /></Form.Item>
      <Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={mutation.isPending || unknown}>创建角色</Button>
    </Form></Modal></>;
}
function RoleEditor({ identity, data, refresh }: { identity: WebIdentity; data: RoleDetail; refresh: () => Promise<string | undefined> }) {
  const { auth } = useSession('STAFF', false), client = useQueryClient();
  const catalogue = useQuery(permissionOptions(auth, identity));
  const [info] = Form.useForm<components['schemas']['EditRole']>();
  const [grants] = Form.useForm<components['schemas']['RoleGrants']>();
  const [unknown, setUnknown] = useState(false), [notice, setNotice] = useState<string>();
  const submitting = useRef(false);
  const versionAtOpen = useRef(data.role.version);
  const baseline = useRef({ info: JSON.stringify({ name: data.role.name, status: data.role.status }), grants: JSON.stringify({ grants: data.grants }) });
  const draftGuard = useDraftGuard(() => JSON.stringify(info.getFieldsValue(true)) !== baseline.current.info || JSON.stringify(grants.getFieldsValue(true)) !== baseline.current.grants);
  const mutation = useMutation({ retry: false, gcTime: 0, mutationFn: ({ kind, input }: { kind: 'info' | 'grants'; input: components['schemas']['EditRole'] | components['schemas']['RoleGrants'] }) => auth.runtime.request<MutationResult>('STAFF', `/admin/identity/roles/${data.role.id}${kind === 'grants' ? '/grants' : ''}`, { method: 'PUT', json: { ...input, version: versionAtOpen.current } }),
    onSuccess: async (result, variables) => { versionAtOpen.current = result.version; baseline.current[variables.kind] = JSON.stringify(variables.kind === 'info' ? info.getFieldsValue(true) : grants.getFieldsValue(true)); setNotice(result.sessionCleanupComplete ? '角色已保存，相关员工旧身份已失效。' : '角色已保存，相关旧身份已失效，物理清理待重试。'); await client.invalidateQueries({ predicate: q => q.queryKey[1] === 'identity' }); },
    onError: error => { info.setFields(managementFieldErrors(error, ['name', 'status'])); grants.setFields(roleGrantFieldErrors(error)); setUnknown(requiresRecheck(error)); }, onSettled: () => { submitting.current = false; } });
  const canEdit = hasPermission(identity, 'STAFF', 'identity:role:update') && !data.role.protectedRole;
  const canGrant = hasPermission(identity, 'STAFF', 'identity:role:grant') && !data.role.protectedRole;
  const save = (kind: 'info' | 'grants', input: components['schemas']['EditRole'] | components['schemas']['RoleGrants']) => { if (submitting.current || mutation.isPending || unknown || (kind === 'info' ? !canEdit : !canGrant || catalogue.isPending || catalogue.isError)) return; submitting.current = true; mutation.mutate({ kind, input }); };
  return <>{draftGuard.contextHolder}<Typography.Paragraph>编码：{data.role.code}；资源版本：{data.role.version}</Typography.Paragraph>
    {data.role.protectedRole && <Alert type="info" title="保留管理员角色受到保护，不能通过页面修改或停用。" />}
    {notice && <Alert type="success" title={notice} />}
    {mutation.isError && <ErrorNotice error={mutation.error} messages={managementErrorMessages(mutation.error)} title={unknown ? '授权受限、版本冲突或保存结果未确认，请重新读取并核对' : '角色保存失败'} />}
    {unknown && <Button onClick={async () => { const version = await refresh(); if (version !== undefined) { versionAtOpen.current = version; setUnknown(false); mutation.reset(); } }}>重新读取版本并保留草稿</Button>}
    <Form form={info} layout="vertical" initialValues={{ name: data.role.name, status: data.role.status }} disabled={!canEdit || mutation.isPending || unknown} onFinish={input => save('info', input)}>
      <Form.Item name="name" label="角色名称" rules={[managementNameRule]}><Input /></Form.Item>
      <Form.Item name="status" label="角色状态" rules={[{ required: true }]}><Select options={[{ value: 'ACTIVE', label: '启用' }, { value: 'DISABLED', label: '停用' }]} /></Form.Item>
      {canEdit && <Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={mutation.isPending || unknown}>保存角色资料</Button>}
    </Form>
    <Typography.Title level={2}>权限与数据范围</Typography.Title>
    {catalogue.isError && <><ErrorNotice error={catalogue.error} messages={managementErrorMessages(catalogue.error)} title="权限目录加载失败" /><Button onClick={() => void catalogue.refetch()}>重试权限目录</Button></>}
    <Form form={grants} layout="vertical" initialValues={{ grants: data.grants }} disabled={!canGrant || mutation.isPending || unknown || catalogue.isPending || catalogue.isError} onFinish={input => save('grants', input)}>
      <Form.List name="grants">{(fields, { add, remove }) => <>{fields.map(field => <div key={field.key} className="role-grant-row">
        <Form.Item name={[field.name, 'permissionCode']} label="权限" rules={[{ required: true, message: '请选择权限' }]}><Select showSearch optionFilterProp="label" onChange={() => grants.setFieldValue(['grants', field.name, 'scopeType'], undefined)} options={(catalogue.data ?? []).map(p => ({ value: p.code, label: `${p.name}（${p.code}）`, disabled: !p.grantableScopes.length }))} /></Form.Item>
        <Form.Item noStyle shouldUpdate>{() => { const permission = grants.getFieldValue(['grants', field.name, 'permissionCode']) as string | undefined; const p = catalogue.data?.find(item => item.code === permission); return <Form.Item name={[field.name, 'scopeType']} label="数据范围" rules={[{ required: true, message: '请选择范围' }]}><Select options={(p?.scopes ?? []).map(s => ({ value: s, label: scopes[s], disabled: !p?.grantableScopes.includes(s) }))} /></Form.Item>; }}</Form.Item>
        {canGrant && <Button danger disabled={mutation.isPending || unknown || catalogue.isPending || catalogue.isError} onClick={() => remove(field.name)}>移除此项权限</Button>}
      </div>)}{canGrant && <Button disabled={mutation.isPending || unknown || catalogue.isPending || catalogue.isError} onClick={() => add()}>添加权限范围</Button>}</>}</Form.List>
      <Typography.Paragraph type="secondary">同一权限的门店与本人范围可取并集；不同权限分别授权。清空后撤销该角色全部权限。</Typography.Paragraph>
      {canGrant && <Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={mutation.isPending || unknown || catalogue.isPending || catalogue.isError}>保存权限与范围</Button>}
    </Form></>;
}

declare module '@tanstack/react-router' { interface HistoryState { rolePageCorrected?: boolean; roleUrlNotice?: string } }
