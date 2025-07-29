import React from 'react';
import { AgentDetailVO } from '@/client';

interface AutoAgentTypeProps {
  agentInfo: AgentDetailVO;
}

const AutoAgentType: React.FC<AutoAgentTypeProps> = ({ agentInfo }) => {

  return (
    <div>
      <div className="text-base font-medium mb-4">类型</div>
      <div className="text-base font-medium mb-2">Auto Multi Agent</div>
      <div className="text-base text-gray-500">{agentInfo?.agent?.description}</div>
    </div>
  );
};

export default AutoAgentType;
