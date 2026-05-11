import React from 'react';
import ChatLogo from '@/assets/agent/agent.png';
import { buildImageUrl } from '@/utils/buildImageUrl';

interface AgentIconProps {
  agentIcon?: string;
}

const AgentIcon: React.FC<AgentIconProps> = ({ agentIcon }) => {
  return (
    <img className="w-10 h-10 rounded-lg mr-3 bg-[#F5F5F5] object-cover" src={agentIcon ? buildImageUrl(agentIcon) : ChatLogo} alt="agent" />
  );
};

AgentIcon.displayName = 'AgentIcon';

const MemoizedAgentIcon = React.memo(AgentIcon, (prevProps, nextProps) => {
  // 只在agentIcon变化时重新渲染
  return prevProps.agentIcon === nextProps.agentIcon;
});

export default MemoizedAgentIcon;