import React, { useCallback, useEffect, useMemo } from 'react';
import { Popover, Slider, Input, Typography, Button } from 'antd';
import { Agent, AgentVO, ModelDTO } from "@/client";

const { Text } = Typography;
export const DEFAULT_Max_TOKENS = 4096
interface AdvancedSettingsPopoverProps {
  agent: Agent;
  readonly?: boolean;
  setAgentInfo: any;
  modelList: ModelDTO[];
}

const AdvancedSettingsPopover: React.FC<AdvancedSettingsPopoverProps> = ({ agent, readonly, setAgentInfo, modelList }) => {
  const currentModalMaxToken = useMemo(() => {
    if (agent?.llmModelId) {
      const currentModel = modelList.find(m => m.id === agent?.llmModelId);

      if (currentModel) {
        return currentModel.maxTokens || DEFAULT_Max_TOKENS;
      } else {
        return DEFAULT_Max_TOKENS
      }
    } else {
      return DEFAULT_Max_TOKENS
    }
  }, [agent, modelList])

  const currentTokenVal = useMemo(() => {
    if (agent.maxTokens === null) {
      return null; // 如果 maxTokens 为 null，则返回 null
    }

    const agentMaxToken = agent.maxTokens ?? DEFAULT_Max_TOKENS;
    if (currentModalMaxToken < agentMaxToken) {
      return currentModalMaxToken
    } else {
      return agentMaxToken
    }
  }, [agent, currentModalMaxToken]);

  const handleTokensInputChange = useCallback((rawValue: string) => {
    if (rawValue === '' || /^[0-9]*$/.test(rawValue)) {
      // 允许空值或数字输入
      setAgentInfo((pre: AgentVO) => ({
        ...pre,
        agent: {
          ...pre.agent,
          maxTokens: rawValue === '' ? null : Number(rawValue), // 如果为空，设置为 null
        },
      }));
    }
  }, [setAgentInfo]);

  const handleTokensInputBlur = useCallback(() => {
    if (currentTokenVal === null || currentTokenVal < 1) {
      setAgentInfo((pre: AgentVO) => ({
        ...pre,
        agent: {
          ...pre.agent,
          maxTokens: 1, // 修正为最小值
        },
      }));
    }
  }, [currentTokenVal, currentModalMaxToken, setAgentInfo]);

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
              return { ...pre, agent: { ...pre.agent, temperature: value } }
            })}
          />
          <Input
            type="number"
            min={0}
            max={1}
            step={0.1}
            className={"ml-2 w-20"}
            value={agent?.temperature || 0}
            onChange={(e) => setAgentInfo((pre: AgentVO) => {
              return { ...pre, agent: { ...pre.agent, temperature: e.target.value } }
            })}
          />
        </div>
      </div>
      <div className="mb-4">
        <Text strong>Max Token</Text>
        <div className="flex">
          <Slider
            min={1}
            max={currentModalMaxToken}
            step={1}
            value={currentTokenVal || 1}
            className="flex-1"
            onChange={(value: number) => setAgentInfo((pre: AgentVO) => {
              return { ...pre, agent: { ...pre.agent, maxTokens: value } }
            })}
          />
          <Input
            type="number"
            step={1}
            min={1}
            max={currentModalMaxToken}
            className={"ml-2 w-20"}
            value={currentTokenVal === null ? '' : currentTokenVal}
            onChange={(e) => handleTokensInputChange(e.target.value)}
            onBlur={handleTokensInputBlur}
          />
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
              return { ...pre, agent: { ...pre.agent, topP: value } }
            })}
          />
          <Input
            type="number"
            min={0}
            max={1}
            step={0.1}
            className={"ml-2 w-20"}
            value={agent?.topP || 0}
            onChange={(e) => setAgentInfo((pre: AgentVO) => {
              return { ...pre, agent: { ...pre.agent, topP: e.target.value } }
            })}
          />
        </div>
      </div>
    </div>
  );

  return (
    <Popover content={advancedSettingsContent} trigger="click">
      <Button
        disabled={!agent?.llmModelId || readonly}
        className={`${readonly ? 'opacity-20' : ''} mt-2 ml-3 h-[40px] border border-black bg-transparent text-black rounded-md hover:bg-gray-100 transition`}
      >
        更多
      </Button>
    </Popover>
  );
};

export default AdvancedSettingsPopover;
