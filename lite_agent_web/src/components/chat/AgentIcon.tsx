import React from 'react';
import logoIcon from '@/assets/login/logo_svg';
import { buildImageUrl } from '@/utils/buildImageUrl';

interface AgentIconProps {
  agentIcon?: string;
}

const AgentIcon: React.FC<AgentIconProps> = ({ agentIcon }) => {
  return agentIcon ? (
    <img className="p-3 w-6 h-6 rounded-md mr-3 bg-[#F5F5F5]" src={buildImageUrl(agentIcon)} alt="agent" />
  ) : (
    <span className="p-3 customeSvg flex items-center justify-center mr-3 text-black bg-[#F5F5F5]">
      <span className="w-6 h-6">{logoIcon}</span>
    </span>
  );
};

AgentIcon.displayName = 'AgentIcon';

const MemoizedAgentIcon = React.memo(AgentIcon, (prevProps, nextProps) => {
  // 只在agentIcon变化时重新渲染
  return prevProps.agentIcon === nextProps.agentIcon;
});

export default MemoizedAgentIcon;