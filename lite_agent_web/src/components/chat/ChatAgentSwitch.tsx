import { FC, useState } from 'react';
import { MessageRole } from '@/types/Message';
import { TaskMessageType } from '../../types/Message';
import { DownOutlined, UpOutlined } from '@ant-design/icons';
import { ChatAgentSwitchProps, KnowledgeMessage, ReflectMessage, ToolMessage } from '@/types/chat';
import ChatReflect from './ChatReflect';
import ChatTool from './ChatTool';
import ChatKnowledge from './ChatKnowledge';
import ChatThink from './ChatThink';
import ChatDispatch from './ChatDispatch';

const ChatAgentSwitch: FC<ChatAgentSwitchProps> = ({ agentSwitchMessage, onSearchKnowledgeResult }) => {
  const [isShowContent, setIsShowContent] = useState(true);

  if (!agentSwitchMessage.messages || agentSwitchMessage.messages?.length === 0) return null;

  return (
    <div className="mb-3">
      <div className="mb-2 flex items-center">
        <span
          onClick={() => setIsShowContent(!isShowContent)}
          className="text-sm cursor-pointer text-[#999]"
        >{`调用agent: ${agentSwitchMessage.content?.agentName || ''}`}</span>
        <span className="ml-1 cursor-pointer text-xs" onClick={() => setIsShowContent(!isShowContent)}>
          {isShowContent ? (
            <UpOutlined style={{ color: '#999' }} />
          ) : (
            <DownOutlined style={{ color: '#999' }} />
          )}
        </span>
      </div>
      {isShowContent && (
        <div className='ml-4'>
          {agentSwitchMessage.messages?.map((message, index) => (
            <div key={`message-${message.id}-${index}`}>
              {message.role === MessageRole.REFLECTION && (
                <ChatReflect
                  key={`reflect-${message.taskId}-${index}`}
                  reflect={message as ReflectMessage}
                />
              )}
              {message.role === MessageRole.TOOL && (
                <ChatTool
                  key={`tool-${message.createTime}-${index}`}
                  tool={message as ToolMessage}
                />
              )}
              {message.role === MessageRole.AGENT && message.type === TaskMessageType.KNOWLEDGE && (
                <ChatKnowledge
                  onSearchKnowledgeResult={onSearchKnowledgeResult}
                  key={`knowledge-${message.createTime}-${index}`}
                  knowledge={message as KnowledgeMessage}
                />
              )}
              {message.role === MessageRole.ASSISTANT && message.type === TaskMessageType.THINK && (
                <ChatThink key={`think-${message.createTime}-${index}`} message={message} />
              )}
              {message.role === MessageRole.AGENT && message.type === TaskMessageType.DISPATCH && (
                <ChatDispatch key={`dispatch-${message.createTime}-${index}`} message={message} />
              )}
              {message.role === MessageRole.ASSISTANT && message.type === TaskMessageType.TEXT && !!message.content.trim() && <div key={`content-${message.createTime}-${index}`} className="mb-2 text-[#666666] text-sm">{`输出内容: ${message.content}`}</div>}
              {message.role === MessageRole.AGENT && message.type === TaskMessageType.AGENT_SWITCH && (
                <ChatAgentSwitch key={`agent-switch-${message.createTime}`} agentSwitchMessage={message} onSearchKnowledgeResult={onSearchKnowledgeResult} />
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default ChatAgentSwitch;
