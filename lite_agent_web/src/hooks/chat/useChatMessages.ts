import { useState, useRef, useCallback, useMemo, useEffect } from 'react';
import { MessageRole, TaskMessageType } from '@/types/Message';
import { getV1ChatAgentChatByAgentId, AgentChatMessageClear, AgentDetailVO, TaskMessage } from '@/client';
import ResponseCode from '@/constants/ResponseCode';
import { AgentMessage, SSEEventData } from '@/types/chat';
import { TOOL_RETURN } from '@/constants/message';
import { useChatMessageFormatter } from './useChatMessageFormatter';
import { useChatReducer } from './useChatReducer';

// 常量配置
const CONFIG = {
  PAGE_SIZE: 10,
  LOADING_DELAY: 100,
  SCROLL_DELAY: 100,
} as const;

interface TaskMessageWithSession extends TaskMessage {
  sessionId?: string;
}

interface ChatDataItem {
  sessionId?: string;
  taskMessage?: TaskMessage[];
}

interface FetchDataResponse {
  messageList?: ChatDataItem[];
  clearList?: AgentChatMessageClear[];
}
interface UseChatMessagesProps {
  agentId: string;
  mode: 'prod' | 'dev';
  agentInfo?: AgentDetailVO;
  scrollToBottom: () => void;
  scrollToThinkMessage: (expandThink?: boolean) => void;
  adjustScrollAfterLoadMore: (oldScrollHeight: number) => void;
}

export const useChatMessages = ({
  agentId,
  mode,
  agentInfo,
  scrollToBottom,
  scrollToThinkMessage,
  adjustScrollAfterLoadMore,
}: UseChatMessagesProps) => {
  const { state, actions } = useChatReducer();
  const messagesMap = state.messagesMap;
  const [clearList, setClearList] = useState<AgentChatMessageClear[]>([]);
  const [selectMessage, setSelectMessage] = useState<AgentMessage | null>(null);
  const [thinkDetailVisible, setThinkDetailVisible] = useState<boolean>(false);
  const [hasMore, setHasMore] = useState(false);
  const fetchDataLoading = useRef(false);
  const loadMoreSessionId = useRef('');
  const { adjustAssistantMsg } = useChatMessageFormatter();

  // API 调用函数
  const fetchChatData = useCallback(async () => {
    const sessionId = loadMoreSessionId.current;
    return await getV1ChatAgentChatByAgentId({
      path: { agentId },
      query: {
        debugFlag: mode === 'prod' ? 0 : 1,
        pageSize: CONFIG.PAGE_SIZE,
        sessionId,
      },
    });
  }, [agentId, mode]);

  // 处理任务消息
  const processTaskMessages = useCallback((resData: ChatDataItem[]) => {
    let taskMessages: TaskMessageWithSession[] = [];

    resData?.forEach((item, index) => {
      if (index === 0) {
        loadMoreSessionId.current = item.sessionId || '';
      }
      item.taskMessage?.forEach((taskMessage: TaskMessageWithSession) => {
        taskMessage.sessionId = item.sessionId;
      });
      taskMessages = taskMessages.concat(item.taskMessage || []);
    });

    return taskMessages;
  }, []);

  // 转换任务消息为聊天消息
  const convertTaskMessagesToChatMessages = useCallback(
    (taskMessages: TaskMessageWithSession[]) => {
      let msgs: AgentMessage[] = [];

      taskMessages.forEach((item, index) => {
        // session分割线处理
        if (index > 0 && item?.sessionId !== taskMessages[index - 1]?.sessionId) {
          msgs.push({
            sessionId: item.sessionId,
            role: MessageRole.SEPARATOR,
          } as unknown as AgentMessage);
        }
        // message预处理
        item.message = adjustAssistantMsg(item.message ?? []);
        msgs = msgs.concat(item.message || []);
      });

      return msgs;
    },
    [adjustAssistantMsg]
  );

  // 过滤消息
  const filterMessages = useCallback((messages: AgentMessage[]) => {
    return messages.filter(
      (message) =>
        ((!!message.content && message.type !== TOOL_RETURN) || message.role === MessageRole.SEPARATOR) &&
        !(message.role === MessageRole.AGENT && message.type === TaskMessageType.BROADCAST) &&
        !(message.role === MessageRole.AGENT && message.type === TaskMessageType.AGENT_SWITCH) &&
        message.role !== MessageRole.SUBAGENT &&
        message.role !== MessageRole.REFLECTION
    );
  }, []);

  // 处理思考过程展开
  const handleThinkExpansion = useCallback(
    (messages: AgentMessage[], expandThink: boolean) => {
      if (!expandThink) return;

      const lastMessage = messages[messages.length - 1];
      setTimeout(() => {
        // 改善类型安全性 - 检查是否有思考过程消息
        const messageWithThought = lastMessage as AgentMessage & { thoughtProcessMessages?: unknown[] };
        const hasThoughtProcess =
          messageWithThought &&
          Array.isArray(messageWithThought.thoughtProcessMessages) &&
          messageWithThought.thoughtProcessMessages.length > 0;

        if (hasThoughtProcess) {
          scrollToThinkMessage(true);
          setSelectMessage(lastMessage);
          setThinkDetailVisible(true);
        } else {
          scrollToBottom();
        }
      }, CONFIG.SCROLL_DELAY);
    },
    [scrollToThinkMessage, scrollToBottom]
  );

  // 更新消息状态
  const updateMessagesState = useCallback(
    (messages: AgentMessage[], sessionId: string) => {
      if (!sessionId) {
        actions.setMessagesMap({ [agentId]: { messages } });
      } else {
        actions.prependMessages(agentId, messages, adjustScrollAfterLoadMore);
      }

      // 添加分割线
      setTimeout(() => {
        if (messages.length > 0 && messages[messages.length - 1]?.role !== MessageRole.SEPARATOR) {
          actions.addSeparator(agentId);
        }
      }, CONFIG.SCROLL_DELAY);
    },
    [agentId, actions, adjustScrollAfterLoadMore]
  );

  // 主要的数据获取函数
  const fetchData = useCallback(
    async (expandThink = false) => {
      if (fetchDataLoading.current) return;
      console.log('fetchData----');

      fetchDataLoading.current = true;
      const sessionId = loadMoreSessionId.current;

      try {
        const res = await fetchChatData();
        const data: FetchDataResponse = res?.data?.data || {};

        setHasMore((data.messageList?.length || 0) >= CONFIG.PAGE_SIZE);

        setTimeout(() => {
          fetchDataLoading.current = false;
        }, CONFIG.LOADING_DELAY);

        if (res?.data?.code === ResponseCode.S_OK) {
          setClearList(data.clearList || []);
          const resData = data.messageList?.reverse() || [];

          // 处理流程：任务消息 -> 聊天消息 -> 过滤 -> 状态更新
          const taskMessages = processTaskMessages(resData);
          const chatMessages = convertTaskMessagesToChatMessages(taskMessages);
          const filteredMessages = filterMessages(chatMessages);

          handleThinkExpansion(filteredMessages, expandThink);
          updateMessagesState(filteredMessages, sessionId);
        }
      } catch (error) {
        console.error('fetchData error:', error);
        setTimeout(() => {
          fetchDataLoading.current = false;
        }, CONFIG.LOADING_DELAY);
      }
    },
    [
      fetchChatData,
      processTaskMessages,
      convertTaskMessagesToChatMessages,
      filterMessages,
      handleThinkExpansion,
      updateMessagesState,
    ]
  );

  const onResetSession = useCallback(
    async (clearSession: () => Promise<void>) => {
      await clearSession();
      if (mode === 'prod') {
        const currentMessages = messagesMap[agentId]?.messages || [];
        if (
          currentMessages.length > 0 &&
          currentMessages[currentMessages.length - 1]?.role !== MessageRole.SEPARATOR
        ) {
          actions.addSeparator(agentId);
        }
      } else {
        actions.clearMessages(agentId);
      }
      scrollToBottom();
      loadMoreSessionId.current = '';
      await fetchData(true);
    },
    [mode, agentId, scrollToBottom, fetchData, messagesMap, actions]
  );

  const onShowThinkMessage = useCallback((event: React.MouseEvent<HTMLDivElement>, message: AgentMessage) => {
    event.stopPropagation();
    setSelectMessage(message);
    setThinkDetailVisible(true);
  }, []);

  const onCloseThinkMessage = useCallback((event: React.MouseEvent<HTMLDivElement>) => {
    event.stopPropagation();
    setSelectMessage(null);
    setThinkDetailVisible(false);
  }, []);

  const thinkMessageIndex = useMemo(() => {
    if (!selectMessage) return undefined;
    const messages = messagesMap?.[agentId]?.messages || [];
    const index = messages.findIndex(
      (msg) =>
        msg.id === selectMessage?.id &&
        msg.taskId === selectMessage?.taskId &&
        msg.role != MessageRole.USER
    );
    if (index > -1) {
      return index;
    } else {
      return undefined;
    }
  }, [messagesMap, selectMessage, agentId]);

  const handleEndEvent = useCallback(
    (id: string) => {
      actions.handleEndEvent(agentId, id, agentInfo?.agent?.ttsModelId);
      scrollToBottom();
    },
    [scrollToBottom, agentInfo?.agent?.ttsModelId, agentId, actions]
  );

  const handleErrorEvent = useCallback(
    (data: SSEEventData, id: string) => {
      actions.handleErrorEvent(agentId, id, data);
      handleEndEvent(id);
    },
    [handleEndEvent, agentId, actions]
  );

  // 初始化加载数据
  useEffect(() => {
    if (agentId && mode) {
      const startUp = async () => {
        loadMoreSessionId.current = '';
        await fetchData(true);
      };
      startUp();
    }
  }, [agentId, mode]);

  return {
    messagesMap,
    clearList,
    selectMessage,
    thinkDetailVisible,
    thinkMessageIndex,
    hasMore,
    fetchData,
    onShowThinkMessage,
    onCloseThinkMessage,
    onResetSession,
    handleEndEvent,
    handleErrorEvent,
    actions,
  };
};
