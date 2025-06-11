export interface SessionInfo {
  sessionId: string;
}

export interface TaskInfo {
  sessionId: string;
  taskId?: string;
}

export type MessageRole = 'developer' | 'user' | 'agent' | 'assistant' | 'dispatcher' | 'subagent' | 'reflection' | 'tool' | 'client';
export type MessageType = 'text' | 'imageUrl' | 'contentList' | 'toolCalls' | 'dispatch' | 'reflection' | 'toolReturn' | 'functionCall' | 'taskStatus';
export type TaskStatus = 'start' | 'stop' | 'done' | 'toolsStart' | 'toolsDone' | 'exception';

// Content types for different message types
export interface TextContent {
  type: 'text' | 'imageUrl';
  message: string;
}

export interface ImageUrlContent {
  type: 'imageUrl';
  message: string; // URL or base64 string
}

export type ContentItem = TextContent | ImageUrlContent;

export interface ToolCall {
  id: string;
  name: string;
  parameters: Record<string, any>;
}

export interface ToolReturn {
  id: string; 
  result: Record<string, any>;
}

export interface ReflectionScore {
  score: number;
  description?: string;
}

export interface MessageScore {
  content: ContentItem[];
  messageType: 'text' | 'functionCalling';
  message: string;
  reflectScoreList: ReflectionScore[];
}

export interface ReflectionContent {
  isPass: boolean;
  messageScore: MessageScore;
  passScore: number;
  count: number;
  maxCount: number;
}

export interface DispatchContent {
  dispatchId: string;
  agentId: string;
  name: string;
  content: ContentItem[];
}

export interface TaskStatusContent {
  status: TaskStatus;
  description: Record<string, any>;
}

export interface FunctionCallContent {
  id: string;
  function: string;
  parameters: Record<string, any>;
}

export interface CompletionInfo {
  usage: {
    promptTokens: number;
    completionTokens: number;
    totalTokens: number;
  };
  id: string;
  model: string;
}

// Message interface for the SSE stream
export interface AgentMessage {
  sessionId: string;
  taskId: string;
  role: MessageRole;
  to: MessageRole;
  type: MessageType;
  content: any; // The type depends on the 'type' field
  completions?: CompletionInfo;
  createTime: string;
}

export interface ChunkMessage {
  sessionId: string;
  taskId: string;
  role: MessageRole;
  to: MessageRole;
  type: MessageType;
  part: string;
  completions?: CompletionInfo;
  createTime: string;
}

// Request and response interfaces
export interface InitSessionResponse {
  sessionId: string;
}

export interface ChatRequest {
  content: ContentItem[];
  isChunk?: boolean;
}

export interface CallbackRequest {
  id: string;
  result: Record<string, any>;
}

export interface StopResponse {
  sessionId: string;
  taskId?: string;
}

export interface ClearResponse {
  id: string;
}

export type HistoryResponse = AgentMessage[];

// Event handlers for message events
export interface EventHandlers {
  onAgentMessage?: (message: AgentMessage) => void;
  onChunkMessage?: (message: ChunkMessage) => void;
  onFunctionCall?: (message: AgentMessage) => void;
  onDone?: () => void;
  onError?: (error: any) => void;
}

