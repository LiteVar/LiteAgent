import { LiteAgentSession } from '../../../src/core/session';
import { ApiService } from '../../../src/core/api-service';
import { StreamHandler } from '../../../src/core/stream-handler';
import { ValidationError } from '../../../src/errors/error-classes';
import { Logger } from '../../../src/utils/logger';
import { ContentItem, AgentMessage, HistoryResponse, ChunkMessage } from '../../../src/types';

// Mock dependencies
jest.mock('../../../src/core/api-service');
jest.mock('../../../src/core/stream-handler');
jest.mock('../../../src/utils/logger');

describe('LiteAgentSession', () => {
  const mockSessionId = 'session-123';
  const mockBaseUrl = 'https://api.example.com';
  const mockApiKey = 'sk-test-key';

  let mockApiService: jest.Mocked<ApiService>;
  let mockLogger: jest.Mocked<Logger>;
  let session: LiteAgentSession;

  beforeEach(() => {
    jest.clearAllMocks();

    mockLogger = new Logger() as jest.Mocked<Logger>;
    mockApiService = new (ApiService as jest.Mock<ApiService>)() as jest.Mocked<ApiService>;

    session = new LiteAgentSession(
      mockSessionId,
      mockBaseUrl,
      mockApiKey,
      mockApiService,
      mockLogger
    );
  });

  describe('constructor and getSessionId', () => {
    it('should initialize and return sessionId', () => {
      expect(session.getSessionId()).toBe(mockSessionId);
      expect(mockLogger.debug).toHaveBeenCalledWith(`Session ${mockSessionId} initialized`);
    });
  });

  describe('chat', () => {
    const content: ContentItem[] = [{ type: 'text', message: 'Hello' }];
    const callbacks = {
      onMessage: jest.fn(),
      onChunk: jest.fn(),
      onFunctionCall: jest.fn(),
      onError: jest.fn(),
      onComplete: jest.fn()
    };

    it('should throw if no sessionId', async () => {
      // Create session without sessionId
      const sessionNoId = new LiteAgentSession('', mockBaseUrl, mockApiKey, mockApiService, mockLogger);
      await expect(sessionNoId.chat(content, false)).rejects.toThrow(ValidationError);
    });

    it('should throw if content is empty', async () => {
      await expect(session.chat([], false)).rejects.toThrow(ValidationError);
    });

    it('should close existing stream before starting new one', async () => {
      // Setup a mocked activeStream with close method
      (StreamHandler as jest.Mock).mockImplementation(() => ({
        start: jest.fn().mockResolvedValue(undefined),
        close: jest.fn()
      }));
      // start first chat to create activeStream
      await session.chat(content, false);

      expect(session['activeStream']).not.toBeNull();

      // Start second chat, should close old stream
      await session.chat(content, false);

      const firstStreamInstance = (StreamHandler as jest.Mock).mock.results[0].value;
      expect(firstStreamInstance.close).toHaveBeenCalled();
    });

    it('should create a new StreamHandler and start streaming', async () => {
      const startMock = jest.fn().mockResolvedValue(undefined);
      (StreamHandler as jest.Mock).mockImplementation(() => ({
        start: startMock,
        close: jest.fn()
      }));

      await session.chat(content, true, callbacks);

      expect(StreamHandler).toHaveBeenCalledWith(
        `${mockBaseUrl}/chat?sessionId=${mockSessionId}`,
        mockApiKey,
        { content, isChunk: true },
        mockApiService,
        mockLogger,
        callbacks
      );
      expect(startMock).toHaveBeenCalled();
    });

    it('should call onError callback on failure', async () => {
      const error = new Error('test error');
      (StreamHandler as jest.Mock).mockImplementation(() => ({
        start: jest.fn().mockRejectedValue(error),
        close: jest.fn()
      }));

      await session.chat(content, false, {
        onError: jest.fn()
      }).catch(() => {});

      expect(mockLogger.error).toHaveBeenCalledWith('Error during chat:', error);
    });
  });

  describe('sendFunctionCallResult', () => {
    it('should throw if no sessionId', async () => {
      const sessionNoId = new LiteAgentSession('', mockBaseUrl, mockApiKey, mockApiService, mockLogger);
      await expect(sessionNoId.sendFunctionCallResult('id', {})).rejects.toThrow(ValidationError);
    });

    it('should post function call result', async () => {
      mockApiService.post.mockResolvedValue(undefined);

      await session.sendFunctionCallResult('funcId', { foo: 'bar' });
      expect(mockApiService.post).toHaveBeenCalledWith(
        'callback',
        { id: 'funcId', result: { foo: 'bar' } },
        { sessionId: mockSessionId }
      );
      expect(mockLogger.debug).toHaveBeenCalledWith('Function call result sent successfully for ID: funcId');
    });
  });

  describe('getHistory', () => {
    it('should throw if no sessionId', async () => {
      const sessionNoId = new LiteAgentSession('', mockBaseUrl, mockApiKey, mockApiService, mockLogger);
      await expect(sessionNoId.getHistory()).rejects.toThrow(ValidationError);
    });

    it('should fetch history and update messageHistory', async () => {
      const historyMock: HistoryResponse = [
        {
          sessionId: mockSessionId,
          taskId: 'task1',
          role: 'user',
          to: 'agent',
          type: 'text',
          content: { type: 'text', message: 'msg' },
          createTime: '2025-05-29T00:00:00Z',
          completions: undefined
        }
      ];
      mockApiService.get.mockResolvedValue(historyMock);

      const history = await session.getHistory();
      expect(mockApiService.get).toHaveBeenCalledWith('history', { sessionId: mockSessionId });
      expect(history).toEqual(historyMock);
      expect((session as any).messageHistory).toEqual(historyMock);
    });
  });

  describe('stop', () => {
    it('should throw if no sessionId', async () => {
      const sessionNoId = new LiteAgentSession('', mockBaseUrl, mockApiKey, mockApiService, mockLogger);
      await expect(sessionNoId.stop()).rejects.toThrow(ValidationError);
    });

    it('should close activeStream and call api get stop without taskId', async () => {
      const closeMock = jest.fn();
      session['activeStream'] = { close: closeMock } as any;
      const response = { sessionId: mockSessionId };
      mockApiService.get.mockResolvedValue(response);

      const result = await session.stop();
      expect(closeMock).toHaveBeenCalled();
      expect(mockApiService.get).toHaveBeenCalledWith('stop', { sessionId: mockSessionId });
      expect(result).toBe(response);
      expect(mockLogger.info).toHaveBeenCalledWith('Session  stopped successfully');
    });

    it('should pass taskId to api get stop', async () => {
      const response = { sessionId: mockSessionId, taskId: 'task-123' };
      mockApiService.get.mockResolvedValue(response);

      const result = await session.stop('task-123');
      expect(mockApiService.get).toHaveBeenCalledWith('stop', {
        sessionId: mockSessionId,
        taskId: 'task-123'
      });
      expect(result).toBe(response);
      expect(mockLogger.info).toHaveBeenCalledWith('Session task stopped successfully');
    });
  });

  describe('clear', () => {
    it('should throw if no sessionId', async () => {
      const sessionNoId = new LiteAgentSession('', mockBaseUrl, mockApiKey, mockApiService, mockLogger);
      await expect(sessionNoId.clear()).rejects.toThrow(ValidationError);
    });

    it('should close activeStream, clear messageHistory and sessionId, call api clear', async () => {
      const closeMock = jest.fn();
      session['activeStream'] = { close: closeMock } as any;
      const response = { id: mockSessionId };
      mockApiService.get.mockResolvedValue(response);

      const result = await session.clear();
      expect(closeMock).toHaveBeenCalled();
      expect(mockApiService.get).toHaveBeenCalledWith('clear', { sessionId: mockSessionId });
      expect(result).toBe(response);
      expect((session as any).messageHistory).toEqual([]);
      expect(session.getSessionId()).toBeNull();
      expect(mockLogger.info).toHaveBeenCalledWith('Session cleared successfully');
    });
  });
});
