import React, { useMemo } from 'react';
import ChatMessage from './ChatMessage';
import { Spin } from 'antd';
import { LoadingOutlined } from '@ant-design/icons';
import { MessageRole } from '../../types/Message';
import '@/assets/styles/chatMessages.css';

interface IChatMessagesProps {
  onShowThinkMessage: (index: number) => void;
  messages: any[];
  agentIcon: string;
  mode: 'dev' | 'prod';
  onRetry: (index: number) => void;
  asrLoading: boolean;
  ttsModelId: string;
  lastThinkMessage: React.RefObject<HTMLDivElement>;
  onSendMessage: (message: string) => void;
}

const ChatMessages: React.FC<IChatMessagesProps> = ({
  onShowThinkMessage,
  messages,
  agentIcon,
  mode,
  onRetry,
  asrLoading,
  ttsModelId,
  onSendMessage,
  lastThinkMessage,
}) => {

  const isLastThinkMessage = useMemo(() => {
    let msgs = JSON.parse(JSON.stringify(messages));
    msgs = msgs.filter(item => item.role != MessageRole.SEPARATOR)
    return msgs.length > 0 && msgs[msgs.length - 1]?.thoughtProcessMessages?.length > 0;
  }, [messages]);

  return (
    <div className={mode === 'dev' ? '' : 'w-[768px] mx-auto text-base'}>
      <div className="pt-8" />
      {messages.map((message, index) => {
        const isLastMessage = index === messages.length - 1;
        return (
          <ChatMessage
            isLastThinkMessage={isLastThinkMessage}
            lastThinkMessage={lastThinkMessage}
            onShowThinkMessage={onShowThinkMessage}
            key={index}
            message={message}
            agentIcon={agentIcon}
            mode={mode}
            isLastMessage={isLastMessage}
            onRetry={onRetry}
            ttsModelId={ttsModelId}
            onSendMessage={onSendMessage}
          />
        )}
      )}
      {asrLoading && (
        <div className="mb-8">
          <div className="min-h-16 text-message flex w-full items-start flex-row-reverse gap-2">
            <Spin size="large" tip="音频识别中..." indicator={<LoadingOutlined spin />}>
              <div style={{ width: 100 }}></div>
            </Spin>
          </div>
        </div>
      )}
      <div className="pb-8" />
    </div>
  );
};

export default ChatMessages;
