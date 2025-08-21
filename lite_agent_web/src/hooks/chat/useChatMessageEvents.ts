import { useCallback, useMemo } from 'react';
import { MessageRole, TaskMessageType } from '@/types/Message';
import { FN_CALL_LIST, TOOL_RETURN } from '@/constants/message';
import { UseChatMessageEventProps, SSEEventData, AgentMessage, ToolCall, ToolMessage } from '@/types/chat';
import { OutMessage } from '@/client';
import { debounce } from 'lodash';

interface UseChatMessageEventsReturn {
  handleMessageEvent: (jsonData: SSEEventData, id: string, agentId: string) => void;
}

type MessageActions = UseChatMessageEventProps['messageActions'];
type AgentSwitchRef = UseChatMessageEventProps['agentSwitchRef'];

// Helper types for better type safety
interface ProcessedMessageData {
  message: AgentMessage;
  subAgents: AgentMessage[];
}

interface ToolRequestMessage {
  taskId: string;
  parentTaskId?: string;
  agentId: string;
  role: MessageRole.TOOL;
  req: SSEEventData & { tool: ToolCall };
  createTime?: string;
  responding: boolean;
}

type MessageIndexResult = {
  found: boolean;
  index: number;
};

// Utility functions for better code organization
const deepClone = <T>(obj: T): T => JSON.parse(JSON.stringify(obj));

const findMessageIndex = (
  messages: AgentMessage[],
  id: string,
  role: MessageRole,
  type?: string
): MessageIndexResult => {
  const index = messages.findIndex(
    (msg) => msg?.id === id && msg.role === role && (!type || msg.type === type)
  );
  return { found: index !== -1, index };
};

const findLoadingMessageIndex = (messages: AgentMessage[], id: string): number => {
  const index = messages.findIndex(
    (msg) => msg?.id === id && msg.role === MessageRole.SYSTEM && msg.type === 'loading'
  );
  return index === -1 ? messages.length : index;
};

const createBaseAssistantMessage = (jsonData: SSEEventData, id: string): AgentMessage =>
  ({
    role: MessageRole.ASSISTANT,
    agentId: jsonData.agentId,
    type: 'text',
    content: '',
    responding: true,
    taskId: jsonData.taskId,
    thoughtProcessMessages: [],
    resultProcessMessages: [],
    id: id,
  }) as unknown as AgentMessage;

const updateMessageArray = (
  agentId: string,
  messageId: string,
  messageActions: MessageActions,
  updater: (messages: AgentMessage[]) => AgentMessage[]
): void => {
  messageActions.complexMessageUpdate(agentId, messageId, (prev) => {
    const msgsMap = deepClone(prev);
    const currentMessages = prev?.[agentId]?.messages || [];
    const newMessages = updater(deepClone(currentMessages));

    msgsMap[agentId] = { messages: newMessages };
    return msgsMap;
  });
};

const handleUserMessage = (jsonData: SSEEventData, id: string, messageActions: MessageActions): void => {
  messageActions.addMessage(jsonData.agentId, jsonData as AgentMessage);
  messageActions.addMessage(jsonData.agentId, {
    role: MessageRole.SYSTEM,
    type: 'loading',
    content: '正在处理中...',
    agentId: jsonData.agentId,
    taskId: jsonData.taskId,
    id: id,
  } as AgentMessage);
};

const updateSubAgentFunctionCallList = (
  messages: AgentMessage[],
  toolMessages: ToolRequestMessage[]
): AgentMessage[] => {
  for (let index = 0; index < messages.length; index++) {
      const element = messages[index];
    //@ts-ignore
    if (toolMessages[0].taskId === element.taskId && toolMessages[0].parentTaskId === element.parentTaskId && element.type === TaskMessageType.AGENT_SWITCH) {
      element.messages = (
        element.messages || []
      ).concat(toolMessages as AgentMessage[]);
      break;
    } else if (element.type === TaskMessageType.AGENT_SWITCH) {
      updateSubAgentFunctionCallList(element.messages || [], toolMessages);
    }
  }
  return messages;
}

const handleFunctionCallList = (
  jsonData: SSEEventData,
  id: string,
  agentId: string,
  messageActions: MessageActions
): void => {
  const toolRequestMessages: ToolRequestMessage[] = jsonData.toolCalls!.map((tool) => ({
    parentTaskId: jsonData.parentTaskId,
    taskId: jsonData.taskId,
    agentId: jsonData.agentId,
    role: MessageRole.TOOL,
    req: { ...jsonData, tool },
    createTime: jsonData.createTime,
    responding: true,
  }));

  updateMessageArray(agentId, id, messageActions, (messages) => {
    const assistantMessageIndex = findMessageIndex(messages, id, MessageRole.ASSISTANT, TaskMessageType.TEXT);

    if (assistantMessageIndex.found) {
      //@ts-ignore
      const agentSwitchIndex = (messages[assistantMessageIndex.index].thoughtProcessMessages || []).findIndex((msg) =>
        msg.type === TaskMessageType.AGENT_SWITCH &&
        msg.taskId === jsonData.taskId &&
        msg.parentTaskId === jsonData.parentTaskId
      );
      messages[assistantMessageIndex.index].responding = true;
      if (agentSwitchIndex !== -1 && jsonData.parentTaskId) {
        messages[assistantMessageIndex.index].thoughtProcessMessages = updateSubAgentFunctionCallList(messages[assistantMessageIndex.index].thoughtProcessMessages || [], toolRequestMessages);
      } else {
        messages[assistantMessageIndex.index].thoughtProcessMessages = (
          messages[assistantMessageIndex.index].thoughtProcessMessages || []
        ).concat(toolRequestMessages as AgentMessage[]);
      }
    } else {
      const loadingIndex = findLoadingMessageIndex(messages, id);
      messages[loadingIndex] = {
        ...createBaseAssistantMessage(jsonData, id),
        thoughtProcessMessages: toolRequestMessages as AgentMessage[],
      } as AgentMessage;
    }
    return messages;
  });
};

const updateSubAgentToolReturn = (
  messages: AgentMessage[],
  message: ToolMessage
): AgentMessage[] => {
  for (let index = 0; index < messages.length; index++) {
    const element = messages[index];
    //@ts-ignore
    if (message.role === MessageRole.TOOL && element.req?.tool?.id === message.toolCallId) {
      //@ts-ignore
      element.res = message;
      break;
    } else if (element.type === TaskMessageType.AGENT_SWITCH) {
      updateSubAgentToolReturn(element.messages || [], message);
    }
  }

  return messages;
}

const handleToolReturn = (
  jsonData: SSEEventData,
  id: string,
  agentId: string,
  messageActions: MessageActions
): void => {
  updateMessageArray(agentId, id, messageActions, (messages) => {
    const assistantMessageIndex = findMessageIndex(messages, id, MessageRole.ASSISTANT, TaskMessageType.TEXT);

    if (assistantMessageIndex.found) {
      messages[assistantMessageIndex.index].responding = true;
      messages[assistantMessageIndex.index].thoughtProcessMessages = updateSubAgentToolReturn(messages[assistantMessageIndex.index].thoughtProcessMessages || [], jsonData as ToolMessage);
    }
    return messages;
  });
};

const processMessageData = (jsonData: SSEEventData): ProcessedMessageData => {
  let message: AgentMessage = { ...jsonData } as AgentMessage;
  const subAgents: AgentMessage[] = [];

  if (jsonData.role === MessageRole.SUBAGENT && jsonData.type === TaskMessageType.DISPATCH) {
    message = {
      ...jsonData,
      role: MessageRole.SUBAGENT,
    } as AgentMessage;

    if (Array.isArray(jsonData.content)) {
      jsonData.content.forEach((subAgent) => {
        subAgents.push({
          agentId: jsonData.agentId,
          taskId: jsonData.taskId,
          role: MessageRole.SUBAGENT,
          ...subAgent,
        } as AgentMessage);
      });
    }
  }

  if (jsonData.role === MessageRole.AGENT && jsonData.type === TaskMessageType.PLANNING) {
    message = {
      ...jsonData,
      visible: true,
    } as AgentMessage;
  }

  return { message, subAgents };
};

const updateExistingMessage = (
  messages: AgentMessage[],
  index: number,
  message: AgentMessage,
  subAgents: AgentMessage[],
  agentSwitchRef: AgentSwitchRef
): void => {
  messages[index].responding = true;

  const isThoughtMessage =
    message.role === MessageRole.TOOL ||
    message.role === MessageRole.REFLECTION ||
    (message.role === MessageRole.AGENT && message.type === TaskMessageType.DISPATCH) ||
    (message.role === MessageRole.AGENT && message.type === TaskMessageType.AGENT_SWITCH) ||
    (message.role === MessageRole.AGENT && message.type === TaskMessageType.KNOWLEDGE);

  const isResultMessage =
    message.role === MessageRole.SUBAGENT ||
    message.type === TaskMessageType.PLANNING ||
    (message.role === MessageRole.AGENT && message.type === TaskMessageType.ERROR);

  if (isThoughtMessage) {
    messages[index].thoughtProcessMessages = messages[index].thoughtProcessMessages || [];
    const currentAgentSwitch = messages[index].thoughtProcessMessages.findIndex(msg => msg.type === TaskMessageType.AGENT_SWITCH && msg.taskId === message.taskId);
    if (currentAgentSwitch !== -1) {
      const agentSwitchMessage = messages[index].thoughtProcessMessages[currentAgentSwitch];
      if (agentSwitchMessage.messages) {
        agentSwitchMessage.messages.push(message);
      } else {
        agentSwitchMessage.messages = [message];
      }
    } else {
      messages[index].thoughtProcessMessages.push(message);
    }
  } else if (isResultMessage) {
    messages[index].resultProcessMessages = messages[index].resultProcessMessages || [];
    if (message.role === MessageRole.SUBAGENT) {
      messages[index].resultProcessMessages = messages[index].resultProcessMessages!.concat(subAgents);
    } else {
      messages[index].resultProcessMessages!.push(message);
    }
  } else if (message.role === MessageRole.AGENT && message.type === TaskMessageType.DISPATCH) {
    messages[index].thoughtProcessMessages = messages[index].thoughtProcessMessages || [];

    if (agentSwitchRef.current) {
      const agentSwitchIndex = messages[index].thoughtProcessMessages!.findIndex(
        (msg) =>
          msg.type === TaskMessageType.AGENT_SWITCH &&
          msg.agentId === agentSwitchRef.current?.agentId &&
          msg.createTime === agentSwitchRef.current?.createTime
      );

      if (agentSwitchIndex !== -1) {
        const agentSwitchMessage = messages[index].thoughtProcessMessages![agentSwitchIndex];
        if (agentSwitchMessage.messages) {
          agentSwitchMessage.messages.push(message);
        } else {
          agentSwitchMessage.messages = [message];
        }
      } else {
        messages[index].thoughtProcessMessages!.push(message);
      }
    } else {
      messages[index].thoughtProcessMessages!.push(message);
    }
  } else if (message.role === MessageRole.AGENT && message.type === TaskMessageType.AGENT_SWITCH) {
    message.messages = [];
    agentSwitchRef.current = message;
    messages[index].thoughtProcessMessages = messages[index].thoughtProcessMessages || [];
    messages[index].thoughtProcessMessages!.push(message);
  }
};

const createNewMessage = (
  messages: AgentMessage[],
  loadingIndex: number,
  message: AgentMessage,
  subAgents: AgentMessage[],
  jsonData: SSEEventData,
  id: string,
  agentSwitchRef: AgentSwitchRef
): void => {
  const baseMessage = createBaseAssistantMessage(jsonData, id);

  const isThoughtMessage =
    message.role === MessageRole.TOOL ||
    message.role === MessageRole.REFLECTION ||
    (message.role === MessageRole.AGENT && message.type === TaskMessageType.KNOWLEDGE);

  const isResultMessage =
    message.role === MessageRole.SUBAGENT ||
    message.type === TaskMessageType.PLANNING ||
    (message.role === MessageRole.AGENT && message.type === TaskMessageType.ERROR);

  if (isThoughtMessage) {
    messages[loadingIndex] = {
      ...baseMessage,
      thoughtProcessMessages: [message],
    } as AgentMessage;
  } else if (isResultMessage) {
    messages[loadingIndex] = {
      ...baseMessage,
      resultProcessMessages: message.role === MessageRole.SUBAGENT ? subAgents : [message],
    } as AgentMessage;
  } else if (message.role === MessageRole.AGENT && message.type === TaskMessageType.DISPATCH) {
    messages[loadingIndex] = {
      ...baseMessage,
      thoughtProcessMessages: [message],
    } as AgentMessage;
  } else if (message.role === MessageRole.AGENT && message.type === TaskMessageType.AGENT_SWITCH) {
    message.messages = [];
    agentSwitchRef.current = message;
    messages[loadingIndex] = {
      ...baseMessage,
      thoughtProcessMessages: [message],
    } as AgentMessage;
  }
};

const handleComplexMessageUpdate = (
  jsonData: SSEEventData,
  id: string,
  agentId: string,
  messageActions: MessageActions,
  agentSwitchRef: AgentSwitchRef
): void => {
  const { message, subAgents } = processMessageData(jsonData);

  updateMessageArray(agentId, id, messageActions, (messages) => {
    const assistantMessageIndex = findMessageIndex(messages, id, MessageRole.ASSISTANT, TaskMessageType.TEXT);

    if (assistantMessageIndex.found) {
      updateExistingMessage(messages, assistantMessageIndex.index, message, subAgents, agentSwitchRef);
    } else {
      const loadingIndex = findLoadingMessageIndex(messages, id);
      createNewMessage(messages, loadingIndex, message, subAgents, jsonData, id, agentSwitchRef);
    }

    return messages;
  });
};

export const useChatMessageEvents = (props: UseChatMessageEventProps): UseChatMessageEventsReturn => {
  const { scrollToBottom, messageActions, agentSwitchRef } = props;

  const debouncedScrollToBottom = useMemo(() => debounce(scrollToBottom, 100), [scrollToBottom]);

  const handleMessageEvent = useCallback(
    (jsonData: SSEEventData, id: string, agentId: string) => {
      if (jsonData.role === 'user' && jsonData.content) {
        handleUserMessage(jsonData, id, messageActions);
      } else if (jsonData.type === FN_CALL_LIST && jsonData.toolCalls) {
        handleFunctionCallList(jsonData, id, agentId, messageActions);
      } else if (jsonData.type === TOOL_RETURN) {
        handleToolReturn(jsonData, id, agentId, messageActions);
      } else {
        handleComplexMessageUpdate(jsonData, id, agentId, messageActions, agentSwitchRef);
      }

      debouncedScrollToBottom();
    },
    [messageActions, debouncedScrollToBottom, agentSwitchRef]
  );

  return {
    handleMessageEvent,
  };
};
