import React from 'react';
import { Table } from 'antd';
import type { TableColumnsType } from 'antd';
import { DatasetRetrieveHistory } from '@/client';
import path from 'path';

interface SearchHistoryProps {
  history: DatasetRetrieveHistory[];
  onSelect: (record: DatasetRetrieveHistory) => void;
  selectedId?: string;
  pageNo: number;
  total: number;
  setPageNo: (pageNo: number) => void;
}

const SearchHistory: React.FC<SearchHistoryProps> = ({
  history,
  onSelect,
  selectedId,
  total,
  pageNo,
  setPageNo,
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
    <div className="flex flex-col">
      <h3 className="text-lg font-medium mb-4">检索记录</h3>
      <div className="flex-1 overflow-auto">
        <Table
          dataSource={history}
          columns={columns}
          rowKey="id"
          scroll={{ y: '500px' }}
          onRow={(record) => ({
            onClick: () => onSelect(record),
            className: `cursor-pointer transition-colors ${
              selectedId === record.id ? 'bg-blue-50 hover:bg-blue-50' : 'hover:bg-gray-50'
            }`,
          })}
          pagination={{
            current: pageNo,
            pageSize: 10,
            total: total,
            onChange: (page) => setPageNo(page),
          }}
        />
      </div>
    </div>
  );
};

export default SearchHistory;
