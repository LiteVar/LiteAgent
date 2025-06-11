export interface RequestOptions {
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  headers?: Record<string, string>;
  body?: any;
  timeout?: number; // ms
}

export interface EventSourceOptions {
  headers?: Record<string, string>;
  method?: 'GET' | 'POST'; // Some polyfills support POST SSE
  body?: any;
}

export interface ApiAdapter {
  request<T = any>(options: RequestOptions): Promise<T>;
  createEventSource(url: string, options: EventSourceOptions): CustomEventSource;
}

export interface EventData {
  data: string;
  event: string;
}

export type EventListener = (event?: Event | EventData | ErrorEvent | Error) => void;

export interface CustomEventSource {
  /**
   * Closes the event source connection
   */
  close: () => void;

  /**
   * Adds an event listener for the specified event
   * @param type - Event type to listen for
   * @param listener - Function to call when the event occurs
   */
  addEventListener: (type: string, listener: EventListener) => void;

  /**
   * Removes an event listener for the specified event
   * @param type - Event type to remove listener from
   * @param listener - Function to remove
   */
  removeEventListener: (type: string, listener: EventListener) => void;

  /**
   * Internal map of event listeners
   */
  listeners?: Map<string, EventListener[]>;
}