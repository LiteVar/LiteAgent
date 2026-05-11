import React from 'react';
import { Table } from 'antd';
import type { TableColumnsType } from 'antd';
import { DatasetRetrieveHistory } from '@/client';

interface SearchHistoryProps {
  history: DatasetRetrieveHistory[];
  onSelect: (record: DatasetRetrieveHistory) => void;
  selectedId?: string;
  pageNo: number;
  total: number;
  setPageNo: (pageNo: number) => void;
  pageSize: number;
  setPageSize: (pageSize: number) => void;
}

const SearchHistory: React.FC<SearchHistoryProps> = ({
  history,
  onSelect,
  selectedId,
  total,
  pageNo,
  setPageNo,
  pageSize,
  setPageSize,
}) => {
  const columns: TableColumnsType<DatasetRetrieveHistory> = [
    {
      title: '来源',
      dataIndex: 'retrieveType',
      key: 'retrieveType',
      width: 100,
      render: (type: string) => {
        return type === 'TEST' ? '检索测试' : 'Agent';
      },
    },
    {
      title: '查询文本',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
    },
    {
      title: '检索时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (time: string) => new Date(time).toLocaleString(),
    },
  ];

  return (
    <div className="flex flex-col h-full gap-4">
      <h3 className="text-base font-semibold text-[#1D4A6B] m-0">检索记录</h3>
      <div className="flex-1 overflow-hidden rounded-xl border border-black/5 bg-white/20">
        <Table
          dataSource={history}
          columns={columns}
          rowKey="id"
          scroll={{ y: 'calc(100vh - 600px)' }}
          onRow={(record) => ({
            onClick: () => onSelect(record),
            className: `cursor-pointer transition-all duration-200 ${
              selectedId === record.id 
                ? 'bg-blue-50/80 !text-blue-600' 
                : 'hover:bg-white/40'
            }`,
          })}
          pagination={{
            current: pageNo,
            pageSize: pageSize,
            total: total,
            showSizeChanger: true,
            pageSizeOptions: [10, 20, 50, 100],
            className: 'px-4 py-2 !m-0 border-t border-black/5',
            onChange: (page, pageSize) => {
              setPageNo(page);
              setPageSize(pageSize || 10);
            },
          }}
          className="customTable smallTable [&_.ant-table]:bg-transparent [&_.ant-table-header_.ant-table-thead_tr_th]:bg-[#F2F3F5] [&_.ant-table-body]:!overflow-y-auto"
        />
      </div>
    </div>
  );
};

export default SearchHistory;
