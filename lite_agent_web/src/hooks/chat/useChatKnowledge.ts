import { useState, useCallback } from 'react';
import { message } from 'antd';
import { getV1DatasetRetrieveHistoryById, DocumentSegment, SegmentVO } from '@/client';
import ResponseCode from '@/constants/ResponseCode';

export const useChatKnowledge = () => {
  const [knowledgeResultVisible, setKnowledgeResultVisible] = useState(false);
  const [knowledgeSearchResults, setKnowledgeSearchResults] = useState<SegmentVO[]>([]);
  const [knowledgeQueryText, setKnowledgeQueryText] = useState<string>('');

  const onSearchKnowledgeResult = useCallback(async (event: React.MouseEvent<HTMLSpanElement>, id: string, query: string) => {
    event.stopPropagation();
    const noResult: DocumentSegment[] = [{ id: '000', content: '无没有找到相关搜索结果' }];
    message.info('正在加载检索记录...');
    const res = await getV1DatasetRetrieveHistoryById({
      path: {
        id: id!,
      },
    });

    message.destroy();
    if (res.data?.code === ResponseCode.S_OK) {
      setKnowledgeQueryText(query);
      const result = res.data.data;
      if (result && result.length > 0) {
        setKnowledgeSearchResults(result);
      } else {
        setKnowledgeSearchResults(noResult);
      }
      setKnowledgeResultVisible(true);
    } else {
      message.error(res.data?.message || '获取检索记录失败');
    }
  }, []);

  const closeKnowledgeResult = useCallback(() => {
    setKnowledgeResultVisible(false);
  }, []);

  return {
    knowledgeResultVisible,
    knowledgeSearchResults,
    knowledgeQueryText,
    onSearchKnowledgeResult,
    setKnowledgeResultVisible,
    closeKnowledgeResult,
  };
};