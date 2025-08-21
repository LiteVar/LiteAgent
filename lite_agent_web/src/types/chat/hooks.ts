/**
 * Chat 模块 Hook 相关类型定义
 */

import React from 'react';
import { AgentDetailVO, AgentChatMessageClear, SegmentVO } from '@/client';
import { AgentMessage, AgentMessageMap, AgentSwitchMessage } from './messages';
import { AgentStatusRef } from './state';

/**
 * useChatHandleEvent Hook Props
 */
export interface UseChatHandleEventProps {
  mode: 'prod' | 'dev';
  agentId: string;
  agentInfo: AgentDetailVO;
}

/**
 * useChatHandleEvent Hook 返回类型
 */
export interface UseChatHandleEventReturn {
  messagesMap: AgentMessageMap;
  clearList: AgentChatMessageClear[];
  onInputChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  onResetSession: () => Promise<void>;
  scrollRef: React.RefObject<HTMLDivElement>;
  thinkScrollRef: React.RefObject<HTMLDivElement>;
  lastThinkMessage: React.RefObject<HTMLDivElement>;
  onRetry: () => Promise<void>;
  showScrollToBottom: boolean;
  scrollToBottom: () => void;
  onShowThinkMessage: (event: React.MouseEvent<HTMLDivElement>, message: AgentMessage) => void;
  onCloseThinkMessage: (event: React.MouseEvent<HTMLDivElement>) => void;
  thinkDetailVisible: boolean;
  thinkMessageIndex: number | undefined;
  value: string;
  onSendMessage: (type: 'text' | 'execute' | 'imageUrl', text?: string) => Promise<void>;
  onSearchKnowledgeResult: (event: React.MouseEvent<HTMLSpanElement>, id: string, query: string) => Promise<void>;
  setKnowledgeResultVisible: (value: boolean) => void;
  knowledgeResultVisible: boolean;
  knowledgeQueryText: string;
  knowledgeSearchResults: SegmentVO[];
  asrLoading: boolean;
  hasMore: boolean;
  setAsrLoading: (value: boolean) => void;
  fetchData: () => Promise<void>;
}

/**
 * Reducer Actions Interface
 */
export interface MessageReducerActions {
  setMessagesMap: (messagesMap: AgentMessageMap) => void;
  updateAgentMessages: (agentId: string, messages: AgentMessage[]) => void;
  addMessage: (agentId: string, message: AgentMessage) => void;
  updateMessage: (agentId: string, messageId: string, updates: Partial<AgentMessage>) => void;
  clearMessages: (agentId: string) => void;
  addSeparator: (agentId: string, sessionId?: string) => void;
  prependMessages: (agentId: string, messages: AgentMessage[], adjustScrollCallback?: (oldScrollHeight: number) => void) => void;
  handleEndEvent: (agentId: string, messageId: string, ttsModelId?: string) => void;
  handleErrorEvent: (agentId: string, messageId: string, data: any) => void;
  complexMessageUpdate: (agentId: string, messageId: string, updater: (prev: AgentMessageMap) => AgentMessageMap) => void;
}

/**
 * useChatMessageEvent Hook Props
 */
export interface UseChatMessageEventProps {
  scrollToBottom: () => void;
  agentSwitchRef: React.MutableRefObject<AgentSwitchMessage | undefined>;
  messageActions: MessageReducerActions;
}

/**
 * useChatStreamDeltaEvent Hook Props
 */
export interface UseChatStreamDeltaEventProps {
  scrollToBottom: () => void;
  messageActions: MessageReducerActions;
  agentStatusRef: React.MutableRefObject<AgentStatusRef[]>;
  agentSwitchRef: React.MutableRefObject<any>;
}