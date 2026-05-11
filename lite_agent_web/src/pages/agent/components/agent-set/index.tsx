import React, { useCallback } from 'react';
import { Divider, Select, message, Collapse } from 'antd';

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
    [agentInfo, setAgentInfo]
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
    [agentInfo, setAgentInfo]
  );

  return (
    <div className="border-t border-white/20 mt-4">
      <Collapse ghost>
        <Panel 
          className='[&_.ant-collapse-content-box]:px-0'
          header={<span className="text-base font-medium text-[#383F44]">执行策略</span>} 
          key="1"
        >
          <div className="flex flex-col gap-2">
            <div className="flex flex-col gap-2">
              <label className="text-xs font-medium text-[#7C8B98]">Agent类型</label>
              <Select
                defaultValue={0}
                value={agentInfo.agent?.type}
                className="w-full custom-select [&_.ant-select-selector]:bg-white"
                style={{ height: 32 }}
                onChange={onTypeChange}
              >
                <Option value={AgentType.NORMAL}>普通</Option>
                <Option value={AgentType.DISTRIBUTION}>分发</Option>
                <Option value={AgentType.REFLECTION}>反思</Option>
              </Select>
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-xs font-medium text-[#7C8B98]">执行模式</label>
              <Select
                defaultValue={0}
                className="w-full custom-select [&_.ant-select-selector]:bg-white"
                style={{ height: 32 }}
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
