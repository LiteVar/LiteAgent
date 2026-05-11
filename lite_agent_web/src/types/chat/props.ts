/**
 * Chat 模块组件 Props 类型定义
 */

import React from 'react';
import { AgentDetailVO } from '@/client';
import { AgentType, TtsStatus } from './enums';
import { AgentMessage, AgentMessageMap, ToolMessage, KnowledgeMessage, ReflectMessage, AgentSwitchMessage } from './messages';
import { ChatMessageItem } from '@/hooks/chat/useChatSSE';

/**
 * Chat 组件 Props
 */
export interface ChatProps {
  mode: 'dev' | 'prod';
  agentInfo: AgentDetailVO | undefined;
  agentId: string;
  setAgentMap?(agentMap: AgentMessageMap): void;
}

/**
 * ChatHeader 组件 Props
 */
export interface ChatHeaderProps {
  mode: 'dev' | 'prod';
  agentId: string;
  agentName?: string;
  onResetSession: () => void;
}

/**
 * ChatInput 组件 Props
 */
export interface ChatInputProps {
  value: string;
  mode: 'dev' | 'prod';
  agentType: AgentType;
  agentId: string;
  onChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  onSend: (messages: ChatMessageItem[]) => void;
  setAsrLoading: (value: boolean) => void;
  asrModelId: string;
  asrStreamSupported?: boolean; // ASR 模型是否支持流式
}

/**
 * ChatMessage 组件 Props
 */
export interface ChatMessageProps {
  agentId: string;
  message: AgentMessage;
  agentIcon?: string;
  mode: 'dev' | 'prod';
  isLastMessage?: boolean;
  onRetry: () => void;
  onSendMessage: (messages: ChatMessageItem[]) => Promise<void>;
  onShowThinkMessage: (event: React.MouseEvent<HTMLDivElement>, message: AgentMessage) => void;
  ttsModelId?: string;
  ttsStreamSupported?: boolean;
  isLastThinkMessage: boolean;
  lastThinkMessage: React.RefObject<HTMLDivElement>;
}

/**
 * ChatMessages 组件 Props
 */
export interface ChatMessagesProps {
  agentId: string;
  onShowThinkMessage: (event: React.MouseEvent<HTMLDivElement>, message: AgentMessage) => void;
  messages: AgentMessage[];
  agentIcon?: string;
  mode: 'dev' | 'prod';
  onRetry: (index: number) => void;
  asrLoading: boolean;
  ttsModelId: string;
  ttsStreamSupported: boolean;
  lastThinkMessage: React.RefObject<HTMLDivElement>;
  onSendMessage: (messages: ChatMessageItem[]) => Promise<void>;
}

/**
 * MessageActions 组件 Props
 */
export interface MessageActionsProps {
  onCopy: (event: React.MouseEvent<HTMLSpanElement>) => void;
  onStop?: (taskId: string) => void;
  onRetry: () => void;
  show: boolean;
  copied: boolean;
  taskId?: string;
  responding?: boolean;
  retryDisabled?: boolean;
  showRetry?: boolean;
  isAssistant?: boolean;
  ttsModelId?: string;
  ttsStatus?: TtsStatus;
  onTtsClick?: () => void;
}

/**
 * ScrollToBottom 组件 Props
 */
export interface ScrollToBottomProps {
  onClick: () => void;
}

/**
 * ChatThoughtProcess 组件 Props
 */
export interface ChatThoughtProcessProps {
  thoughtProcessMessages: AgentMessage[];
  onSearchKnowledgeResult: (event: React.MouseEvent<HTMLSpanElement>, id: string, query: string) => Promise<void>;
}

/**
 * ChatResultProcess 组件 Props
 */
export interface ChatResultProcessProps {
  resultProcessMessages: AgentMessage[];
  onSendMessage: (messages: ChatMessageItem[]) => Promise<void>;
}

/**
 * ChatPlanning 组件 Props
 */
export interface ChatPlanningProps {
  message: AgentMessage;
  onSendMessage: (messages: ChatMessageItem[]) => Promise<void>;
}

/**
 * ChatTool 组件 Props
 */
export interface ChatToolProps {
  tool: ToolMessage;
}

/**
 * ChatKnowledge 组件 Props
 */
export interface ChatKnowledgeProps {
  knowledge: KnowledgeMessage;
  onSearchKnowledgeResult: (event: React.MouseEvent<HTMLSpanElement>, id: string, query: string) => Promise<void>;
}

/**
 * ChatReflect 组件 Props
 */
export interface ChatReflectProps {
  reflect: ReflectMessage;
}

/**
 * ChatThink 组件 Props
 */
export interface ChatThinkProps {
  message: AgentMessage;
}

/**
 * ChatDispatch 组件 Props
 */
export interface ChatDispatchProps {
  message: AgentMessage;
}

/**
 * ChatAgentSwitch 组件 Props
 */
export interface ChatAgentSwitchProps {
  agentSwitchMessage: AgentSwitchMessage;
  onSearchKnowledgeResult: (event: React.MouseEvent<HTMLSpanElement>, id: string, query: string) => Promise<void>;
}