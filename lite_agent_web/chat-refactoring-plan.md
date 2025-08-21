# Chat 模块重构方案

## 重构目标
1. **提高可维护性**: 降低组件耦合度，提取可复用逻辑
2. **提升性能**: 优化渲染性能，减少不必要的重渲染
3. **增强类型安全**: 完善 TypeScript 类型定义
4. **改善代码结构**: 单一职责原则，更好的关注点分离

## 重构原则
- 渐进式重构，每个步骤独立可交付
- 保持功能完整性，不影响现有功能
- 优先重构独立性强的部分
- 每步都要有测试验证

## 重构计划（分6个阶段）

### 第一阶段：类型系统完善（1-2天）
**目标**: 建立完整的类型定义，消除 any 类型

#### 步骤1.1：创建独立的类型定义文件
```typescript
// src/types/chat
export interface ChatMessage {
  id: string;
  role: MessageRole;
  type: MessageType;
  content: string;
  taskId: string;
  // ... 完整定义
}

export interface ChatState {
  messages: ChatMessage[];
  session: string;
  loading: boolean;
  // ... 其他状态
}
```

#### 步骤1.2：消除 any 类型
- 为所有事件处理函数定义明确的类型
- 为 API 响应定义接口
- 为组件 props 完善类型定义

**验证**: 代码编译通过，类型检查无错误

### 第二阶段：组件优化（2-3天）
**目标**: 优化组件结构，提升渲染性能

#### 步骤2.1：使用 React.memo 优化纯展示组件
```typescript
// 优化消息组件
export const ChatMessage = React.memo<ChatMessageProps>(({ message, ... }) => {
  // 组件逻辑
}, (prevProps, nextProps) => {
  // 自定义比较逻辑
  return prevProps.message.id === nextProps.message.id && 
         prevProps.message.content === nextProps.message.content;
});
```

#### 步骤2.2：提取小组件，减少大组件复杂度
- 将 ChatMessage 中的 TTS 逻辑提取为 `useTTS` hook
- 将消息渲染逻辑拆分为更小的组件
- 提取 `MessageContent`, `MessageAvatar`, `MessageStatus` 等子组件

#### 步骤2.3：实现虚拟滚动（可选，用于大量消息）
```typescript
// 使用 react-window 或 react-virtualized
import { VariableSizeList } from 'react-window';

const VirtualChatMessages = ({ messages, ... }) => {
  return (
    <VariableSizeList
      height={600}
      itemCount={messages.length}
      itemSize={getItemSize}
      width="100%"
    >
      {Row}
    </VariableSizeList>
  );
};
```

**验证**: 组件正常渲染，React DevTools 显示渲染次数减少

### 第三阶段：状态管理重构（3-4天）
**目标**: 简化状态管理，提高可维护性

#### 步骤3.1：拆分 useChatHandleEvent
将巨大的 hook 拆分为多个专注的 hooks：
```typescript
// 会话管理
export const useChatSession = (agentId: string) => {
  // 只处理会话相关逻辑
};

// 消息管理
export const useChatMessages = (agentId: string) => {
  // 只处理消息相关逻辑
};

// 输入管理
export const useChatInput = () => {
  // 只处理输入相关逻辑
};

// SSE 连接管理
export const useChatSSE = (session: string) => {
  // 只处理 SSE 相关逻辑
};
```

#### 步骤3.2：引入 Context API 减少 prop drilling
```typescript
// ChatContext.tsx
interface ChatContextValue {
  messages: ChatMessage[];
  sendMessage: (message: string) => void;
  // ... 其他共享状态和方法
}

export const ChatContext = createContext<ChatContextValue>();

export const ChatProvider: React.FC = ({ children }) => {
  // 状态管理逻辑
  return (
    <ChatContext.Provider value={contextValue}>
      {children}
    </ChatContext.Provider>
  );
};
```

#### 步骤3.3：使用 useReducer 管理复杂状态
```typescript
// chatReducer.ts
type ChatAction = 
  | { type: 'ADD_MESSAGE'; payload: ChatMessage }
  | { type: 'UPDATE_MESSAGE'; payload: { id: string; updates: Partial<ChatMessage> } }
  | { type: 'CLEAR_MESSAGES' }
  // ... 其他 actions

const chatReducer = (state: ChatState, action: ChatAction): ChatState => {
  switch (action.type) {
    case 'ADD_MESSAGE':
      return { ...state, messages: [...state.messages, action.payload] };
    // ... 其他 cases
  }
};
```

**验证**: 状态更新正常，组件间通信无问题

### 第四阶段：消息处理逻辑优化（2-3天）
**目标**: 统一消息处理流程，提高代码复用性

#### 步骤4.1：创建消息处理管道
```typescript
// messageProcessor.ts
interface MessageProcessor {
  process(message: RawMessage): ProcessedMessage;
}

class MessageProcessorPipeline {
  private processors: MessageProcessor[] = [];
  
  addProcessor(processor: MessageProcessor) {
    this.processors.push(processor);
  }
  
  process(message: RawMessage): ProcessedMessage {
    return this.processors.reduce((msg, processor) => 
      processor.process(msg), message
    );
  }
}

// 使用示例
const pipeline = new MessageProcessorPipeline();
pipeline.addProcessor(new ToolMessageProcessor());
pipeline.addProcessor(new ThoughtMessageProcessor());
pipeline.addProcessor(new ResultMessageProcessor());
```

#### 步骤4.2：统一 SSE 事件处理
```typescript
// sseEventHandler.ts
class SSEEventHandler {
  private handlers = new Map<string, EventHandler>();
  
  register(eventType: string, handler: EventHandler) {
    this.handlers.set(eventType, handler);
  }
  
  handle(event: EventSourceMessage) {
    const handler = this.handlers.get(event.event);
    if (handler) {
      return handler(JSON.parse(event.data));
    }
  }
}
```

**验证**: 消息处理流程正常，各类消息类型都能正确处理

### 第五阶段：音频功能模块化（1-2天）
**目标**: 将音频相关功能独立成可复用模块

#### 步骤5.1：创建独立的音频服务
```typescript
// audioService.ts
class AudioService {
  private audioCache = new Map<string, HTMLAudioElement>();
  
  async playTTS(text: string, modelId: string): Promise<void> {
    // TTS 播放逻辑
  }
  
  async recordAndTranscribe(modelId: string): Promise<string> {
    // ASR 录音和转写逻辑
  }
  
  stopAll(): void {
    // 停止所有音频
  }
}

export const audioService = new AudioService();
```

#### 步骤5.2：创建音频相关 hooks
```typescript
// useAudioPlayer.ts
export const useAudioPlayer = () => {
  const [status, setStatus] = useState<'idle' | 'loading' | 'playing'>('idle');
  
  const play = useCallback(async (text: string) => {
    setStatus('loading');
    try {
      await audioService.playTTS(text, modelId);
      setStatus('playing');
    } catch (error) {
      setStatus('idle');
    }
  }, []);
  
  return { status, play, stop: audioService.stopAll };
};
```

**验证**: 音频功能正常工作，代码更加模块化

### 第六阶段：性能优化和错误处理（2-3天）
**目标**: 提升整体性能，增强错误处理

#### 步骤6.1：添加错误边界
```typescript
// ChatErrorBoundary.tsx
class ChatErrorBoundary extends React.Component<Props, State> {
  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // 记录错误
    console.error('Chat Error:', error, errorInfo);
    // 可以发送到错误追踪服务
  }
  
  render() {
    if (this.state.hasError) {
      return <ErrorFallback onRetry={this.handleRetry} />;
    }
    return this.props.children;
  }
}
```

#### 步骤6.2：优化防抖和节流
```typescript
// 优化滚动处理
const handleScroll = useMemo(
  () => throttle(() => {
    // 滚动逻辑
  }, 100),
  []
);

// 优化输入处理
const debouncedSave = useMemo(
  () => debounce((value: string) => {
    // 保存逻辑
  }, 500),
  []
);
```

#### 步骤6.3：添加性能监控
```typescript
// performanceMonitor.ts
export const measureChatPerformance = () => {
  // 使用 Performance API 监控关键指标
  performance.mark('chat-render-start');
  // ... 渲染逻辑
  performance.mark('chat-render-end');
  performance.measure('chat-render', 'chat-render-start', 'chat-render-end');
};
```

**验证**: 错误能被正确捕获和处理，性能指标有所提升

## 实施建议

### 测试策略
1. 每个阶段完成后进行全面的功能测试
2. 编写单元测试覆盖关键逻辑
3. 使用 React Testing Library 进行组件测试
4. 进行性能对比测试

### 回滚计划
- 使用 Git 分支进行每个阶段的开发
- 每个阶段完成后创建标签
- 保留原始代码的备份分支
- 出现问题可快速回滚到上一个稳定版本

### 监控指标
1. **性能指标**
   - 首次渲染时间
   - 消息发送响应时间
   - 内存使用情况
   - React 组件渲染次数

2. **质量指标**
   - TypeScript 覆盖率
   - 测试覆盖率
   - 代码复杂度评分
   - Bundle 大小

### 风险评估
- **低风险**: 类型定义、组件优化
- **中风险**: 状态管理重构、消息处理优化
- **需要特别注意**: SSE 连接管理、音频功能重构

## 时间估算
- 总时长：约15-20个工作日
- 可以根据团队情况调整每个阶段的时间
- 建议每周完成1-2个阶段

## 预期收益
1. **可维护性提升**: 代码结构更清晰，职责更单一
2. **性能提升**: 减少30-50%的不必要渲染
3. **开发效率**: 新功能开发速度提升
4. **代码质量**: 类型安全，错误更少
5. **可测试性**: 更容易编写和维护测试

这个重构方案采用渐进式方法，确保每一步都是安全的，并且能带来实际的改进。