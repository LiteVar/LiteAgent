import { FC, useState, useMemo } from 'react';
import { Checkbox, Collapse, Divider } from 'antd';
import type { CheckboxChangeEvent } from 'antd/es/checkbox';

import { AgentDetailVO } from '@/client';

export type AutoEnableType = 0 | 1;

const { Panel } = Collapse;

interface MultiAgentProps {
  agentInfo: AgentDetailVO;
  onAutoEnableChange: (enabled: AutoEnableType) => void;
}

const MultiAgent: FC<MultiAgentProps> = ({ 
  agentInfo, 
  onAutoEnableChange 
}) => {

  const autoEnabled = useMemo(() => {
    return !!agentInfo?.agent?.auto
  }, [agentInfo]);

  const handleTtsEnableChange = (e: CheckboxChangeEvent) => {
    const checked = e.target.checked;
    checked? onAutoEnableChange(1) : onAutoEnableChange(0);
  };

  return (
    <div className="convert-container">
      <Divider />
      <Collapse ghost>
        <Panel 
          header={<span className="text-base font-medium">Auto Multi-Agent 控制</span>} 
          key="1"
        >
          <div className="mb-4">
            <div className="flex items-center mb-2">
              <div className="text-gray-500">
                开启后，agent将分析输入的指令，并且依据已有的工具/知识库/agent自动生成临时agent完成任务。
              </div>
            </div>

            <div className="flex justify-between items-center py-4 border-b">
              <div className="text-base">Auto Multi-Agent</div>
              <Checkbox checked={autoEnabled} onChange={handleTtsEnableChange}>
                开启
              </Checkbox>
            </div>
          </div>
        </Panel>
      </Collapse>
    </div>
  );
};

export default MultiAgent;
