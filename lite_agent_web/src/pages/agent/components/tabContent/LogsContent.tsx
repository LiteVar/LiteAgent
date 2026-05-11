import React, { useCallback, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getV1ChatAgentChatOptions } from '@/client/@tanstack/query.gen';
import { MessageDTO } from '@/client';
import { Empty } from 'antd';
import LogsHeader from '../logs/LogsHeader';
import LogItem from '../logs/LogItem';
import KnowledgeModal from '../logs/KnowledgeModal';

interface ILogsContentProps {
  agentId: string;
  visible: boolean;
}

interface PaginationState {
  current: number;
  pageSize: number;
}

const LogsContent: React.FC<ILogsContentProps> = ({ agentId, visible }) => {
  const [sessionId, setSessionId] = useState<string | undefined>(undefined);
  const [knowledgeSearchResults, setKnowledgeSearchResults] = useState<any[]>([]);
  const [knowledgeQueryText, setKnowledgeQueryText] = useState<string>('');
  const [knowledgeResultVisible, setKnowledgeResultVisible] = useState<boolean>(false);
  const [pagination, setPagination] = useState<PaginationState>({
    current: 0,
    pageSize: 10,
  });

  const { data, refetch } = useQuery({
    ...getV1ChatAgentChatOptions({
      query: {
        agentId: agentId!,
        pageNo: pagination.current,
        pageSize: pagination.pageSize,
        sessionId: sessionId,
      },
    }),
    enabled: !!agentId,
  });

  const handleRefresh = useCallback(() => {
    setPagination({ current: 0, pageSize: 10 });
    setSessionId(undefined);
    refetch();
  }, [refetch]);

  const handleSearch = useCallback((value: string) => {
    setSessionId(value || undefined);
    setPagination({ current: 0, pageSize: 10 });
  }, []);

  const handlePaginationChange = useCallback((page: number, pageSize?: number) => {
    setPagination({ current: page, pageSize: pageSize || 10 });
  }, []);

  const handleKnowledgeClick = useCallback((queryText: string, results: any[]) => {
    setKnowledgeQueryText(queryText);
    setKnowledgeSearchResults(results);
    setKnowledgeResultVisible(true);
  }, []);

  const handleCloseModal = useCallback(() => {
    setKnowledgeResultVisible(false);
  }, []);

  return (
    <div className={visible ? "px-4 py-6 flex flex-col bg-white/60 rounded-2xl h-[calc(100%-48px)]" : "invisible w-0 h-0 m-0 p-0 overflow-hidden"}>
      <LogsHeader
        onRefresh={handleRefresh}
        onSearch={handleSearch}
        sessionId={sessionId}
        total={data?.data?.total || 0}
        pagination={pagination}
        onPaginationChange={handlePaginationChange}
      />

      <div className="space-y-6 overflow-y-auto flex-1">
        {data?.data?.list?.map((item: MessageDTO, index: number) => (
          <LogItem
            key={item.sessionId}
            item={item}
            index={index}
            onKnowledgeClick={handleKnowledgeClick}
          />
        ))}
        {data?.data?.total === 0 && (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无聊天记录" />
        )}
      </div>

      <KnowledgeModal
        visible={knowledgeResultVisible}
        queryText={knowledgeQueryText}
        results={knowledgeSearchResults}
        onClose={handleCloseModal}
      />
    </div>
  );
};

export default LogsContent;
