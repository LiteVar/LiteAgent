import React, { useCallback, useMemo } from 'react';
import { Popover, Slider, Input, Button } from 'antd';
import { Agent, AgentVO, ModelDTO } from "@/client";

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
  }, [currentTokenVal, setAgentInfo]);

  const advancedSettingsContent = (
    <div className="w-[204px] px-4 py-3 flex flex-col gap-2 [&_.ant-slider-track]:bg-[#40A5EE]">
      <div className="flex flex-col gap-1">
        <div className="text-sm font-medium text-[#383F44] leading-[22px]">Temperature</div>
        <div className="flex items-center gap-2">
          <Slider
            min={0}
            max={1}
            step={0.1}
            className="flex-1 m-0"
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
            className="w-[60px] h-8 rounded-lg text-center border-[#E0E3E6] ml-2"
            value={agent?.temperature || 0}
            onChange={(e) => setAgentInfo((pre: AgentVO) => {
              return { ...pre, agent: { ...pre.agent, temperature: e.target.value } }
            })}
          />
        </div>
      </div>
      
      <div className="flex flex-col gap-1">
        <div className="text-sm font-medium text-[#383F44] leading-[22px]">Max Token</div>
        <div className="flex items-center gap-2">
          <Slider
            min={1}
            max={currentModalMaxToken}
            step={1}
            value={currentTokenVal || 1}
            className="flex-1 m-0"
            onChange={(value: number) => setAgentInfo((pre: AgentVO) => {
              return { ...pre, agent: { ...pre.agent, maxTokens: value } }
            })}
          />
          
          <Input
            type="number"
            min={1}
            max={currentModalMaxToken}
            className="hide-controls w-[60px] h-8 rounded-lg text-center border-[#E0E3E6] ml-2"
            value={currentTokenVal === null ? '' : currentTokenVal}
            onChange={(e) => handleTokensInputChange(e.target.value)}
            onBlur={handleTokensInputBlur}
          />
        </div>
      </div>

      <div className="flex flex-col gap-1">
        <div className="text-sm font-medium text-[#383F44] leading-[22px]">Top P</div>
        <div className="flex items-center gap-2">
          <Slider
            min={0}
            max={1}
            step={0.1}
            value={agent?.topP || 0}
            className="flex-1 m-0"
            onChange={(value: number) => setAgentInfo((pre: AgentVO) => {
              return { ...pre, agent: { ...pre.agent, topP: value } }
            })}
          />
          <Input
            type="number"
            min={0}
            max={1}
            step={0.1}
            className="w-[60px] h-8 rounded-lg text-center border-[#E0E3E6] ml-2"
            value={agent?.topP || 0}
            onChange={(e) => setAgentInfo((pre: AgentVO) => {
              return { ...pre, agent: { ...pre.agent, topP: e.target.value } }
            })}
          />
        </div>
      </div>

      <div className="flex flex-col gap-1">
        <div className="text-sm font-medium text-[#383F44] leading-[22px]">Keep recent turns</div>
        <div className="flex items-center gap-2">
          <Slider
            min={1}
            max={10}
            step={1}
            value={agent?.turns || 5}
            className="flex-1 m-0"
            onChange={(value: number) => setAgentInfo((pre: AgentVO) => {
              return { ...pre, agent: { ...pre.agent, turns: value } }
            })}
          />
          <Input
            type="number"
            min={1}
            max={10}
            step={1}
            className="w-[60px] h-8 rounded-lg text-center border-[#E0E3E6] ml-2"
            value={agent?.turns || 5}
            onChange={(e) => setAgentInfo((pre: AgentVO) => {
              return { ...pre, agent: { ...pre.agent, turns: (Number(e.target.value) > 10) ? 10 : Number(e.target.value) } }
            })}
          />
        </div>
      </div>
    </div>
  );

  return (
    <Popover 
      content={advancedSettingsContent} 
      trigger="click" 
      placement="bottomLeft"
      overlayClassName="custom-popover"
      overlayInnerStyle={{ padding: 0, borderRadius: 8 }}
    >
      <Button
        disabled={!agent?.llmModelId || readonly}
        className={`${readonly ? 'opacity-20' : ''} h-10 border-[#E0E3E6] bg-white/60 text-[#383F44] rounded-xl hover:bg-white transition flex-none`}
      >
        更多
      </Button>
    </Popover>
  );
};



export default AdvancedSettingsPopover;
