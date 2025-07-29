import { FunctionCall } from '@/client';
import React from 'react';

interface ToolCallsDisplayProps {
  toolCalls: FunctionCall[];
}

const ToolCallsDisplay: React.FC<ToolCallsDisplayProps> = ({ toolCalls }) => {
  return (
    <>
      {toolCalls.map((tool) => (
        <div key={tool.id} className="bg-gray-50 px-2 py-1 rounded-md">
          <p className="text-sm text-gray-500 mb-1">调用工具：{tool.name}</p>
          <p className="text-sm text-gray-500">工具Id：{tool.id}</p>
          <p className="text-sm text-gray-500">调用参数：{JSON.stringify(tool.arguments)}</p>
        </div>
      ))}
    </>
  );
};

export default ToolCallsDisplay;