import React, { FC } from 'react';
import { ChatThinkProps } from '@/types/chat';

const ChatThink: FC<ChatThinkProps> = ({ message }) => {
  if (message.content.trim() === '') return null;
  return (
    <div className="mb-6">
      <div className="flex items-center mb-3">
        <div className="w-2 h-2 rounded-full bg-[#40A5EE] mr-2"></div>
        <div className="text-sm font-medium text-[#1D4A6B]">思考过程</div>
      </div>
      <div className="pl-4 border-l-2 border-[#E0E3E6]">
        <div className="text-sm leading-relaxed text-[#383F44] whitespace-pre-wrap">{message.content}</div>
      </div>
    </div>
  );
};

export default ChatThink;
