import { NetworkError, TimeoutError } from '../errors/error-classes';
import type { RequestOptions } from '../types/api-types';

export function createRequestWithTimeout(fetchImpl: typeof fetch) {
  return async function requestWithTimeout<T = any>(options: RequestOptions): Promise<T> {
    const { method, url, headers, body, timeout = 30000 } = options;

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeout);

    try {
      const response = await fetchImpl(url, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      const contentType = response.headers.get('content-type');
      const responseData = contentType?.includes('application/json')
        ? await response.json()
        : await response.text();

      if (!response.ok) {
        throw new NetworkError(`Request failed with status ${response.status}`, {
          status: response.status,
          statusText: response.statusText,
          body: responseData,
        });
      }

      return responseData;
    } catch (err: any) {
      if (err.name === 'AbortError') {
        throw new TimeoutError(`Request timed out after ${timeout}ms`, timeout);
      }

      throw new NetworkError(err.message || 'Fetch failed', { statusText: err.toString() });
    }
  };
}
