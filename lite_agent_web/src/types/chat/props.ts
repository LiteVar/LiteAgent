/**
 * Chat 模块组件 Props 类型定义
 */

import React from 'react';
import { AgentDetailVO } from '@/client';
import { AgentType, TtsStatus } from './enums';
import { AgentMessage, AgentMessageMap, ToolMessage, KnowledgeMessage, ReflectMessage, AgentSwitchMessage } from './messages';

/**
 * Chat 组件 Props
 */
export interface ChatProps {
  mode: 'dev' | 'prod';
  agentInfo: AgentDetailVO | undefined;
  agentId: string;
  asrEnabled: boolean;
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
  onChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  onSend: (type: 'text' | 'execute' | 'imageUrl', text?: string) => void;
  setAsrLoading: (value: boolean) => void;
  asrModelId: string;
}

/**
 * ChatMessage 组件 Props
 */
export interface ChatMessageProps {
  message: AgentMessage;
  agentIcon?: string;
  mode: 'dev' | 'prod';
  isLastMessage?: boolean;
  onRetry: () => void;
  onSendMessage: (type: 'text' | 'execute' | 'imageUrl', text?: string) => Promise<void>;
  onShowThinkMessage: (event: React.MouseEvent<HTMLDivElement>, message: AgentMessage) => void;
  ttsModelId?: string;
  isLastThinkMessage: boolean;
  lastThinkMessage: React.RefObject<HTMLDivElement>;
}

/**
 * ChatMessages 组件 Props
 */
export interface ChatMessagesProps {
  onShowThinkMessage: (event: React.MouseEvent<HTMLDivElement>, message: AgentMessage) => void;
  messages: AgentMessage[];
  agentIcon?: string;
  mode: 'dev' | 'prod';
  onRetry: (index: number) => void;
  asrLoading: boolean;
  ttsModelId: string;
  lastThinkMessage: React.RefObject<HTMLDivElement>;
  onSendMessage: (type: 'text' | 'execute' | 'imageUrl', text?: string) => Promise<void>;
}

/**
 * MessageActions 组件 Props
 */
export interface MessageActionsProps {
  onCopy: () => void;
  onRetry: () => void;
  show: boolean;
  copied: boolean;
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
  onSendMessage: (type: 'text' | 'execute' | 'imageUrl', text?: string) => Promise<void>;
}

/**
 * ChatPlanning 组件 Props
 */
export interface ChatPlanningProps {
  message: AgentMessage;
  onSendMessage: (type: 'text' | 'execute' | 'imageUrl', text?: string) => Promise<void>;
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