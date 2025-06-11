# LiteAgent TypeScript SDK

English · [中文](README-zh_CN.md)

A comprehensive TypeScript SDK for integrating with the LiteAgent API.

## Features

- 🌐 **Cross-Platform Support**: Works seamlessly in browsers, Node.js, and React Native
- 🔄 **Streaming Support**: Real-time chat responses with streaming capabilities
- 🛠️ **Function Calling**: Built-in support for agent function calling
- 🔒 **Type Safety**: Fully typed API with TypeScript
- 🧩 **Modular Design**: Adaptable architecture for different environments

## Installation

Install the package using npm:

```bash
npm install liteagent-sdk
```

Or using yarn:

```bash
yarn add liteagent-sdk
```

## Usage

### Initializing the SDK

```typescript
import { LiteAgentClient } from 'liteagent-sdk';

const client = new LiteAgentClient({
  apiKey: 'sk-your-api-key',
  baseUrl: 'https://your-liteagent-api-endpoint.com/liteAgent/v1',
  enableDebugLogs: true // Optional
});
```

### Starting a Chat Session

```typescript
// Initialize a session
const session = await client.initSession();
console.log(`Session created with ID: ${session.getSessionId()}`);

// Send a message with streaming response
await session.chat(
  [{ type: 'text', message: 'What is LiteAgent?' }], 
  true, // enable streaming
  {
    onMessage: (message) => {
      console.log('Full message:', message);
    },
    onChunk: (chunk) => {
      process.stdout.write(chunk.part || ''); // Print streaming text
    },
    onFunctionCall: async (funcCall) => {
      console.log('Function call received:', funcCall);
      
      // Send function call results back
      await session.sendFunctionCallResult(
        funcCall.content.id,
        { status: 'success', data: 'Function result' }
      );
    },
    onComplete: () => {
      console.log('\nStreaming completed');
    },
    onError: (error) => {
      console.error('Error in streaming:', error);
    }
  }
);
```

### Managing Sessions

```typescript
// Get message history
const history = await session.getHistory();
console.log('Message history:', history);

// Stop a running task
await session.stop();

// Clear the session
await session.clear();
```

### Sending Images

```typescript
// Send a message with an image
await session.chat([
  { type: 'text', message: 'What's in this image?' },
  { type: 'imageUrl', message: 'https://example.com/image.jpg' }
], true);

// With a base64 image
import { imageToBase64 } from 'liteagent-sdk';

const fileInput = document.querySelector('input[type="file"]');
const file = fileInput.files[0];
const base64Image = await imageToBase64(file);

await session.chat([
  { type: 'text', message: 'Describe this image' },
  { type: 'imageUrl', message: `data:image/jpeg;base64,${base64Image}` }
], true);
```

## Browser Usage

For browser environments, the SDK is available as a UMD bundle:

```html
<script src="https://unpkg.com/liteagent-sdk/dist/index.umd.js"></script>
<script>
  const client = new LiteAgent.LiteAgentClient({
    apiKey: 'sk-your-api-key',
    baseUrl: 'https://your-api-endpoint.com/liteAgent/v1'
  });
  
  // Use the SDK...
</script>
```

## Error Handling

The SDK provides custom error classes for better error handling:

```typescript
import { 
  LiteAgentError, 
  NetworkError, 
  AuthenticationError,
  ValidationError,
  TimeoutError 
} from 'liteagent-sdk';

try {
  await client.chat([{ type: 'text', message: 'Hello' }], true);
} catch (error) {
  if (error instanceof NetworkError) {
    console.error('Network issue:', error.message);
  } else if (error instanceof AuthenticationError) {
    console.error('Authentication failed:', error.message);
  } else if (error instanceof ValidationError) {
    console.error('Validation error:', error.message);
  } else if (error instanceof TimeoutError) {
    console.error('Request timed out:', error.message);
  } else {
    console.error('Unknown error:', error);
  }
}
```

## License

MIT