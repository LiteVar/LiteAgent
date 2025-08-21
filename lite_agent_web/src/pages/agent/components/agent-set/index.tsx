import React, { useCallback } from 'react';
import { Button, Divider, Select, message, Collapse } from 'antd';
import { SettingTwoTone } from '@ant-design/icons';

import { AgentDetailVO } from '@/client';
import { AgentType } from '@/types/chat';

export type AgentTypeMode = 0 | 1 | 2 | undefined;

interface AgentSetProps {
  agentInfo: AgentDetailVO;
  setAgentInfo: (agent: AgentDetailVO) => void;
}

const { Option } = Select;
const { Panel } = Collapse;

const AgentSet: React.FC<AgentSetProps> = ({ 
  agentInfo, 
  setAgentInfo
}) => {

  const onTypeChange = useCallback(
    (val: AgentTypeMode) => {
      const subAgentIds = agentInfo.agent?.subAgentIds || [];
      if (val === AgentType.REFLECTION && subAgentIds.length > 0) {
        message.warning('已添加子 agent，无法改为反思类型');
        return;
      }

      setAgentInfo({
        ...agentInfo,
        agent: {
          ...agentInfo.agent,
          type: val,
        },
      });
    },
    [agentInfo]
  );

  const onModeChange = useCallback(
    (val: AgentTypeMode) => {
      setAgentInfo({
        ...agentInfo,
        agent: {
          ...agentInfo.agent,
          mode: val,
        },
      });
    },
    [agentInfo]
  );

  return (
    <div className="">
      <Divider />
      <Collapse ghost>
        <Panel 
          header={<span className="text-base font-medium">执行策略</span>} 
          key="1"
        >
          <div className="space-y-4">
            <div className="flex justify-between space-x-4">
              <label className="mr-4 w-20 text-sm">Agent类型</label>
              <Select
                defaultValue={0}
                value={agentInfo.agent?.type}
                className="flex-1"
                onChange={onTypeChange}
              >
                <Option value={AgentType.NORMAL}>普通</Option>
                <Option value={AgentType.DISTRIBUTION}>分发</Option>
                <Option value={AgentType.REFLECTION}>反思</Option>
              </Select>
            </div>

            <div className="flex justify-between space-x-4">
              <label className="mr-4 w-20 text-sm">执行模式</label>
              <Select
                defaultValue={0}
                className="flex-1"
                onChange={onModeChange}
                value={agentInfo.agent?.mode}
              >
                <Option value={0}>并行</Option>
                <Option value={1}>串行</Option>
                <Option value={2}>拒绝</Option>
              </Select>
            </div>
          </div>
        </Panel>
      </Collapse>
    </div>
  );
};

export default AgentSet;
