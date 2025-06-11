import { LiteAgentClient } from '../../../src/core/client';
import { AuthenticationError, ValidationError } from '../../../src/errors/error-classes';
import { LiteAgentSession } from '../../../src/core/session';
import { ApiService } from '../../../src/core/api-service';
import { ContentItem, AgentMessage } from '../../../src/types/message-types';

// Mock the dependencies
jest.mock('../../../src/core/session');
jest.mock('../../../src/core/api-service');
jest.mock('../../../src/utils/logger');

describe('LiteAgentClient', () => {
  const mockConfig = {
    apiKey: 'sk-test-key',
    baseUrl: 'https://api.example.com',
    enableDebugLogs: false
  };

  let client: LiteAgentClient;
  let mockApiService: jest.Mocked<ApiService>;
  let mockSession: jest.Mocked<LiteAgentSession>;

  beforeEach(() => {
    // Reset all mocks before each test
    jest.clearAllMocks();

    // Create fresh instances
    mockApiService = new (ApiService as jest.Mock<ApiService>)() as jest.Mocked<ApiService>;
    mockSession = new (LiteAgentSession as jest.Mock<LiteAgentSession>)() as jest.Mocked<LiteAgentSession>;

    // Setup mock implementations
    (ApiService as jest.Mock).mockImplementation(() => mockApiService);
    (LiteAgentSession as jest.Mock).mockImplementation(() => mockSession);

    client = new LiteAgentClient(mockConfig);
  });

  describe('constructor', () => {
    it('should initialize with valid config', () => {
      expect(client).toBeInstanceOf(LiteAgentClient);
      expect(ApiService).toHaveBeenCalledWith(
        mockConfig.baseUrl,
        mockConfig.apiKey,
        expect.anything(),
        expect.anything()
      );
    });

    it('should throw AuthenticationError for invalid API key', () => {
      expect(() => new LiteAgentClient({
        ...mockConfig,
        apiKey: 'invalid-key'
      })).toThrow(AuthenticationError);
    });

    it('should throw ValidationError for missing baseUrl', () => {
      expect(() => new LiteAgentClient({
        ...mockConfig,
        baseUrl: ''
      })).toThrow(ValidationError);
    });
  });

  describe('getVersion', () => {
    it('should return version from API', async () => {
      const mockVersion = '1.0.0';
      mockApiService.get.mockResolvedValue({ version: mockVersion });

      const version = await client.getVersion();
      expect(version).toBe(mockVersion);
      expect(mockApiService.get).toHaveBeenCalledWith('version');
    });
  });

  describe('session management', () => {
    describe('initSession', () => {
      it('should initialize a new session', async () => {
        const mockSessionId = 'session-123';
        mockApiService.post.mockResolvedValue({ sessionId: mockSessionId });

        await client.initSession();
        expect(client.getSessionId()).toBeDefined();
        expect(mockApiService.post).toHaveBeenCalledWith('initSession');
        expect(LiteAgentSession).toHaveBeenCalledWith(
          mockSessionId,
          mockConfig.baseUrl,
          mockConfig.apiKey,
          mockApiService,
          expect.anything()
        );
      });

      it('should throw ValidationError for invalid response', async () => {
        mockApiService.post.mockResolvedValue({});

        await expect(client.initSession()).rejects.toThrow(ValidationError);
      });
    });

    describe('getSessionId', () => {
      it('should return current session ID', () => {
        const mockSessionId = 'session-123';
        (client as any).currentSession = { getSessionId: jest.fn(() => mockSessionId) };
        expect(client.getSessionId()).toBe(mockSessionId);
      });
    
      it('should return null if no session exists', () => {
        (client as any).currentSession = null;
        expect(client.getSessionId()).toBeNull();
      });
    });
  });

  describe('chat', () => {
    const mockContent: ContentItem[] = [{ type: 'text' as const, message: 'Hello' }];
    const mockCallbacks = {
      onMessage: jest.fn(),
      onChunk: jest.fn(),
      onFunctionCall: jest.fn(),
      onError: jest.fn(),
      onComplete: jest.fn()
    };

    it('should create new session if none exists', async () => {
      mockApiService.post.mockResolvedValue({ sessionId: 'session-123' });

      await client.chat(mockContent, false);
      expect(mockApiService.post).toHaveBeenCalledWith('initSession');
      expect(mockSession.chat).toHaveBeenCalledWith(mockContent, false, undefined);
    });

    it('should use existing session if available', async () => {
      mockApiService.post.mockResolvedValue({ sessionId: 'session-123' });
      await client.initSession();

      await client.chat(mockContent, false);
      expect(mockSession.chat).toHaveBeenCalledWith(mockContent, false, undefined);
    });

    it('should pass isChunk and callbacks to session', async () => {
      mockApiService.post.mockResolvedValue({ sessionId: 'session-123' });
      await client.initSession();

      await client.chat(mockContent, true, mockCallbacks);
      expect(mockSession.chat).toHaveBeenCalledWith(mockContent, true, mockCallbacks);
    });
  });

  describe('session operations', () => {
    beforeEach(async () => {
      mockApiService.post.mockResolvedValue({ sessionId: 'session-123' });
      await client.initSession();
    });

    describe('getHistory', () => {
      it('should delegate to session', async () => {
        const mockHistory: AgentMessage[] = [{
          sessionId: 'session-123',
          taskId: 'task-123',
          role: 'user' as const,
          to: 'agent' as const,
          type: 'text' as const,
          content: { type: 'text' as const, message: 'Hello' },
          createTime: '2025-05-19T10:00:00Z',
          completions: undefined
        }];
        mockSession.getHistory.mockResolvedValue(mockHistory);

        const history = await client.getHistory();
        expect(history).toBe(mockHistory);
        expect(mockSession.getHistory).toHaveBeenCalled();
      });

      it('should throw if no session exists', async () => {
        client = new LiteAgentClient(mockConfig);
        await expect(client.getHistory()).rejects.toThrow(ValidationError);
      });
    });

    describe('sendFunctionCallResult', () => {
      it('should delegate to session', async () => {
        const mockResult = { foo: 'bar' };
        await client.sendFunctionCallResult('func-123', mockResult);
        expect(mockSession.sendFunctionCallResult).toHaveBeenCalledWith('func-123', mockResult);
      });

      it('should throw if no session exists', async () => {
        client = new LiteAgentClient(mockConfig);
        await expect(client.sendFunctionCallResult('func-123', {})).rejects.toThrow(ValidationError);
      });
    });

    describe('stop', () => {
      it('should delegate to session', async () => {
        const mockResponse = { sessionId: 'session-123' };
        mockSession.stop.mockResolvedValue(mockResponse);

        const response = await client.stop();
        expect(response).toBe(mockResponse);
        expect(mockSession.stop).toHaveBeenCalledWith(undefined);
      });

      it('should pass taskId to session', async () => {
        const taskId = 'task-123';
        await client.stop(taskId);
        expect(mockSession.stop).toHaveBeenCalledWith(taskId);
      });

      it('should throw if no session exists', async () => {
        client = new LiteAgentClient(mockConfig);
        await expect(client.stop()).rejects.toThrow(ValidationError);
      });
    });

    describe('clear', () => {
      it('should delegate to session', async () => {
        const mockResponse = { id: 'session-123' };
        mockSession.clear.mockResolvedValue(mockResponse);

        const response = await client.clear();
        expect(response).toBe(mockResponse);
        expect(mockSession.clear).toHaveBeenCalled();
      });

      it('should throw if no session exists', async () => {
        client = new LiteAgentClient(mockConfig);
        await expect(client.clear()).rejects.toThrow(ValidationError);
      });
    });
  });
});
