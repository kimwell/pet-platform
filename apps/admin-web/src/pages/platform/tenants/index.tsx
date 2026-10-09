import { useLocalDetail } from '../../../hooks/useLocalDetail';
import { ProTable } from '../../../components/ProTable';
import { DialogPanel as Modal } from '../../../components/ResourceDialog';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useLocation, useRouter } from '@tanstack/react-router';
import { Alert, Button, Descriptions, Form, Input, Select, Space, Spin, Tag, Typography } from 'antd';
import type { components } from '@pet/api-contracts';
import type { WebIdentity } from '../../../utils/auth/spaces';
import { useSession } from '../../../hooks/useSession';
import { hasPermission } from '../../../utils/auth/permissions';
import { ErrorNotice } from '../../../components/ErrorNotice/index';
import { managementFieldErrors, managementNameRule, requiresRecheck } from '../../../api/identity/management';
import { useDraftGuard } from '../../../hooks/useDraftGuard';
import { safePagination, correctedPage } from '../../../utils/identity/pagination';
import { options, controlSearch, listHref } from '../../../api/control';
import type { Tenant, TenantPage, Result, Store } from '../../../api/control';
const path = '/platform/tenants';
export function TenantsPage({ identity }: { identity: WebIdentity }) {
  const { auth } = useSession('PLATFORM', false), client = useQueryClient(), router = useRouter(), location = useLocation();
  const [id, setId] = useLocalDetail('tenants'); const detail = Boolean(id), q = useMemo(() => controlSearch(location.searchStr), [location.searchStr]);
  const allowed = (p: string) => hasPermission(identity, 'PLATFORM', 'platform:tenant:' + p);
  const list = useQuery({ ...options<TenantPage>(auth, identity, 'PLATFORM', 'tenants', 'platform:tenant:list', path, q), enabled: allowed('list') && !auth.runtime.isBusy(identity.principalType) });
  const selected = useQuery({ ...options<Tenant>(auth, identity, 'PLATFORM', 'tenants', 'platform:tenant:detail', path + '/' + id), enabled: detail && allowed('detail') && !auth.runtime.isBusy(identity.principalType) });
  const [mode, setMode] = useState<'create' | 'edit' | 'status'>(), [closing, setClosing] = useState(false), [recheck, setRecheck] = useState(false);
  const [create] = Form.useForm<components['schemas']['CreateTenant']>(), [edit] = Form.useForm<components['schemas']['EditControlName']>();
  const submitting = useRef(false), corrected = useRef(false);
  const draft = useDraftGuard(() => Boolean(mode && (create.isFieldsTouched() || edit.isFieldsTouched())), 'PLATFORM');
  const mutation = useMutation({ retry: false, gcTime: 0, mutationFn: () => {
    if (mode === 'create') return auth.runtime.request<Result>('PLATFORM', path, { method: 'POST', json: create.getFieldsValue(true) });
    const t = selected.data; if (!t) throw new Error('请先读取租户详情');
    return auth.runtime.request<Result>('PLATFORM', path + '/' + t.id + (mode === 'status' ? '/status' : ''), { method: 'PUT', json: mode === 'status' ? { version: t.version, status: t.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' } satisfies components['schemas']['ControlStatus'] : { version: t.version, name: edit.getFieldValue('name') } satisfies components['schemas']['EditControlName'] });
  }, onSuccess: async result => { create.resetFields(); edit.resetFields(); setClosing(true); setMode(undefined); await client.invalidateQueries({ predicate: x => x.queryKey[1] === 'control' }); await auth.current('PLATFORM'); if (allowed('detail')) setId(result.id); }, onError: error => { create.setFields(managementFieldErrors(error, ['code', 'name'])); edit.setFields(managementFieldErrors(error, ['name'])); setRecheck(requiresRecheck(error)); }, onSettled: () => { submitting.current = false; } });
  const close = async () => { if (mutation.isPending || !await draft.mayDiscard()) return; create.resetFields(); edit.resetFields(); mutation.reset(); setClosing(true); setMode(undefined); };
  const open = (value: typeof mode) => { mutation.reset(); setRecheck(false); create.resetFields(); edit.resetFields(); if (value === 'edit') edit.setFieldsValue({ name: selected.data?.name }); setMode(value); };
  const submit = () => { if (submitting.current || mutation.isPending || recheck) return; submitting.current = true; mutation.mutate(); };
  const pagination = list.data ? safePagination(list.data.total, q.page, q.pageSize) : undefined;
  useEffect(() => { if (!detail && pagination) { const page = correctedPage(pagination, q.page, corrected.current); if (page) { corrected.current = true; void router.navigate({ href: listHref(path, { ...q, page }), replace: true }); } } }, [detail, pagination, q, router]);
  const [filter] = Form.useForm();
  useEffect(() => { filter.setFieldsValue(q); }, [filter, q]);
  return <>{draft.contextHolder}<Typography.Title level={2}>租户管理</Typography.Title>
    <ProTable<Tenant>
      filters={<Form form={filter} initialValues={q} layout="inline" className="control-filters" onFinish={values => { corrected.current = false; void router.navigate({ href: listHref(path, { ...q, keyword: values.keyword?.trim() || undefined, status: values.status, sortBy: values.sortBy, sortOrder: values.sortOrder, page: 1 }) }); }}>
        <Form.Item name="keyword" label="关键词"><Input maxLength={100} allowClear /></Form.Item><Form.Item name="status" label="状态"><Select allowClear style={{ width: 110 }} options={[{ value: 'ACTIVE', label: '启用' }, { value: 'DISABLED', label: '停用' }]} /></Form.Item>
        <Form.Item name="sortBy" label="排序"><Select style={{ width: 120 }} options={[{ value: 'createdAt', label: '创建时间' }, { value: 'code', label: '编码' }, { value: 'name', label: '名称' }, { value: 'status', label: '状态' }]} /></Form.Item><Form.Item name="sortOrder" label="顺序"><Select style={{ width: 100 }} options={[{ value: 'asc', label: '升序' }, { value: 'desc', label: '降序' }]} /></Form.Item><Button htmlType="submit">查询</Button>
      </Form>}
      toolbar={<Space wrap><Button onClick={() => void list.refetch()}>刷新列表</Button>{allowed('create') && <Button type="primary" disabled={closing || mutation.isPending} onClick={() => open('create')}>新建租户</Button>}</Space>}
      showTable={allowed('list') && !list.isError}
      requestError={list.isError ? list.error : undefined} onRetry={() => void list.refetch()}
      statusContent={!allowed('list') ? <Alert type="warning" title="当前账号没有查看列表的权限" /> : pagination?.kind === 'limited' ? <Alert type="warning" title={pagination.reason} /> : undefined}
      rowKey="id" dataSource={list.data?.items} loading={list.isPending || list.isFetching} scroll={{ x: 640 }} columns={[{ title: '编码', dataIndex: 'code' }, { title: '租户名称', dataIndex: 'name' }, { title: '状态', render: (_, t) => <Tag>{t.status === 'ACTIVE' ? '启用' : '停用'}</Tag> }, { title: '初始化', render: (_, t) => t.initialized ? '已初始化' : '待初始化' }, { title: '操作', render: (_, t) => allowed('detail') && <Button onClick={() => setId(t.id)}>查看详情</Button> }]} pagination={pagination?.kind === 'safe' ? { current: q.page, pageSize: q.pageSize, total: pagination.total, showSizeChanger: true, pageSizeOptions: [10, 20, 50, 100], onChange: (page, pageSize) => { corrected.current = false; void router.navigate({ href: listHref(path, { ...q, page, pageSize }) }); } } : false} />
    <Modal open={detail} title="租户详情" onCancel={() => setId('')}>

      <Button onClick={() => setId('')}>返回租户列表</Button><Button onClick={() => void selected.refetch()}>重新读取详情</Button>
      {selected.isError ? <ErrorNotice error={selected.error} /> : selected.isPending ? <Spin /> : selected.data && <><Descriptions bordered column={1} items={[{ key: 'id', label: '租户标识', children: selected.data.id }, { key: 'code', label: '编码', children: selected.data.code }, { key: 'name', label: '名称', children: selected.data.name }, { key: 'status', label: '状态', children: selected.data.status === 'ACTIVE' ? '启用' : '停用' }, { key: 'initialized', label: '可使用条件', children: selected.data.initialized ? '已完成首位员工初始化；登录仍受状态和权限控制' : '待受控初始化，创建成功尚不能使用员工或客户入口' }]} />
        {!selected.data.initialized && <Alert type="info" title="下一步：由持有独立初始化凭据的运维人员执行已有 bootstrap 命令" description={<><p>使用此租户编码与当前名称，安全输入首位员工管理员密码；门店可不提供。成功后执行 identity-management-upgrade 和 organization-upgrade，再刷新此页。初始化失败全部回滚，重试不会覆盖已有账号。</p><Typography.Text code>scripts/backend-identity.sh bootstrap --tenant-code {selected.data.code} --tenant-name &lt;当前租户名称&gt; --admin-login &lt;员工账号&gt;</Typography.Text></>} />}
        <Space wrap>{allowed('update') && <Button disabled={closing || mutation.isPending || selected.isFetching} onClick={() => open('edit')}>修改租户资料</Button>}{allowed(selected.data.status === 'ACTIVE' ? 'disable' : 'enable') && <Button danger={selected.data.status === 'ACTIVE'} disabled={closing || mutation.isPending || selected.isFetching} onClick={() => open('status')}>{selected.data.status === 'ACTIVE' ? '停用租户' : '启用租户'}</Button>}</Space>
        <TenantStores identity={identity} tenant={selected.data} />
      </>}
    
    </Modal>
    <Modal open={Boolean(mode)} title={mode === 'create' ? '新建租户' : mode === 'edit' ? '修改租户资料' : '确认租户状态操作'} destroyOnHidden maskClosable={false} footer={null} onCancel={close} afterClose={() => setClosing(false)}>
      {mutation.isError && <ErrorNotice error={mutation.error} title={recheck ? '操作未确认或资源已变化，请关闭并重新读取后操作' : undefined} />}
      {mode === 'create' ? <><Alert type="info" title="创建后为待初始化状态；不会生成员工账号或假门店。" /><Form form={create} layout="vertical" disabled={mutation.isPending || recheck} onFinish={submit}><Form.Item name="code" label="租户编码" rules={[{ required: true, message: '请输入编码' }, { pattern: /^[A-Za-z0-9][A-Za-z0-9-]{0,31}$/, message: '编码须为1至32位字母、数字或连字符' }]}><Input autoComplete="off" /></Form.Item><Form.Item name="name" label="租户名称" rules={[managementNameRule]}><Input /></Form.Item><Button htmlType="submit" type="primary" loading={mutation.isPending} disabled={recheck}>创建租户</Button></Form></> : mode === 'edit' ? <Form form={edit} layout="vertical" disabled={mutation.isPending || recheck} onFinish={submit}><Form.Item name="name" label="租户名称" rules={[managementNameRule]}><Input /></Form.Item><Button htmlType="submit" type="primary" loading={mutation.isPending} disabled={recheck}>保存资料</Button></Form> : <><Alert type="warning" title={selected.data?.status === 'ACTIVE' ? '停用后拒绝新的员工与客户登录，现有身份及受控异步任务将被服务端拒绝。其他租户与平台账号不受影响。' : '启用后允许重新登录；已经失效的旧会话不会复活。'} /><Button danger type="primary" loading={mutation.isPending} disabled={recheck} onClick={submit}>确认状态操作</Button></>}
      <Button disabled={mutation.isPending} onClick={close}>取消</Button>
    </Modal>
  </>;
}
function TenantStores({ identity, tenant }: { identity: WebIdentity; tenant: Tenant }) {
  const { auth } = useSession('PLATFORM', false), client = useQueryClient();
  const allowed = (a: string) => hasPermission(identity, 'PLATFORM', 'platform:store:control-' + a);
  const data = useQuery(options<Store[]>(auth, identity, 'PLATFORM', 'stores', 'platform:store:control-list', `${path}/${tenant.id}/stores`));
  const [target, setTarget] = useState<Store>(), [mode, setMode] = useState<'create' | 'edit' | 'status'>(), [closing, setClosing] = useState(false), [recheck, setRecheck] = useState(false);
  const [form] = Form.useForm<components['schemas']['CreateControlStore']>(); const lock = useRef(false);
  const draft = useDraftGuard(() => Boolean(mode && form.isFieldsTouched()), 'PLATFORM');
  const mutation = useMutation({ retry: false, gcTime: 0, mutationFn: () => auth.runtime.request<Result>('PLATFORM', `${path}/${tenant.id}/stores${target ? '/' + target.id : ''}${mode === 'status' ? '/status' : ''}`, { method: mode === 'create' ? 'POST' : 'PUT', json: mode === 'status' ? { version: target!.version, status: target!.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' } satisfies components['schemas']['ControlStatus'] : mode === 'edit' ? { version: target!.version, name: form.getFieldValue('name') } satisfies components['schemas']['EditControlName'] : form.getFieldsValue(true) }), onSuccess: async () => { form.resetFields(); setClosing(true); setMode(undefined); await client.invalidateQueries({ predicate: q => q.queryKey[1] === 'control' }); }, onError: error => { form.setFields(managementFieldErrors(error, ['code', 'name'])); setRecheck(requiresRecheck(error)); }, onSettled: () => { lock.current = false; } });
  const open = (m: typeof mode, store?: Store) => { mutation.reset(); setRecheck(false); form.resetFields(); if (store) form.setFieldValue('name', store.name); setTarget(store); setMode(m); };
  const close = async () => { if (mutation.isPending || !await draft.mayDiscard()) return; form.resetFields(); setClosing(true); setMode(undefined); };
  const submit = () => { if (lock.current || mutation.isPending || recheck) return; lock.current = true; mutation.mutate(); };
  if (!allowed('list')) return null;
  return <>{draft.contextHolder}<Typography.Title level={3}>正式门店目录</Typography.Title><Typography.Paragraph>门店是可选经营范围，与内部组织独立。创建不会自动授予员工；在员工授权页面选择正式门店。</Typography.Paragraph>
    <ProTable<Store> toolbar={<>{allowed('create') && <Button disabled={!tenant.initialized || tenant.status !== 'ACTIVE' || closing || mutation.isPending} onClick={() => open('create')}>新建门店</Button>}</>} requestError={data.isError ? data.error : undefined} onRetry={() => void data.refetch()} showTable={!data.isError} rowKey="id" loading={data.isPending || data.isFetching} dataSource={data.data} pagination={false} scroll={{ x: 500 }} columns={[{ title: '编码', dataIndex: 'code' }, { title: '名称', dataIndex: 'name' }, { title: '状态', render: (_, s) => s.status === 'ACTIVE' ? '启用' : '停用' }, { title: '操作', render: (_, s) => <Space wrap>{allowed('update') && <Button disabled={closing || mutation.isPending || tenant.status !== 'ACTIVE'} onClick={() => open('edit', s)}>修改名称</Button>}{allowed('status') && <Button danger={s.status === 'ACTIVE'} disabled={closing || mutation.isPending || tenant.status !== 'ACTIVE'} onClick={() => open('status', s)}>{s.status === 'ACTIVE' ? '停用门店' : '启用门店'}</Button>}</Space> }]} />
    <Modal title={mode === 'create' ? '新建正式门店' : mode === 'edit' ? '修改门店名称' : '确认门店状态'} open={Boolean(mode)} destroyOnHidden maskClosable={false} footer={null} onCancel={close} afterClose={() => setClosing(false)}>{mutation.isError && <ErrorNotice error={mutation.error} title={recheck ? '请关闭并重新读取后操作' : undefined} />}{mode === 'status' ? <><Alert type="warning" title="门店状态改变后，现有租户身份失效，需重新登录后核对新的门店范围。" /><Button danger onClick={submit} loading={mutation.isPending} disabled={recheck}>确认门店状态</Button></> : <Form form={form} layout="vertical" onFinish={submit} disabled={mutation.isPending || recheck}>{mode === 'create' && <Form.Item name="code" label="门店编码" rules={[{ required: true, pattern: /^[A-Za-z0-9][A-Za-z0-9-]{0,31}$/, message: '请输入1至32位有效编码' }]}><Input /></Form.Item>}<Form.Item name="name" label="门店名称" rules={[managementNameRule]}><Input /></Form.Item><Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={recheck}>保存门店</Button></Form>}<Button disabled={mutation.isPending} onClick={close}>取消</Button></Modal>
  </>;
}
