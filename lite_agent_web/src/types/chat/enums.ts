/**
 * Chat 模块枚举类型定义
 */

/**
 * 消息角色枚举
 */
export enum MessageRole {
  USER = 'user',
  ASSISTANT = 'assistant',
  SYSTEM = 'system',
  AGENT = 'agent',
  SUBAGENT = 'subagent',
  TOOL = 'tool',
  REFLECTION = 'reflection',
  SEPARATOR = 'separator'
}

/**
 * 消息类型枚举
 */
export enum MessageType {
  MESSAGE = 'message',
  DELTA = 'delta',
  ERROR = 'error',
  END = 'end'
}

/**
 * 任务消息类型枚举
 */
export enum TaskMessageType {
  TEXT = 'text',
  THINK = 'think',
  ERROR = 'error',
  LOADING = 'loading',
  KNOWLEDGE = 'knowledge',
  DISPATCH = 'dispatch',
  AGENT_SWITCH = 'agent_switch',
  BROADCAST = 'broadcast',
  PLANNING = 'planning',
  REFLECT = 'reflect'
}

export enum AgentType {
  NORMAL = 0,
  DISTRIBUTION = 1,
  REFLECTION = 2,
}

/**
 * TTS 状态枚举
 */
export enum TtsStatus {
  Init = 'init',
  Loading = 'loading',
  Playing = 'playing'
}

/**
 * 输入模式枚举
 */
export enum InputMode {
  NORMAL = 'normal',
  VOICE_INIT = 'voice_init',
  VOICE_RECORDING = 'voice_recording',
  VOICE_DONE = 'voice_done'
}

export enum ChunkType {
  MODEL_OUTPUT = 0,
  THINKING = 1
}

/**
 * 消息块类型变化标志
 */
export enum MessageChunkChangeFlag {
  /** 继续当前消息 */
  CONTINUE = 0,
  /** 开始新消息 */
  NEW_CHUNK = 1,
}