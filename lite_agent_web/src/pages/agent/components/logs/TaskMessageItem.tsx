import React from 'react';
import { TaskMessage, OutMessage, SegmentVO } from '@/client';
import MessageItem from './MessageItem';

interface TaskMessageItemProps {
  task: TaskMessage;
  onKnowledgeClick: (queryText: string, results: SegmentVO[]) => void;
}

const TaskMessageItem: React.FC<TaskMessageItemProps> = ({ task, onKnowledgeClick }) => {
  return (
    <div className="border mb-4">
      {task?.message?.map((mes: OutMessage, index: number) => (
        <MessageItem
          key={`${task.taskId}-${index}`}
          message={mes}
          onKnowledgeClick={onKnowledgeClick}
        />
      ))}
    </div>
  );
};

export default TaskMessageItem;