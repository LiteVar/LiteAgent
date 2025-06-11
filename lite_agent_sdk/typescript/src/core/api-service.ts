import { ApiAdapter } from '../types';
import { Logger } from '../utils/logger';
import { NetworkError } from '../errors/error-classes';

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

export interface ApiRequestOptions<T = any> {
  method: HttpMethod;
  url: string;
  headers?: Record<string, string>;
  body?: any;
  queryParams?: Record<string, string>;
}

/**
 * Service for handling API requests to the LiteAgent backend
 */
export class ApiService {
  private baseUrl: string;
  private apiKey: string;
  private adapter: ApiAdapter;
  private logger: Logger;

  /**
   * Creates a new API service instance
   * 
   * @param baseUrl - Base URL for all API requests
   * @param apiKey - API key for authentication
   * @param adapter - Platform-specific adapter for making HTTP requests
   * @param logger - Logger instance
   */
  constructor(
    baseUrl: string,
    apiKey: string,
    adapter: ApiAdapter,
    logger: Logger
  ) {
    this.baseUrl = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
    this.apiKey = apiKey;
    this.adapter = adapter;
    this.logger = logger;
  }

  /**
   * Builds the full URL for an API endpoint with optional query parameters
   * 
   * @param endpoint - API endpoint path (with or without leading slash)
   * @param queryParams - Optional query parameters
   * @returns Complete URL
   */
  private buildUrl(endpoint: string, queryParams?: Record<string, string>): string {
    const normalizedEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    const url = `${this.baseUrl}${normalizedEndpoint}`;
    
    if (!queryParams || Object.keys(queryParams).length === 0) {
      return url;
    }
    
    const queryString = Object.entries(queryParams)
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
      .join('&');
      
    return `${url}?${queryString}`;
  }

  /**
   * Creates standard headers for API requests
   * 
   * @param customHeaders - Additional headers to include
   * @returns Combined headers
   */
  private createHeaders(customHeaders?: Record<string, string>): Record<string, string> {
    const defaultHeaders = {
      'Authorization': `Bearer ${this.apiKey}`,
      'Content-Type': 'application/json'
    };
    
    return { ...defaultHeaders, ...customHeaders };
  }

  getAdapter(): ApiAdapter {
    return this.adapter;
  }

  /**
   * Makes an API request and handles common error cases
   * 
   * @param endpoint - API endpoint (with or without leading slash)
   * @param options - Request options
   * @returns Promise resolving to the response data
   */
  async request<T>(
    endpoint: string, 
    options: Partial<ApiRequestOptions> = {}
  ): Promise<T> {
    try {
      const url = this.buildUrl(endpoint, options.queryParams);
      const headers = this.createHeaders(options.headers);
      
      const requestOptions = {
        method: options.method || 'GET',
        url,
        headers,
        ...(options.body && { body: options.body })
      };

      this.logger.debug(`Making ${requestOptions.method} request to: ${url}`);
      
      return await this.adapter.request<T>(requestOptions);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      this.logger.error(`API request failed: ${errorMessage}`, error);
      
      throw error instanceof Error 
        ? error 
        : new NetworkError(`API request to ${endpoint} failed: ${errorMessage}`);
    }
  }

  /**
   * GET request helper
   */
  async get<T>(endpoint: string, queryParams?: Record<string, string>): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET', queryParams });
  }

  /**
   * POST request helper
   */
  async post<T>(endpoint: string, body?: any, queryParams?: Record<string, string>): Promise<T> {
    return this.request<T>(endpoint, { method: 'POST', body, queryParams });
  }

  /**
   * PUT request helper
   */
  async put<T>(endpoint: string, body?: any, queryParams?: Record<string, string>): Promise<T> {
    return this.request<T>(endpoint, { method: 'PUT', body, queryParams });
  }

  /**
   * DELETE request helper
   */
  async delete<T>(endpoint: string, queryParams?: Record<string, string>): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE', queryParams });
  }
}