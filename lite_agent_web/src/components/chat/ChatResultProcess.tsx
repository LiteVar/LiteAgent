import { FC } from 'react';
import { MessageRole } from '@/types/Message';
import { marked } from 'marked';
import { TaskMessageType } from '../../types/Message';
import ChatPlanning from './ChatPlanning';
import { ChatResultProcessProps } from '@/types/chat';

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
            <div
              className={`prose markdown w-full overflow-hidden 
                              ${message.type === 'error' ? 'text-red-500' : ''}`}
              dangerouslySetInnerHTML={{
                __html: marked.parse(String(message.content || '')),
              }}
            ></div>
          )}
        </div>
      ))}
    </div>
  );
};

export default ChatResultProcess;
