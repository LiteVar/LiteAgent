import React, { MouseEvent } from 'react';
import { Card, Image } from 'antd';
import { EllipsisOutlined } from '@ant-design/icons';
import { AgentDTO } from '@/client';
import { buildImageUrl } from '@/utils/buildImageUrl';
import logoIcon from '@/assets/login/logo_svg';
import agentPcBlackSvg from '@/assets/dashboard/agent-pc-black.svg';

interface IAgentCard {
  tab: number;
  agent: AgentDTO;
  onSelectAgent: (agent: AgentDTO, event: MouseEvent<HTMLElement>) => void;
}

const AgentCard: React.FC<IAgentCard> = ({ agent, onSelectAgent, tab }) => {
  return (
    <Card onClick={(e) => onSelectAgent(agent, e)} className="hover:shadow-md transition-shadow cursor-pointer">
      <div>
        <div className="flex items-center flex-none">
          {agent.icon ? (
            <img
              src={buildImageUrl(agent.icon!)}
              alt={`图标`}
              width={40}
              height={40}
              className="mr-3 rounded"
            />
          ) : (
            <span className="customeSvg w-12 h-12 flex items-center justify-center rounded-md mr-3 bg-[#F5F5F5]">
              <span className="w-6 h-6 text-black">{logoIcon}</span>
            </span>
          )}
          <h3 className="text-lg font-semibold break-all m-2 flex-1">{agent.name}</h3>
          {tab != 4 && <EllipsisOutlined style={{ fontSize: '24px', color: '#000' }} className="flex-none" />}
          {tab === 4 && <img className="flex-none w-4" src={agentPcBlackSvg} alt="本地Agent" />}
        </div>
        {!!agent.autoAgentFlag && <p className='text-base text-gray-500 mt-4'>类型：Auto Multi Agent</p>}
        <p className="text-gray-500 mt-4 mb-2 line-clamp-3">{agent.description}</p>
      </div>
    </Card>
  );
};

export default AgentCard; 