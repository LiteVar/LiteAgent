/**
 * Chat 模块事件相关类型定义
 */

import { MessageRole } from './enums';
import { ToolCall } from './messages';

/**
 * SSE 事件数据
 */
export interface SSEEventData {
  taskId: string;
  agentId: string;
  parentTaskId?: string;
  role?: MessageRole;
  type?: string;
  content?: any;
  chunkType?: number;
  part?: string;
  toolCalls?: ToolCall[];
  toolCallId?: string;
  createTime?: string;
}

/**
 * Delta 事件数据
 */
export interface DeltaEventData extends SSEEventData {
  part: string;
  chunkType: number;
}

/**
 * 事件处理器类型
 */
export type EventHandler = (data: SSEEventData) => void;

/**
 * API 响应类型
 */
export interface ApiResponse<T = any> {
  data?: {
    code?: number;
    message?: string;
    data?: T;
  };
}

/**
 * 音频转写响应类型
 */
export interface AudioTranscriptionResponse {
  text: string;
}

/**
 * 消息发送类型
 */
export type MessageSendType = 'text' | 'execute' | 'imageUrl';

/**
 * Agent 类型模式
 */
export type AgentTypeMode = string; // 可以根据实际情况细化