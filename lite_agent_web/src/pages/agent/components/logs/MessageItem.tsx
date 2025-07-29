import React, { useCallback } from 'react';
import { message } from 'antd';
import { DocumentSegment, getV1DatasetRetrieveHistoryById, OutMessage, SegmentVO } from '@/client';
import { TaskMessageType, MessageRole } from '@/types/Message';
import ResponseCode from '@/constants/ResponseCode';
import ToolCallsDisplay from './ToolCallsDisplay';

interface MessageItemProps {
  message: OutMessage | any;
  onKnowledgeClick: (queryText: string, results: SegmentVO[]) => void;
}

const MessageItem: React.FC<MessageItemProps> = ({ message: mes, onKnowledgeClick }) => {
  const getRoleDisplay = useCallback((role: string, type: string): string => {
    if (role === MessageRole.USER) return 'Human';
    if (role === MessageRole.TOOL) return 'Tool Return';
    if (role === MessageRole.SUBAGENT) return 'Sub Agent';
    if (role === MessageRole.REFLECTION) return 'Reflection';
    
    if ([MessageRole.ASSISTANT, MessageRole.AGENT].includes(role as MessageRole)) {
      if (type === TaskMessageType.FUNCTION_CALL_LIST) return 'Tool';
      if (type === TaskMessageType.THINK) return 'Think';
      if (type === TaskMessageType.REFLECT) return 'Reflect';
      if (type === TaskMessageType.KNOWLEDGE) return 'Data Retrieval';
      
      return role === MessageRole.ASSISTANT ? 'AI End' : 'Agent';
    }
    
    return 'AI';
  }, []);

  const handleKnowledgeItemClick = useCallback(async (item: { id?: string; datasetName?: string }, retrieveContent: string) => {
    const noResult: DocumentSegment[] = [{ id: '000', content: '无没有找到相关搜索结果' }];
    
    if (!item.id) {
      message.error('检索项ID无效');
      return;
    }

    message.info('正在加载检索记录...');
    
    try {
      const res = await getV1DatasetRetrieveHistoryById({
        path: {
          id: item.id,
        },
      });

      message.destroy();
      if (res.data?.code === ResponseCode.S_OK) {
        const result = res.data.data;
        const searchResults = result && result.length > 0 ? result : noResult;
        onKnowledgeClick(retrieveContent, searchResults);
      } else {
        message.error(res.data?.message || '获取检索记录失败');
      }
    } catch (error) {
      message.destroy();
      message.error('获取检索记录失败');
      console.error('Failed to fetch retrieval history:', error);
    }
  }, [onKnowledgeClick]);

  const renderToolReturn = useCallback(() => (
    <div className="bg-gray-50 px-2 py-1 rounded-md">
      <p className="text-sm text-gray-500 mb-1">工具调用结果：{JSON.stringify(mes.content)}</p>
      <p className="text-sm text-gray-500">工具Id：{JSON.stringify(mes.toolCallId)}</p>
    </div>
  ), [mes.content, mes.toolCallId]);

  const renderDispatch = useCallback(() => (
    <p className="text-base mb-2">
      输入指令：{mes.content?.cmd}
    </p>
  ), [mes.content?.cmd]);

  const renderAgentSwitch = useCallback(() => (
    <p className="text-base mb-2">
      调用agent：{mes.content?.agentName}
    </p>
  ), [mes.content?.agentName]);

  const renderKnowledge = useCallback(() => {
    const retrieveContent = mes.content?.retrieveContent || '';
    const infoList = mes.content?.info || [];

    return (
      <div className="text-sm mb-2 text-gray-600">
        <span>检索知识库</span>
        <div className="my-2">
          <span>检索内容: </span>
          <strong>{retrieveContent}</strong>
        </div>
        <div>
          <span>检索结果: </span>
          {infoList.length > 0 ? (
            infoList.map((item: { id?: string; datasetName?: string }, index: number) => (
              <span 
                className="cursor-pointer text-blue-400 hover:text-blue-600 transition-colors" 
                key={item.id || index} 
                onClick={(e) => {
                  e.stopPropagation();
                  handleKnowledgeItemClick(item, retrieveContent);
                }}
              >
                {`${item.datasetName || 'Unknown'}${index < infoList.length - 1 ? ', ' : ''}`}
              </span>
            ))
          ) : (
            <strong>空</strong>
          )}
        </div>
      </div>
    );
  }, [mes.content, handleKnowledgeItemClick]);

  const renderContent = useCallback(() => {
    switch (mes.type) {
      case TaskMessageType.TOOL_RETURN:
        return renderToolReturn();
      case TaskMessageType.DISPATCH:
        return renderDispatch();
      case TaskMessageType.AGENT_SWITCH:
        return renderAgentSwitch();
      case TaskMessageType.KNOWLEDGE:
        return renderKnowledge();
      default:
        return (
          <p className={`text-base mb-2 ${
            (mes.type === TaskMessageType.THINK || mes.type === TaskMessageType.REFLECT) 
              ? 'text-sm text-gray-500' 
              : ''
          }`}>
            {JSON.stringify(mes.content)}
          </p>
        );
    }
  }, [mes.type, mes.content, renderToolReturn, renderDispatch, renderAgentSwitch, renderKnowledge]);

  return (
    <div className="border-b border-gray-100 px-4 py-2"
    style={{ borderBottom: '1px solid #e5e7eb' }}>
      {mes.content && renderContent()}
      {mes.toolCalls && <ToolCallsDisplay toolCalls={mes.toolCalls} />}
      <div className="flex items-start gap-2 text-sm text-gray-500">
        <span>{getRoleDisplay(mes.role || '', mes.type || '')}</span>
        <span>{mes.createTime}</span>
      </div>
    </div>
  );
};

export default MessageItem;