import { 
  ContentItem, 
  HistoryResponse,
  AgentMessage,
  ChunkMessage
} from '../types';
import { ApiService } from './api-service';
import { StreamHandler } from './stream-handler';
import { ValidationError } from '../errors/error-classes';
import { Logger } from '../utils/logger';

/**
 * Manages an individual session with the LiteAgent API
 */
export class LiteAgentSession {
  private sessionId: string | null;
  private baseUrl: string;
  private apiKey: string;
  private api: ApiService;
  private logger: Logger;
  private activeStream: StreamHandler | null = null;
  private messageHistory: AgentMessage[] = [];

  /**
   * Creates a new session instance
   * 
   * @param sessionId - The session ID from the API
   * @param apiKey - The API key for authentication
   * @param api - API service for making requests
   * @param logger - The logger instance
   */
  constructor(
    sessionId: string, 
    baseUrl: string,
    apiKey: string, 
    api: ApiService,
    logger: Logger
  ) {
    this.sessionId = sessionId;
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = api;
    this.logger = logger;
    
    this.logger.debug(`Session ${sessionId} initialized`);
  }

  /**
   * Get the session ID
   */
  getSessionId(): string | null {
    return this.sessionId || null;
  }

  /**
   * Send a message to the agent and optionally stream the response
   * 
   * @param content - The message content to send
   * @param isChunk - Whether to isChunk the response
   * @param callbacks - Optional callbacks for stream events
   * @returns Promise that resolves when the request completes
   */
  async chat(
    content: ContentItem[], 
    isChunk: boolean,
    callbacks?: {
      onMessage?: (message: AgentMessage) => void;
      onChunk?: (chunk: ChunkMessage) => void;
      onFunctionCall?: (functionCall: any) => void;
      onError?: (error: Error) => void;
      onComplete?: () => void;
    }
  ): Promise<void> {
    try {
      if (!this.sessionId) {
        throw new ValidationError('No active session to send message');
      }

      if (this.activeStream) {
        this.logger.warn('Closing existing stream before starting a new one');
        this.activeStream.close();
        this.activeStream = null;
      }

      if (!content || content.length === 0) {
        throw new ValidationError('Message content cannot be empty');
      }

      const streamUrl = `${this.baseUrl}/chat?sessionId=${this.sessionId}`;
      this.activeStream = new StreamHandler(
        streamUrl,
        this.apiKey,
        { content, isChunk },
        this.api,
        this.logger,
        callbacks
      );
        
      await this.activeStream.start();
    } catch (error) {
      this.logger.error('Error during chat:', error);
      
      if (callbacks?.onError) {
        callbacks.onError(error instanceof Error ? error : new Error('Unknown error during chat request'));
      } else {
        throw error;
      }
    }
  }

  /**
   * Send function call result back to the agent
   * 
   * @param id - The function call ID
   * @param result - The function call result
   */
  async sendFunctionCallResult(id: string, result: Record<string, any>): Promise<void> {
    if (!this.sessionId) {
      throw new ValidationError('No active session to send function call result');
    }
    await this.api.post(
      'callback', 
      { id, result }, 
      { sessionId: this.sessionId }
    );
    
    this.logger.debug(`Function call result sent successfully for ID: ${id}`);
  }

  /**
   * Retrieve message history for this session
   * 
   * @returns Promise resolving to the message history
   */
  async getHistory(): Promise<HistoryResponse> {
    if (!this.sessionId) {
      throw new ValidationError('No active session to retrieve history');
    }
    const response = await this.api.get<HistoryResponse>(
      'history', 
      { sessionId: this.sessionId }
    );

    this.messageHistory = response;
    return response;
  }

  /**
   * Stop the current session or a specific task
   * 
   * @param taskId - Optional task ID to stop a specific task
   * @returns Promise resolving when the stop request completes
   */
  async stop(taskId?: string): Promise<{ sessionId: string; taskId?: string }> {
    if (!this.sessionId) {
      throw new ValidationError('No active session to stop');
    }
    const queryParams: Record<string, string> = { sessionId: this.sessionId };
    
    if (taskId) {
      queryParams.taskId = taskId;
    }
    
    if (this.activeStream) {
      this.activeStream.close();
      this.activeStream = null;
    }

    const response = await this.api.get<{ sessionId: string; taskId?: string }>(
      'stop', 
      queryParams
    );

    this.logger.info(`Session ${taskId ? 'task' : ''} stopped successfully`);
    return response;
  }

  /**
   * Clear the session data
   * 
   * @returns Promise resolving to the session ID
   */
  async clear(): Promise<{ id: string }> {
    if (this.activeStream) {
      this.activeStream.close();
      this.activeStream = null;
    }

    if (!this.sessionId) {
      throw new ValidationError('No active session to clear');
    }

    const response = await this.api.get<{ id: string }>(
      'clear', 
      { sessionId: this.sessionId }
    );

    this.messageHistory = [];
    this.sessionId = null;
    this.logger.info('Session cleared successfully');
    return response;
  }
}