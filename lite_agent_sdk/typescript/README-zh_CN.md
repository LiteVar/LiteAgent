# LiteAgent TypeScript SDK

中文 · [English](README.md)

全面的 TypeScript SDK，用于集成 LiteAgent API。

## 特性

- 🌐 **跨平台支持**：在浏览器、Node.js 和 React Native 中无缝运行
- 🔄 **流式响应**：实时聊天响应与流式能力
- 🛠️ **函数调用**：内置支持 agent 函数调用
- 🔒 **类型安全**：使用 TypeScript 实现完全类型化的 API
- 🧩 **模块化设计**：适应不同环境的可调整架构

## 安装

使用 npm 安装：

```bash
npm install liteagent-sdk
```

或使用 yarn：

```bash
yarn add liteagent-sdk
```

## 使用方法

### 初始化 SDK

```typescript
import { LiteAgentClient } from 'liteagent-sdk';

const client = new LiteAgentClient({
  apiKey: 'sk-your-api-key',
  baseUrl: 'https://your-liteagent-api-endpoint.com/liteAgent/v1',
  enableDebugLogs: true // 可选
});
```

### 开始聊天会话

```typescript
// 初始化会话
const session = await client.initSession();
console.log(`会话已创建，ID：${session.getSessionId()}`);

// 发送带流式响应的消息
await session.chat(
  [{ type: 'text', message: '什么是 LiteAgent？' }], 
  true, // 启用流式响应
  {
    onMessage: (message) => {
      console.log('完整消息:', message);
    },
    onChunk: (chunk) => {
      process.stdout.write(chunk.part || ''); // 打印流式文本
    },
    onFunctionCall: async (funcCall) => {
      console.log('收到函数调用:', funcCall);
      
      // 发送函数调用结果
      await session.sendFunctionCallResult(
        funcCall.content.id,
        { status: 'success', data: '函数结果' }
      );
    },
    onComplete: () => {
      console.log('\n流式响应完成');
    },
    onError: (error) => {
      console.error('流式响应错误:', error);
    }
  }
);
```

### 管理会话

```typescript
// 获取消息历史
const history = await session.getHistory();
console.log('消息历史:', history);

// 停止运行中的任务
await session.stop();

// 清除会话
await session.clear();
```

### 发送图片

```typescript
// 发送带图片的消息
await session.chat([
  { type: 'text', message: '这张图片里有什么？' },
  { type: 'imageUrl', message: 'https://example.com/image.jpg' }
], true);

// 使用 base64 图片
import { imageToBase64 } from 'liteagent-sdk';

const fileInput = document.querySelector('input[type="file"]');
const file = fileInput.files[0];
const base64Image = await imageToBase64(file);

await session.chat([
  { type: 'text', message: '描述这张图片' },
  { type: 'imageUrl', message: `data:image/jpeg;base64,${base64Image}` }
], true);
```

## 浏览器使用

对于浏览器环境，SDK 提供了 UMD 格式：

```html
<script src="https://unpkg.com/liteagent-sdk/dist/index.umd.js"></script>
<script>
  const client = new LiteAgent.LiteAgentClient({
    apiKey: 'sk-your-api-key',
    baseUrl: 'https://your-api-endpoint.com/liteAgent/v1'
  });
  
  // 使用 SDK...
</script>
```

## 错误处理

SDK 提供了自定义错误类，以便更好地处理错误：

```typescript
import { 
  LiteAgentError, 
  NetworkError, 
  AuthenticationError,
  ValidationError,
  TimeoutError 
} from 'liteagent-sdk';

try {
  await client.chat([{ type: 'text', message: '你好' }], true);
} catch (error) {
  if (error instanceof NetworkError) {
    console.error('网络问题:', error.message);
  } else if (error instanceof AuthenticationError) {
    console.error('认证失败:', error.message);
  } else if (error instanceof ValidationError) {
    console.error('验证错误:', error.message);
  } else if (error instanceof TimeoutError) {
    console.error('请求超时:', error.message);
  } else {
    console.error('未知错误:', error);
  }
}
```

## 许可证

MIT