import React, { useCallback, useState } from 'react';
import { Select, Input, Typography, Button } from 'antd';
import AdvancedSettingsPopover, { DEFAULT_Max_TOKENS } from './AdvancedSettingsPopover';
import { AgentDetailVO, ModelDTO } from '@/client';
import { SearchOutlined } from '@ant-design/icons';
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
    [agentInfo, setAgentInfo, modelList]
  );

  const onChangePrompt = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setPrompt(e.target.value);
    setAgentInfo({ ...agentInfo, agent: { ...agentInfo.agent, prompt: e.target.value } })
  }, [agentInfo, setAgentInfo]);

  return (
    <div className="flex flex-col gap-4">
      <div className='pb-4 border-0 border-solid border-b border-b-[#D8E6EF]'>
        <div className="text-[#383F44] font-medium mb-2 flex items-center gap-1">
          模型
        </div>
        <div className="flex items-center gap-2">
          <Select
            variant="filled"
            onChange={onChangeAgentModel}
            value={(agentInfo?.model?.status === 2 && agentInfo?.model?.id === agentInfo?.agent?.llmModelId) ? '' : agentInfo?.agent?.llmModelId}
            className="flex-1 custom-select"
            placeholder="请选择大模型"
            style={{ height: 40 }}
          >
            {modelList.filter((model) => model.status === 1)?.map((model) => (
              <Select.Option key={model.id} value={model.id}>
                {model.alias}
              </Select.Option>
            ))}
            <Select.Option value={'-1'} key={'new'}>
              <span className="text-[#40A5EE]">创建新模型</span>
            </Select.Option>
          </Select>
          <AdvancedSettingsPopover
            agent={agentInfo?.agent!}
            setAgentInfo={setAgentInfo}
            modelList={modelList}
          />
        </div>
        {(agentInfo?.model?.status === 2 && agentInfo?.model?.id === agentInfo?.agent?.llmModelId) && <div className="text-red-500 text-xs mt-2 flex items-center">
          <div className='text-ellipsis overflow-hidden whitespace-nowrap'>{agentInfo?.model?.alias || agentInfo?.model?.name}</div>
          <div className='flex-none'>，模型已停用，请重新选择并保存、发布</div>
        </div>}
      </div>
      {!agentInfo?.agent?.autoAgentFlag && (
        <div className='pb-4 border-0 border-solid border-b border-b-[#D8E6EF]'>
          <div className='flex items-center justify-between mb-2'>
            <div className="text-[#383F44] font-medium">系统提示词</div>
            <Button
              type="link"
              size="small"
              icon={<SearchOutlined />}
              disabled={!prompt}
              onClick={() => setShowPromptPreview(true)}
              className="text-[#40A5EE] hover:text-[#40A5EE]/80 p-0 h-auto"
            >
              提示词预览
            </Button>
          </div>
          <TextArea
            value={prompt}
            className='rounded-xl border-[#E0E3E6] hover:border-[#40A5EE] focus:border-[#40A5EE] text-[#1D4A6B]'
            placeholder='请输入系统提示词'
            rows={6}
            onChange={onChangePrompt}
          />
        </div>
      )}
      {!agentInfo?.agent?.autoAgentFlag && <PromptPreview
        prompt={prompt || ''}
        visible={showPromptPreview}
        onClose={() => setShowPromptPreview(false)}
      />}
    </div>
  );
};



export default AgentBaseSet;
