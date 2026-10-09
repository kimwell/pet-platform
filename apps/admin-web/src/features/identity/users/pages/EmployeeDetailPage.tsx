import { EmployeeActions } from '../../management/EmployeeActions';
import { useEffect, useMemo, useRef } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams, useRouter, useSearch } from '@tanstack/react-router';
import { Button, Descriptions, Result, Spin, Tag, Typography } from 'antd';
import type { WebIdentity } from '../../../../shared/auth/spaces';
import { useSession } from '../../../../shared/auth/useSession';
import { hasPermission } from '../../../../shared/auth/permissions';
import { ApiError, errorText } from '../../../../shared/api/ApiError';
import { ErrorNotice } from '../../../../shared/api/ErrorNotice';
import { displayInstant, displayText, displayTimeZone, employeeStatusText } from '../../../../shared/format';
import { employeeDetailOptions } from '../queries/employees';
import { employeeDetailPath, isEmployeeId, listReturnTo } from '../queries/detailSearch';
import { employeePath } from '../queries/search';

export function EmployeeDetailPage({ identity }: { identity: WebIdentity }) {
  const { employeeId } = useParams({ from: employeeDetailPath });
  const router = useRouter();
  const search = useSearch({ from: employeeDetailPath });
  const { auth } = useSession('STAFF', false);
  const client = useQueryClient();
  const heading = useRef<HTMLHeadingElement>(null);
  const options = useMemo(() => employeeDetailOptions(auth, identity, employeeId), [auth, identity, employeeId]);
  const query = useQuery(options);
  const allowed = hasPermission(identity, 'STAFF', 'identity:user:detail');
  const hasList = hasPermission(identity, 'STAFF', 'identity:user:list');
  const terminal = query.error instanceof ApiError && [401, 403, 404].includes(query.error.status ?? 0);
  const cancelled = query.error instanceof ApiError && query.error.kind === 'CANCELLED';
  const data = allowed && !terminal ? query.data : undefined;
  useEffect(() => { heading.current?.focus(); }, [employeeId, query.isError]);
  useEffect(() => () => {
    // 等待观察者卸载；StrictMode 的探测重挂载不能清掉仍在使用的查询。
    queueMicrotask(() => {
      const current = client.getQueryCache().find({ queryKey: options.queryKey, exact: true });
      if (current?.getObserversCount() === 0) {
        void client.cancelQueries({ queryKey: options.queryKey, exact: true });
        client.removeQueries({ queryKey: options.queryKey, exact: true });
      }
    });
  }, [client, options.queryKey]);
  useEffect(() => {
    if (terminal) {
      // 保留错误与trace供页面定位，移除已失去授权的旧详情。
      client.getQueryCache().find({ queryKey: options.queryKey, exact: true })?.setState({ data: undefined });
    }
  }, [terminal, client, options.queryKey]);
  const retry = () => { void query.refetch(); };
  const title = query.error instanceof ApiError && query.error.status === 404 ? '员工不存在或不可访问'
    : query.error instanceof ApiError && query.error.status === 403 ? '当前账号没有查看员工详情的权限'
    : data ? '刷新失败，以下仍为上次成功结果' : '员工详情加载失败';
  const text = (value: string) => <Typography.Text className="employee-detail-text" title={displayText(value)} tabIndex={0}>{displayText(value)}</Typography.Text>;
  return <section className="employee-detail" aria-labelledby="employee-detail-heading">
    <div className="page-heading"><div><Typography.Title ref={heading} tabIndex={-1} level={1} id="employee-detail-heading">员工详情</Typography.Title>
      <Typography.Paragraph type="secondary">当前详情操作授权范围内的员工。时间：{displayTimeZone}</Typography.Paragraph></div>
      <Button onClick={() => void router.navigate({ href: hasList ? listReturnTo(search.returnTo) ?? employeePath : '/admin' })}>{hasList ? '返回列表' : '返回当前身份'}</Button>
    </div>
    {!allowed ? <Result status="403" title="当前账号没有查看员工详情的权限" />
      : !isEmployeeId(employeeId) ? <Result status="warning" title="员工地址中的 ID 格式无效" subTitle="请使用小写 UUID v4 员工地址。" />
      : <>
        {!data && query.isPending && <div className="page-loading" role="status"><Spin /> 正在加载员工详情…</div>}
        {query.isError && !cancelled && <div className="employee-query-error"><ErrorNotice error={query.error} title={title}
          messages={terminal ? [] : [errorText(query.error)]} />
          {!terminal && <Button aria-label="重试" onClick={retry} disabled={query.isFetching}>重试</Button>}</div>}
        {data && <>
          <div role="status" aria-live="polite">{query.isFetching ? '正在刷新，以下仍为上次成功结果。' : query.isError ? '刷新失败，以下仍为上次成功结果。' : '员工详情已加载。'}</div>
          <Button onClick={retry} disabled={query.isFetching} loading={query.isFetching}>刷新详情</Button>
          <EmployeeActions identity={identity} employeeId={employeeId} /><Descriptions bordered column={{ xs: 1, sm: 1, md: 2 }} items={[
            { key: 'id', label: '员工 ID', children: text(data.id) },
            { key: 'loginName', label: '账号', children: text(data.loginName) },
            { key: 'displayName', label: '姓名', children: text(data.displayName) },
            { key: 'status', label: '状态', children: <Tag color={data.status === 'ACTIVE' ? 'success' : 'default'}>{employeeStatusText(data.status)}</Tag> },
            { key: 'createdAt', label: '创建时间', children: displayInstant(data.createdAt) },
            { key: 'updatedAt', label: '更新时间', children: displayInstant(data.updatedAt) },
          ]} />
        </>}
      </>}
  </section>;
}
