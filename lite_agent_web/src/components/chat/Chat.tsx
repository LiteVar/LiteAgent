import React, { useEffect, useMemo, useRef } from 'react';
import {
  AgentDetailVO,
  OutMessage,
} from '@/client';
import ChatMessages from './ChatMessages';
import ChatInput from './ChatInput';
import ScrollToBottom from './ScrollToBottom';
import ChatHeader from './ChatHeader';
import useChatHandleEvent from '@/hooks/useChatHandleEvent';
import CloseSvg from '@/assets/dashboard/close.svg';
import { Modal } from 'antd';
import SearchResults from '../../pages/dataset/retrievalTest/components/SearchResults';
import ChatThoughtProcess from './ChatThoughtProcess';
import { debounce } from 'lodash';

// 扩展消息类型，兼容后续扩展
export type AgentMessage = OutMessage & {
  content?: any;
  think?: any;
  createTime?: string;
  messages?: AgentMessage[];
};

export type AgentMessageMap = {
  [id: string]: {
    messages: AgentMessage[];
  };
};

interface IChatProps {
  mode: 'dev' | 'prod';
  agentInfo: AgentDetailVO | undefined;
  agentId: string;
  asrEnabled: boolean;
  setAgentMap?(agentMap: AgentMessageMap): void;
}

const Chat: React.FC<IChatProps> = ({ mode = 'prod', agentId, agentInfo, setAgentMap }) => {

  const {
    messagesMap,
    onResetSession,
    scrollRef,
    thinkScrollRef,
    clearList,
    onRetry,
    showScrollToBottom,
    scrollToBottom,
    value,
    onInputChange,
    onSendMessage,
    knowledgeSearchResults,
    knowledgeResultVisible,
    setKnowledgeResultVisible,
    onSearchKnowledgeResult,
    knowledgeQueryText,
    onShowThinkMessage,
    onCloseThinkMessage,
    thinkDetailVisible,
    thinkMessageIndex,
    asrLoading,
    setAsrLoading,
    fetchData,
    hasMore,
    lastThinkMessage,
  } = useChatHandleEvent({ mode, agentId, agentInfo });

  const messages = useMemo(() => messagesMap?.[agentId]?.messages || [], [agentId, messagesMap]);

  useEffect(() => {
    setAgentMap?.(messagesMap);
  }, [messagesMap, setAgentMap]);

  const ttsModelId = useMemo(() => {
    return agentInfo?.agent?.ttsModelId || '';
  }, [agentInfo]);

  const asrModelId = useMemo(() => {
    return agentInfo?.agent?.asrModelId || '';
  }, [agentInfo]);

  // 新增：滚动事件处理函数
  const handleScroll = debounce(() => {
    if (scrollRef.current) {
      const { scrollTop } = scrollRef.current;
      // 判断是否滚动到顶部
      if (scrollTop === 0 && hasMore) {
        fetchData(); // 调用加载更多消息的函数
      }
    }
  }, 200);

  // 新增：监听滚动事件
  useEffect(() => {
    const current = scrollRef.current;
    if (current) {
      current.addEventListener('scroll', handleScroll);
    }
    return () => {
      if (current) {
        current.removeEventListener('scroll', handleScroll);
      }
    };
  }, [fetchData, hasMore]);

  return (
    <div className="w-full flex h-full overflow-hidden">
      <div
        className={
          mode === 'dev'
            ? 'max-w-full flex-1 flex flex-col bg-[#FFF] h-full overflow-hidden'
            : 'w-full h-[100vh] overflow-hidden flex flex-col'
        }
      >
        <ChatHeader
          mode={mode}
          agentId={agentId}
          agentName={agentInfo?.agent?.name}
          onResetSession={onResetSession}
        />
        <div
          className={
            mode === 'dev'
              ? 'flex-1 flex flex-col overflow-hidden'
              : 'w-full flex-1 flex flex-col relative overflow-hidden'
          }
        >
          <div
            ref={scrollRef}
            className={
              mode === 'dev'
                ? 'flex-1 px-6 py-7 overflow-y-auto text-black/85'
                : 'w-full flex-1 overflow-y-auto text-black/85'
            }
          >
            {clearList.slice(0, 3).map((clearMessage) => (
              <div className="mb-3 text-[#999] text-xs text-center">{`${clearMessage.createTime} 消息已被清空`}</div>
            ))}
            <ChatMessages
              lastThinkMessage={lastThinkMessage}
              onShowThinkMessage={onShowThinkMessage}
              messages={messages}
              agentIcon={agentInfo?.agent?.icon}
              mode={mode}
              onRetry={onRetry}
              asrLoading={asrLoading}
              ttsModelId={ttsModelId}
              onSendMessage={onSendMessage}
            />
          </div>
          <Modal
            title=""
            centered
            open={knowledgeResultVisible}
            footer={null}
            width={800}
            onCancel={() => setKnowledgeResultVisible(false)}
          >
            <div className="pt-3">
              <div className="mb-6">{`检索内容: `}<span className="text-blue-400">{knowledgeQueryText}</span></div>
              <SearchResults results={knowledgeSearchResults} />
            </div>
          </Modal>
          <div className="relative flex items-center justify-center mb-2">
            {showScrollToBottom && <ScrollToBottom onClick={scrollToBottom} />}
            <ChatInput
              value={value}
              mode={mode}
              agentType={agentInfo?.agent?.type}
              onChange={onInputChange}
              onSend={onSendMessage}
              setAsrLoading={setAsrLoading}
              asrModelId={asrModelId}
            />
          </div>
        </div>
      </div>
      {!!thinkDetailVisible && thinkMessageIndex != undefined && <div
        className='flex-none w-[40%] min-w-[360px] bg-[#fff] h-full flex flex-col border-0 border-solid border-l border-l-[#D9D9D9]'>
        <div className='h-[60px] pl-6 pr-8 flex items-center border-0 border-solid border-b border-b-[#D9D9D9]'>
          <div className='text-lg text-[#333] flex-1'>过程详情</div>
          <img onClick={onCloseThinkMessage} className='w-5 h-5 flex-none cursor-pointer' src={CloseSvg} />
        </div>
        <div ref={thinkScrollRef} className='p-6 flex-1 overflow-y-auto overflow-x-hidden text-black/85'>
          {messages[thinkMessageIndex]?.thoughtProcessMessages?.length > 0 &&
            <ChatThoughtProcess onSearchKnowledgeResult={onSearchKnowledgeResult}
              thoughtProcessMessages={messages[thinkMessageIndex]?.thoughtProcessMessages} />}
        </div>
      </div>}
    </div>
  );
};

export default Chat;
