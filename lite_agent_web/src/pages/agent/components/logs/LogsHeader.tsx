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
    <div className="flex items-center justify-between mb-4">
      <div>
        <h2 className="text-lg font-medium mt-0 mb-1">聊天记录</h2>
        <div className="text-gray-500 text-sm">
          聊天记录包含了使用发布的agent、agent的API进行聊天后产生的对话记录
        </div>
      </div>
      <div className="flex items-center">
        <Button type="primary" className="h-10 bg-white border border-[#E0E3E6] text-[#383F44]" onClick={onRefresh}>
          刷新
        </Button>
        <Input.Search
          className="w-64 h-10 ml-4 overflow-hidden [&_.ant-input-affix-wrapper]:border-0 [&_.ant-input-affix-wrapper]:flex [&_.ant-input-affix-wrapper]:h-10 [&_.ant-btn]:border-0 [&_.ant-btn]:h-10"
          placeholder="输入会话ID查询"
          allowClear
          value={sessionId}
          onSearch={onSearch}
        />
        <Pagination
          className="ml-8 [&_li]:rounded-xl [&_li]:border [&_li]:border-solid [&_li]:border-[#E0E3E6] [&_li]:bg-white"
          total={total}
          current={pagination.current}
          pageSize={pagination.pageSize}
          showSizeChanger={false}
          onChange={onPaginationChange}
        />
      </div>
    </div>
  );
};

export default LogsHeader;