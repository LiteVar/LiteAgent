import React from 'react';
import { Button, Input, Pagination } from 'antd';

interface PaginationState {
  current: number;
  pageSize: number;
}

interface LogsHeaderProps {
  onRefresh: () => void;
  onSearch: (value: string) => void;
  sessionId: string | undefined;
  total: number;
  pagination: PaginationState;
  onPaginationChange: (page: number, pageSize?: number) => void;
}

const LogsHeader: React.FC<LogsHeaderProps> = ({
  onRefresh,
  onSearch,
  sessionId,
  total,
  pagination,
  onPaginationChange,
}) => {
  return (
    <>
      <h2 className="text-lg font-medium">聊天记录</h2>
      <div className="flex items-center">
        <Button type="primary" onClick={onRefresh}>
          刷新
        </Button>
        <Input.Search
          className="w-64 ml-4"
          placeholder="输入会话ID查询"
          allowClear
          value={sessionId}
          onSearch={onSearch}
        />
        <Pagination
          className="ml-8"
          total={total}
          current={pagination.current}
          pageSize={pagination.pageSize}
          showSizeChanger={false}
          onChange={onPaginationChange}
        />
      </div>
    </>
  );
};

export default LogsHeader;