import React, { useState, useEffect, useCallback, useRef } from 'react';
import { marked } from 'marked';
import 'highlight.js/styles/github.css';
import logoIcon from '@/assets/login/logo_svg';
import { buildImageUrl } from '@/utils/buildImageUrl';
import MessageActions from './MessageActions';
import { message as Toast, Divider } from 'antd';
import { AgentMessage } from './Chat';
import { copyToClipboard } from '@/utils/clipboard';
import { codeRenderer, linkRenderer } from '@/utils/markdownRenderer';
import { MessageRole } from '@/types/Message';
import { TaskMessageType } from '../../types/Message';
import ChatResultProcess from './ChatResultProcess';
import { Loading3QuartersOutlined } from '@ant-design/icons';
import { TtsStatus } from './MessageActions';
import { postV1ChatAudioSpeech, getV1ModelProviders, getV1ChatAudioSpeech } from '@/client';

interface ChatMessageProps {
  message: AgentMessage;
  agentIcon?: string;
  mode: 'dev' | 'prod';
  isLastMessage?: boolean;
  onRetry: () => void;
  onSendMessage: (type: 'text' | 'execute' | 'imageUrl', text?: string) => Promise<void>;
  onShowThinkMessage: (event: React.MouseEvent<HTMLDivElement>, message: AgentMessage) => void;
  ttsModelId?: string;
  isLastThinkMessage: boolean;
  lastThinkMessage: React.RefObject<HTMLDivElement>;
}

interface AgentIconProps {
  agentIcon?: string;
}

const AgentIcon: React.FC<AgentIconProps> = ({ agentIcon }) => {
  return agentIcon ? (
    <img className="p-3 w-6 h-6 rounded-md mr-3 bg-[#F5F5F5]" src={buildImageUrl(agentIcon)} alt="agent" />
  ) : (
    <span
      className="p-3 customeSvg flex items-center justify-center mr-3 text-black bg-[#F5F5F5]"
    >
      <span className="w-6 h-6">{logoIcon}</span>
    </span>
  );
};

const renderer = new marked.Renderer();
renderer.link = linkRenderer;

marked.setOptions({
  gfm: true,
  renderer: renderer,
  extensions: {
    renderers: {
      code: codeRenderer,
    },
    childTokens: {},
  },
});

const ChatMessage: React.FC<ChatMessageProps> = (props) => {
  const {
    message,
    agentIcon,
    isLastMessage,
    onRetry,
    onShowThinkMessage,
    ttsModelId = '',
    onSendMessage,
    lastThinkMessage,
    isLastThinkMessage,
  } = props;
  const [isHovered, setIsHovered] = useState(false);
  const [copied, setCopied] = useState(false);
  const [ttsStatus, setTtsStatus] = useState<TtsStatus>(TtsStatus.Init);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const audioHasPlayedRef = useRef(false);
  const controllerRef = useRef<AbortController | null>(null);


  useEffect(() => {
    const handleCopyClick = async (event: Event) => {
      const target = event.target as HTMLElement;
      if (target.classList.contains('copy-btn')) {
        const codeWrapper = target.closest('.code-block-wrapper');
        const code = codeWrapper?.querySelector('code')?.textContent;
        if (code) {
          await copyToClipboard(code!);
          target.textContent = '已复制';
          setTimeout(() => {
            target.textContent = '复制';
          }, 2000);
        }
      }
    };

    document.addEventListener('click', handleCopyClick);
    return () => {
      document.removeEventListener('click', handleCopyClick);
    };
  }, []);

  // 添加组件卸载时的清理逻辑
  useEffect(() => {
    return () => {
      // 组件卸载时停止音频播放并清理资源
      if (audioRef.current) {
        audioRef.current.pause();
        audioRef.current.currentTime = 0;
        // 如果音频src是blob URL，需要释放内存
        if (audioRef.current.src && audioRef.current.src.startsWith('blob:')) {
          URL.revokeObjectURL(audioRef.current.src);
        }
        audioRef.current = null;
      }
      setTtsStatus(TtsStatus.Init);
    };
  }, []);

  const renderPlanningChildrenText = (children) => {
    let text = '';
    children.map((child, index) => {
      text = text + `\n   (${index + 1}) ${child.name}, ${child.description?.duty}, ${child.description?.constraint}`;
      if (child.children) {
        return renderPlanningChildrenText(child.children);
      }
    });
    return text;
  }

  const getMessageContent = useCallback(() => {
    let copyText = '';

    if (message.role === MessageRole.USER && message.type === TaskMessageType.TEXT) {
      copyText = message.content;
    } else {
      message.resultProcessMessages?.forEach((item, index) => {
        if (((item.role === MessageRole.ASSISTANT && item.type === TaskMessageType.TEXT) || (item.role === MessageRole.AGENT && item.type != TaskMessageType.PLANNING))) {
          copyText = copyText + `${index != 0 ? '\n' : ''}${item.content}`;
        } else if (item.role === MessageRole.SUBAGENT) {
          copyText = copyText + `${index != 0 ? '\n' : ''}${item.message}`;
        } else if (item.role === MessageRole.AGENT && item.type === TaskMessageType.PLANNING) {
          if (index != 0) {
            copyText = copyText + '\n';
          }
          item?.content?.taskList?.map((task, idx) => {
            copyText = copyText + `${idx != 0 ? '\n' : ''}${idx + 1}、${task.name}, ${task.description?.duty}, ${task.description?.constraint}`;
            if (task.children) {
              copyText = copyText + renderPlanningChildrenText(task.children);
            }
          });
        }
      });
    }

    return copyText;
  }, [message]);

  const handleCopy = useCallback(async () => {
    try {
      const copyText = getMessageContent();

      setTimeout(async () => {
        await copyToClipboard(copyText);
        setCopied(true);
        Toast.success('复制成功');
      }, 200);
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      if (error === 'Failed to copy text') {
        Toast.error('请确保已获得足够的剪贴板权限');
        setCopied(false);
      }
    }
  }, [getMessageContent]);

  // 文字转音频并播放
  const onTtsClick = useCallback(async () => {
    console.log(message);

    if (ttsStatus === 'playing') {
      // 停止播放
      audioRef.current?.pause();
      controllerRef.current?.abort();
      audioRef.current = null;
      setTtsStatus(TtsStatus.Init);
      return;
    }
    if (ttsStatus === 'loading') return;

    setTtsStatus(TtsStatus.Loading);

    const content = getMessageContent().trim().replace(/^```[\s\S]*?```/, '').replace(/```[\s\S]*?```/g, ' 代码块 ');
    const maxLength = 500; // 单段最大长度

    // 按句子分段，避免截断句子
    const splitBySentences = (text: string, maxLen: number) => {
      const segments = [];
      // 使用正则表达式保留分隔符
      const sentences = text.split(/([。！？；\n.!?;])/);
      let currentSegment = '';

      for (let i = 0; i < sentences.length; i += 2) {
        const sentence = sentences[i] || '';
        const punctuation = sentences[i + 1] || '';
        const fullSentence = sentence + punctuation;

        if ((currentSegment + fullSentence).length <= maxLen) {
          currentSegment += fullSentence;
        } else {
          if (currentSegment.trim()) {
            segments.push(currentSegment.trim());
          }
          currentSegment = fullSentence;
        }
      }

      if (currentSegment.trim()) {
        segments.push(currentSegment.trim());
      }

      return segments.filter(seg => seg.length > 0);
    };

    const segments = splitBySentences(content, maxLength);

    try {
      let currentIndex = 0;

      const playSegment = async (index: number) => {
        if (index >= segments.length) {
          setTtsStatus(TtsStatus.Init);
          return;
        }

        controllerRef.current = new AbortController();

        const res = await getV1ChatAudioSpeech({
          query: {
            modelId: ttsModelId,
            content: segments[index],
            stream: true
          },
          signal: controllerRef.current.signal
        });

        if (!!audioRef.current) {
          return;
        }

        if (res.data && res.data instanceof Blob) {
          const blob = res.data;
          const url = URL.createObjectURL(blob);
          const audio = new Audio(url);
          audioRef.current = audio;

          audio.play();

          audio.onended = () => {
            URL.revokeObjectURL(url); // 清理内存
            audioRef.current = null;
            // 播放下一段
            playSegment(index + 1);
          };

          audio.onerror = (e) => {
            console.error('error---111', e.message);
            setTtsStatus(TtsStatus.Init);
            audioRef.current = null;
            if (e.message === 'signal is aborted without reason') {
              Toast.info('音频播放已取消');
            } else {
              Toast.error('音频播放失败');
            }
          };
        } else {
          setTtsStatus(TtsStatus.Init);
          Toast.error('音频播放失败');
        }
      };
      audioHasPlayedRef.current = true;
      setTtsStatus(TtsStatus.Playing);
      await playSegment(currentIndex);

    } catch (e) {
      console.error('error---', e.message);
      setTtsStatus(TtsStatus.Init);
      if (e.message === 'signal is aborted without reason') {
        Toast.info('音频播放已取消');
      } else {
        Toast.error('音频播放失败');
      }
    }
  }, [ttsModelId, ttsStatus, getMessageContent]);

  useEffect(() => {
    if (message.playAudio && ttsModelId && ttsStatus === TtsStatus.Init && !audioHasPlayedRef.current) {
      console.log('onTtsClick---')
      onTtsClick();
    }
  }, [message, ttsModelId, ttsStatus]);

  if (message.role === MessageRole.USER) {
    return (
      <div className="mb-8" onMouseLeave={() => setIsHovered(false)}>
        <div className="min-h-8 text-message flex w-full items-start flex-row-reverse gap-2 whitespace-normal break-words">
          <div
            className="relative max-w-[70%] whitespace-pre-wrap break-words rounded-3xl px-5 py-2.5 bg-[#f4f4f4]"
            onMouseEnter={() => setIsHovered(true)}
          >
            {message.content}
          </div>
        </div>
        <div className="mt-2 flex justify-end">
          <MessageActions
            onCopy={handleCopy}
            show={isLastMessage || isHovered}
            copied={copied}
            onRetry={onRetry}
            retryDisabled={message.type !== 'error'}
          />
        </div>
      </div>
    );
  } else if ((message.role === MessageRole.ASSISTANT && message.type === TaskMessageType.TEXT) || (message.role === MessageRole.AGENT && message.type === TaskMessageType.ERROR)) {
    return (
      <div
        id={isLastThinkMessage ? 'isLastThinkMessage' : ''}
        ref={isLastThinkMessage ? lastThinkMessage : undefined}
        className="mb-8 flex"
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
      >
        <div className="flex-shrink-0 flex flex-col relative items-end">
          <AgentIcon agentIcon={agentIcon} />
        </div>
        <div className="group/conversation-turn relative flex w-full min-w-0 flex-col agent-turn">
          {message.thoughtProcessMessages?.length > 0 && <div onClick={(event) => onShowThinkMessage(event, message)} className='w-fit flex mb-3 mt-1 cursor-pointer px-4 py-2 border border-solid border-[#2A82E4] rounded-lg'>
            <span className="text-[#2A82E4]">过程详情</span>
            {message.responding && <Loading3QuartersOutlined className='ml-2 text-[#2A82E4]' spin />}
          </div>}
          {message.resultProcessMessages?.length > 0 && <ChatResultProcess onSendMessage={onSendMessage} resultProcessMessages={message.resultProcessMessages} />}
          <MessageActions
            onCopy={handleCopy}
            show={isLastMessage || isHovered}
            copied={copied}
            showRetry={true}
            onRetry={onRetry}
            retryDisabled={message.type !== 'error'}
            isAssistant
            ttsModelId={ttsModelId}
            ttsStatus={ttsStatus}
            onTtsClick={onTtsClick}
          />
        </div>
      </div>
    );
  } else if (message.role === MessageRole.SYSTEM && message.type === 'loading') {
    return (
      <div className="flex messages-loader">
        <div className="flex-shrink-0 flex flex-col relative items-end">
          <AgentIcon agentIcon={agentIcon} />
        </div>
        <div className="loader">
          <span className={`dot dot1`}></span>
          <span className={`dot dot2`}></span>
          <span className={`dot dot3`}></span>
        </div>
      </div>
    );
  } else if (message.role === MessageRole.SEPARATOR) {
    return <Divider plain className="my-8 text-[#999] text-xs">聊聊新话题</Divider>;
  }

  return null;
};

export default ChatMessage;
