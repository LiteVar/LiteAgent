import React, { createContext, useContext, ReactNode, useRef, useEffect } from 'react';
import { ChangeEvent } from 'react';
import { 
  AgentMessage, 
  AgentMessageMap, 
  UseChatHandleEventProps, 
  AgentStatusRef,
  SSEEventData,
  DeltaEventData
} from '@/types/chat';
import { AgentChatMessageClear, SegmentVO } from '@/client';
import { useChatSession } from '@/hooks/chat/useChatSession';
import { useChatScroll } from '@/hooks/chat/useChatScroll';
import { useChatInput } from '@/hooks/chat/useChatInput';
import { useChatKnowledge } from '@/hooks/chat/useChatKnowledge';
import { useChatAudio } from '@/hooks/chat/useChatAudio';
import { useChatSSE } from '@/hooks/chat/useChatSSE';
import { useChatMessages } from '@/hooks/chat/useChatMessages';
import { useChatMessageEvents } from '@/hooks/chat/useChatMessageEvents';
import { useChatDeltaEvents } from '@/hooks/chat/useChatDeltaEvents';
import { use } from 'marked';

// 完整的 Context 接口定义
interface ChatContextValue {
  // 消息相关
  messagesMap: AgentMessageMap;
  clearList: AgentChatMessageClear[];
  
  // 输入相关
  value: string;
  onInputChange: (e: ChangeEvent<HTMLTextAreaElement>) => void;
  onSendMessage: (type: 'text' | 'execute' | 'imageUrl', text?: string) => Promise<void>;
  asrLoading: boolean;
  setAsrLoading: (loading: boolean) => void;
  
  // 会话相关
  onResetSession: () => void;
  initializeSession: () => Promise<boolean>;
  clearSession: () => Promise<void>;
  resetSession: () => void;
  getCurrentSession: () => string;
  
  // 滚动相关
  scrollRef: React.RefObject<HTMLDivElement>;
  thinkScrollRef: React.RefObject<HTMLDivElement>;
  showScrollToBottom: boolean;
  scrollToBottom: () => void;
  lastThinkMessage: React.RefObject<HTMLDivElement>;
  
  // 思考过程相关
  onShowThinkMessage: (event: React.MouseEvent<HTMLDivElement>, message: AgentMessage) => void;
  onCloseThinkMessage: (event: React.MouseEvent<HTMLDivElement>) => void;
  thinkDetailVisible: boolean;
  thinkMessageIndex?: number;
  
  // 知识库相关
  onSearchKnowledgeResult: (event: React.MouseEvent<HTMLSpanElement>, id: string, query: string) => Promise<void>;
  knowledgeResultVisible: boolean;
  knowledgeSearchResults: SegmentVO[];
  setKnowledgeResultVisible: (visible: boolean) => void;
  knowledgeQueryText: string;
  
  // 分页相关
  fetchData: (expandThink?: boolean) => Promise<void>;
  hasMore: boolean;
  
  // 重试相关
  onRetry: () => Promise<void>;
  
  // 消息处理（高级功能）
  handleMessageEvent: (jsonData: SSEEventData, id: string, agentId: string) => void;
  handleDeltaEvent: (jsonData: DeltaEventData, id: string, agentId: string) => void;
  handleEndEvent: (id: string) => void;
  handleErrorEvent: (data: SSEEventData, id: string) => void;
  sendMessage: (type: 'text' | 'execute' | 'imageUrl', text?: string) => Promise<void>;
  
  // 音频管理
  resetAudioFlag: () => void;
  
  // 内部状态引用（供高级用法使用）
  agentSwitchRef: React.MutableRefObject<any>;
  agentStatusRef: React.MutableRefObject<AgentStatusRef[]>;
}

const ChatContext = createContext<ChatContextValue | undefined>(undefined);

interface ChatProviderProps extends UseChatHandleEventProps {
  children: ReactNode;
}

export const ChatProvider: React.FC<ChatProviderProps> = ({ 
  children, 
  agentId,
  mode,
  agentInfo 
}) => {
  // 内部引用
  const agentSwitchRef = useRef<any>();
  const agentStatusRef = useRef<AgentStatusRef[]>([]);

  // 会话管理
  const {
    initializeSession,
    clearSession,
    resetSession,
    getCurrentSession,
  } = useChatSession({
    agentId,
    mode,
    agentInfo,
  });

  // 滚动管理
  const {
    scrollRef,
    thinkScrollRef,
    lastThinkMessage,
    showScrollToBottom,
    scrollToBottom,
    handleScroll,
    scrollToThinkMessage,
    adjustScrollAfterLoadMore,
  } = useChatScroll();

  // 音频管理
  const {
    resetAudioFlag,
  } = useChatAudio({
    agentInfo,
  });

  // 消息管理 - 使用 reducer 架构
  const {
    messagesMap,
    clearList,
    thinkDetailVisible,
    thinkMessageIndex,
    hasMore,
    fetchData,
    onShowThinkMessage,
    onCloseThinkMessage,
    onResetSession,
    handleEndEvent,
    handleErrorEvent,
    actions: messageActions,
  } = useChatMessages({
    agentId,
    mode,
    agentInfo,
    scrollToBottom,
    scrollToThinkMessage,
    adjustScrollAfterLoadMore,
  });

  // 输入管理
  const {
    value,
    asrLoading,
    setAsrLoading,
    onInputChange,
    clearInput,
  } = useChatInput();

  // 消息事件处理 - 直接使用 reducer actions
  const { handleMessageEvent } = useChatMessageEvents({
    scrollToBottom,
    messageActions,
    agentSwitchRef
  });

  const { handleDeltaEvent } = useChatDeltaEvents({
    scrollToBottom,
    agentStatusRef,
    messageActions,
    agentSwitchRef
  });

  // SSE 连接管理
  const {
    sendMessage,
  } = useChatSSE({
    agentId,
    getCurrentSession,
    initializeSession,
    handleMessageEvent,
    handleDeltaEvent,
    handleErrorEvent,
    handleEndEvent,
    resetAudioFlag,
    inputValue: value,
    clearInput,
    agentStatusRef,
  });

  // 知识库管理
  const {
    knowledgeResultVisible,
    knowledgeSearchResults,
    knowledgeQueryText,
    onSearchKnowledgeResult,
    setKnowledgeResultVisible,
  } = useChatKnowledge();

  // 消息发送函数 - 结合输入验证和 SSE 发送
  const onSendMessage = async (type: 'text' | 'execute' | 'imageUrl', text?: string) => {
    if (!value.trim() && !text && type !== 'execute') {
      return;
    }
    await sendMessage(type, text);
  };

  // 更新重置会话函数
  const onResetSessionWrapper = () => onResetSession(clearSession);

  // 重试函数（保持原有接口）
  const onRetry = async () => {
    console.log('retry');
  };

  // 初始化效果
  useEffect(() => {
    const scrollElement = scrollRef.current;
    if (scrollElement) {
      scrollElement.addEventListener('scroll', handleScroll);
    }
    return () => {
      if (scrollElement) {
        scrollElement.removeEventListener('scroll', handleScroll);
      }
      // 清理 agentStatusRef
      agentStatusRef.current = [];
    };
  }, [handleScroll, scrollRef]);

  useEffect(() => {
    resetSession();
  }, [agentInfo, resetSession]);

  const contextValue: ChatContextValue = {
    // 基础状态和函数
    messagesMap,
    clearList,
    value,
    onInputChange,
    onSendMessage,
    asrLoading,
    setAsrLoading,
    onResetSession: onResetSessionWrapper,
    scrollRef,
    thinkScrollRef,
    showScrollToBottom,
    scrollToBottom,
    lastThinkMessage,
    onShowThinkMessage,
    onCloseThinkMessage,
    thinkDetailVisible,
    thinkMessageIndex,
    onSearchKnowledgeResult,
    knowledgeResultVisible,
    knowledgeSearchResults,
    setKnowledgeResultVisible,
    knowledgeQueryText,
    fetchData,
    hasMore,
    onRetry,

    // 扩展功能
    initializeSession,
    clearSession,
    resetSession,
    getCurrentSession,
    handleMessageEvent,
    handleDeltaEvent,
    handleEndEvent,
    handleErrorEvent,
    sendMessage,
    resetAudioFlag,

    // 内部状态引用
    agentSwitchRef,
    agentStatusRef,
  };

  return (
    <ChatContext.Provider value={contextValue}>
      {children}
    </ChatContext.Provider>
  );
};

// 主 Context Hook
export const useChatContext = (): ChatContextValue => {
  const context = useContext(ChatContext);
  if (context === undefined) {
    throw new Error('useChatContext must be used within a ChatProvider');
  }
  return context;
};

// 专门的功能 Hooks - 让组件可以只访问需要的功能
export const useContextChatSession = () => {
  const { 
    initializeSession, 
    clearSession, 
    resetSession, 
    getCurrentSession,
    onResetSession
  } = useChatContext();
  
  return { 
    initializeSession, 
    clearSession, 
    resetSession, 
    getCurrentSession,
    onResetSession
  };
};

export const useContextChatMessages = () => {
  const { 
    messagesMap, 
    onSendMessage, 
    handleMessageEvent, 
    handleDeltaEvent, 
    handleEndEvent, 
    handleErrorEvent,
    sendMessage
  } = useChatContext();
  
  return { 
    messagesMap, 
    onSendMessage, 
    handleMessageEvent, 
    handleDeltaEvent, 
    handleEndEvent, 
    handleErrorEvent,
    sendMessage
  };
};

export const useContextChatUI = () => {
  const {
    value,
    onInputChange,
    scrollRef,
    thinkScrollRef,
    showScrollToBottom,
    scrollToBottom,
    thinkDetailVisible,
    thinkMessageIndex,
    onShowThinkMessage,
    onCloseThinkMessage,
    asrLoading,
    setAsrLoading,
    fetchData,
    hasMore,
    lastThinkMessage,
  } = useChatContext();
  
  return {
    value,
    onInputChange,
    scrollRef,
    thinkScrollRef,
    showScrollToBottom,
    scrollToBottom,
    thinkDetailVisible,
    thinkMessageIndex,
    onShowThinkMessage,
    onCloseThinkMessage,
    asrLoading,
    setAsrLoading,
    fetchData,
    hasMore,
    lastThinkMessage,
  };
};

export const useContextChatKnowledge = () => {
  const {
    knowledgeResultVisible,
    knowledgeSearchResults,
    knowledgeQueryText,
    onSearchKnowledgeResult,
    setKnowledgeResultVisible,
  } = useChatContext();
  
  return {
    knowledgeResultVisible,
    knowledgeSearchResults,
    knowledgeQueryText,
    onSearchKnowledgeResult,
    setKnowledgeResultVisible,
  };
};

// 为了避免名称冲突，重新导出 Context 
export default ChatContext;