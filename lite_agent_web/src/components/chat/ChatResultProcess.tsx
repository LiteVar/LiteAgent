import { FC, useMemo, memo } from 'react';
import { MessageRole } from '@/types/Message';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { TaskMessageType } from '../../types/Message';
import ChatPlanning from './ChatPlanning';
import { ChatResultProcessProps } from '@/types/chat';

const tryFormatJson = (text: string): string | null => {
  const sanitized = text
    .replace(/&nbsp;/g, ' ')
    .replace(/\u00A0/g, ' ')
    .replace(/\u202F/g, ' ')
    .replace(/\u3000/g, ' ')
    .replace(/[\u200B-\u200D\uFEFF]/g, '');
  const trimmed = sanitized.trim();
  if (!trimmed) return null;

  const tryParse = (candidate: string) => {
    try {
      const parsed = JSON.parse(candidate);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return null;
    }
  };

  // 直接是对象/数组
  if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
    const formatted = tryParse(trimmed);
    if (formatted) return formatted;
  }

  // 兼容被字符串包裹的 JSON："[{\"id\":\"1\"}]"
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    const unquoted = trimmed.slice(1, -1).trim();
    if (unquoted.startsWith('{') || unquoted.startsWith('[')) {
      const formatted = tryParse(unquoted);
      if (formatted) return formatted;
    }
  }

  try {
    return JSON.stringify(JSON.parse(trimmed), null, 2);
  } catch {
    return null;
  }
};

const normalizeJsonMarkdown = (raw: string): string => {
  const trimmed = raw.trim();
  const fencedJsonMatch = trimmed.match(/^```([a-zA-Z0-9_-]*)\s*\n([\s\S]*?)\n```$/);

  // 内容本身已是 json 代码块时，尝试格式化块内 JSON，解决短 JSON 只显示一行的问题
  if (fencedJsonMatch) {
    const [, language = '', body = ''] = fencedJsonMatch;
    const normalizedLanguage = language.toLowerCase();

    if (!normalizedLanguage || normalizedLanguage === 'json') {
      const formatted = tryFormatJson(body);
      if (formatted) {
        return `\`\`\`json\n${formatted}\n\`\`\``;
      }
    }

    return raw;
  }

  const whole = tryFormatJson(raw);
  if (whole) {
    return `\`\`\`json\n${whole}\n\`\`\``;
  }
  return raw;
};

const unwrapMarkdownPayload = (raw: string): string => {
  const trimmed = raw.trim();

  // 部分后端会把 markdown 文本整体序列化成 JSON string 返回，这里优先还原
  if (trimmed.startsWith('"') && trimmed.endsWith('"') && /\\[nrt"]/.test(trimmed)) {
    try {
      const parsed = JSON.parse(trimmed);
      if (typeof parsed === 'string') {
        return parsed;
      }
    } catch {
      // ignore
    }
  }

  return raw;
};

const MarkdownBlock: FC<{ content: string; isError: boolean }> = memo(({ content, isError }) => {
  const markdown = useMemo(() => {
    const raw = normalizeJsonMarkdown(unwrapMarkdownPayload(String(content || '')));
    return raw
      // 后端有时会夹带零宽字符，marked 对 `**` 与后续字符的边界判断会失败
      .replace(/&nbsp;/g, ' ')
      .replace(/\u00A0/g, ' ')
      .replace(/\u202F/g, ' ')
      .replace(/\u3000/g, ' ')
      .replace(/[\u200B-\u200D\uFEFF]/g, '');
  }, [content]);

  return (
    <div
      className={`prose markdown w-full break-words overflow-hidden ${isError ? 'text-red-500' : ''}`}
    >
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          a: ({ href, children, ...props }) => (
            <a href={href} target="_blank" rel="noopener noreferrer" {...props}>
              {children}
            </a>
          ),
        }}
      >
        {markdown}
      </ReactMarkdown>
    </div>
  );
});

MarkdownBlock.displayName = 'MarkdownBlock';

const ChatResultProcess: FC<ChatResultProcessProps> = ({ resultProcessMessages, onSendMessage }) => {
  return (
    <div className="pb-3">
      {resultProcessMessages.map((message, index) => (
        <div key={`resultProcessWrapper-${message.createTime}-${index}`}>
          {message.role === MessageRole.SUBAGENT && (
            <div key={`subAgent-${message.agentId}-${index}`} className="text-sm text-[#999] pb-3">
              {String(message.message || '')}
            </div>
          )}
          {message.role === MessageRole.AGENT && message.type === TaskMessageType.PLANNING && (
            <ChatPlanning onSendMessage={onSendMessage} message={message} />
          )}
          {((message.role === MessageRole.ASSISTANT && message.type === TaskMessageType.TEXT) ||
            (message.role === MessageRole.AGENT && message.type != TaskMessageType.PLANNING)) && (
            <MarkdownBlock content={message.content} isError={message.type === 'error'} />
          )}
        </div>
      ))}
    </div>
  );
};

export default ChatResultProcess;
