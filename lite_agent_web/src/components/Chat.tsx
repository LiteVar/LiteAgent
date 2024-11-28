import React, {ChangeEvent, useCallback, useEffect, useRef, useState} from 'react';
import {Button, Drawer, Image, message} from 'antd';
import {EllipsisOutlined} from '@ant-design/icons';
import sendSvg from "@/assets/dashboard/send.svg";
import logo from "@/assets/login/logo.png";
import chatClear from "@/assets/dashboard/chat-clear.png";
import {
  AgentDetailVO,
  getV1ChatAgentChatByAgentId,
  OutMessage,
  postV1ChatClearDebugRecord,
  postV1ChatClearSession,
  postV1ChatInitSession,
  postV1ChatInitSessionByAgentId
} from '@/client';
import {fetchEventSource} from '@microsoft/fetch-event-source';
import {getAccessToken} from "@/utils/cache";
import {marked} from "marked";
import ResponseCode from "@/config/ResponseCode";
import {buildImageUrl} from '@/utils/buildImageUrl';

interface IChatProps {
    mode: 'dev' | 'prod'
    agentInfo: AgentDetailVO | undefined
    agentId: string
}
export type AgentMessage = OutMessage & {
  content?: string;
};

const Chat: React.FC<IChatProps> = ({ mode = 'prod', agentId, agentInfo }) => {
    const [open, setOpen] = useState(false);
    const drawerRef = useRef<HTMLDivElement>(null);
    const scrollRef = useRef<HTMLDivElement>(null);
    const loadingRef = useRef(false);
    const userMessageRef = useRef(null);
    const agentMessageRef = useRef<AgentMessage | null>(null);
    const sessionRef = useRef<string>('');
    const [messages, setMessages] = useState<AgentMessage[]>([]);
    const [value, setValue] = useState('');
    const ctrlRef = useRef(new AbortController());
    const scrollTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const token = getAccessToken()

  const scrollToBottom = () => {
    if (scrollTimerRef.current) {
      clearTimeout(scrollTimerRef.current);
    }

    scrollTimerRef.current = setTimeout(() => {
      if (scrollRef.current) {
        console.log('scroll----');
        scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
      }
    }, 200);
  };

    useEffect(() => {
        sessionRef.current = '';
    }, [agentInfo]);

    useEffect(() => {
        const callback = async () => {
            if (!sessionRef.current) return;
            await postV1ChatClearSession({
                query: {
                    sessionId: sessionRef.current,
                }
            })
        }

        return () => {
            callback();
            ctrlRef.current.abort();
        }
    }, [])

    useEffect(() => {
        if (!token || !mode || !agentId) return;

        const startUp = async () => {
            const res = await getV1ChatAgentChatByAgentId({
                path: {
                    agentId: agentId,
                },
                query: {
                    debugFlag: mode === 'prod' ? 0 : 1,
                },
            })

            if (res?.data?.code === ResponseCode.S_OK) {
                let messages:OutMessage[] = [];
                res?.data?.data?.reverse().forEach(item => {
                    messages = messages.concat(item.message || []);
                })
                console.log('messages', messages);
                messages = messages.filter(message => !!message.content);
                setMessages(messages as unknown as AgentMessage[]);
                scrollToBottom();
            }
        }

        startUp();
    }, [token, mode, agentId])

    const toggleDrawer = () => {
        setOpen(!open);
    };

    const onInputChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setValue(e.target.value);
    }, []);

    const onResetSession = useCallback(async () => {
        if (sessionRef.current) {
            await postV1ChatClearSession({
                query: {
                    sessionId: sessionRef.current,
                }
            })
        }
        if (mode === 'dev') {
            await postV1ChatClearDebugRecord({
                query: {
                    agentId: agentId,
                }
            })
            setMessages([]);
        }
        sessionRef.current = '';
        message.success(mode === 'prod' ? "上下文联系已清除" : "记录已清空");
    }, [mode, agentId]);

    const onSendMessage = useCallback(async () => {
        if (!value.trim()) {
            message.info('内容不能为空');
            return;
        }

        if (!sessionRef.current) {
            if (mode === 'prod') {
                const res = await postV1ChatInitSessionByAgentId({
                    path: {
                        agentId
                    }
                })

                if (res?.data?.code === ResponseCode.S_OK) {
                    sessionRef.current = res?.data?.data || "";
                } else if (res?.data?.code === ResponseCode.AGENT_NOT_FOUND) {
                    message.error(res.data.message);
                    return;
                } else {
                    message.error("ai模型初始化失败，请正确配置模型");
                    return;
                }
            } else {
                const res = await postV1ChatInitSession({
                    body: {
                        agentId: agentInfo?.agent?.id!,
                        modelId: agentInfo?.agent?.llmModelId!,
                        prompt: agentInfo?.agent?.prompt,
                        toolIds: agentInfo?.toolList?.map(t => t.id!) || [],
                        temperature: agentInfo?.agent?.temperature,
                        topP: agentInfo?.agent?.topP,
                        maxTokens: agentInfo?.agent?.maxTokens,
                    }
                })
                if (res?.data?.code === ResponseCode.S_OK) {
                    sessionRef.current = res?.data?.data || '';
                } else if (res?.data?.code === ResponseCode.AGENT_NOT_FOUND) {
                    message.error(res.data.message);
                    return;
                } else {
                    console.error("AI 模型初始化失败：", res?.data?.message);
                    message.error("ai模型初始化失败，请正确配置模型");
                    return;
                }
            }
        }

        const raw = JSON.stringify([
            {
                "type": "text",
                "message": value
            }
        ]);
        let responding = false;
        await fetchEventSource(`/v1/chat/stream/${sessionRef.current}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Connection': 'keep-alive',
                'Authorization': 'Bearer ' + token,
            },
            openWhenHidden: true,
            body: raw,
            signal: ctrlRef.current.signal,
            async onmessage(event) {
                setValue('');
                console.log('event---', event);
                if (event.event === 'delta') {
                    const json = JSON.parse(event.data);
                    agentMessageRef.current = agentMessageRef.current ? {...agentMessageRef.current, content: agentMessageRef.current.content + json.part} :
                      {role: "assistant", type: "text", content: json.part};
                    console.log('agentMessageRef.current', agentMessageRef.current);
                    if (responding) {
                        setMessages(prev => {
                            const messages = JSON.parse(JSON.stringify(prev));
                            messages[messages.length - 1] = Object.assign({}, agentMessageRef.current);
                            return messages;
                        })
                    } else {
                        setMessages(prev => {
                          return [...prev, agentMessageRef.current!];
                        })
                    }

                    responding = true;
                    scrollToBottom();
                    return;
                }

                const jsonData = event.data ? JSON.parse(event.data) : {};

                console.log('jsonData', jsonData);
                if (!jsonData.content) {
                    return
                }

                if (jsonData.role === "user" && !userMessageRef.current ) {
                    userMessageRef.current = jsonData;
                    const newMessages = JSON.parse(JSON.stringify(messages));
                    newMessages.push(userMessageRef.current);
                    setMessages(newMessages);
                    scrollToBottom();
                    return
                }
            },
            onerror(err) {
                message.error('消息发送失败');
                console.error('Error:', err)
                throw err;
            },
            onclose() {
                console.log('Close')
                responding = false;
                setTimeout(() => {
                    userMessageRef.current = null;
                    agentMessageRef.current = null;
                    loadingRef.current = false;
                }, 200);
            }
        })
    }, [value, messages, agentInfo]);

    const onInputKeyPress = useCallback(async (event: React.KeyboardEvent<HTMLInputElement>) => {
        console.log('event.key', event.key);
        if (event.key === 'Enter') {
            await onSendMessage();
        }
    }, [onSendMessage]);

    const onClose = () => {
        setOpen(false);
    };

    return (
        <div className={mode == 'dev' ? "flex flex-col bg-[#FFF] px-6 h-full" : "w-full h-[100vh] overflow-hidden flex flex-col"}>
            <div style={{borderBottom: mode == 'prod' ? "2px solid rgba(0,0,0,0.05)" : "none"}} className={mode == 'dev' ? "bg-white flex-none flex items-center h-16 ml-1.5" : "bg-white h-12 flex-none flex items-center px-6"}>
                {mode == 'prod' && <div className='flex-1 text-[18px]'>{agentInfo?.agent?.name}</div>}
                {mode == 'prod' && <EllipsisOutlined onClick={toggleDrawer} style={{ fontSize: '24px' }} className='flex-none' />}
                {mode == 'dev' && <div className='flex-1 text-[18px] text-black/85 font-bold'>调试</div>}
                {mode == 'dev' && <div onClick={onResetSession} className='flex-none flex items-center cursor-pointer'>
                    <Image preview={false} className='w-5 h-5' src={chatClear} alt='clear' />
                    <div className='font-xs text-[#1296DB] ml-3 mr-6'>清空</div>
                </div>}
            </div>
            <div ref={drawerRef} className={mode == 'dev' ? "w-full flex-1 flex flex-col overflow-hidden p-[2px]" : "w-full flex-1 relative pt-20 pb-32 overflow-hidden"}>
                <div ref={scrollRef} className={mode == 'dev' ? "flex-1 px-9 py-7 border border-[#D9D9D9] border-solid rounded-lg overflow-y-auto text-black/85" : "w-full h-[80vh] overflow-y-auto text-black/85"}>
                    <div className={mode == 'dev' ? "" : "w-[768px] mx-auto text-base"}>
                        {messages.map(message => {
                            switch (message.role) {
                                case 'user':
                                  return <div className='mb-8' key={message.createTime}>
                                      <div className='min-h-8 text-message flex w-full flex-col items-end gap-2 whitespace-normal break-words [.text-message+&]:mt-5'>
                                        <div className="relative max-w-[70%] rounded-3xl px-5 py-2.5 bg-[#f4f4f4] dark:bg-token-main-surface-secondary">
                                          {message?.content}
                                        </div>
                                      </div>
                                    </div>
                              case 'assistant':
                                    return (
                                        <div className="mb-8 flex" key={message.createTime}>
                                            <div className="flex-shrink-0 flex flex-col relative items-end">
                                              <img className="w-6 h-6 rounded-md mr-3 mt-1" src={buildImageUrl(agentInfo?.agent?.icon!) || logo} />
                                            </div>
                                            <div className="group/conversation-turn relative flex w-full min-w-0 flex-col agent-turn">
                                                <div className="agentResult w-full overflow-hidden" dangerouslySetInnerHTML={{
                                                    __html: marked.parse(message?.content || ""),
                                                }}></div>
                                            </div>
                                        </div>
                                    )
                                default:  return <div key={message.createTime}></div>
                            }
                        })}
                    </div>
                </div>

                <div className={`bg-white h-[44px]  flex items-center border border-[#D9D9D9] border-solid rounded-md ${mode == 'dev' ? 'w-full flex-none my-4' : 'w-[806px] bottom-[40px] absolute left-1/2 -translate-x-1/2'}`}>
                    <input value={value} onChange={onInputChange} onKeyPress={onInputKeyPress}
                           className="border-0 text-[14px] flex-1 px-3 outline-none" type="text" placeholder={"请输入聊天内容"} />
                    <img onClick={onSendMessage} className="flex-none mr-6 w-5 cursor-pointer" src={sendSvg} alt='send' />
                </div>
            </div>
            <Drawer
                rootClassName="overflow-hidden chatDrawerContainer"
                rootStyle={{position: 'absolute'}}
                placement="right"
                title=""
                getContainer={drawerRef.current!}
                closable={false}
                mask={false}
                onClose={onClose}
                open={open}
                key="chatDetail"
            >
                <div className="flex items-center h-full w-full">
                    <div className="flex flex-col items-center justify-center w-full">
                        <img className="w-[82px] h-[82px] rounded-md mb-12" src={buildImageUrl(agentInfo?.agent?.icon!) || logo} />
                        <div className="text-6 mb-8">{agentInfo?.agent?.name}</div>
                        <div className="text-base text-[#C2C2C2] mb-24">{agentInfo?.agent?.description}</div>
                        <Button onClick={onResetSession} size="large" type="primary">
                          清除上下文联系
                        </Button>
                      <div className="mt-3 text-base text-[#C2C2C2]">清空后，新的回答将不会再引用前面的回答的数据</div>
                    </div>
                </div>
            </Drawer>
        </div>
    );
};

export default Chat;
