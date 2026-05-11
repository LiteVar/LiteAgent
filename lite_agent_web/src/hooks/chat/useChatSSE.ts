import { useRef, useCallback } from 'react';
import { message } from 'antd';
import { EventSourceMessage, fetchEventSource } from '@microsoft/fetch-event-source';
import { getAccessToken } from '@/utils/cache';
import { MessageType } from '@/types/Message';
import { AgentStatusRef, SSEEventData, DeltaEventData } from '@/types/chat';
import pLimit from 'p-limit';

const sseQueueLimit = pLimit(3);

/**
 * 聊天消息项类型
 */
export interface ChatMessageItem {
  type: 'text' | 'imageUrl' | 'videoUrl' | 'execute';
  message: string; // text 时为文本内容，imageUrl/videoUrl 时为 fileId 或 url，execute 时为 planId
}

interface UseChatSSEProps {
  agentId: string;
  getCurrentSession: () => string;
  initializeSession: () => Promise<boolean>;
  handleMessageEvent: (jsonData: SSEEventData, id: string, agentId: string) => void;
  handleDeltaEvent: (jsonData: DeltaEventData, id: string, agentId: string) => void;
  handleErrorEvent: (data: SSEEventData, id: string) => void;
  handleEndEvent: (id: string) => void;
  resetAudioFlag: () => void;
  clearInput: () => void;
  agentStatusRef: React.MutableRefObject<AgentStatusRef[]>;
}

export const useChatSSE = ({
  agentId,
  getCurrentSession,
  initializeSession,
  handleMessageEvent,
  handleDeltaEvent,
  handleErrorEvent,
  handleEndEvent,
  resetAudioFlag,
  clearInput,
  agentStatusRef,
}: UseChatSSEProps) => {
  const errorRetryCountRef = useRef(0);
  const sendMessageTipEnableRef = useRef(true);
  const activeControllersRef = useRef<AbortController[]>([]);
  const token = getAccessToken();

  const onmessage = useCallback(
    (event: EventSourceMessage, id: string, agentId: string) => {
      const currentAgentStatus = agentStatusRef.current.find((item) => item.id === id);
      if (!!currentAgentStatus && !!currentAgentStatus.agentMessage && event.event != MessageType.DELTA) {
        //字符串流设置结束标识
        currentAgentStatus.agentMessage.flag = 1;
      }

      switch (event.event) {
        case MessageType.MESSAGE:
          handleMessageEvent(JSON.parse(event.data), id, agentId);
          break;
        case MessageType.DELTA:
          handleDeltaEvent(JSON.parse(event.data), id, agentId);
          break;
        case MessageType.ERROR:
          handleErrorEvent(JSON.parse(event.data), id);
          break;
        case MessageType.END:
          handleEndEvent(id);
          break;
        default:
          handleMessageEvent(JSON.parse(event.data), id, agentId);
          break;
      }
    },
    [handleMessageEvent, handleDeltaEvent, handleErrorEvent, handleEndEvent, agentStatusRef]
  );

  const enqueueSSERequest = useCallback((agentId: string, messages: ChatMessageItem[]) => {
    const id = Date.now().toString();
    const sessionId = getCurrentSession();
    
    // 如果消息中包含非 execute 类型，清空输入
    const hasNonExecuteMessage = messages.some(msg => msg.type !== 'execute');
    if (hasNonExecuteMessage) {
      clearInput();
    }
    
    agentStatusRef.current.push({
      id,
      agentMessage: null,
      responding: true,
    });
    
    resetAudioFlag();
    sendMessageTipEnableRef.current = true;
    const controller = new AbortController();
    activeControllersRef.current.push(controller);

    const removeController = () => {
      activeControllersRef.current = activeControllersRef.current.filter(c => c !== controller);
    };
    
    return sseQueueLimit(() => {
      return fetchEventSource(`/v1/chat/stream/${sessionId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Connection: 'keep-alive',
          Authorization: 'Bearer ' + token,
        },
        openWhenHidden: true,
        body: JSON.stringify(messages),
        signal: controller.signal,
        onmessage: (e) => onmessage(e, id, agentId),
        onopen: async (response) => {
          if (response.status === 500) {
            response.text()?.then(async (data) => {
              if (data && typeof data === 'string') {
                sendMessageTipEnableRef.current = false;
                const responseData = JSON.parse(data);
                if (responseData?.code === 30002) {
                  await initializeSession();
                  message.error('会话已过期，请重新发送');
                } else {
                  message.error(responseData?.message || responseData?.data || '消息发送失败');
                }
              } else {
                message.error('消息发送失败');
              }
            });
          } else if (response.status !== 200) {
            response.text()?.then(async (data) => {
              if (data && typeof data === 'string') {
                sendMessageTipEnableRef.current = false;
                const responseData = JSON.parse(data);
                message.error(responseData?.message || responseData?.data || '消息发送失败');
              } else {
                message.error('消息发送失败');
              }
            });
          }
        },
        onerror(err) {
          handleEndEvent(id);
          removeController();
          console.error('Error:', err);
          errorRetryCountRef.current = errorRetryCountRef.current + 1;
          const hasExecuteMessage = messages.some(msg => msg.type === 'execute');
          if (errorRetryCountRef.current > 1 || hasExecuteMessage) {
            setTimeout(() => {
              if (sendMessageTipEnableRef.current) {
                message.error('消息发送失败');
              }
            }, 200);
            errorRetryCountRef.current = 0;
            throw err;
          }
        },
        onclose: () => { handleEndEvent(id); removeController(); },
      });
    })
  }, [getCurrentSession, clearInput, resetAudioFlag, token, onmessage, handleEndEvent, initializeSession, agentStatusRef]);

  const sendMessage = useCallback(async (messages: ChatMessageItem[]) => {
    const sessionId = getCurrentSession();
    
    if (!sessionId) {
      const sessionInitialized = await initializeSession();
      if (!sessionInitialized) return;
    }
    
    // 验证消息数组不为空
    if (!messages || messages.length === 0) {
      return;
    }
    
    enqueueSSERequest(agentId, messages);
  }, [agentId, getCurrentSession, initializeSession, enqueueSSERequest]);

  const abortAllSSE = useCallback(() => {
    activeControllersRef.current.forEach(c => c.abort());
    activeControllersRef.current = [];
  }, []);

  return {
    sendMessage,
    enqueueSSERequest,
    abortAllSSE,
  };
};