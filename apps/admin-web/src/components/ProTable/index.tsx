import { Empty, Table } from 'antd';
import type { TableProps } from 'antd';
import type { ReactNode } from 'react';
import { ErrorNotice } from '../ErrorNotice';
import { Button } from 'antd';

export interface ProTableProps<T extends object> extends TableProps<T> {
  filters?: ReactNode;
  toolbar?: ReactNode;
  requestError?: unknown;
  errorContent?: ReactNode;
  showTable?: boolean;
  onRetry?: () => void;
  paginationContent?: ReactNode;
  statusContent?: ReactNode;
  regionLabel?: string;
}
/** 统一列表状态与操作布局；筛选草稿、URL和服务端查询仍由页面各自拥有。 */
export function ProTable<T extends object>({ filters, toolbar, requestError, errorContent, showTable = true, onRetry, paginationContent, statusContent, regionLabel = '列表表格，可横向滚动', ...table }: ProTableProps<T>) {
  return <section className="pro-table">
    {filters && <div className="pro-table-filters">{filters}</div>}
    {toolbar && <div className="pro-table-toolbar">{toolbar}</div>}
    {statusContent && <div className="pro-table-status" role="status">{statusContent}</div>}
    {errorContent ?? (requestError ? <div className="pro-table-error"><ErrorNotice error={requestError} />{onRetry && <Button onClick={onRetry}>重试</Button>}</div> : null)}
    {showTable && <div className="pro-table-region" role="region" aria-label={regionLabel} tabIndex={0}>
      <Table<T> bordered size="small" locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无数据" /> }} {...table} />
    </div>}
    {paginationContent && <div className="pro-table-pagination">{paginationContent}</div>}
  </section>;
}
