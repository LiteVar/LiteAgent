import React, { MouseEvent } from 'react';
import { Button } from 'antd';
import { AgentDTO } from '@/client';
import { buildImageUrl } from '@/utils/buildImageUrl';
import AgentIconSvg from '@/assets/common/agent_icon_svg';
import agentPcBlackSvg from '@/assets/dashboard/agent-pc-black.svg';

interface IAgentCard {
  tab: number;
  agent: AgentDTO;
  onSelectAgent: (agent: AgentDTO, event: MouseEvent<HTMLElement>) => void;
}

const AgentCard: React.FC<IAgentCard> = ({ agent, onSelectAgent, tab }) => {
  return (
    <div
      onClick={(e) => onSelectAgent(agent, e)}
      className="flex flex-row items-center justify-between h-[84px] px-4 cursor-pointer transition-all hover:shadow-lg bg-white/60 backdrop-blur-[4px] border border-white/80 rounded-xl"
    >
      {/* 左侧：图标 + 名称 */}
      <div className="flex items-center gap-[10px] flex-1 overflow-hidden">
        {agent.icon ? (
          <img
            src={buildImageUrl(agent.icon!)}
            alt="图标"
            width={40}
            height={40}
            className="flex-none rounded-lg object-cover"
          />
        ) : (
          <AgentIconSvg seed={agent.id} />
        )}
        <div className="flex-1 overflow-hidden">
          <p className="text-sm font-medium text-[#383F44] truncate whitespace-pre-line" 
          style={{display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden'}}>
            {agent.name}
          </p>
          {tab === 4 && (
            <img className="w-3 mt-0.5" src={agentPcBlackSvg} alt="本地Agent" />
          )}
        </div>
      </div>

      {/* 右侧：详情按钮 */}
      <Button
        size="small"
        onClick={(e) => onSelectAgent(agent, e)}
        className="flex-none text-xs h-8 px-4 rounded-lg bg-transparent hover:!bg-[#40A5EE]/5 border-[#40A5EE] text-[#40A5EE]"
      >
        详情
      </Button>
    </div>
  );
};

export default AgentCard;