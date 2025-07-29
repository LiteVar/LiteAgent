export enum MessageType {
  END = 'end',
  DELTA = 'delta',
  ERROR = 'error',
  MESSAGE = 'message',
}

export enum MessageRole {
  USER = 'user',
  TOOL = 'tool',
  AGENT = 'agent',
  ASSISTANT = 'assistant',
  SEPARATOR = 'separator',
  SYSTEM = 'system',
  SUBAGENT = 'subagent',
  REFLECTION = 'reflection',
}

export enum TaskMessageType {
  TEXT = 'text',
  THINK = 'think',
  ERROR = 'error',
  IMAGE_URL = 'imageUrl',
  FUNCTION_CALL_LIST = 'functionCallList',
  TOOL_RETURN = 'toolReturn',
  FLAG = 'flag',
  BROADCAST = 'broadcast',
  DISPATCH = 'dispatch',
  REFLECT = 'reflect',
  KNOWLEDGE = 'knowledge',
  AGENT_STATUS = 'agentStatus',
  AGENT_SWITCH = 'agentSwitch',
  PLANNING = 'planning',
}