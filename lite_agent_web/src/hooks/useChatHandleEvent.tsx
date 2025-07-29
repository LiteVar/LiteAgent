import React, { ChangeEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AgentMessage, AgentMessageMap } from '../components/chat/Chat';
import { message } from 'antd';
import { EventSourceMessage, fetchEventSource } from '@microsoft/fetch-event-source';
import {
  AgentDetailVO,
  getV1ChatAgentChatByAgentId,
  OutMessage,
  postV1ChatClearDebugRecord,
  postV1ChatClearSession,
  postV1ChatInitSession,
  postV1ChatInitSessionByAgentId,
  getV1ChatAudioSpeech
} from '@/client';
import { TOOL_RETURN } from '@/constants/message';
import { getAccessToken } from '@/utils/cache';
import ResponseCode from '@/constants/ResponseCode';
import { MessageRole, MessageType, TaskMessageType } from '@/types/Message';
import { getV1DatasetRetrieveHistoryById, DocumentSegment, SegmentVO, postV1ChatAudioSpeech } from '@/client';
import useChatMessageEvent from './useChatMessageEvent';
import useChatStreamDeltaEvent from './useChatStreamDeltaEvent';
import useChatAdjustAssistantMsg from './useChatAdjustAssistantMsg';
import { AgentChatMessageClear } from '../client';
import pLimit from 'p-limit';

const sseQueueLimit = pLimit(3);

interface UseChatHandleEventProps {
  mode: 'prod' | 'dev';
  agentId: string;
  agentInfo: AgentDetailVO;
}

interface UseChatHandleEventExport {
  messagesMap: AgentMessageMap;
  clearList: AgentChatMessageClear[];
  onInputChange: (e: ChangeEvent<HTMLTextAreaElement>) => void;
  onResetSession: () => Promise<void>;
  scrollRef: React.RefObject<HTMLDivElement>;
  thinkScrollRef: React.RefObject<HTMLDivElement>;
  lastThinkMessage: React.RefObject<HTMLDivElement>;
  onRetry: () => Promise<void>;
  showScrollToBottom: boolean;
  scrollToBottom: () => void;
  onShowThinkMessage: (event: React.MouseEvent<HTMLDivElement>, message: AgentMessage) => void;
  onCloseThinkMessage: (event: React.MouseEvent<HTMLDivElement>) => void;
  thinkDetailVisible: boolean;
  thinkMessageIndex: number | undefined;
  value: string;
  onSendMessage: (type: 'text' | 'execute' | 'imageUrl', text?: string) => Promise<void>;
  onSearchKnowledgeResult: (event: React.MouseEvent<HTMLSpanElement>, id: string, query: string) => Promise<void>;
  setKnowledgeResultVisible: (value: boolean) => void;
  knowledgeResultVisible: boolean;
  knowledgeQueryText: string;
  knowledgeSearchResults: SegmentVO[];
  asrLoading: boolean;
  hasMore: boolean;
  setAsrLoading: (value: boolean) => void;
  fetchData: () => Promise<void>;
}

export interface AgentStatusRef {
  id: string | null;
  agentMessage: AgentMessage | null;
  responding: boolean;
}

export default function useChatHandleEvent(props: UseChatHandleEventProps): UseChatHandleEventExport {
  const scrollRef = useRef<HTMLDivElement>(null);
  const thinkScrollRef = useRef<HTMLDivElement>(null);
  const agentStatusRef = useRef<AgentStatusRef[]>([]);
  const sessionRef = useRef<string>('');
  const [messagesMap, setMessagesMap] = useState<AgentMessageMap>({});
  const [clearList, setClearList] = useState<AgentChatMessageClear[]>([]);
  const [selectMessage, setSelectMessage] = useState<AgentMessage | null>(null);
  const [thinkDetailVisible, setThinkDetailVisible] = useState<boolean>(false);
  const [value, setValue] = useState('');
  const scrollTimerRef = useRef();
  const token = getAccessToken();
  const [showScrollToBottom, setShowScrollToBottom] = useState(false);
  const [knowledgeResultVisible, setKnowledgeResultVisible] = useState(false);
  const [knowledgeSearchResults, setKnowledgeSearchResults] = useState<SegmentVO[]>([]);
  const [knowledgeQueryText, setKnowledgeQueryText] = useState<string>('');
  const [asrLoading, setAsrLoading] = useState(false);
  const [sessionLoading, setSessionLoading] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const fetchDataLoading = useRef(false);
  const errorRetryCountRef = useRef(0);
  const loadMoreSessionId = useRef('');
  const hasPlayedAudioRef = useRef(false);
  const sendMessageTipEnableRef = useRef(true);
  const lastThinkMessage = useRef<HTMLDivElement>(null);
  const agentSwitchRef = useRef();

  const scrollToBottom = useCallback(() => {
    if (scrollTimerRef.current) {
      clearTimeout(scrollTimerRef.current);
    }
    //@ts-ignore
    scrollTimerRef.current = setTimeout(() => {
      if (scrollRef.current) {
        scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
      }
      if (thinkScrollRef.current) {
        thinkScrollRef.current.scrollTop = thinkScrollRef.current.scrollHeight;
      }
    }, 100);
  }, []);

  const { handleMessageEvent } = useChatMessageEvent({ scrollToBottom, setMessagesMap, agentSwitchRef });
  const { handleDeltaEvent } = useChatStreamDeltaEvent({ scrollToBottom, agentStatusRef, setMessagesMap, agentSwitchRef });
  const { adjustAssistantMsg } = useChatAdjustAssistantMsg();

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.addEventListener('scroll', handleScroll);
    }
    return () => {
      if (scrollRef.current) {
        scrollRef.current.removeEventListener('scroll', handleScroll);
      }
      agentStatusRef.current = [];
      setMessagesMap({});
    };
  }, []);

  useEffect(() => {
    sessionRef.current = '';
  }, [props.agentInfo]);

  const fetchData = useCallback(async (expandThink = false) => {
    const oldScrollHeight = scrollRef.current.scrollHeight;
    if (fetchDataLoading.current) return;
    console.log('fetchData----');
    fetchDataLoading.current = true;
    const sessionId = loadMoreSessionId.current;
    const res = await getV1ChatAgentChatByAgentId({
      path: {
        agentId: props.agentId,
      },
      query: {
        debugFlag: props.mode === 'prod' ? 0 : 1,
        pageSize: 10,
        sessionId: sessionId,
      },
    });
    setHasMore(res?.data?.data?.messageList?.length >= 10);
    setTimeout(() => {
      fetchDataLoading.current = false;
    }, 100);

    if (res?.data?.code === ResponseCode.S_OK) {
      let msgs: OutMessage[] = [];
      setClearList(res?.data?.data?.clearList || []);
      const resData = res?.data?.data?.messageList?.reverse();
      let taskMessages = [];
      resData.forEach((item, index) => {
        if (index == 0) {
          loadMoreSessionId.current = item.sessionId;
        }
        taskMessages = taskMessages.concat(item.taskMessage);
      });

      taskMessages.forEach((item, index) => {
        //session分割线处理
        if (index > 0 && item.sessionId != taskMessages[index - 1].sessionId) {
          msgs.push({
            sessionId: item.sessionId,
            role: MessageRole.SEPARATOR,
          });
        }
        //message预处理
        item.message = adjustAssistantMsg(item.message ?? []);
        msgs = msgs.concat(item.message || []);
      })

      msgs = msgs.filter(
        (message) =>
          ((!!message.content && message.type !== TOOL_RETURN) || message.role === MessageRole.SEPARATOR) &&
          !(message.role === MessageRole.AGENT && message.type === TaskMessageType.BROADCAST) &&
          !(message.role === MessageRole.AGENT && message.type === TaskMessageType.AGENT_SWITCH) &&
          message.role != MessageRole.SUBAGENT &&
          message.role != MessageRole.REFLECTION
      );

      if (expandThink) {
        //如果最后一条消息包含思考过程自动展开
        const newMsgs = JSON.parse(JSON.stringify(msgs));
        setTimeout(() => {
          //@ts-ignore
          if (newMsgs[newMsgs.length - 1]?.thoughtProcessMessages?.length > 0) {
            if (lastThinkMessage.current && scrollRef.current.scrollHeight > scrollRef.current.clientHeight) {
              scrollRef.current.scrollTop = lastThinkMessage.current.offsetTop - scrollRef.current.offsetTop;
            } else {
              scrollToBottom();
            }
            setSelectMessage(msgs[newMsgs.length - 1]);
            setThinkDetailVisible(true);
          } else {
            scrollToBottom();
          }
        }, 100);
      }

      // console.log('msgs---', msgs);
      if (!sessionId) {
        setMessagesMap({ [props.agentId]: { messages: msgs } });
      } else {
        setMessagesMap(prev => ({ ...prev, [props.agentId]: { messages: [...msgs, (prev[props.agentId]?.messages || [])] } }));
        setTimeout(() => {
          const newScrollHeight = scrollRef.current.scrollHeight;
          scrollRef.current.scrollTop = newScrollHeight - oldScrollHeight;
        }, 100)
      }

      setTimeout(() => {
        if (msgs.length > 0 && !sessionRef.current && msgs[msgs.length - 1]?.role != MessageRole.SEPARATOR) {
          msgs.push({
            sessionId: null,
            role: MessageRole.SEPARATOR,
          });
        }
      }, 100)

    }
  }, [props.agentId, props.mode]);

  useEffect(() => {
    if (!token || !props.mode || !props.agentId) return;

    const startUp = async () => {
      loadMoreSessionId.current = '';
      await fetchData(true);
    };

    startUp();
  }, [token, props.mode, props.agentId]);

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
    const messages = messagesMap?.[props.agentId]?.messages || [];
    const index = messages.findIndex((msg) => msg.taskId === selectMessage?.taskId && msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.TEXT);
    if (index > -1) {
      return index;
    } else {
      return undefined;
    }
  }, [messagesMap, selectMessage])

  const onSearchKnowledgeResult = useCallback(async (event: React.MouseEvent<HTMLSpanElement>, id: string, query: string) => {
    event.stopPropagation();
    const noResult: DocumentSegment[] = [{ id: '000', content: '无没有找到相关搜索结果' }];
    message.info('正在加载检索记录...');
    const res = await getV1DatasetRetrieveHistoryById({
      path: {
        id: id!,
      },
    });

    message.destroy();
    if (res.data?.code === ResponseCode.S_OK) {
      setKnowledgeQueryText(query);
      const result = res.data.data;
      if (result && result.length > 0) {
        setKnowledgeSearchResults(result);
      } else {
        setKnowledgeSearchResults(noResult);
      }
      setKnowledgeResultVisible(true);
    } else {
      message.error(res.data?.message || '获取检索记录失败');
    }
  }, []);

  const handleScroll = () => {
    if (scrollRef.current) {
      const { scrollTop, scrollHeight, clientHeight } = scrollRef.current;
      setShowScrollToBottom(scrollTop + clientHeight < scrollHeight - 100);
    }
  };

  const onInputChange = useCallback((e: ChangeEvent<HTMLTextAreaElement>) => {
    setValue(e.target.value);
  }, []);

  const onResetSession = useCallback(async () => {
    if (sessionRef.current) {
      await postV1ChatClearSession({
        query: {
          sessionId: sessionRef.current,
        },
      });
      setMessagesMap((prev) => {
        let msgMap = JSON.parse(JSON.stringify(prev));
        const msgs = JSON.parse(JSON.stringify(prev?.[props.agentId]?.messages || []));
        if (msgs.length > 0 && msgs[msgs.length - 1]?.role != MessageRole.SEPARATOR) {
          msgs.push({
            sessionId: null,
            role: MessageRole.SEPARATOR,
          });
        }
        msgMap[props.agentId] = { messages: msgs };
        return msgMap;
      })
      scrollToBottom();
    }
    if (props.mode === 'dev') {
      await postV1ChatClearDebugRecord({
        query: {
          agentId: props.agentId,
          debugFlag: 1,
        },
      });
      setMessagesMap((prev) => {
        let msgMap = JSON.parse(JSON.stringify(prev));
        msgMap[props.agentId] = { messages: [] };
        return msgMap;
      })
    }
    sessionRef.current = '';
    message.success(props.mode === 'prod' ? '上下文联系已清除' : '记录已清空');
    //刷新页面
    loadMoreSessionId.current = '';
    await fetchData(true);
  }, [props.mode, props.agentId, scrollToBottom, token]);

  const initializeSession = useCallback(async () => {
    setSessionLoading(true);
    if (props.mode === 'prod') {
      const res = await postV1ChatInitSessionByAgentId({
        path: {
          agentId: props.agentId,
        },
      });

      if (res?.data?.code === ResponseCode.S_OK) {
        sessionRef.current = res?.data?.data || '';
        setSessionLoading(false);
      } else if (res?.data?.code === ResponseCode.AGENT_NOT_FOUND) {
        message.error(res.data.message);
        setSessionLoading(false);
        return false;
      } else {
        message.error(res?.data?.message || 'ai模型初始化失败，请正确配置模型');
        setSessionLoading(false);
        return false;
      }
    } else {
      const res = await postV1ChatInitSession({
        body: {
          agentId: props.agentInfo?.agent?.id!,
          modelId: props.agentInfo?.agent?.llmModelId!,
          prompt: props.agentInfo?.agent?.prompt,
          toolIds: props.agentInfo?.toolList?.map((t) => t.id!) || [],
          temperature: props.agentInfo?.agent?.temperature,
          topP: props.agentInfo?.agent?.topP,
          maxTokens: props.agentInfo?.agent?.maxTokens,
          subAgentIds: props.agentInfo?.agent?.subAgentIds || [],
          mode: props.agentInfo?.agent?.mode,
          type: props.agentInfo?.agent?.type,
          functionList: props.agentInfo?.agent?.functionList || [],
          datasetIds: props.agentInfo?.datasetList?.map((d) => d.id!) || [],
          sequence: props.agentInfo?.agent?.sequence || [],
          auto: props.agentInfo?.agent?.auto,
        },
      });
      if (res?.data?.code === ResponseCode.S_OK) {
        sessionRef.current = res?.data?.data || '';
        setSessionLoading(false);
      } else if (res?.data?.code === ResponseCode.AGENT_NOT_FOUND) {
        message.error(res.data.message);
        setSessionLoading(false);
        return false;
      } else {
        console.error('AI 模型初始化失败：', res?.data?.message);
        message.error(res?.data?.message || 'ai模型初始化失败，请正确配置模型');
        setSessionLoading(false);
        return false;
      }
    }
    return true;
  }, [props.mode, props.agentId, props.agentInfo]);

  const playAudioFromText = useCallback(
    async (text: string) => {
      if (!text) return;
      if (hasPlayedAudioRef.current) return; // 已经执行过，直接返回

      hasPlayedAudioRef.current = true;

      try {
        const modelId = props.agentInfo?.agent?.ttsModelId || '';

        const res = await getV1ChatAudioSpeech({
          query: {
            modelId,
            content: text,
            stream: true
          },
        });

        console.log('res', res);

        if (res.data && res.data instanceof Blob) {
          const blob = res.data;
          const url = URL.createObjectURL(blob);
          const audio = new Audio(url);
          audio.play();
        } else {
          message.error('音频播放失败');
        }
      } catch (e) {
        message.error('音频播放异常');
      }
    }, [props.agentInfo]
  );

  const handleEndEvent = useCallback(
    (id: string) => {
      agentStatusRef.current
        .filter((item) => item.id === id)
        .forEach((item) => {
          item.responding = false;
        });
      setMessagesMap((prev) => {
        let msgMap = JSON.parse(JSON.stringify(prev));
        const newMsgs = JSON.parse(JSON.stringify(prev?.[props.agentId]?.messages || []));
        const index = newMsgs.findIndex(
          (msg) => (msg.id === id && msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.TEXT)
        );

        if (index != -1) {
          newMsgs[index].responding = false;
          newMsgs[index].thoughtProcessMessages?.map((message) => {
            if (message.role === MessageRole.TOOL) {
              message.responding = false;
            }
          });

          // 新增：AI回复完成后自动播报（一次性读完所有内容）
          const allContent = (newMsgs[index].resultProcessMessages || [])
            .map(item => item?.content)
            .filter(Boolean)
            .join('\n');
          if (props.agentInfo?.agent?.ttsModelId && allContent && allContent.length > 0) {
            // playAudioFromText(allContent);
            newMsgs[index].playAudio = true; // 标记为需要播放音频
          }
        }
        msgMap[props.agentId] = { messages: newMsgs };
        agentSwitchRef.current = undefined;
        return msgMap;
      })
      scrollToBottom();
    },
    [scrollToBottom, props.agentInfo?.agent?.ttsModelId, props.agentId]
  );

  const handleErrorEvent = useCallback(
    (data: any, id: string) => {
      setMessagesMap((prev) => {
        let msgMap = JSON.parse(JSON.stringify(prev));
        const newMsgs = JSON.parse(JSON.stringify(prev?.[props.agentId]?.messages || []));
        const index = newMsgs.findIndex(
          (msg) => msg.taskId === data.taskId && msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.TEXT
        );
        if (index != -1) {
          newMsgs[index].responding = true;
          newMsgs[index].resultProcessMessages.push(data);
        } else {
          // 添加新消息
          let loadingIndex = newMsgs.findIndex(
            (msg) => msg.taskId === data.taskId && msg.role === MessageRole.SYSTEM && msg.type === 'loading');
          if (loadingIndex === -1) {
            loadingIndex = newMsgs.length;
          }
          newMsgs[loadingIndex] = {
            role: MessageRole.ASSISTANT,
            type: 'text',
            content: '',
            taskId: data.taskId,
            thoughtProcessMessages: [],
            resultProcessMessages: [data],
            id: id,
          }!;
        }
        msgMap[props.agentId] = { messages: newMsgs };
        return msgMap;
      })
      handleEndEvent(id);
    },
    [handleEndEvent, props.agentId]
  );

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

  const enqueueSSERequest = (agentId: string, type: 'text' | 'execute' | 'imageUrl', text?: string) => {
    const id = Date.now().toString();
    //@ts-ignore
    if (type != 'execute') {
      setValue('');
    }
    agentStatusRef.current.push({
      id,
      agentMessage: null,
      responding: true,
    });
    hasPlayedAudioRef.current = false;
    sendMessageTipEnableRef.current = true;
    const controller = new AbortController();
    const inputMes = value || text;
    return sseQueueLimit(() => {
      return fetchEventSource(`/v1/chat/stream/${sessionRef.current}`, {
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
                let responseData = JSON.parse(data);
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
                let responseData = JSON.parse(data);
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
  }

  const onSendMessage = useCallback(async (type: 'text' | 'execute' | 'imageUrl', text?: string) => {
    if (!value.trim() && !text && type != 'execute') {
      message.info('内容不能为空');
      return;
    }

    if (sessionLoading) {
      message.info('服务器响应中，请稍后再试');
      return;
    };
    if (!sessionRef.current) {
      const sessionInitialized = await initializeSession();
      if (!sessionInitialized) return;
    }
    enqueueSSERequest(props.agentId, type, text)
  }, [value, token, onmessage, handleEndEvent, initializeSession, errorRetryCountRef.current, sessionLoading, props.agentId]);

  const onRetry = useCallback(async () => {
    console.log('retry');
  }, []);

  return {
    messagesMap,
    clearList,
    onInputChange,
    onResetSession,
    scrollRef,
    thinkScrollRef,
    onRetry,
    showScrollToBottom,
    scrollToBottom,
    value,
    onSendMessage,
    onSearchKnowledgeResult,
    knowledgeResultVisible,
    knowledgeSearchResults,
    setKnowledgeResultVisible,
    knowledgeQueryText,
    onShowThinkMessage,
    onCloseThinkMessage,
    thinkDetailVisible,
    thinkMessageIndex,
    asrLoading,
    setAsrLoading,
    fetchData,
    hasMore,
    lastThinkMessage
  };
}
