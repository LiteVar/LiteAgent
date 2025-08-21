import React, { useState, useEffect, useCallback } from 'react';
import { marked } from 'marked';
import 'highlight.js/styles/github.css';
import AgentIcon from './AgentIcon';
import MessageActions from './MessageActions';
import { message as Toast, Divider } from 'antd';
import { copyToClipboard } from '@/utils/clipboard';
import { linkRenderer } from '@/utils/markdownRenderer';
import { MessageRole } from '@/types/Message';
import { TaskMessageType } from '../../types/Message';
import ChatResultProcess from './ChatResultProcess';
import { Loading3QuartersOutlined } from '@ant-design/icons';
import { useTTS } from '@/hooks/useTTS';
import { ChatMessageProps } from '@/types/chat';

// 添加类型定义
interface PlanningTask {
  name: string;
  description?: {
    duty?: string;
    constraint?: string;
  };
  children?: PlanningTask[];
}

const renderer = new marked.Renderer();
renderer.link = linkRenderer;

marked.setOptions({
  gfm: true,
  renderer: renderer,
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

  // 递归渲染子任务文本的辅助函数
  const renderPlanningChildrenText = useCallback((children: PlanningTask[]): string => {
    let text = '';
    children.map((child, index) => {
      text =
        text +
        `\n   (${index + 1}) ${child.name}, ${child.description?.duty}, ${child.description?.constraint}`;
      if (child.children) {
        return renderPlanningChildrenText(child.children);
      }
    });
    return text;
  }, []);

  // 消息内容提取逻辑
  const getMessageContent = useCallback(() => {
    let copyText = '';

    if (message.role === MessageRole.USER && message.type === TaskMessageType.TEXT) {
      copyText = message.content;
    } else {
      message.resultProcessMessages?.forEach((item, index) => {
        if (
          (item.role === MessageRole.ASSISTANT && item.type === TaskMessageType.TEXT) ||
          (item.role === MessageRole.AGENT && item.type != TaskMessageType.PLANNING)
        ) {
          copyText = copyText + `${index != 0 ? '\n' : ''}${item.content}`;
        } else if (item.role === MessageRole.SUBAGENT) {
          copyText = copyText + `${index != 0 ? '\n' : ''}${item.message}`;
        } else if (item.role === MessageRole.AGENT && item.type === TaskMessageType.PLANNING) {
          if (index != 0) {
            copyText = copyText + '\n';
          }
          item?.content?.taskList?.map((task: PlanningTask, idx: number) => {
            copyText =
              copyText +
              `${idx != 0 ? '\n' : ''}${idx + 1}、${task.name}, ${task.description?.duty}, ${task.description?.constraint}`;
            if (task.children) {
              copyText = copyText + renderPlanningChildrenText(task.children);
            }
          });
        }
      });
    }

    return copyText;
  }, [message, renderPlanningChildrenText]);

  // 使用TTS hook
  const { ttsStatus, onTtsClick } = useTTS({
    ttsModelId,
    getMessageContent,
    playAudio: message.playAudio,
  });

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

  if (message.role === MessageRole.USER) {
    return (
      <div className="mb-8" onMouseEnter={() => setIsHovered(true)} onMouseLeave={() => setIsHovered(false)}>
        <div className="min-h-8 text-message flex w-full items-start flex-row-reverse gap-2 whitespace-normal break-words">
          <div
            className="relative max-w-[70%] whitespace-pre-wrap break-words rounded-3xl px-5 py-2.5 bg-[#f4f4f4]"
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
  } else if (
    (message.role === MessageRole.ASSISTANT && message.type === TaskMessageType.TEXT) ||
    (message.role === MessageRole.AGENT && message.type === TaskMessageType.ERROR)
  ) {
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
          {message.thoughtProcessMessages && message.thoughtProcessMessages.length > 0 && (
            <div
              onClick={(event) => onShowThinkMessage(event, message)}
              className="w-fit flex mb-3 mt-1 cursor-pointer px-4 py-2 border border-solid border-[#2A82E4] rounded-lg"
            >
              <span className="text-[#2A82E4]">过程详情</span>
              {message.responding && <Loading3QuartersOutlined className="ml-2 text-[#2A82E4]" spin />}
            </div>
          )}
          {message.resultProcessMessages && message.resultProcessMessages.length > 0 && (
            <ChatResultProcess
              onSendMessage={onSendMessage}
              resultProcessMessages={message.resultProcessMessages}
            />
          )}
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
    return (
      <Divider plain className="my-8 text-[#999] text-xs">
        聊聊新话题
      </Divider>
    );
  }

  return null;
};

const MemoizedChatMessage = React.memo(ChatMessage, (prevProps, nextProps) => {
  // 检查 resultProcessMessages 的变化（修复打字效果问题）
  const prevResultMessages = prevProps.message.resultProcessMessages || [];
  const nextResultMessages = nextProps.message.resultProcessMessages || [];
  
  if (prevResultMessages.length !== nextResultMessages.length) {
    return false;
  }
  
  // 检查每个 resultProcessMessage 的内容变化
  for (let i = 0; i < prevResultMessages.length; i++) {
    if (prevResultMessages[i].content !== nextResultMessages[i].content) {
      return false;
    }
  }
  
  // 检查 thoughtProcessMessages 的变化
  const prevThoughtMessages = prevProps.message.thoughtProcessMessages || [];
  const nextThoughtMessages = nextProps.message.thoughtProcessMessages || [];
  
  if (prevThoughtMessages.length !== nextThoughtMessages.length) {
    return false;
  }
  
  for (let i = 0; i < prevThoughtMessages.length; i++) {
    if (prevThoughtMessages[i].content !== nextThoughtMessages[i].content) {
      return false;
    }
  }
  
  // 优化比较逻辑，只在关键属性变化时重新渲染
  return (
    prevProps.message.id === nextProps.message.id &&
    prevProps.message.content === nextProps.message.content &&
    prevProps.message.responding === nextProps.message.responding &&
    prevProps.agentIcon === nextProps.agentIcon &&
    prevProps.isLastMessage === nextProps.isLastMessage &&
    prevProps.ttsModelId === nextProps.ttsModelId &&
    prevProps.isLastThinkMessage === nextProps.isLastThinkMessage
  );
});

export default MemoizedChatMessage;
