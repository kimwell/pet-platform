import { useEffect, useMemo, useRef, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useLocation, useRouter, useSearch } from '@tanstack/react-router';
import { Alert, Button, Empty, Form, Input, Pagination, Select, Space, Spin, Table, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import type { WebIdentity } from '../../../../shared/auth/spaces';
import { useSession } from '../../../../shared/auth/useSession';
import { ApiError } from '../../../../shared/api/ApiError';
import { ErrorNotice } from '../../../../shared/api/ErrorNotice';
import { displayInstant, displayText, displayTimeZone, employeeStatusText } from '../../../../shared/format';
import { hasPermission } from '../../../../shared/auth/permissions';
import { employeeDetailHref } from '../queries/detailSearch';
import { employeeListOptions } from '../queries/employees';
import { correctedPage, safePagination } from '../queries/pagination';
import { defaultSearch, employeeHref, keywordError, submitFilters, tableSort } from '../queries/search';
import type { EmployeeSearch } from '../queries/search';
import type { Employee } from '../api/employees';

export function EmployeeListPage({ identity }: { identity: WebIdentity }) {
  const router = useRouter(), location = useLocation(), queryClient = useQueryClient();
  const heading = useRef<HTMLHeadingElement>(null);
  const search = useSearch({ from: '/admin/identity/users' });
  const { auth } = useSession('STAFF', false);
  const options = useMemo(() => employeeListOptions(auth, identity, search), [auth, identity, search]);
  const query = useQuery(options);
  const [form] = Form.useForm<Pick<EmployeeSearch, 'keyword' | 'status'>>();
  const [pageNotice, setPageNotice] = useState<{ search: string; text: string }>();
  const forbidden = query.error instanceof ApiError && ['PERMISSION_DENIED', 'PASSWORD_CHANGE_REQUIRED'].includes(query.error.code ?? '');
  const pagination = query.data ? safePagination(query.data.total, search.page, search.pageSize) : undefined;
  const correction = pagination ? correctedPage(pagination, search.page, Boolean(location.state.employeePageCorrected)) : undefined;
  const fingerprint = JSON.stringify(search);
  useEffect(() => { heading.current?.focus(); }, []);
  useEffect(() => {
    // 前进后退、刷新及新查询均以 URL 为准，覆盖尚未提交的草稿。
    form.setFieldsValue({ keyword: search.keyword ?? '', status: search.status });
    form.setFields([{ name: 'keyword', errors: [] }, { name: 'status', errors: [] }]);
  }, [form, search.keyword, search.status, fingerprint]);
  useEffect(() => {
    if (forbidden) {
      void queryClient.cancelQueries({ queryKey: options.queryKey, exact: true });
      queryClient.removeQueries({ queryKey: options.queryKey, exact: true });
    }
  }, [forbidden, queryClient, options.queryKey]);
  useEffect(() => {
    if (correction !== undefined) void router.navigate({ href: employeeHref({ ...search, page: correction }), replace: true,
      state: { employeePageCorrected: true, employeeUrlNotice: '数据数量已变化，已返回有效页。' } });
  }, [correction, router, search]);
  const change = (next: EmployeeSearch) => {
    if (router.state.isLoading) return;
    if (employeeHref(next) !== employeeHref(search)) void router.navigate({ href: employeeHref(next), state: { employeePageCorrected: false } });
    else void query.refetch({ cancelRefetch: false });
  };
  const refresh = () => { setPageNotice(undefined); void query.refetch(); };
  const sortOrder = search.sortOrder === 'asc' ? 'ascend' : 'descend';
  const longText = (value: string) => <Typography.Text ellipsis={{ tooltip: displayText(value) }} title={displayText(value)} tabIndex={0}>{displayText(value)}</Typography.Text>;
  const columns: TableColumnsType<Employee> = [
    { title: '员工 ID', dataIndex: 'id', key: 'id', width: 310, render: longText },
    { title: '账号', dataIndex: 'loginName', key: 'loginName', width: 220, render: longText },
    { title: '姓名', dataIndex: 'displayName', key: 'displayName', width: 200, render: longText },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (status: Employee['status']) => <Tag color={status === 'ACTIVE' ? 'success' : 'default'}>{employeeStatusText(status)}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 200, render: displayInstant },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 200, render: displayInstant },
  ].map(column => ({ ...column, sorter: true, sortOrder: column.key === search.sortBy ? sortOrder : null,
    // 默认降序列先切升序，清除时恢复默认；否则受控默认值会使点击一直落回降序。
    sortDirections: column.key === 'createdAt' ? ['descend', 'ascend'] : ['ascend', 'descend'], showSorterTooltip: { title: '按此列进行服务端排序' } }));
  if (hasPermission(identity, 'STAFF', 'identity:user:detail')) columns.push({ title: '操作', key: 'actions', width: 120,
    render: (_, employee) => <Button type="link" onClick={() => void router.navigate({ href: employeeDetailHref(employee.id, employeeHref(search)) })} aria-label={`查看 ${employee.displayName} 的详情`}>查看详情</Button> });
  const failed = query.isError && !(query.error instanceof ApiError && query.error.kind === 'CANCELLED');
  // 权限错误立即隐藏已有行；统一 401 的身份失效仍交由会话层处理。
  const data = forbidden ? undefined : query.data;
  return <section className="employee-list" aria-labelledby="employees-heading">
    <div className="page-heading"><div><Typography.Title ref={heading} tabIndex={-1} level={1} id="employees-heading">员工列表</Typography.Title><Typography.Paragraph type="secondary">当前授权范围内的员工。时间：{displayTimeZone}</Typography.Paragraph></div>
      <Button onClick={refresh} disabled={query.isFetching || forbidden} loading={Boolean(data && query.isFetching)}>刷新列表</Button></div>
    {location.state.employeeUrlNotice && <Alert showIcon type="warning" title={location.state.employeeUrlNotice} closable />}
    <Form form={form} layout="vertical" className="employee-filters" onFinish={draft => change(submitFilters(search, draft))} scrollToFirstError={{ focus: true }}>
      <Form.Item name="keyword" label="关键词" rules={[{ validator: async (_, value: string | undefined) => { if (value) { const error = keywordError(value); if (error) throw new Error(error); } } }]}>
        <Input placeholder="搜索账号或姓名" allowClear autoComplete="off" />
      </Form.Item>
      <Form.Item name="status" label="状态"><Select allowClear placeholder="全部状态" options={[{ value: 'ACTIVE', label: '启用' }, { value: 'DISABLED', label: '停用' }]} /></Form.Item>
      <Space className="employee-filter-actions"><Button type="primary" htmlType="submit" aria-label="查询">查询</Button><Button aria-label="重置" onClick={() => {
        form.setFieldsValue({ keyword: '', status: undefined }); form.setFields([{ name: 'keyword', errors: [] }]); change({ ...defaultSearch });
      }}>重置</Button></Space>
    </Form>
    {pageNotice?.search === fingerprint && <Alert showIcon type="warning" title={pageNotice.text} />}
    {pagination?.kind === 'limited' && <Alert showIcon type="warning" title="分页范围受限" description={<>{pagination.reason} 精确总数：{query.data?.total}。请缩小筛选范围。</>} />}
    {pagination?.kind === 'safe' && pagination.lastPage > pagination.maximumPage && <Alert showIcon type="warning" title={`当前接口最多可访问第 ${pagination.maximumPage} 页；更深结果请缩小筛选范围。`} />}
    {pagination?.kind === 'safe' && search.page > pagination.lastPage && location.state.employeePageCorrected && <Alert showIcon type="warning"
      title="数据数量再次变化，请刷新或返回第一页。" action={<Button onClick={() => change({ ...search, page: 1 })}>返回第一页</Button>} />}
    {failed && <div className="employee-query-error"><ErrorNotice error={query.error} title={forbidden ? '当前账号没有查看员工列表的权限' : data ? '刷新失败，以下仍为上次成功结果' : '员工列表加载失败'}
      messages={query.error instanceof ApiError && query.error.status === 422 ? [query.error.code === 'RESULT_TOO_LARGE' ? '当前页码过深，请返回第一页或缩小查询范围。' : '请检查关键词及查询条件后重新查询。', ...(query.error.fieldErrors?.map(field => `${field.field === 'keyword' ? '关键词' : '查询参数'}：${field.message}`) ?? [])] : []} />
      {!forbidden && <Button aria-label="重试" onClick={refresh} disabled={query.isFetching}>重试</Button>}</div>}
    {!data && query.isPending && <div className="page-loading" role="status"><Spin /> 正在加载员工列表…</div>}
    {data && correction === undefined && <>
      <div role="status" aria-live="polite">{query.isFetching ? '正在刷新，以下仍为上次成功结果。' : `本页 ${data.items.length} 条，共 ${data.total} 条。`}</div>
      <div className="employee-table-region" role="region" aria-label="员工列表表格，可横向滚动" tabIndex={0}>
        <Table<Employee> columns={columns} dataSource={data.items} rowKey="id" pagination={false} scroll={{ x: 1230 }} tableLayout="fixed"
          locale={{ emptyText: <Empty description={search.keyword !== undefined || search.status !== undefined ? '没有符合筛选条件的员工' : location.state.employeePageCorrected && search.page > 1 ? '当前页数据已变化，请刷新或返回第一页' : '当前授权范围内暂无员工'} image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
          onChange={(_, __, sorter, extra) => { if (extra.action === 'sort') { const single = Array.isArray(sorter) ? sorter[0] : sorter; change({ ...search, ...tableSort(single?.columnKey, single?.order), page: 1 }); } }} />
      </div>
      {pagination?.kind === 'safe' && <Pagination className="employee-pagination" current={search.page} pageSize={search.pageSize} total={pagination.total}
        showSizeChanger pageSizeOptions={[10, 20, 50, 100]} showQuickJumper responsive
        showTotal={total => `共 ${total} 条`} onChange={(page, pageSize) => {
          const next = pageSize !== search.pageSize ? 1 : page;
          // 组件可能给出安全整数 total 的极深末页；先检查接口上限，避免页号校验抛错。
          const bound = safePagination(data.total, 1, pageSize);
          if (bound.kind === 'safe' && (!Number.isSafeInteger(next) || next < 1 || next > bound.maximumPage)) { setPageNotice({ search: fingerprint, text: '当前页码超过接口可访问范围，请缩小筛选范围。' }); return; }
          change({ ...search, page: next, pageSize });
        }} />}
    </>}
    {query.error instanceof ApiError && query.error.code === 'RESULT_TOO_LARGE' && <Button onClick={() => change({ ...search, page: 1 })}>返回第一页</Button>}
  </section>;
}

declare module '@tanstack/react-router' {
  interface HistoryState { employeeUrlNotice?: string; employeePageCorrected?: boolean }
}
