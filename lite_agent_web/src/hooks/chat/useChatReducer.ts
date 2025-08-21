import { useReducer, useCallback } from 'react';
import { AgentMessage, AgentMessageMap, SSEEventData } from '@/types/chat';
import { MessageRole, TaskMessageType } from '@/types/Message';

interface ChatState {
  messagesMap: AgentMessageMap;
}

type ChatAction =
  | { type: 'SET_MESSAGES_MAP'; payload: AgentMessageMap }
  | { type: 'UPDATE_AGENT_MESSAGES'; payload: { agentId: string; messages: AgentMessage[] } }
  | { type: 'ADD_MESSAGE'; payload: { agentId: string; message: AgentMessage } }
  | { type: 'UPDATE_MESSAGE'; payload: { agentId: string; messageId: string; updates: Partial<AgentMessage> } }
  | { type: 'CLEAR_MESSAGES'; payload: string }
  | { type: 'ADD_SEPARATOR'; payload: { agentId: string; sessionId?: string } }
  | { type: 'PREPEND_MESSAGES'; payload: { agentId: string; messages: AgentMessage[]; adjustScrollCallback?: (oldScrollHeight: number) => void } }
  | { type: 'HANDLE_END_EVENT'; payload: { agentId: string; messageId: string; ttsModelId?: string } }
  | { type: 'HANDLE_ERROR_EVENT'; payload: { agentId: string; messageId: string; data: SSEEventData } }
  | { type: 'COMPLEX_MESSAGE_UPDATE'; payload: { agentId: string; messageId: string; updater: (prev: AgentMessageMap) => AgentMessageMap } };

const initialState: ChatState = {
  messagesMap: {},
};

function chatReducer(state: ChatState, action: ChatAction): ChatState {
  switch (action.type) {
    case 'SET_MESSAGES_MAP':
      return {
        ...state,
        messagesMap: action.payload,
      };

    case 'UPDATE_AGENT_MESSAGES': {
      return {
        ...state,
        messagesMap: {
          ...state.messagesMap,
          [action.payload.agentId]: {
            messages: action.payload.messages,
          },
        },
      };
    }

    case 'ADD_MESSAGE': {
      const currentMessages = state.messagesMap[action.payload.agentId]?.messages || [];
      return {
        ...state,
        messagesMap: {
          ...state.messagesMap,
          [action.payload.agentId]: {
            messages: [...currentMessages, action.payload.message],
          },
        },
      };
    }

    case 'UPDATE_MESSAGE': {
      const agentMessages = state.messagesMap[action.payload.agentId]?.messages || [];
      const updatedMessages = agentMessages.map(msg =>
        msg.id === action.payload.messageId
          ? { ...msg, ...action.payload.updates }
          : msg
      );
      return {
        ...state,
        messagesMap: {
          ...state.messagesMap,
          [action.payload.agentId]: {
            messages: updatedMessages,
          },
        },
      };
    }

    case 'CLEAR_MESSAGES':
      return {
        ...state,
        messagesMap: {
          ...state.messagesMap,
          [action.payload]: {
            messages: [],
          },
        },
      };

    case 'ADD_SEPARATOR': {
      const existingMessages = state.messagesMap[action.payload.agentId]?.messages || [];
      const separatorMessage = {
        sessionId: action.payload.sessionId || undefined,
        role: MessageRole.SEPARATOR,
      } as unknown as AgentMessage;
      
      return {
        ...state,
        messagesMap: {
          ...state.messagesMap,
          [action.payload.agentId]: {
            messages: [...existingMessages, separatorMessage],
          },
        },
      };
    }

    case 'PREPEND_MESSAGES': {
      const currentMessages = state.messagesMap[action.payload.agentId]?.messages || [];
      const oldScrollHeight = document.querySelector('.chat-scroll-container')?.scrollHeight || 0;
      
      const newState = {
        ...state,
        messagesMap: {
          ...state.messagesMap,
          [action.payload.agentId]: {
            messages: [...action.payload.messages, ...currentMessages],
          },
        },
      };
      
      // 如果有滚动调整回调，在下次渲染后调用
      if (action.payload.adjustScrollCallback) {
        setTimeout(() => action.payload.adjustScrollCallback!(oldScrollHeight), 0);
      }
      
      return newState;
    }

    case 'HANDLE_END_EVENT': {
      const messages = state.messagesMap[action.payload.agentId]?.messages || [];
      const messageIndex = messages.findIndex(
        (msg) => (msg.id === action.payload.messageId && msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.TEXT)
      );
      
      if (messageIndex !== -1) {
        const updatedMessage = {
          ...messages[messageIndex],
          responding: false,
          thoughtProcessMessages: messages[messageIndex].thoughtProcessMessages?.map((message) => 
            message.role === MessageRole.TOOL ? { ...message, responding: false } as unknown as AgentMessage : message
          ),
        } as AgentMessage;

        // 自动播放音频标记
        const allContent = (messages[messageIndex].resultProcessMessages || [])
          .map(item => item?.content)
          .filter(Boolean)
          .join('\n');
          
        if (action.payload.ttsModelId && allContent && allContent.length > 0) {
          updatedMessage.playAudio = true;
        }

        const newMessages = [...messages];
        newMessages[messageIndex] = updatedMessage;

        return {
          ...state,
          messagesMap: {
            ...state.messagesMap,
            [action.payload.agentId]: {
              messages: newMessages,
            },
          },
        };
      }
      return state;
    }

    case 'HANDLE_ERROR_EVENT': {
      const agentMsgs = state.messagesMap[action.payload.agentId]?.messages || [];
      const errorMessageIndex = agentMsgs.findIndex(
        (msg) => msg.id === action.payload.messageId && msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.TEXT
      );
      
      if (errorMessageIndex !== -1) {
        const updatedErrorMessage = {
          ...agentMsgs[errorMessageIndex],
          responding: true,
          resultProcessMessages: [...(agentMsgs[errorMessageIndex].resultProcessMessages || []), action.payload.data as unknown as AgentMessage],
        } as AgentMessage;
        
        const newErrorMessages = [...agentMsgs];
        newErrorMessages[errorMessageIndex] = updatedErrorMessage;

        return {
          ...state,
          messagesMap: {
            ...state.messagesMap,
            [action.payload.agentId]: {
              messages: newErrorMessages,
            },
          },
        };
      } else {
        // 添加新消息
        const loadingIndex = agentMsgs.findIndex(
          (msg) => msg.id === action.payload.messageId && msg.role === MessageRole.SYSTEM && msg.type === 'loading'
        );
        
        const newMessage = {
          role: MessageRole.ASSISTANT,
          type: TaskMessageType.TEXT,
          content: '',
          taskId: action.payload.data.taskId,
          thoughtProcessMessages: [],
          resultProcessMessages: [action.payload.data as unknown as AgentMessage],
          id: action.payload.messageId,
        } as unknown as AgentMessage;

        if (loadingIndex === -1) {
          return {
            ...state,
            messagesMap: {
              ...state.messagesMap,
              [action.payload.agentId]: {
                messages: [...agentMsgs, newMessage],
              },
            },
          };
        } else {
          const newErrorMessages = [...agentMsgs];
          newErrorMessages[loadingIndex] = newMessage;
          return {
            ...state,
            messagesMap: {
              ...state.messagesMap,
              [action.payload.agentId]: {
                messages: newErrorMessages,
              },
            },
          };
        }
      }
    }

    case 'COMPLEX_MESSAGE_UPDATE': {
      // 用于处理复杂的消息更新逻辑，暂时保持兼容性
      return {
        ...state,
        messagesMap: action.payload.updater(state.messagesMap),
      };
    }

    default:
      return state;
  }
}

export const useChatReducer = () => {
  const [state, dispatch] = useReducer(chatReducer, initialState);

  const actions = {
    setMessagesMap: useCallback((messagesMap: AgentMessageMap) => {
      dispatch({ type: 'SET_MESSAGES_MAP', payload: messagesMap });
    }, []),

    updateAgentMessages: useCallback((agentId: string, messages: AgentMessage[]) => {
      dispatch({ type: 'UPDATE_AGENT_MESSAGES', payload: { agentId, messages } });
    }, []),

    addMessage: useCallback((agentId: string, message: AgentMessage) => {
      dispatch({ type: 'ADD_MESSAGE', payload: { agentId, message } });
    }, []),

    updateMessage: useCallback((agentId: string, messageId: string, updates: Partial<AgentMessage>) => {
      dispatch({ type: 'UPDATE_MESSAGE', payload: { agentId, messageId, updates } });
    }, []),

    clearMessages: useCallback((agentId: string) => {
      dispatch({ type: 'CLEAR_MESSAGES', payload: agentId });
    }, []),

    addSeparator: useCallback((agentId: string, sessionId?: string) => {
      dispatch({ type: 'ADD_SEPARATOR', payload: { agentId, sessionId } });
    }, []),

    prependMessages: useCallback((agentId: string, messages: AgentMessage[], adjustScrollCallback?: (oldScrollHeight: number) => void) => {
      dispatch({ type: 'PREPEND_MESSAGES', payload: { agentId, messages, adjustScrollCallback } });
    }, []),

    handleEndEvent: useCallback((agentId: string, messageId: string, ttsModelId?: string) => {
      dispatch({ type: 'HANDLE_END_EVENT', payload: { agentId, messageId, ttsModelId } });
    }, []),

    handleErrorEvent: useCallback((agentId: string, messageId: string, data: SSEEventData) => {
      dispatch({ type: 'HANDLE_ERROR_EVENT', payload: { agentId, messageId, data } });
    }, []),

    complexMessageUpdate: useCallback((agentId: string, messageId: string, updater: (prev: AgentMessageMap) => AgentMessageMap) => {
      dispatch({ type: 'COMPLEX_MESSAGE_UPDATE', payload: { agentId, messageId, updater } });
    }, []),
  };

  return {
    state,
    actions,
  };
};