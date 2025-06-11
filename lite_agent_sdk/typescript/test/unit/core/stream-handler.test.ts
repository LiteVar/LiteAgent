import { StreamHandler } from '../../../src/core/stream-handler';
import { ApiService } from '../../../src/core/api-service';
import { Logger } from '../../../src/utils/logger';
import { StreamError } from '../../../src/errors/error-classes';
import { CustomEventSource, EventListener } from '../../../src/types';

jest.mock('../../../src/core/api-service');
jest.mock('../../../src/utils/logger');

describe('StreamHandler', () => {
  jest.setTimeout(10_000);

  let mockApiService: jest.Mocked<ApiService>;
  let mockLogger: jest.Mocked<Logger>;
  let eventListeners: Record<string, EventListener>;
  let mockEventSource: CustomEventSource;
  let streamHandler: StreamHandler;

  const url = 'stream-endpoint';
  const apiKey = 'test-api-key';
  const body = { prompt: 'test' };
  const mockCallbacks = {
    onMessage: jest.fn(),
    onChunk: jest.fn(),
    onFunctionCall: jest.fn(),
    onError: jest.fn(),
    onComplete: jest.fn()
  };

  beforeEach(() => {
    eventListeners = {};

    mockEventSource = {
      addEventListener: jest.fn((event, cb) => {
        eventListeners[event] = cb;
      }),
      close: jest.fn()
    } as any;

    mockApiService = new (ApiService as jest.Mock<ApiService>)() as jest.Mocked<ApiService>;

    mockLogger = {
      debug: jest.fn(),
      info: jest.fn(),
      warn: jest.fn(),
      error: jest.fn(),
    } as unknown as jest.Mocked<Logger>;

    mockApiService.getAdapter.mockReturnValue({
      createEventSource: jest.fn(() => mockEventSource)
    } as any);

    streamHandler = new StreamHandler(url, apiKey, body, mockApiService, mockLogger, mockCallbacks);
  });

  it('should resolve on end event', async () => {
    const promise = streamHandler.start();
    eventListeners['open']();
    eventListeners['end']();
    await expect(promise).resolves.toBeUndefined();
    expect(mockCallbacks.onComplete).toHaveBeenCalled();
    expect(mockEventSource.close).toHaveBeenCalled();
  });

  it('should call onMessage callback on message event', async () => {
    const promise = streamHandler.start();
    eventListeners['open']();
    const data = { hello: 'world' };
    eventListeners['message']({ data: JSON.stringify(data), event: 'message' });
    expect(mockCallbacks.onMessage).toHaveBeenCalledWith(data);
    eventListeners['end']();
    await promise;
  });

  it('should call onChunk callback on chunk event', async () => {
    const promise = streamHandler.start();
    eventListeners['open']();
    const chunkData = { part: 'chunk' };
    eventListeners['chunk']({ data: JSON.stringify(chunkData), event: 'chunk' });
    expect(mockCallbacks.onChunk).toHaveBeenCalledWith(chunkData);
    eventListeners['end']();
    await promise;
  });

  it('should call onFunctionCall callback on functionCall event', async () => {
    const promise = streamHandler.start();
    eventListeners['open']();
    const callData = { name: 'call' };
    eventListeners['functionCall']({ data: JSON.stringify(callData), event: 'functionCall' });
    expect(mockCallbacks.onFunctionCall).toHaveBeenCalledWith(callData);
    eventListeners['end']();
    await promise;
  });

  it('should call onError and reject on error event', async () => {
    const promise = streamHandler.start();

    eventListeners['open'](); // 触发打开

    const errorEvent = new Error('connection failed');
    eventListeners['error'](errorEvent); // 触发错误事件，promise reject

    await expect(promise).rejects.toThrow(StreamError);
    expect(mockCallbacks.onError).toHaveBeenCalledWith(expect.any(StreamError));
    expect(mockLogger.error).toHaveBeenCalledWith('Stream error:', errorEvent.message);
    expect(mockEventSource.close).toHaveBeenCalled();
  });

  it('should close stream connection', () => {
    (streamHandler as any).eventSource = mockEventSource;
    streamHandler.close();
    expect(mockEventSource.close).toHaveBeenCalled();
  });
});