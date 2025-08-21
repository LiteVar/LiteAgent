import { useRef, useCallback } from 'react';
import { message } from 'antd';
import { EventSourceMessage, fetchEventSource } from '@microsoft/fetch-event-source';
import { getAccessToken } from '@/utils/cache';
import { MessageType } from '@/types/Message';
import { AgentStatusRef, SSEEventData, DeltaEventData } from '@/types/chat';
import pLimit from 'p-limit';

const sseQueueLimit = pLimit(3);

interface UseChatSSEProps {
  agentId: string;
  getCurrentSession: () => string;
  initializeSession: () => Promise<boolean>;
  handleMessageEvent: (jsonData: SSEEventData, id: string, agentId: string) => void;
  handleDeltaEvent: (jsonData: DeltaEventData, id: string, agentId: string) => void;
  handleErrorEvent: (data: SSEEventData, id: string) => void;
  handleEndEvent: (id: string) => void;
  resetAudioFlag: () => void;
  inputValue: string;
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
  inputValue,
  clearInput,
  agentStatusRef,
}: UseChatSSEProps) => {
  const errorRetryCountRef = useRef(0);
  const sendMessageTipEnableRef = useRef(true);
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
    [handleMessageEvent, handleDeltaEvent, handleErrorEvent, handleEndEvent]
  );

  const enqueueSSERequest = useCallback((agentId: string, type: 'text' | 'execute' | 'imageUrl', text?: string) => {
    const id = Date.now().toString();
    const sessionId = getCurrentSession();
    
    if (type !== 'execute') {
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
    const inputMes = inputValue || text;
    
    return sseQueueLimit(() => {
      return fetchEventSource(`/v1/chat/stream/${sessionId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Connection: 'keep-alive',
          Authorization: 'Bearer ' + token,
        },
        openWhenHidden: true,
        body: JSON.stringify([
          {
            type: type,
            message: type === 'execute' ? text : inputMes,
          },
        ]),
        signal: controller.signal,
        onmessage: (e) => onmessage(e, id, agentId),
        onopen: async (response) => {
          console.log('onopen', response);
          if (response.status === 500) {
            console.log('request fail---500');
            response.text()?.then(async (data) => {
              console.log('data----', data);
              if (data && typeof data === 'string') {
                sendMessageTipEnableRef.current = false;
                const responseData = JSON.parse(data);
                //判断是否session过期
                if (responseData?.code === 30002) {
                  await initializeSession();
                  message.error('会话已过期，请重新发送');
                } else {
                  message.error(responseData?.message || responseData?.data || '消息发送失败');
                }
                console.log('response data', responseData);
              } else {
                message.error('消息发送失败');
              }
            });
          } else if (response.status !== 200) {
            console.log('request fail---', response.status);
            response.text()?.then(async (data) => {
              console.log('data----', data);
              if (data && typeof data === 'string') {
                sendMessageTipEnableRef.current = false;
                const responseData = JSON.parse(data);
                console.log('response data', responseData);
                message.error(responseData?.message || responseData?.data || '消息发送失败');
              } else {
                message.error('消息发送失败');
              }
            });
          }
        },
        onerror(err) {
          handleEndEvent(id)
          console.error('Error:', err);
          errorRetryCountRef.current = errorRetryCountRef.current + 1;
          if (errorRetryCountRef.current > 1 || type === 'execute') {
            setTimeout(() => {
              if (sendMessageTipEnableRef.current) {
                message.error('消息发送失败');
              }
            }, 200);
            errorRetryCountRef.current = 0;
            throw err;
          }
        },
        onclose: () => handleEndEvent(id),
      });
    })
  }, [getCurrentSession, clearInput, resetAudioFlag, inputValue, token, onmessage, handleEndEvent, initializeSession]);

  const sendMessage = useCallback(async (type: 'text' | 'execute' | 'imageUrl', text?: string) => {
    const sessionId = getCurrentSession();
    
    if (!sessionId) {
      const sessionInitialized = await initializeSession();
      if (!sessionInitialized) return;
    }
    
    enqueueSSERequest(agentId, type, text);
  }, [agentId, getCurrentSession, initializeSession, enqueueSSERequest]);

  return {
    sendMessage,
    enqueueSSERequest,
  };
};