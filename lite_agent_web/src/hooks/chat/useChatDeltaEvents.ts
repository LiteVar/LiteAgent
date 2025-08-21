import React, { useCallback, useMemo } from 'react';
import { debounce } from 'lodash';
import { MessageRole, TaskMessageType } from '@/types/Message';
import {
  AgentMessage,
  UseChatStreamDeltaEventProps,
  DeltaEventData,
  AgentSwitchMessage,
  ChunkType,
  MessageChunkChangeFlag,
  MessageReducerActions,
} from '@/types/chat';
import { AgentStatusRef } from '@/types/chat/state';

interface UseChatDeltaEventsReturn {
  handleDeltaEvent: (jsonData: DeltaEventData, id: string, agentId: string) => void;
}

/**
 * 检查是否应该创建新的消息块
 * @param agentMessage 当前智能体消息
 * @returns 是否应该创建新消息块
 */
const shouldCreateNewChunk = (agentMessage?: AgentMessage | null): boolean => {
  return agentMessage?.flag === MessageChunkChangeFlag.NEW_CHUNK;
};

const createAgentMessage = (
  jsonData: DeltaEventData,
  id: string,
  type: TaskMessageType,
  content: string
): AgentMessage =>
  ({
    role: MessageRole.ASSISTANT,
    type,
    content,
    chunkType: jsonData.chunkType,
    taskId: jsonData.taskId,
    parentTaskId: jsonData.parentTaskId,
    agentId: jsonData.agentId,
    id,
    flag: MessageChunkChangeFlag.CONTINUE,
  }) as AgentMessage;

const updateThoughtProcessMessages = (
  messages: AgentMessage[],
  agentMessage: AgentMessage,
  createNewMessage: boolean
): void => {
  if (createNewMessage) {
    messages.push({ ...agentMessage, type: TaskMessageType.THINK } as AgentMessage);
  } else if (messages.length > 0) {
    const agentSwitchIndex = messages.findIndex(msg => msg.type === TaskMessageType.AGENT_SWITCH && msg.taskId === agentMessage.taskId);
    if (agentSwitchIndex !== -1) {
      messages[agentSwitchIndex].messages = messages[agentSwitchIndex].messages || [];
      const thinkMessageIndex = messages[agentSwitchIndex].messages.findIndex(msg => msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.THINK);
      if (thinkMessageIndex != -1) {
        for (let i = messages[agentSwitchIndex].messages.length - 1; i >= 0; i--) {
          if (
            (messages[agentSwitchIndex].messages[i].role === MessageRole.ASSISTANT && messages[i].type === TaskMessageType.THINK)
          ) {
            messages[i].content = agentMessage?.content;
            break;
          }
        }
      } else {
        messages[agentSwitchIndex].messages.push({ ...agentMessage, type: TaskMessageType.THINK } as AgentMessage);
      }
    } else {
      for (let i = messages.length - 1; i >= 0; i--) {
        if (
          (messages[i].role === MessageRole.ASSISTANT && messages[i].type === TaskMessageType.THINK) ||
          messages[i].role === MessageRole.AGENT
        ) {
          messages[i].content = agentMessage?.content;
          break;
        }
      }
    }
    
  } else {
    messages.push({ ...agentMessage, type: TaskMessageType.THINK } as AgentMessage);
  }
};

const updateResultProcessMessages = (
  messages: AgentMessage[],
  agentMessage: AgentMessage,
  createNewMessage: boolean
): void => {
  if (createNewMessage) {
    messages.push({ ...agentMessage, type: TaskMessageType.TEXT } as AgentMessage);
  } else if (messages.length > 0) {
    for (let i = messages.length - 1; i >= 0; i--) {
      if (
        (messages[i].role === MessageRole.ASSISTANT && messages[i].type === TaskMessageType.TEXT) ||
        messages[i].role === MessageRole.AGENT
      ) {
        messages[i].content = agentMessage?.content;
        break;
      }
    }
  } else {
    messages.push({ ...agentMessage, type: TaskMessageType.TEXT } as AgentMessage);
  }
};

const updateMessages = (
  agentSwitchRef: React.MutableRefObject<AgentSwitchMessage | undefined>,
  prevMessages: AgentMessage[],
  agentMessage: AgentMessage,
  jsonData: DeltaEventData,
  type: TaskMessageType,
  id: string,
  createNewMessage?: boolean
): AgentMessage[] => {
  const index = prevMessages.findIndex(
    (msg) => msg.id === id && msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.TEXT
  );
  const newMsgs = JSON.parse(JSON.stringify(prevMessages));

  if (index !== -1) {
    newMsgs[index].responding = true;

    if (type === TaskMessageType.THINK) {
      if ((agentMessage.parentTaskId && agentMessage.taskId) || agentMessage.agentId !== newMsgs[index].agentId) {
        const agentSwitchIndex = newMsgs[index].thoughtProcessMessages.findIndex(
          (msg) => msg.type === TaskMessageType.AGENT_SWITCH && msg.taskId === agentMessage.taskId && msg.parentTaskId === agentMessage.parentTaskId
        );
        if (agentSwitchIndex != -1) {
          if (createNewMessage) {
            newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages = newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages || [];
            newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages.push({ ...agentMessage, type: TaskMessageType.THINK } as AgentMessage);
          } else {
            newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages = newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages || [];
            const thinkMessageIndex = newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages.findIndex(msg => msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.THINK);
            if (thinkMessageIndex != -1) {
              for (let i = newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages.length - 1; i >= 0; i--) {
                if (
                  (newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages[i].role === MessageRole.ASSISTANT && newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages[i].type === TaskMessageType.THINK)
                ) {
                  newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages[i].content = agentMessage?.content;
                  break;
                }
              }
            } else {
              newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages.push({ ...agentMessage, type: TaskMessageType.THINK } as AgentMessage);
            }
          }
        }
      } else {
        updateThoughtProcessMessages(
          newMsgs[index].thoughtProcessMessages,
          agentMessage,
          createNewMessage || false
        );
      }
      
    } else {
      if ((agentMessage.parentTaskId && agentMessage.taskId) || agentMessage.agentId != newMsgs[index].agentId) {
        const agentSwitchIndex = newMsgs[index].thoughtProcessMessages.findIndex(
          (msg) => msg.type === TaskMessageType.AGENT_SWITCH && msg.taskId === agentMessage.taskId && msg.parentTaskId === agentMessage.parentTaskId
        );
        if (agentSwitchIndex != -1) {
          newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages = newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages || [];
          const contentMessageIndex = newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages.findIndex(msg => msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.TEXT);
          if (contentMessageIndex != -1) {
            newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages[contentMessageIndex].content = agentMessage.content;
          } else {
            newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages.push({ ...agentMessage, type: TaskMessageType.TEXT } as AgentMessage);
          }
        }
      }

      updateResultProcessMessages(
        newMsgs[index].resultProcessMessages,
        agentMessage,
        createNewMessage || false
      );
    }
  } else {
    let loadingIndex = newMsgs.findIndex(
      (msg: AgentMessage) => msg?.id === id && msg.role === MessageRole.SYSTEM && msg.type === 'loading'
    );
    if (loadingIndex === -1) {
      loadingIndex = newMsgs.length;
    }
    newMsgs[loadingIndex] = {
      role: MessageRole.ASSISTANT,
      type: TaskMessageType.TEXT,
      content: '',
      agentId: jsonData.agentId,
      taskId: jsonData.taskId,
      parentTaskId: jsonData.parentTaskId,
      thoughtProcessMessages: type === TaskMessageType.THINK ? [{ ...agentMessage, type }] : [],
      resultProcessMessages: type === TaskMessageType.TEXT ? [{ ...agentMessage, type }] : [],
      id,
    };
  }
  return newMsgs;
};

const filterEmptyContent = (content: string): boolean => {
  return !content.replace('\n\n', '');
};

const updateCurrentAgentMessage = (
  currentAgentStatus: AgentStatusRef,
  jsonData: DeltaEventData,
  id: string,
  type: TaskMessageType
): void => {
  if (currentAgentStatus.agentMessage) {
    currentAgentStatus.agentMessage = {
      ...currentAgentStatus.agentMessage,
      content: (currentAgentStatus.agentMessage.content || '') + jsonData.part,
      taskId: jsonData.taskId,
      parentTaskId: jsonData.parentTaskId,
      chunkType: jsonData.chunkType,
      agentId: jsonData.agentId,
      id,
      flag: MessageChunkChangeFlag.CONTINUE,
    } as AgentMessage;
  } else {
    currentAgentStatus.agentMessage = createAgentMessage(jsonData, id, type, jsonData.part);
  }
};

const performComplexMessageUpdate = (
  messageActions: MessageReducerActions,
  agentSwitchRef: React.MutableRefObject<AgentSwitchMessage | undefined>,
  agentId: string,
  id: string,
  agentMessage: AgentMessage,
  jsonData: DeltaEventData,
  type: TaskMessageType,
  createNewMessage?: boolean
): void => {
  const newAgentMessage = JSON.parse(JSON.stringify(agentMessage));
  messageActions.complexMessageUpdate(agentId, id, (prev) => {
    const msgsMap = JSON.parse(JSON.stringify(prev));
    const msgs = JSON.parse(JSON.stringify(prev?.[agentId]?.messages || []));
    const newMsgs = updateMessages(
      agentSwitchRef,
      msgs,
      newAgentMessage,
      jsonData,
      type,
      id,
      createNewMessage
    );
    msgsMap[agentId] = {
      messages: newMsgs,
    };
    return msgsMap;
  });
};

const processThinkingChunk = (
  currentAgentStatus: AgentStatusRef,
  jsonData: DeltaEventData,
  id: string,
  agentId: string,
  messageActions: MessageReducerActions,
  agentSwitchRef: React.MutableRefObject<AgentSwitchMessage | undefined>
): void => {
  if (shouldCreateNewChunk(currentAgentStatus.agentMessage)) {
    currentAgentStatus.agentMessage = createAgentMessage(jsonData, id, TaskMessageType.THINK, jsonData.part);

    performComplexMessageUpdate(
      messageActions,
      agentSwitchRef,
      agentId,
      id,
      currentAgentStatus.agentMessage,
      jsonData,
      TaskMessageType.THINK,
      true
    );
  } else {
    updateCurrentAgentMessage(currentAgentStatus, jsonData, id, TaskMessageType.THINK);
  }

  if (currentAgentStatus.agentMessage) {
    performComplexMessageUpdate(
      messageActions,
      agentSwitchRef,
      agentId,
      id,
      currentAgentStatus.agentMessage,
      jsonData,
      TaskMessageType.THINK
    );
  }
};

const processModelOutputChunk = (
  currentAgentStatus: AgentStatusRef,
  jsonData: DeltaEventData,
  id: string,
  agentId: string,
  messageActions: MessageReducerActions,
  agentSwitchRef: React.MutableRefObject<AgentSwitchMessage | undefined>
): void => {
  if (shouldCreateNewChunk(currentAgentStatus.agentMessage)) {
    currentAgentStatus.agentMessage = createAgentMessage(jsonData, id, TaskMessageType.TEXT, jsonData.part);

    performComplexMessageUpdate(
      messageActions,
      agentSwitchRef,
      agentId,
      id,
      currentAgentStatus.agentMessage,
      jsonData,
      TaskMessageType.TEXT,
      true
    );
  } else {
    updateCurrentAgentMessage(currentAgentStatus, jsonData, id, TaskMessageType.TEXT);

    if (currentAgentStatus.agentMessage) {
      performComplexMessageUpdate(
        messageActions,
        agentSwitchRef,
        agentId,
        id,
        currentAgentStatus.agentMessage,
        jsonData,
        TaskMessageType.TEXT
      );
    }
  }
};

export const useChatDeltaEvents = (props: UseChatStreamDeltaEventProps): UseChatDeltaEventsReturn => {
  const { scrollToBottom, agentStatusRef, messageActions, agentSwitchRef } = props;

  const debouncedScrollToBottom = useMemo(
    () => debounce(scrollToBottom, 100),
    [scrollToBottom]
  );

  const handleDeltaEvent = useCallback(
    (jsonData: DeltaEventData, id: string, agentId: string) => {
      if (filterEmptyContent(jsonData.part)) return;
      const currentAgentStatus = agentStatusRef.current.find((item) => item.id === id);
      if (!currentAgentStatus) return;

      if (
        currentAgentStatus.agentMessage &&
        currentAgentStatus.agentMessage?.chunkType !== jsonData.chunkType
      ) {
        currentAgentStatus.agentMessage.flag = MessageChunkChangeFlag.NEW_CHUNK;
      }

      if (jsonData.chunkType === ChunkType.THINKING) {
        processThinkingChunk(
          currentAgentStatus,
          jsonData,
          id,
          agentId,
          messageActions,
          agentSwitchRef
        );
      } else if (jsonData.chunkType === ChunkType.MODEL_OUTPUT) {
        processModelOutputChunk(
          currentAgentStatus,
          jsonData,
          id,
          agentId,
          messageActions,
          agentSwitchRef
        );
      }

      debouncedScrollToBottom();
    },
    [messageActions, debouncedScrollToBottom, agentStatusRef, agentSwitchRef]
  );

  return {
    handleDeltaEvent,
  };
};
