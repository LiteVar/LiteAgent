/**
 * Chat 模块状态管理相关类型定义
 */

import { AgentChatMessageClear, SegmentVO } from '@/client';
import { AgentMessage, AgentMessageMap } from './messages';

/**
 * Agent 状态引用
 */
export interface AgentStatusRef {
  id: string | null;
  agentMessage: AgentMessage | null;
  responding: boolean;
}

/**
 * Chat 状态
 */
export interface ChatState {
  messagesMap: AgentMessageMap;
  clearList: AgentChatMessageClear[];
  value: string;
  showScrollToBottom: boolean;
  knowledgeResultVisible: boolean;
  knowledgeSearchResults: SegmentVO[];
  knowledgeQueryText: string;
  asrLoading: boolean;
  sessionLoading: boolean;
  hasMore: boolean;
  thinkDetailVisible: boolean;
  selectMessage: AgentMessage | null;
}