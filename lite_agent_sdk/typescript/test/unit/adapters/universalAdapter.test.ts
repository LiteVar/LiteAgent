import { UniversalAdapter } from "../../../src/adapters/universalAdapter";
import { NetworkError } from "../../../src/errors/error-classes";

jest.mock("../../../src/utils", () => {
  return {
    createRequestWithTimeout: jest.fn(() => {
      return async (options: any) => {
        const url = options.url;
        if (!url) throw new Error("URL is required");

        const response = await fetch(url, {
          method: options.method || "GET",
          headers: options.headers || {},
          body: options.body,
        });

        const contentType =
          response.headers?.get?.("content-type") || "application/json";
        const responseData = contentType.includes("application/json")
          ? await response.json()
          : await response.text();

        if (!response.ok) {
          throw new Error(`Request failed with status ${response.status}`);
        }

        return responseData;
      };
    }),
  };
});

describe("UniversalAdapter", () => {
  let adapter: UniversalAdapter;

  beforeEach(() => {
    adapter = new UniversalAdapter();
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
  });

  describe("request()", () => {
    it("should send request and return JSON response", async () => {
      const mockJson = { result: "ok" };

      (fetch as jest.Mock).mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockJson),
        headers: {
          get: () => "application/json",
        },
      });

      const response = await adapter.request({
        url: "https://example.com",
        method: "GET",
        headers: {},
      });

      expect(response).toEqual(mockJson);
      expect(fetch).toHaveBeenCalledWith(
        "https://example.com",
        expect.objectContaining({
          method: "GET",
        })
      );
    });

    it("should throw if fetch fails", async () => {
      (fetch as jest.Mock).mockImplementation(() => {
        throw new Error("network failed");
      });

      await expect(
        adapter.request({
          url: "https://example.com",
          method: "GET",
          headers: {},
        })
      ).rejects.toThrow("network failed");
    });

    it("should handle non-OK responses and throw error", async () => {
      (fetch as jest.Mock).mockResolvedValue({
        ok: false,
        status: 403,
        statusText: "Forbidden",
        json: () => Promise.resolve({ error: "Access denied" }),
      });

      await expect(
        adapter.request({
          url: "https://example.com/protected",
          method: "GET",
          headers: { Authorization: "Bearer token" },
        })
      ).rejects.toThrow();
    });

    it("should handle response without JSON data", async () => {
      (fetch as jest.Mock).mockResolvedValue({
        ok: true,
        json: () => Promise.reject(new Error("Invalid JSON")),
        text: () => Promise.resolve("Hello World"),
      });

      await expect(
        adapter.request({
          url: "https://example.com/text",
          method: "GET",
        })
      ).rejects.toThrow();
    });

    it("should pass timeout options correctly", async () => {
      const mockJson = { success: true };
      (fetch as jest.Mock).mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockJson),
      });

      await adapter.request({
        url: "https://example.com/api",
        method: "GET",
        timeout: 5000,
      });

      expect(fetch).toHaveBeenCalledWith(
        "https://example.com/api",
        expect.objectContaining({
          method: "GET",
        })
      );
    });
  });

  describe("createEventSource()", () => {
    const streamData = [
      "event: message",
      'data: {"msg":"hello"}',
      "",
      "event: end",
      "data: done",
      "",
    ].join("\n");

    const createStream = (text: string): ReadableStream<Uint8Array> => {
      const encoder = new TextEncoder();
      const chunks = encoder.encode(text);
      return new ReadableStream({
        start(controller) {
          controller.enqueue(chunks);
          controller.close();
        },
      });
    };

    it("should handle open/message/end events", async () => {
      const listeners: Record<string, jest.Mock> = {
        open: jest.fn(),
        message: jest.fn(),
        end: jest.fn(),
      };

      (fetch as jest.Mock).mockResolvedValue({
        ok: true,
        body: createStream(streamData),
        statusText: "OK",
      });

      const eventSource = adapter.createEventSource(
        "https://example.com/stream",
        {
          method: "POST",
          headers: {},
          body: {},
        }
      );

      for (const [type, fn] of Object.entries(listeners)) {
        eventSource.addEventListener(type, fn);
      }

      await new Promise((r) => setTimeout(r, 100));

      expect(listeners.open).toHaveBeenCalled();
      expect(listeners.message).toHaveBeenCalledWith({
        data: '{"msg":"hello"}',
        event: "message",
      });
      expect(listeners.end).toHaveBeenCalled();
    });

    it("should call error listener on non-OK response", async () => {
      const errorListener = jest.fn();
      (fetch as jest.Mock).mockResolvedValue({
        ok: false,
        status: 500,
        statusText: "Server Error",
      });

      const eventSource = adapter.createEventSource(
        "https://example.com/stream",
        {
          method: "POST",
          headers: {},
          body: {},
        }
      );

      eventSource.addEventListener("error", errorListener);

      await new Promise((r) => setTimeout(r, 50));

      expect(errorListener).toHaveBeenCalledWith(
        expect.objectContaining({
          message: expect.stringContaining("Server responded with 500"),
        })
      );
    });
  });
});
