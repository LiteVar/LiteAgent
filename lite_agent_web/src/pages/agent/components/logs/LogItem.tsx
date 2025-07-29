import React from 'react';
import { MessageDTO, TaskMessage } from '@/client';
import TaskMessageItem from './TaskMessageItem';

interface LogItemProps {
  item: MessageDTO;
  index: number;
  onKnowledgeClick: (queryText: string, results: any[]) => void;
}

const LogItem: React.FC<LogItemProps> = ({ item, index, onKnowledgeClick }) => {
  const getOrigin = (origin: string | undefined, user: string | undefined): string => {
    if (origin === 'debug') {
      return `${user} debug`;
    } else if (origin === 'user') {
      return `${user} 用户`;
    } else {
      return origin || '';
    }
  };

  const formatIndex = (idx: number): string => {
    return idx > 9 ? `${idx + 1}` : `0${idx + 1}`;
  };

  return (
    <div className="border border-gray-300 rounded-xl px-4 mb-2 break-all break-words">
      <div className="flex items-center gap-4 p-4 rounded-md text-sm text-gray-500 bg-[#f5f5f5]">
        <span className="text-blue-400">#{formatIndex(index)}</span>
        <span>SessionId: {item.sessionId}</span>
        <span>{item.createTime}</span>
        {item.origin && <span>来自{getOrigin(item.origin, item.user)}调用</span>}
      </div>
      <div>
        {item?.taskMessage?.map((task: TaskMessage) => (
          <TaskMessageItem
            key={task.taskId}
            task={task}
            onKnowledgeClick={onKnowledgeClick}
          />
        ))}
      </div>
    </div>
  );
};

export default LogItem;