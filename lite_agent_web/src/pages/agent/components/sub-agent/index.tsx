import React, { useState, useMemo, useCallback } from 'react';
import { 
  List, 
  Button, 
  Space, 
  Collapse,
  Divider 
} from 'antd';
import { 
  PlusCircleTwoTone, 
  UpOutlined, 
  DownOutlined 
} from '@ant-design/icons';

import { 
  AgentDetailVO, 
  AgentDTO,
} from '@/client';
import { AgentType } from '@/types/chat';
import ImgIcon from '../img-icon';
import removeImg from '@/assets/agent/remove.png';
import agentImg from '@/assets/agent/agent.png';
import { buildImageUrl } from "@/utils/buildImageUrl";

const { Panel } = Collapse;

interface ToolsListProps {
  agentInfo: AgentDetailVO;
  agentList: AgentDTO[];
  onAddClick: () => void;
  toggleSubAgent: (subAgentId: string) => void;
}

const SubAgent: React.FC<ToolsListProps> = ({
  agentInfo,
  agentList,
  onAddClick,
  toggleSubAgent
}) => {

  const { agent } = agentInfo;
  const [showAll, setShowAll] = useState(false);

  const currentSubAgents = useMemo(() => {
    return agentList.filter((subAgent) => 
      agent?.subAgentIds?.includes(subAgent.id!)
    );
  }, [agent, agentList]);

  const displaySubAgents = useMemo(() => {
    return showAll ? currentSubAgents : currentSubAgents.slice(0, 2);
  }, [showAll, currentSubAgents]);

  const renderType = useCallback((type: number | undefined) => {
    switch (type) {
      case AgentType.NORMAL:
        return '普通';
      case AgentType.DISTRIBUTION:
        return '分发';
      case AgentType.REFLECTION:
        return '反思';
      default:
        return '';
    }
  }, []);
  
  return (
    <div>
      <Divider />
      <Collapse ghost>
        <Panel
          className='[&_.ant-collapse-content-box]:px-0'
          header={<span className="text-base font-medium">子 Agent 设置</span>}
          collapsible="header"
          key="1"
          extra={
            <Space size={24}>        
              <Button 
                color="primary" 
                variant="filled" 
                icon={
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M7 9.1875C8.20812 9.1875 9.1875 8.20812 9.1875 7C9.1875 5.79188 8.20812 4.8125 7 4.8125C5.79188 4.8125 4.8125 5.79188 4.8125 7C4.8125 8.20812 5.79188 9.1875 7 9.1875Z" stroke="#40A5EE" strokeLinecap="round" strokeLinejoin="round"/>
                    <path d="M12.0225 9.99905L7.21 12.6339C7.14561 12.6691 7.0734 12.6876 7 12.6876C6.9266 12.6876 6.85439 12.6691 6.79 12.6339L1.9775 9.99905C1.90878 9.96145 1.85142 9.90609 1.8114 9.83875C1.77138 9.77141 1.75018 9.69457 1.75 9.61623V4.38483C1.75018 4.3065 1.77138 4.22965 1.8114 4.16231C1.85142 4.09497 1.90878 4.03961 1.9775 4.00202L6.79 1.36717C6.85439 1.33194 6.9266 1.31348 7 1.31348C7.0734 1.31348 7.14561 1.33194 7.21 1.36717L12.0225 4.00202C12.0912 4.03961 12.1486 4.09497 12.1886 4.16231C12.2286 4.22965 12.2498 4.3065 12.25 4.38483V9.61514C12.25 9.69366 12.2289 9.77073 12.1889 9.83828C12.1488 9.90583 12.0914 9.96136 12.0225 9.99905Z" stroke="#40A5EE" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                } 
                onClick={onAddClick}
                className="w-[60px] h-7 text-xs border border-[#40A5EE] rounded-lg hover:text-[#40A5EE]/80 font-medium [&_.ant-btn-icon]:w-[14px] [&_.ant-btn-icon]:h-[14px]"
              >
                设置
              </Button>
            </Space>
          }
        >
          <div className="flex flex-col">
            {currentSubAgents.length === 0 && <div className=""></div>}

            {currentSubAgents.length > 0 && (
              <List
                className={`flex-grow ${showAll ? 'max-h-[400px] overflow-y-auto' : 'overflow-hidden'}`}
                dataSource={displaySubAgents}
                renderItem={(subAgent) => (
                  <List.Item
                    key={subAgent.id}
                    className="border rounded p-2 mb-2 hover:bg-white [&_.ant-list-item-meta]:flex [&_.ant-list-item-meta]:items-center"
                    actions={[
                      <div key="actions" className="flex items-center space-x-8">
                        <ImgIcon
                          key="remove"
                          src={removeImg}
                          width={24}
                          className="cursor-pointer"
                          onClick={() => toggleSubAgent(subAgent.id!)}
                        />
                      </div>
                    ]}
                  >
                    <List.Item.Meta
                      className="[&_.ant-list-item-meta-avatar]:flex [&_.ant-list-item-meta-avatar]:items-center"
                      avatar={
                        <img
                          src={buildImageUrl(subAgent.icon!) || agentImg}
                          alt={subAgent.name}
                          className="w-10 h-10 rounded"
                        />
                      }
                      title={
                        <div className="flex flex-col justify-between">
                          <div className='break-all line-clamp-1'>{subAgent.name}</div>
                          <div className="mt-1 text-xs text-[#7C8B98]">
                            类型：{renderType(subAgent.type)}
                          </div>
                        </div>
                      }
                    />
                  </List.Item>
                )}
              />
            )}

            {currentSubAgents.length > 2 && (
              <div className="flex justify-center mt-2">
                <Button
                  type="link"
                  onClick={() => setShowAll(!showAll)}
                  icon={showAll ? <UpOutlined /> : <DownOutlined />}
                  className="text-[#40A5EE] hover:text-[#40A5EE]/80"
                >
                  {showAll ? '收起' : '更多'}
                </Button>
              </div>
            )}
          </div>
        </Panel>
      </Collapse>
    </div>
  );
};

export default SubAgent;
