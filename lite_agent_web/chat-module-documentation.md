# Chat 模块架构文档

## 概述

Chat 模块是一个复杂的实时对话系统，支持文本、语音输入输出，具备 AI Agent 调用、工具使用、知识库检索等高级功能。该模块采用组件化设计，通过自定义 Hooks 管理状态和业务逻辑。

## 核心架构

### 1. 主要组件结构

```
src/components/chat/
├── Chat.tsx                 # 主容器组件
├── ChatHeader.tsx           # 聊天头部
├── ChatInput.tsx            # 输入框（支持文本/语音）
├── ChatMessages.tsx         # 消息列表容器
├── ChatMessage.tsx          # 单条消息组件
├── ChatThoughtProcess.tsx   # 思考过程展示
├── ChatResultProcess.tsx    # 结果处理展示
├── ChatDispatch.tsx         # 指令分发展示
├── ChatAgentSwitch.tsx      # Agent 切换展示
├── ChatKnowledge.tsx        # 知识库检索展示
├── ChatReflect.tsx          # 反思内容展示
├── ChatTool.tsx             # 工具调用展示
├── ChatThink.tsx            # 思考过程展示
├── ChatPlanning.tsx         # 任务规划展示
├── MessageActions.tsx       # 消息操作（复制/TTS）
└── ScrollToBottom.tsx       # 滚动到底部按钮
```

### 2. 状态管理 Hooks

```
src/hooks/
├── useChatHandleEvent.tsx       # 主要的事件处理和状态管理
├── useChatMessageEvent.tsx      # 消息事件处理
├── useChatStreamDeltaEvent.tsx  # 流式响应处理
└── useChatAdjustAssistantMsg.tsx # 消息格式调整
```

## 组件详细说明

### Chat.tsx - 主容器组件
- **功能**: 整个聊天模块的容器，协调所有子组件
- **核心属性**:
  - `mode`: 'dev' | 'prod' - 开发/生产模式
  - `agentInfo`: Agent 详细信息
  - `agentId`: Agent ID
  - `asrEnabled`: 是否启用语音识别
- **关键特性**:
  - 历史消息加载（滚动到顶部时触发）
  - 思考过程详情面板（右侧抽屉）
  - 知识库检索结果弹窗

### ChatHeader.tsx - 聊天头部
- **功能**: 显示 Agent 名称和操作菜单
- **操作**: 清空上下文功能
- **模式差异**: 开发模式显示"调试"标题，生产模式显示 Agent 名称

### ChatInput.tsx - 输入组件
- **功能**: 多模态输入支持
- **输入模式**:
  - 文本输入（支持 Shift+Enter 换行）
  - 语音输入（长按录音/空格键录音）
- **特殊处理**:
  - 反思类 Agent 不支持聊天
  - 自动调整文本框高度
  - 语音转文字（ASR）集成

### ChatMessages.tsx & ChatMessage.tsx - 消息展示
- **消息类型**:
  - 用户消息（右对齐，灰色背景）
  - AI 助手消息（左对齐，带 Agent 图标）
  - 系统消息（加载中状态）
  - 分隔符（聊天新话题）
- **消息结构**:
  - `thoughtProcessMessages`: 思考过程消息
  - `resultProcessMessages`: 结果处理消息
- **交互功能**:
  - 复制消息内容
  - 文字转语音（TTS）播放
  - 查看思考过程详情

### ChatThoughtProcess.tsx - 思考过程容器
- **功能**: 展示 AI 的思考过程
- **包含组件**:
  - ChatReflect: 反思内容
  - ChatTool: 工具调用过程
  - ChatKnowledge: 知识库检索
  - ChatThink: 思考内容
  - ChatDispatch: 指令分发
  - ChatAgentSwitch: Agent 切换

### 特殊功能组件

#### ChatTool.tsx - 工具调用
- 展示工具名称、请求参数、响应结果
- 支持折叠/展开显示
- 实时显示"正在响应中..."状态

#### ChatKnowledge.tsx - 知识库检索
- 显示检索内容和结果
- 支持点击查看详细检索记录
- 可折叠展示

#### ChatPlanning.tsx - 任务规划
- 树状展示任务列表
- 支持嵌套子任务
- 提供"执行方案"按钮

#### ChatAgentSwitch.tsx - Agent 切换
- 显示调用的子 Agent 信息
- 展示输入指令和输出内容
- 支持折叠查看详情

## 数据流和状态管理

### 1. 核心状态（useChatHandleEvent）
```typescript
- messagesMap: 消息映射表 { [agentId]: { messages: AgentMessage[] } }
- sessionRef: 当前会话 ID
- scrollRef/thinkScrollRef: 滚动容器引用
- value: 输入框内容
- knowledgeSearchResults: 知识库搜索结果
- asrLoading: 语音识别加载状态
```

### 2. 消息流处理

#### 发送消息流程:
1. 用户输入（文本/语音）
2. 初始化会话（如需要）
3. 通过 SSE (Server-Sent Events) 发送消息
4. 实时接收流式响应

#### 消息事件类型:
- `MESSAGE`: 完整消息事件
- `DELTA`: 流式文本片段
- `ERROR`: 错误消息
- `END`: 结束标记

### 3. 消息格式化流程（useChatAdjustAssistantMsg）
- 合并工具请求和响应
- 按时间排序消息
- 分离思考过程和结果消息
- 处理子 Agent 消息

## 高级功能

### 1. 语音功能
- **ASR（语音识别）**:
  - 支持长按/空格键录音
  - 30秒超时处理
  - 自动发送识别结果
- **TTS（语音合成）**:
  - 支持将 AI 回复转为语音
  - 分段处理长文本（500字符/段）
  - 可暂停/继续播放

### 2. 历史消息加载
- 滚动到顶部触发加载
- 每次加载10条消息
- 保持滚动位置不变

### 3. 会话管理
- 自动会话初始化
- 会话过期处理
- 清空上下文功能
- 开发/生产环境区分

### 4. 实时更新
- 流式文本渲染
- 思考过程实时展示
- 工具调用状态更新
- 滚动位置自动调整

## 修改建议和扩展点

### 1. 性能优化
- 消息列表虚拟滚动（大量消息时）
- 消息组件 memo 优化
- 防抖/节流优化（滚动事件）

### 2. 功能增强
- 消息搜索功能
- 消息编辑/删除
- 多媒体消息支持（图片/文件）
- 消息收藏/标记

### 3. 用户体验
- 键盘快捷键支持
- 消息发送状态指示
- 重试机制优化
- 离线消息缓存

### 4. 代码质量
- TypeScript 类型完善
- 错误边界处理
- 单元测试覆盖
- 组件拆分优化

## 注意事项

1. **消息 ID 管理**: 使用时间戳作为消息 ID，注意并发情况
2. **流式响应处理**: 需要正确处理文本片段拼接和状态更新
3. **内存管理**: 音频播放后需要释放 blob URL
4. **会话状态**: 注意会话过期和重新初始化的处理
5. **组件卸载**: 清理定时器、事件监听器和进行中的请求

## 依赖关系图

```
Chat.tsx
├── ChatHeader.tsx
├── ChatMessages.tsx
│   └── ChatMessage.tsx
│       ├── MessageActions.tsx
│       └── ChatResultProcess.tsx
│           └── ChatPlanning.tsx
├── ChatInput.tsx
├── ScrollToBottom.tsx
└── ChatThoughtProcess.tsx
    ├── ChatReflect.tsx
    ├── ChatTool.tsx
    ├── ChatKnowledge.tsx
    ├── ChatThink.tsx
    ├── ChatDispatch.tsx
    └── ChatAgentSwitch.tsx

Hooks 依赖:
useChatHandleEvent.tsx
├── useChatMessageEvent.tsx
├── useChatStreamDeltaEvent.tsx
└── useChatAdjustAssistantMsg.tsx
```

这个架构设计体现了良好的关注点分离和组件化思想，通过 Hooks 实现了复杂的状态管理和业务逻辑封装。