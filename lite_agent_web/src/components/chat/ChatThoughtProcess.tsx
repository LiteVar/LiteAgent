import { FC } from 'react';
import { MessageRole } from '@/types/Message';
import ChatReflect from './ChatReflect';
import ChatTool from './ChatTool';
import ChatKnowledge from './ChatKnowledge';
import { TaskMessageType } from '@/types/Message';
import ChatThink from './ChatThink';
import ChatDispatch from './ChatDispatch';
import ChatAgentSwitch from './ChatAgentSwitch';
import { ChatThoughtProcessProps, ReflectMessage, ToolMessage, KnowledgeMessage } from '@/types/chat';

const ChatThoughtProcess: FC<ChatThoughtProcessProps> = ({
  thoughtProcessMessages,
  onSearchKnowledgeResult,
}) => {
  return (
    <div className="pb-3">
      <div>
        {thoughtProcessMessages.map((message, index) => (
          <div key={`thoughtProcessWrapper-${message.createTime}-${index}`}>
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
            {message.role === MessageRole.AGENT && message.type === TaskMessageType.AGENT_SWITCH && (
              <ChatAgentSwitch key={`agent-switch-${message.createTime}`} agentSwitchMessage={message} onSearchKnowledgeResult={onSearchKnowledgeResult} />
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default ChatThoughtProcess;
