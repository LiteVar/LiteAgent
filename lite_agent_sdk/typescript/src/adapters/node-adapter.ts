import { ApiAdapter, RequestOptions, EventSourceOptions, EventListener } from '../types/api-types';
import { createRequestWithTimeout } from '../utils';
import { NetworkError } from '../errors/error-classes';

const request = createRequestWithTimeout(fetch);

// Node.js 中没有 Event 构造器，手动创建简单兼容对象
function createSimpleEvent(type: string, payload: any = {}) {
  return { type, ...payload };
}

export class NodeAdapter implements ApiAdapter {
  request<T = any>(options: RequestOptions): Promise<T> {
    return request(options);
  }

  createEventSource(url: string, options: EventSourceOptions): any {
    try {
      const controller = new AbortController();
      const { headers, method, body } = options;

      const eventSourceInstance = {
        listeners: new Map(),
        close: () => controller.abort(),
        addEventListener: (type: string, listener: EventListener) => {
          if (!eventSourceInstance.listeners.has(type)) {
            eventSourceInstance.listeners.set(type, []);
          }
          eventSourceInstance.listeners.get(type).push(listener);
        },
        removeEventListener: (type: string, listener: EventListener) => {
          if (eventSourceInstance.listeners.has(type)) {
            const listeners = eventSourceInstance.listeners.get(type);
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
            headers: {
              ...headers,
            },
            body: body ? (typeof body === 'string' ? body : JSON.stringify(body)) : undefined,
            signal: controller.signal
          });

          if (eventSourceInstance.listeners.has('open')) {
            const event = createSimpleEvent('open');
            eventSourceInstance.listeners.get('open').forEach((listener: EventListener) => listener(event));
          }

          if (!response.ok) {
            throw new Error(`Server responded with ${response.status}: ${response.statusText}`);
          }

          if (!response.body) {
            throw new Error('ReadableStream not supported in this Node.js version.');
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
                const event = createSimpleEvent('end');
                eventSourceInstance.listeners.get('end').forEach((listener: EventListener) => listener(event));
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
                try {
                  const eventData = { data: currentData, event: currentEvent };
                  if (eventSourceInstance.listeners.has(currentEvent)) {
                    eventSourceInstance.listeners.get(currentEvent).forEach((listener: EventListener) =>
                      listener(eventData)
                    );
                  }
                } catch (err) {
                  console.error('Error dispatching event:', err);
                }
              }
            }
          }
        } catch (err: any) {
          if (!controller.signal.aborted) {
            if (eventSourceInstance.listeners.has('error')) {
              const event = createSimpleEvent('error', { error: err, message: err.message });
              eventSourceInstance.listeners.get('error').forEach((listener: EventListener) => listener(event));
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
