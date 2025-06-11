import { LiteAgentSession } from './session';
import { defaultAdapter } from '../adapters';
import { ApiService } from './api-service';
import { 
  ApiAdapter,
  LiteAgentConfig, 
  InitSessionResponse, 
  ContentItem,
  AgentMessage, 
  ChunkMessage,
  HistoryResponse
} from '../types';
import { AuthenticationError, ValidationError } from '../errors/error-classes';
import { validateApiKey } from '../utils/validation';
import { Logger, LogLevel } from '../utils/logger';

/**
 * Main client for interacting with the LiteAgent API
 */
export class LiteAgentClient {
  private apiKey: string;
  private baseUrl: string;
  private adapter: ApiAdapter;
  private logger: Logger;
  private api: ApiService;
  private currentSession: LiteAgentSession | null = null;

  /**
   * Creates a new LiteAgent client instance
   * 
   * @param apiKey - The API key for authentication
   * @param config - Configuration options for the client
   */
  constructor(config: LiteAgentConfig) {
    if (!validateApiKey(config.apiKey)) {
      throw new AuthenticationError('Invalid API key format. API key should start with "sk-"');
    }

    if (!config.baseUrl) {
      throw new ValidationError('Base URL is required');
    }

    this.apiKey = config.apiKey;
    this.baseUrl = config.baseUrl;
    this.adapter = defaultAdapter;
    this.logger = new Logger(config.enableDebugLogs ? LogLevel.DEBUG : LogLevel.INFO);
    
    // Initialize API service
    this.api = new ApiService(
      this.baseUrl,
      this.apiKey,
      this.adapter,
      this.logger
    );

    this.logger.debug('LiteAgentClient initialized');
  }

  /**
   * Retrieves the current API version
   * 
   * @returns Promise resolving to the version string
   */
  async getVersion(): Promise<string> {
    const response = await this.api.get<{version: string}>('version');
    return response.version;
  }

   /**
   * Retrieves message history
   * 
   * @throws Error if no active session exists
   * @returns Promise resolving to the history response
   */
  async getHistory(): Promise<HistoryResponse> {
    this.ensureSessionExists('No active session to get history. Call initSession or chat first.');
    return this.currentSession!.getHistory();
  }

  /**
   * Returns the current session ID
   * @returns The current session ID or null if no session exists
   * */
  getSessionId(): string | null {
    return this.currentSession?.getSessionId() || null;
  }

  /**
   * Initializes a new session with the LiteAgent API
   * 
   * @returns Promise resolving when the session is initialized
   * @throws ValidationError if the response is invalid
   */
  async initSession(): Promise<void> {
    const response = await this.api.post<InitSessionResponse>('initSession');

    if (!response.sessionId) {
      throw new ValidationError('Invalid response from initSession: missing sessionId');
    }

    this.logger.info(`Session initialized with ID: ${response.sessionId}`);
    
    this.currentSession = new LiteAgentSession(
      response.sessionId, 
      this.baseUrl,
      this.apiKey, 
      this.api,
      this.logger
    );
  }

  /**
   * Shorthand method to create a session and immediately send a message
   * 
   * @param content - The message content to send
   * @param isChunk - Whether to isChunk the response
   * @param callbacks - Optional callbacks for stream events
   * @returns Promise resolving to a LiteAgentSession object
   */
  async chat(
    content: ContentItem[], 
    isChunk: boolean,
    callbacks?: {
      onMessage?: (message: AgentMessage) => void;
      onChunk?: (chunk: ChunkMessage) => void;
      onFunctionCall?: (functionCall: AgentMessage) => void;
      onError?: (error: Error) => void;
      onComplete?: () => void;
    }
  ): Promise<void> {
    // Use existing session if available, otherwise create a new one
    if (!this.currentSession) {
      await this.initSession();
    }
    
    await this.currentSession!.chat(content, isChunk, callbacks);
  }

  /**
   * Send function call result back to the agent
   * 
   * @param id - The function call ID
   * @param result - The function call result
   * @throws Error if no active session exists
   */
  async sendFunctionCallResult(id: string, result: Record<string, any>): Promise<void> {
    this.ensureSessionExists('No active session. Call initSession or chat first.');
    return this.currentSession!.sendFunctionCallResult(id, result);
  }

  /**
   * Stop the current session or a specific task
   * 
   * @param taskId - Optional task ID to stop a specific task
   * @returns Promise resolving when the stop request completes
   * @throws Error if no active session exists
   */
  async stop(taskId?: string): Promise<{ sessionId: string; taskId?: string }> {
    this.ensureSessionExists('No active session to stop. Call initSession or chat first.');
    return this.currentSession!.stop(taskId);
  }

  /**
   * Clear the session data
   * 
   * @returns Promise resolving to the session ID
   * @throws Error if no active session exists
   */
  async clear(): Promise<{ id: string }> {
    this.ensureSessionExists('No active session to clear. Call initSession or chat first.');
    return this.currentSession!.clear();
  }

  /**
   * Ensures that a session exists, throwing an error with the provided message if not
   * 
   * @param errorMessage - The error message to use
   * @throws ValidationError if no session exists
   */
  private ensureSessionExists(errorMessage: string): void {
    if (!this.currentSession) {
      throw new ValidationError(errorMessage);
    }
  }
}