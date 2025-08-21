/**
 * Chat 模块消息相关类型定义
 */

import { OutMessage } from '@/client';
import { MessageChunkChangeFlag, MessageRole, TaskMessageType } from './enums';

/**
 * 基础消息接口
 */
export interface BaseMessage {
  id?: string;
  taskId?: string;
  agentId?: string;
  sessionId?: string;
  role: MessageRole;
  type?: TaskMessageType | string;
  content?: any;
  createTime?: string;
  responding?: boolean;
}

/**
 * Agent 消息类型，扩展自 OutMessage
 */
export type AgentMessage =  OutMessage & {
  taskId?: string;
  parentTaskId?: string;
  content?: any;
  think?: any;
  createTime?: string;
  messages?: AgentMessage[];
  thoughtProcessMessages?: AgentMessage[];
  resultProcessMessages?: AgentMessage[];
  /** 消息块变化标志：0-继续当前消息，1-开始新消息 */
  flag?: MessageChunkChangeFlag;
  chunkType?: number;
  visible?: boolean;
  playAudio?: boolean;
  // 为了兼容 ToolMessage
  responding?: boolean;
  req?: OutMessage & {
    tool?: ToolCall;
  };
  res?: OutMessage;
  // 为了兼容 KnowledgeMessage 
  dispatchId?: string;
}

/**
 * Agent 消息映射表
 */
export interface AgentMessageMap {
  [agentId: string]: {
    messages: AgentMessage[];
  };
}

/**
 * 工具调用信息
 */
export interface ToolCall {
  id: string;
  name: string;
  toolName: string;
  functionName: string;
  arguments: any;
}

/**
 * 工具消息
 */
export interface ToolMessage {
  req?: OutMessage & {
    tool?: ToolCall;
  };
  res?: OutMessage;
  role?: MessageRole | string; // 兼容 OutMessage 的 role?: string
  createTime?: string;
  responding?: boolean; // 改为可选，兼容 AgentMessage
  taskId?: string;
  agentId?: string;
  content?: any;
  type?: TaskMessageType | string;
}

/**
 * 反思消息
 */
export interface ReflectMessage {
  taskId?: string;
  role?: MessageRole | string; // 兼容 OutMessage 的 role?: string
  type?: TaskMessageType | string; // 兼容 OutMessage 的 type?: string
  content?: {
    input: any;
    rawInput: string;
    rawOutput: string;
    output: {
      score: number;
      information: string;
    }[];
  } | any; // 兼容 AgentMessage 的 content?: any
  createTime?: string;
  agentId?: string;
}

/**
 * 知识库消息
 */
export interface KnowledgeMessage {
  agentId?: string;
  taskId?: string;
  dispatchId?: string;
  role?: MessageRole | string; // 兼容 OutMessage 的 role?: string
  type?: TaskMessageType | string; // 兼容 OutMessage 的 type?: string
  content?: {
    retrieveContent: string;
    info: Array<{
      id: string;
      datasetName: string;
      datasetId: string;
    }>;
  } | any; // 兼容 AgentMessage 的 content?: any
  createTime?: string;
}

/**
 * Agent 切换消息
 */
export type AgentSwitchMessage = OutMessage & {
  messages?: AgentMessage[];
}

/**
 * 规划任务
 */
export interface PlanningTask {
  name: string;
  description?: {
    duty?: string;
    constraint?: string;
  };
  children?: PlanningTask[];
}

/**
 * 规划消息内容
 */
export interface PlanningContent {
  planId?: string;
  taskList?: PlanningTask[];
}