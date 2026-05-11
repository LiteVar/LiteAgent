import React, { useEffect, useMemo, useRef } from 'react';
import ChatMessages from './ChatMessages';
import ChatInput from './ChatInput';
import ScrollToBottom from './ScrollToBottom';
import ChatHeader from './ChatHeader';
import { useChatContext, ChatProvider } from '@/contexts/ChatContext';
import CloseSvg from '@/assets/dashboard/close.svg';
import { Modal } from 'antd';
import ChatThoughtProcess from './ChatThoughtProcess';
import { throttle } from 'lodash';
import { ChatProps } from '@/types/chat';
import SearchResults from '@/pages/dataset/retrievalTest/components/SearchResults';
import { AgentDetailVO } from '@/client';

// 内部 Chat 组件，使用 Context
const ChatInner: React.FC<{
  agentId: string;
  agentInfo: AgentDetailVO;
  setAgentMap?: (map: any) => void;
  mode: 'prod' | 'dev';
}> = ({ agentId, agentInfo, setAgentMap, mode }) => {
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
  } = useChatContext();

  const messages = useMemo(() => messagesMap?.[agentId]?.messages || [], [agentId, messagesMap]);

  const agentMapRef = useRef(setAgentMap);
  agentMapRef.current = setAgentMap;

  const throttledSetAgentMap = useMemo(
    () => throttle((map: any) => agentMapRef.current?.(map), 100, { leading: true, trailing: true }),
    []
  );

  useEffect(() => {
    throttledSetAgentMap(messagesMap);
  }, [messagesMap, throttledSetAgentMap]);

  useEffect(() => {
    return () => { throttledSetAgentMap.cancel(); };
  }, [throttledSetAgentMap]);

  const ttsModelId = useMemo(() => {
    return agentInfo?.agent?.ttsModelId || '';
  }, [agentInfo]);

  const asrModelId = useMemo(() => {
    return agentInfo?.agent?.asrModelId || '';
  }, [agentInfo]);

  const fetchDataRef = useRef(fetchData);
  fetchDataRef.current = fetchData;
  const hasMoreRef = useRef(hasMore);
  hasMoreRef.current = hasMore;

  const handleLoadMoreScroll = useMemo(
    () => throttle(() => {
      if (scrollRef.current) {
        const { scrollTop } = scrollRef.current;
        if (scrollTop === 0 && hasMoreRef.current) {
          fetchDataRef.current();
        }
      }
    }, 200, { leading: false, trailing: true }),
    [scrollRef]
  );

  useEffect(() => {
    const current = scrollRef.current;
    if (current) {
      current.addEventListener('scroll', handleLoadMoreScroll);
    }
    return () => {
      if (current) {
        current.removeEventListener('scroll', handleLoadMoreScroll);
      }
      handleLoadMoreScroll.cancel();
    };
  }, [handleLoadMoreScroll, scrollRef]);

  return (
    <div className="w-full flex h-full overflow-hidden pl-0">
      <div
        className={
          mode === 'dev'
            ? 'max-w-full flex-1 flex flex-col bg-white/60 rounded-2xl h-full overflow-hidden'
            : 'flex-1 h-full overflow-hidden flex flex-col bg-white/60 border border-solid border-white rounded-2xl shadow-sm'
        }
      >
        <ChatHeader
          mode={mode}
          agentId={agentId}
          agentName={agentInfo?.agent?.name}
          onResetSession={onResetSession}
        />
        <div className="flex-1 flex flex-col relative overflow-hidden pt-0 pb-6">
          <div
            ref={scrollRef}
            className="flex-1 overflow-y-auto text-black/85 scroll-smooth px-6"
          >
            {clearList.slice(0, 3).map((clearMessage) => (
              <div
                key={clearMessage.id}
                className="mb-3 text-[#ACB6BE] text-xs text-center"
              >{`${clearMessage.createTime} 消息已被清空`}</div>
            ))}
            <ChatMessages
              agentId={agentInfo?.agent?.id!}
              lastThinkMessage={lastThinkMessage}
              onShowThinkMessage={onShowThinkMessage}
              messages={messages}
              agentIcon={agentInfo?.agent?.icon}
              mode={mode}
              onRetry={onRetry}
              asrLoading={asrLoading}
              ttsModelId={ttsModelId}
              ttsStreamSupported={!!agentInfo?.ttsModel?.streamable}
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
              <div className="mb-6">
                {`检索内容: `}
                <span className="text-blue-400">{knowledgeQueryText}</span>
              </div>
              <SearchResults results={knowledgeSearchResults} />
            </div>
          </Modal>
          <div className="relative flex flex-col items-center justify-center mt-4">
            {showScrollToBottom && <ScrollToBottom onClick={scrollToBottom} />}
            <ChatInput
              value={value}
              mode={mode}
              agentType={agentInfo?.agent?.type!}
              agentId={agentId}
              onChange={onInputChange}
              onSend={onSendMessage}
              setAsrLoading={setAsrLoading}
              asrModelId={asrModelId}
              asrStreamSupported={agentInfo?.asrModel?.streamable}
            />
          </div>
        </div>
      </div>
      {!!thinkDetailVisible && thinkMessageIndex != undefined && (
        <div className="flex-none w-[40%] bg-white h-full flex flex-col border-0 border-solid border-l border-l-[#D9D9D9] ml-4 rounded-2xl">
          <div className="h-[60px] pl-6 pr-8 flex items-center border-0 border-solid border-b border-b-[#E0E3E6]">
            <div className="text-lg text-[#1D4A6B] font-medium flex-1">过程详情</div>
            <img onClick={onCloseThinkMessage} className="w-5 h-5 flex-none cursor-pointer hover:bg-black/5 rounded-full transition-colors" src={CloseSvg} />
          </div>
          <div ref={thinkScrollRef} className="p-6 flex-1 overflow-y-auto overflow-x-hidden text-black/85">
            {(messages[thinkMessageIndex]?.thoughtProcessMessages?.length ?? 0) > 0 && (
              <ChatThoughtProcess
                onSearchKnowledgeResult={onSearchKnowledgeResult}
                thoughtProcessMessages={messages[thinkMessageIndex]?.thoughtProcessMessages!}
              />
            )}
          </div>
        </div>
      )}
    </div>
  );

};

// 主 Chat 组件包装器，提供 Context
const Chat: React.FC<ChatProps> = ({ mode = 'prod', agentId, agentInfo, setAgentMap }) => {
  return (
    <ChatProvider mode={mode} agentId={agentId} agentInfo={agentInfo!}>
      <ChatInner agentId={agentId} agentInfo={agentInfo!} setAgentMap={setAgentMap} mode={mode} />
    </ChatProvider>
  );
};

export default Chat;
