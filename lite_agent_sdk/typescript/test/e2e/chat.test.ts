import { LiteAgentClient } from '../../src/core/client';
import { AgentMessage, ContentItem } from '../../src/types/message-types';
import nock from 'nock';

jest.mock('../../src/core/session', () => {
  return {
    LiteAgentSession: jest.fn().mockImplementation(() => ({
      getSessionId: () => 'session-123',

      chat: async (
        _content: ContentItem[],
        _isChunk: boolean,
        callbacks?: {
          onFunctionCall?: (msg: AgentMessage) => void;
          onMessage?: (msg: AgentMessage) => void;
        }
      ) => {
        // 模拟服务端返回 functionCall 类型的响应
        if (callbacks?.onFunctionCall) {
          callbacks.onFunctionCall({
            type: 'functionCall',
            content: {
              id: 'func-123',
              function: 'get_weather',
              parameters: { location: 'Beijing' }
            }
          } as AgentMessage);
        }
      },

      sendFunctionCallResult: jest.fn().mockResolvedValue(undefined),
      stop: jest.fn().mockResolvedValue({ sessionId: 'session-123' }),
      clear: jest.fn().mockResolvedValue({ id: 'session-123' }),
      getHistory: jest.fn().mockResolvedValue([
        {
          sessionId: 'session-123',
          taskId: 'task-123',
          role: 'user',
          to: 'agent',
          type: 'text',
          content: { type: 'text', message: 'Hello' },
          createTime: new Date().toISOString()
        }
      ])
    }))
  };
});

describe('LiteAgent E2E', () => {
  const baseUrl = 'https://api.example.com';
  const apiKey = 'sk-test-key';
  let client: LiteAgentClient;

  beforeEach(() => {
    client = new LiteAgentClient({
      apiKey,
      baseUrl,
      enableDebugLogs: false
    });
  });

  describe('chat flow', () => {
    it('should complete a full chat session', async () => {
      // Mock initSession
      nock(baseUrl)
        .post('/initSession')
        .reply(200, { sessionId: 'session-123' });

      // Mock chat response
      nock(baseUrl)
        .post('/chat', (body) => {
          return body.content[0].message === 'Hello';
        })
        .query({ sessionId: 'session-123' })
        .reply(200, {
          sessionId: 'session-123',
          taskId: 'task-123',
          role: 'agent',
          to: 'user',
          type: 'text',
          content: { type: 'text', message: 'Hello back' },
          createTime: new Date().toISOString()
        });

      // Mock history response
      nock(baseUrl)
        .get('/history')
        .query({ sessionId: 'session-123' })
        .reply(200, [{
          sessionId: 'session-123',
          taskId: 'task-123',
          role: 'user',
          to: 'agent',
          type: 'text',
          content: { type: 'text', message: 'Hello' },
          createTime: new Date().toISOString()
        }]);

      const mockContent: ContentItem[] = [{ type: 'text', message: 'Hello' }];

      // Start session and chat
      await client.chat(mockContent, true);
      expect(client.getSessionId()).toBeDefined();

      // Get history
      const history = await client.getHistory();
      expect(history).toHaveLength(1);
      expect(history[0].content.message).toBe('Hello');
    });

    it('should handle function calling flow', async () => {
      // Mock initSession
      nock(baseUrl)
        .post('/initSession')
        .reply(200, { sessionId: 'session-123' });

      // Mock function call request
      nock(baseUrl)
        .post('/chat')
        .query({ sessionId: 'session-123' })
        .reply(200, {
          sessionId: 'session-123',
          taskId: 'task-123',
          role: 'agent',
          to: 'user',
          type: 'functionCall',
          content: {
            id: 'func-123',
            function: 'get_weather',
            parameters: { location: 'Beijing' }
          },
          createTime: new Date().toISOString()
        });

      // Mock function result callback
      nock(baseUrl)
        .post('/callback')
        .query({ sessionId: 'session-123' })
        .reply(200, {});

      const mockContent: ContentItem[] = [{ type: 'text', message: 'What is the weather in Beijing?' }];

      const callbacks = {
        onFunctionCall: jest.fn(),
        onMessage: jest.fn()
      };

      await client.chat(mockContent, true, callbacks);
      
      // Verify function call was received
      expect(callbacks.onFunctionCall).toHaveBeenCalled();

      // Send function result
      await client.sendFunctionCallResult('func-123', { weather: 'sunny', temp: 25 });
    });

    it('should handle session lifecycle', async () => {
      // Mock initSession
      nock(baseUrl)
        .post('/initSession')
        .reply(200, { sessionId: 'session-123' });

      // Mock stop
      nock(baseUrl)
        .get('/stop')
        .query({ sessionId: 'session-123' })
        .reply(200, { sessionId: 'session-123' });

      // Mock clear
      nock(baseUrl)
        .get('/clear')
        .query({ sessionId: 'session-123' })
        .reply(200, { id: 'session-123' });

      await client.initSession();
      expect(client.getSessionId()).toBeDefined();

      await client.stop();
      await client.clear();
    });
  });
});
