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
import { AgentType } from '../agent-set';
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
          header={<span className="text-base font-medium">子 Agent 设置</span>}
          collapsible="header"
          key="1"
          extra={
            <Space size={24}>        
              <Button 
                color="primary" 
                variant="filled" 
                icon={<PlusCircleTwoTone />} 
                onClick={onAddClick}
              >
                添加
              </Button>
            </Space>
          }
        >
          <div className="flex flex-col">
            {currentSubAgents.length === 0 && <div className=""></div>}

            {currentSubAgents.length > 0 && (
              <List
                className={`flex-grow ${showAll ? 'overflow-y-scroll' : 'overflow-hidden'}`}
                style={{ height: showAll ? '29vh' : 'auto' }}
                dataSource={displaySubAgents}
                renderItem={(subAgent) => (
                  <List.Item
                    key={subAgent.id}
                    className="border rounded p-2 mb-2 hover:bg-gray-100"
                    actions={[
                      <div key="actions" className="flex items-center space-x-8">
                        <span>
                          {renderType(subAgent.type)}
                        </span>
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
                      avatar={
                        <img
                          src={buildImageUrl(subAgent.icon!) || agentImg}
                          alt={subAgent.name}
                          className="w-10 h-10 rounded"
                        />
                      }
                      title={<div className="pt-2">{subAgent.name}</div>}
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
                  iconPosition="end"
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
