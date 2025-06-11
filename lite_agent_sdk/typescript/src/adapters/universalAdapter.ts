import { ApiAdapter, RequestOptions, EventSourceOptions, EventListener } from '../types/api-types';
import { createRequestWithTimeout } from '../utils';
import { NetworkError } from '../errors/error-classes';

const request = createRequestWithTimeout(fetch);

// 环境兼容处理
const isEventSupported = typeof Event === 'function';
const isErrorEventSupported = typeof ErrorEvent === 'function';

function createCompatibleEvent(type: string, init: any = {}): Event {
  if (isEventSupported) {
    return new Event(type, init);
  }
  return { type, ...init };
}

function createCompatibleErrorEvent(message: string, error: any): ErrorEvent | any {
  if (isErrorEventSupported) {
    return new ErrorEvent('error', { message, error });
  }
  return { type: 'error', message, error };
}

export class UniversalAdapter implements ApiAdapter {
  request<T = any>(options: RequestOptions): Promise<T> {
    return request(options);
  }

  createEventSource(url: string, options: EventSourceOptions): any {
    try {
      const controller = new AbortController();
      const { headers, method, body } = options;

      const eventSourceInstance = {
        listeners: new Map<string, EventListener[]>(),
        close: () => controller.abort(),
        addEventListener: (type: string, listener: EventListener) => {
          if (!eventSourceInstance.listeners.has(type)) {
            eventSourceInstance.listeners.set(type, []);
          }
          eventSourceInstance.listeners.get(type)!.push(listener);
        },
        removeEventListener: (type: string, listener: EventListener) => {
          const listeners = eventSourceInstance.listeners.get(type);
          if (listeners) {
            const index = listeners.indexOf(listener);
            if (index !== -1) {
              listeners.splice(index, 1);
            }
          }
        }
      };

      (async () => {
        try {
          const response = await fetch(url, {
            method,
            headers,
            body: body ? (typeof body === 'string' ? body : JSON.stringify(body)) : undefined,
            signal: controller.signal
          });

          // open
          if (eventSourceInstance.listeners.has('open')) {
            const event = createCompatibleEvent('open');
            eventSourceInstance.listeners.get('open')!.forEach((listener) => listener(event));
          }

          if (!response.ok) {
            throw new Error(`Server responded with ${response.status}: ${response.statusText}`);
          }

          if (!response.body) {
            throw new Error('ReadableStream not supported.');
          }

          const reader = response.body.getReader();
          const decoder = new TextDecoder();
          let buffer = '';
          let currentEvent = 'message';
          let currentData = '';

          while (!controller.signal.aborted) {
            const { done, value } = await reader.read();
            if (done) {
              if (eventSourceInstance.listeners.has('end')) {
                const event = createCompatibleEvent('end');
                eventSourceInstance.listeners.get('end')!.forEach((listener) => listener(event));
              }
              break;
            }

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split(/\r\n|\r|\n/);
            buffer = lines.pop() || '';

            for (const line of lines) {
              if (line.startsWith('event:')) {
                currentEvent = line.slice(6).trim();
              } else if (line.startsWith('data:')) {
                currentData = line.slice(5).trim();
                const eventData = { data: currentData, event: currentEvent };
                if (eventSourceInstance.listeners.has(currentEvent)) {
                  eventSourceInstance.listeners.get(currentEvent)!.forEach((listener) => {
                    try {
                      listener(eventData);
                    } catch (err) {
                      console.error('Error dispatching event:', err);
                    }
                  });
                }
              }
            }
          }
        } catch (err: any) {
          if (!controller.signal.aborted) {
            if (eventSourceInstance.listeners.has('error')) {
              const errorEvent = createCompatibleErrorEvent(err.message, err);
              eventSourceInstance.listeners.get('error')!.forEach((listener) => listener(errorEvent));
            }
          }
        }
      })();

      return eventSourceInstance;
    } catch (err: any) {
      throw new NetworkError(`Failed to create EventSource: ${err.message}`);
    }
  }
}
