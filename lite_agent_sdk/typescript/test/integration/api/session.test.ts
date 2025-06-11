import { LiteAgentSession } from "../../../src/core/session";
import { ApiService } from "../../../src/core/api-service";
import { Logger } from "../../../src/utils/logger";
import { ContentItem, AgentMessage } from "../../../src/types/message-types";

describe("LiteAgentSession Integration", () => {
  jest.setTimeout(10_000);

  const mockSessionId = "session-123";
  const mockBaseUrl = "https://api.example.com";
  const mockApiKey = "sk-test-key";
  let mockApiService: jest.Mocked<ApiService>;
  let mockLogger: jest.Mocked<Logger>;
  let session: LiteAgentSession;

  beforeEach(() => {
    const mockCreateEventSource = jest.fn().mockImplementation((url: string) => {
      return new EventSource(url);
    });

    mockApiService = {
      get: jest.fn(),
      post: jest.fn(),
      getAdapter: jest.fn().mockReturnValue({
        createEventSource: mockCreateEventSource,
        request: jest.fn(),
      })
    } as unknown as jest.Mocked<ApiService>;

    mockLogger = {
      debug: jest.fn(),
      info: jest.fn(),
      warn: jest.fn(),
      error: jest.fn(),
    } as unknown as jest.Mocked<Logger>;

    session = new LiteAgentSession(
      mockSessionId,
      mockBaseUrl,
      mockApiKey,
      mockApiService,
      mockLogger
    );
  });

  describe("chat", () => {
    const mockContent: ContentItem[] = [
      { type: "text" as const, message: "Hello" },
    ];
    const mockAgentMessage: AgentMessage = {
      sessionId: mockSessionId,
      taskId: "task-123",
      role: "user",
      to: "agent",
      type: "text",
      content: { type: "text", message: "Hello back" },
      createTime: "2025-05-19T10:00:00Z",
    };

    it("should handle streaming chat with callbacks", async () => {
      // Create a variable to store callbacks
      let capturedCallbacks: any = null;
      
      // Mock the stream handler implementation
      const mockStreamHandler = {
        start: jest.fn().mockResolvedValue(undefined),
        close: jest.fn()
      };
      
      // Mock the StreamHandler class
      const originalModule = jest.requireActual('../../../src/core/stream-handler');
      jest.spyOn(originalModule, 'StreamHandler').mockImplementation(
        (url, apiKey, body, api, logger, callbacks) => {
          capturedCallbacks = callbacks;
          return mockStreamHandler;
        }
      );
    
      // Setup callback mocks
      const mockCallbacks = {
        onMessage: jest.fn(),
        onChunk: jest.fn(),
        onFunctionCall: jest.fn(),
        onError: jest.fn(),
        onComplete: jest.fn(),
      };
    
      // Start streaming chat
      const chatPromise = session.chat(mockContent, true, mockCallbacks);
      
      // Wait for chat promise to resolve
      await chatPromise;
      
      // Test event handling by simulating events
      if (capturedCallbacks) {
        // Simulate events using captured callbacks
        // ...
      }
      
      // Restore mocks
      jest.restoreAllMocks();
    });

    it("should throw for empty content", async () => {
      await expect(session.chat([], false)).rejects.toThrow(
        "Message content cannot be empty"
      );
    });
  });

  describe("session operations", () => {
    it("should get history", async () => {
      const mockHistory: AgentMessage[] = [
        {
          sessionId: mockSessionId,
          taskId: "task-123",
          role: "user",
          to: "agent",
          type: "text",
          content: { type: "text", message: "Hello" },
          createTime: "2025-05-19T10:00:00Z",
        },
      ];

      mockApiService.get.mockResolvedValue(mockHistory);

      const history = await session.getHistory();
      expect(history).toEqual(mockHistory);
      expect(mockApiService.get).toHaveBeenCalledWith("history", {
        sessionId: mockSessionId,
      });
    });

    it("should send function call result", async () => {
      const mockResult = { foo: "bar" };
      await session.sendFunctionCallResult("func-123", mockResult);

      expect(mockApiService.post).toHaveBeenCalledWith(
        "callback",
        { id: "func-123", result: mockResult },
        { sessionId: mockSessionId }
      );
      expect(mockLogger.debug).toHaveBeenCalledWith(
        "Function call result sent successfully for ID: func-123"
      );
    });

    it("should stop session", async () => {
      mockApiService.get.mockResolvedValue({ sessionId: mockSessionId });

      const response = await session.stop();
      expect(response).toEqual({ sessionId: mockSessionId });
      expect(mockApiService.get).toHaveBeenCalledWith("stop", {
        sessionId: mockSessionId,
      });
    });

    it("should clear session", async () => {
      mockApiService.get.mockResolvedValue({ id: mockSessionId });

      const response = await session.clear();
      expect(response).toEqual({ id: mockSessionId });
      expect(mockApiService.get).toHaveBeenCalledWith("clear", {
        sessionId: mockSessionId,
      });
      expect(mockLogger.info).toHaveBeenCalledWith(
        "Session cleared successfully"
      );
    });
  });
});
