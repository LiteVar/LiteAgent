import React, { useCallback, useState } from 'react';
import { Select, Input, Typography, Button, Modal } from 'antd';
import AdvancedSettingsPopover, { DEFAULT_Max_TOKENS } from './AdvancedSettingsPopover';
import { AgentDetailVO, ModelDTO } from '@/client';
import { SearchOutlined } from '@ant-design/icons';
import { marked } from 'marked';
import PromptPreview from './modal/promptPreview';

const { TextArea } = Input;
const { Text } = Typography;

interface AgentSettingsProps {
  agentInfo: AgentDetailVO;
  modelList: ModelDTO[];
  setAgentInfo: (agent: AgentDetailVO) => void;
}

const AgentBaseSet: React.FC<AgentSettingsProps> = ({ agentInfo, setAgentInfo, modelList }) => {
  const [prompt, setPrompt] = useState(agentInfo.agent?.prompt);
  const [showPromptPreview, setShowPromptPreview] = useState(false);

  const onChangeAgentModel = useCallback(
    (value: any) => {
      if (value === '-1') {
        const workspaceId = agentInfo?.agent?.workspaceId;
        window.open(`/workspaces/${workspaceId}/models`, '_blank');
      } else {
        const maxTokens = modelList?.filter((v) => v.id === value)[0]?.maxTokens || DEFAULT_Max_TOKENS;
        setAgentInfo({
          ...agentInfo,
          agent: { ...agentInfo.agent, llmModelId: value, maxTokens: maxTokens },
        });
      }
    },
    [agentInfo, setAgentInfo]
  );

  const onChangePrompt = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setPrompt(e.target.value);
    setAgentInfo({ ...agentInfo, agent: { ...agentInfo.agent, prompt: e.target.value } })
  }, [agentInfo, agentInfo?.agent?.prompt]);

  return (
    <div>
      <div className="mb-6">
        <Text strong>模型</Text>
        <div className="flex">
          <Select
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
          />
        </div>
      </div>
      {!agentInfo?.agent?.autoAgentFlag && <div className='mb-6'>
        <div className='flex items-center justify-between'>
          <Text strong>系统提示词</Text>
          <Button
            color='primary'
            variant='filled'
            icon={<SearchOutlined />}
            disabled={!prompt}
            onClick={() => setShowPromptPreview(true)}
          >
            提示词预览
          </Button>
        </div>
        <TextArea
          value={prompt}
          className='mt-2'
          placeholder='请输入系统提示词'
          rows={6}
          onChange={onChangePrompt}
        />
      </div>}
      {!agentInfo?.agent?.autoAgentFlag && <PromptPreview
        prompt={prompt || ''}
        visible={showPromptPreview}
        onClose={() => setShowPromptPreview(false)}
      />}
    </div>
  );
};

export default AgentBaseSet;
