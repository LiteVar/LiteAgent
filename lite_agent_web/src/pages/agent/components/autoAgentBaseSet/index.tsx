import React, { useCallback } from 'react';
import { Divider, Select, Typography } from 'antd';
import AdvancedSettingsPopover, { DEFAULT_Max_TOKENS } from '../AdvancedSettingsPopover';
import { AgentDetailVO, ModelDTO } from '@/client';

const { Text } = Typography;

interface AgentSettingsProps {
  agentInfo: AgentDetailVO;
  readonly?: boolean;
  modelList: ModelDTO[];
  setAgentInfo?: (agent: AgentDetailVO) => void;
}

const AutoAgentBaseSet: React.FC<AgentSettingsProps> = ({ agentInfo, setAgentInfo, modelList, readonly }) => {

  const onChangeAgentModel = useCallback(
    (value: any) => {
      if (value === '-1') {
        const workspaceId = agentInfo?.agent?.workspaceId;
        window.open(`/workspaces/${workspaceId}/models`, '_blank');
      } else {
        const maxTokens = modelList?.filter((v) => v.id === value)[0]?.maxTokens || DEFAULT_Max_TOKENS;
        setAgentInfo?.({
          ...agentInfo,
          agent: { ...agentInfo.agent, llmModelId: value, maxTokens: maxTokens },
        });
      }
    },
    [agentInfo, setAgentInfo]
  );

  return (
    <div>
      <Divider />
      <div className="mb-6">
        <Text className="text-base mb-2" strong>系统模型</Text>
        <div className="text-base text-gray-500 mb-2">负责规划的大语言模型，统筹所有信息</div>
        <div className="flex">
          <Select
            disabled={readonly}
            variant="filled"
            onChange={onChangeAgentModel}
            value={agentInfo?.agent?.llmModelId}
            className="w-full mt-2"
            placeholder="请选择大模型"
          >
            {modelList?.map((model) => (
              <Select.Option key={model.id} value={model.id}>
                {model.alias}
              </Select.Option>
            ))}
            <Select.Option value={'-1'} key={'new'}>
              <a href="" onClick={(e) => e.preventDefault()}>
                创建新模型
              </a>
            </Select.Option>
          </Select>
          <AdvancedSettingsPopover
            agent={agentInfo?.agent!}
            setAgentInfo={setAgentInfo}
            modelList={modelList}
            readonly={readonly}
          />
        </div>
      </div>
    </div>
  );
};

export default AutoAgentBaseSet;
