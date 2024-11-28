import React, {useEffect, useMemo} from 'react';
import { Popover, Slider, Input, Typography, Button } from 'antd';
import {Agent, AgentVO, ModelDTO} from "@/client";

const { Text } = Typography;
export const DEFAULT_Max_TOKENS = 4096
interface AdvancedSettingsPopoverProps {
  agent: Agent;
  setAgentInfo: any;
  modelList: ModelDTO[];
}

const AdvancedSettingsPopover: React.FC<AdvancedSettingsPopoverProps> = ({ agent, setAgentInfo, modelList }) => {
  const maxMaxToken = useMemo(() => {
    if (agent?.llmModelId) {
      return modelList?.filter(v => v.id === agent?.llmModelId)?.[0]?.maxTokens || DEFAULT_Max_TOKENS
    } else {
      return DEFAULT_Max_TOKENS
    }
  }, [modelList, agent])

  const advancedSettingsContent = (
    <div className="w-80">
      <div className="mb-4">
        <Text strong>Temperature</Text>
        <div className="flex">
          <Slider
            min={0}
            max={1}
            step={0.1}
            className="flex-1"
            value={agent?.temperature || 0}
            onChange={(value: number) => setAgentInfo((pre: AgentVO) => {
              return {...pre, agent: {...pre.agent, temperature: value}}
            })}
          />
          <Input className={"ml-2 w-16"} value={agent?.temperature || 0} readOnly />
        </div>
      </div>
      <div className="mb-4">
        <Text strong>Max Token</Text>
        <div className="flex">
          <Slider
            min={1}
            max={maxMaxToken}
            step={1}
            value={agent?.maxTokens || maxMaxToken}
            className="flex-1"
            onChange={(value: number) => setAgentInfo((pre: AgentVO) => {
              return {...pre, agent: {...pre.agent, maxTokens: value}}
            })}
          />
          <Input className={"ml-2 w-16"} value={agent?.maxTokens || maxMaxToken} readOnly />
        </div>
      </div>
      <div className="mb-4">
        <Text strong>Top P</Text>
        <div className="flex">
          <Slider
            min={0}
            max={1}
            step={0.1}
            value={agent?.topP || 0}
            className="flex-1"
            onChange={(value: number) => setAgentInfo((pre: AgentVO) => {
              return {...pre, agent: {...pre.agent, topP: value}}
            })}
          />
          <Input className={"ml-2 w-16"} value={agent?.topP || 0} readOnly />
        </div>
      </div>
    </div>
  );

  return (
    <Popover content={advancedSettingsContent} trigger="click">
      <Button disabled={!agent?.llmModelId} className="mt-2 ml-3">更多</Button>
    </Popover>
  );
};

export default AdvancedSettingsPopover;
