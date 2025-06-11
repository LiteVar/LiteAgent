import { ApiService } from './api-service';
import { StreamError } from '../errors/error-classes';
import { Logger } from '../utils/logger';
import { ChunkMessage, CustomEventSource } from '../types';

/**
 * Handles Server-Sent Events (SSE) stream connections with the LiteAgent API
 */
export class StreamHandler {
  private url: string;
  private apiKey: string;
  private body: any;
  private api: ApiService;
  private logger: Logger;
  private eventSource: CustomEventSource | null = null;
  private callbacks: {
    onMessage?: (message: any) => void;
    onChunk?: (chunk: ChunkMessage) => void;
    onFunctionCall?: (functionCall: any) => void;
    onError?: (error: Error) => void;
    onComplete?: () => void;
  };

  /**
   * Creates a new stream handler
   * 
   * @param url - The streaming endpoint URL (without base URL)
   * @param apiKey - The API key for authentication
   * @param body - The request body
   * @param api - The API service
   * @param logger - The logger instance
   * @param callbacks - Optional callbacks for stream events
   */
  constructor(
    url: string,
    apiKey: string,
    body: any,
    api: ApiService,
    logger: Logger,
    callbacks?: {
      onMessage?: (message: any) => void;
      onChunk?: (chunk: ChunkMessage) => void;
      onFunctionCall?: (functionCall: any) => void;
      onError?: (error: Error) => void;
      onComplete?: () => void;
    }
  ) {
    this.url = url;
    this.apiKey = apiKey;
    this.body = body;
    this.api = api;
    this.logger = logger;
    this.callbacks = callbacks || {};
  }

  /**
   * Start the SSE connection
   * 
   * @returns Promise that resolves when the connection is established
   */
  async start(): Promise<void> {
    try {
      return new Promise((resolve, reject) => {
        this.eventSource = this.api.getAdapter().createEventSource(this.url, {
          method: 'POST',
          headers: {
            'Authorization': 'Bearer ' + this.apiKey,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(this.body)
        });

        // Set up event listeners
        this.eventSource.addEventListener('open', () => {
          this.logger.debug('Stream connection opened');
        });

        this.eventSource.addEventListener('message', (event: any) => {
          try {
            const data = JSON.parse(event.data);
            if (this.callbacks.onMessage) {
              this.callbacks.onMessage(data);
            }
          } catch (error) {
            this.logger.error('Error parsing message event data:', error);
          }
        });

        this.eventSource.addEventListener('chunk', (event: any) => {
          try {
            const data = JSON.parse(event.data);
            if (this.callbacks.onChunk) {
              this.callbacks.onChunk(data as ChunkMessage);
            }
          } catch (error) {
            this.logger.error('Error parsing chunk event data:', error);
          }
        });

        this.eventSource.addEventListener('functionCall', (event: any) => {
          try {
            const data = JSON.parse(event.data);
            if (this.callbacks.onFunctionCall) {
              this.callbacks.onFunctionCall(data);
            }
          } catch (error) {
            this.logger.error('Error parsing functionCall event data:', error);
          }
        });

        this.eventSource.addEventListener('error', (error: any) => {
          const errorMessage = error.message || 'Unknown stream error';
          this.logger.error('Stream error:', errorMessage);
          
          if (this.callbacks.onError) {
            this.callbacks.onError(new StreamError(errorMessage));
          }
          
          this.close();
          reject(new StreamError(errorMessage));
        });

        this.eventSource.addEventListener('end', () => {
          this.logger.debug('Stream ended');
          if (this.callbacks.onComplete) {
            this.callbacks.onComplete();
          }
          this.close();
          resolve();
        });
      });
    } catch (error) {
      this.logger.error('Failed to start stream:', error);
      throw error;
    }
  }

  /**
   * Close the SSE connection
   */
  close(): void {
    if (this.eventSource) {
      this.logger.debug('Closing stream connection');
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}