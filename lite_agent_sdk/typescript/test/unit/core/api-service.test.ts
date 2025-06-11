import { ApiService } from '../../../src/core/api-service';
import { NetworkError } from '../../../src/errors/error-classes';
import { Logger, LogLevel } from '../../../src/utils/logger';

const createMockAdapter = () => ({
  request: jest.fn(),
  createEventSource: jest.fn()
});

describe('ApiService', () => {
  const baseUrl = 'https://api.example.com';
  const apiKey = 'sk-test-key';
  let adapter: ReturnType<typeof createMockAdapter>;
  let logger: Logger;
  let service: ApiService;

  beforeEach(() => {
    adapter = createMockAdapter();
    logger = new Logger(LogLevel.DEBUG);
    jest.spyOn(logger, 'debug').mockImplementation(() => { return; });
    jest.spyOn(logger, 'error').mockImplementation(() => { return; });
    service = new ApiService(baseUrl, apiKey, adapter, logger);
  });

  it('should build full URL with query params', async () => {
    adapter.request.mockResolvedValue({ status: 200, body: { success: true } });

    await service.get('test', { foo: 'bar', q: '1 2' });

    expect(adapter.request).toHaveBeenCalledWith(
      expect.objectContaining({
        url: 'https://api.example.com/test?foo=bar&q=1%202'
      })
    );
  });

  it('should merge headers and include API key', async () => {
    adapter.request.mockResolvedValue({ status: 200, body: {} });

    await service.request('hello', {
      method: 'GET',
      headers: { 'X-Custom': 'yes' }
    });

    expect(adapter.request).toHaveBeenCalledWith(
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: `Bearer ${apiKey}`,
          'Content-Type': 'application/json',
          'X-Custom': 'yes'
        })
      })
    );
  });

  it('should handle POST body', async () => {
    const body = { a: 1 };
    adapter.request.mockResolvedValue({ status: 200, body: {} });

    await service.post('submit', body);

    expect(adapter.request).toHaveBeenCalledWith(
      expect.objectContaining({
        method: 'POST',
        body
      })
    );
  });

  it('should throw NetworkError for non-error object', async () => {
    adapter.request.mockRejectedValue('boom');

    await expect(service.get('fail')).rejects.toThrow(NetworkError);
  });

  it('should pass through original error if instance of Error', async () => {
    const err = new Error('fail hard');
    adapter.request.mockRejectedValue(err);

    await expect(service.get('fail')).rejects.toThrow(err);
  });

  it('should trim trailing slash from baseUrl', async () => {
    const service2 = new ApiService(baseUrl + '/', apiKey, adapter, logger);
    adapter.request.mockResolvedValue({ status: 200, body: {} });

    await service2.get('trim');

    expect(adapter.request).toHaveBeenCalledWith(
      expect.objectContaining({
        url: 'https://api.example.com/trim'
      })
    );
  });

  it('should support delete method', async () => {
    adapter.request.mockResolvedValue({ status: 200, body: {} });
    await service.delete('remove');

    expect(adapter.request).toHaveBeenCalledWith(
      expect.objectContaining({
        method: 'DELETE',
        url: `${baseUrl}/remove`
      })
    );
  });
});