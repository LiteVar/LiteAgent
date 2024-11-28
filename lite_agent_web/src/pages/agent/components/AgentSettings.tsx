import React, { useCallback, useState } from 'react';
import {Select, Input, Typography} from 'antd';
import AdvancedSettingsPopover, {DEFAULT_Max_TOKENS} from './AdvancedSettingsPopover';
import {AgentDetailVO, ModelDTO} from "@/client";

const {TextArea} = Input;
const {Text} = Typography;

interface AgentSettingsProps {
  agentInfo: AgentDetailVO;
  modelList: ModelDTO[];
  setAgentInfo: (agent: AgentDetailVO) => void;
}

const AgentSettings: React.FC<AgentSettingsProps> = ({agentInfo, setAgentInfo, modelList}) => {
  const [prompt, setPrompt] = useState(agentInfo.agent?.prompt);

  const onChangeAgentModel = useCallback((value: any) => {
    if (value === "-1") {
      const workspaceId = agentInfo?.agent?.workspaceId;
      window.open(`/workspaces/${workspaceId}/models`, "_blank");
    } else {
      const maxTokens = modelList?.filter(v => v.id === value)[0]?.maxTokens || DEFAULT_Max_TOKENS
      setAgentInfo({...agentInfo, agent: {...agentInfo.agent, llmModelId: value, maxTokens: maxTokens}});
    }
  }, [agentInfo, setAgentInfo])

  return (
    <div>
      <div className="mb-6">
        <Text strong>模型</Text>
        <div className="flex">
          <Select onChange={onChangeAgentModel} value={agentInfo?.agent?.llmModelId} className="w-full mt-2"
          placeholder="请选择大模型">
            {modelList?.map(model => (
              <Select.Option key={model.id} value={model.id}>{model.name}</Select.Option>
            ))}
            <Select.Option value={"-1"} key={"new"}>
              <a href="" onClick={(e) => e.preventDefault()}>创建新模型</a>
            </Select.Option>
          </Select>
          <AdvancedSettingsPopover agent={agentInfo?.agent!} setAgentInfo={setAgentInfo} modelList={modelList}/>
        </div>
      </div>
      <div className="mb-6">
        <Text strong>系统提示词</Text>
        <TextArea
          value={prompt}
          className="mt-2"
          rows={6}
          onChange={(e) => setPrompt(e.target.value)}
          onBlur={() => setAgentInfo({...agentInfo, agent: {...agentInfo.agent, prompt: prompt}})}
        />
      </div>
    </div>
  );
};

export default AgentSettings;
