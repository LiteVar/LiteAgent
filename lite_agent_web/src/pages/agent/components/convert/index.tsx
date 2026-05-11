import { FC, useState, useMemo, useCallback } from 'react';
import { Checkbox, Select, Collapse } from 'antd';
import type { CheckboxChangeEvent } from 'antd/es/checkbox';
import { ExclamationCircleOutlined } from '@ant-design/icons';
import type { SelectProps } from 'antd';

import { AgentDetailVO, ModelDTO } from '@/client';

const { Panel } = Collapse;

enum ModelType {
  LLM = 'LLM',
  EMBEDDING = 'embedding',
  TTS = 'tts',
  ASR = 'asr'
}

interface ConvertProps {
  readonly?: boolean;
  agentInfo: AgentDetailVO;
  audioModelList: ModelDTO[];
  onTtsEnableChange?: (ttsModelId: string) => void;
  onAsrEnableChange?: (asrModelId: string) => void;
}

const Convert: FC<ConvertProps> = ({
  readonly,
  agentInfo,
  audioModelList = [],
  onTtsEnableChange,
  onAsrEnableChange,
}) => {

  const ttsModelId = useMemo(() => {
    return agentInfo?.agent?.ttsModelId || '';
  }, [agentInfo]);

  const asrModelId = useMemo(() => {
    return agentInfo?.agent?.asrModelId || '';
  }, [agentInfo]);

  const [selectedTtsModel, setSelectedTtsModel] = useState(ttsModelId);
  const [selectedAsrModel, setSelectedAsrModel] = useState(asrModelId);

  const ttsModels: SelectProps['options'] = useMemo(() => {
    return audioModelList.filter(
      model => model.type === ModelType.TTS
    ).map(
      model => ({
        value: model.id,
        label: model.alias,
      })
    )
  }, [audioModelList]);

  const asrModels: SelectProps['options'] = useMemo(() => {
    return audioModelList.filter(
      model => model.type === ModelType.ASR
    ).map(
      model => ({
        value: model.id,
        label: model.alias,
      })
    )
  }, [audioModelList]);

  const handleTtsEnableChange = useCallback((e: CheckboxChangeEvent) => {
    const checked = e.target.checked;
    if (checked) {
      onTtsEnableChange?.(selectedTtsModel);
      return;
    }
    onTtsEnableChange?.('');
  }, [selectedTtsModel, onTtsEnableChange]);

  const handleAsrEnableChange = useCallback((e: CheckboxChangeEvent) => {
    const checked = e.target.checked;
    if (checked) {
      onAsrEnableChange?.(selectedAsrModel);
      return;
    }
    onAsrEnableChange?.('');
  }, [selectedAsrModel, onAsrEnableChange]);

  const handleTtsModelChange = useCallback((value: string) => {
    setSelectedTtsModel(value);
    if (ttsModelId) {
      onTtsEnableChange?.(value);
    }
  }, [ttsModelId, onTtsEnableChange]);

  const handleAsrModelChange = useCallback((value: string) => {
    setSelectedAsrModel(value);
    if (asrModelId) {
      onAsrEnableChange?.(value);
    }
  }, [asrModelId, onAsrEnableChange]);

  return (
    <div className="border-t border-white/20 mt-4">
      <Collapse ghost>
        <Panel
          className='[&_.ant-collapse-content-box]:px-0'
          header={<span className="text-base font-medium text-[#383F44]">语音配置</span>}
          key="1"
        >
          <div className="flex flex-col gap-2">
            <div className="flex items-start gap-2 text-xs text-[#7C8B98] leading-relaxed">
              <ExclamationCircleOutlined className="mt-0.5" />
              <span>开启文字转语音后，Ai回复的信息中将显示语音播放功能。开启语音转文字后，可以使用麦克风进行语音输入</span>
            </div>

            <div className="flex flex-col gap-2">
              {/* TTS Section */}
              <div className="rounded-xl p-4">
                <div className="flex justify-between items-center mb-3">
                  <div className="text-sm font-medium text-[#1D4A6B]">文字转语音 (TTS)</div>
                  {!readonly && (
                    <Checkbox
                      checked={!!ttsModelId}
                      onChange={handleTtsEnableChange}
                      disabled={!selectedTtsModel}
                      className="custom-checkbox [&_.ant-checkbox-inner]:rounded-sm [&_.ant-checkbox-inner]:bg-[#F5F5F5] [&_.ant-checkbox-inner]:border-[#D9D9D9] [&_.ant-checkbox-checked_.ant-checkbox-inner]:!bg-[#1677FF] [&_.ant-checkbox-checked_.ant-checkbox-inner]:!border-[#1677FF] [&_.ant-checkbox-wrapper:hover_.ant-checkbox-checked_.ant-checkbox-inner]:!bg-[#1677FF] [&_.ant-checkbox-wrapper:hover_.ant-checkbox-checked_.ant-checkbox-inner]:!border-[#1677FF]"
                    >
                      开启
                    </Checkbox>
                  )}
                </div>
                {!readonly ? (
                  <Select
                    placeholder="请选择 TTS 模型"
                    className="w-full custom-select [&_.ant-select-selector]:bg-[#F5F5F5]"
                    style={{ height: 32 }}
                    options={ttsModels}
                    value={selectedTtsModel || undefined}
                    onChange={handleTtsModelChange}
                  />
                ) : (
                  <div className="text-xs text-[#7C8B98]">
                    {ttsModelId ? audioModelList.find(m => m.id === ttsModelId)?.alias : '未开启'}
                  </div>
                )}
              </div>

              {/* ASR Section */}
              <div className="rounded-xl p-4">
                <div className="flex justify-between items-center mb-3">
                  <div className="text-sm font-medium text-[#1D4A6B]">语音转文字 (ASR)</div>
                  {!readonly && (
                    <Checkbox
                      checked={!!asrModelId}
                      onChange={handleAsrEnableChange}
                      disabled={!selectedAsrModel}
                      className="custom-checkbox [&_.ant-checkbox-inner]:rounded-sm [&_.ant-checkbox-inner]:bg-[#F5F5F5] [&_.ant-checkbox-inner]:border-[#D9D9D9] [&_.ant-checkbox-checked_.ant-checkbox-inner]:!bg-[#1677FF] [&_.ant-checkbox-checked_.ant-checkbox-inner]:!border-[#1677FF] [&_.ant-checkbox-wrapper:hover_.ant-checkbox-checked_.ant-checkbox-inner]:!bg-[#1677FF] [&_.ant-checkbox-wrapper:hover_.ant-checkbox-checked_.ant-checkbox-inner]:!border-[#1677FF]"
                    >
                      开启
                    </Checkbox>
                  )}
                </div>
                {!readonly ? (
                  <Select
                    placeholder="请选择 ASR 模型"
                    className="w-full custom-select [&_.ant-select-selector]:bg-[#F5F5F5]"
                    style={{ height: 32 }}
                    options={asrModels}
                    value={selectedAsrModel || undefined}
                    onChange={handleAsrModelChange}
                  />
                ) : (
                  <div className="text-xs text-[#7C8B98]">
                    {asrModelId ? audioModelList.find(m => m.id === asrModelId)?.alias : '未开启'}
                  </div>
                )}
              </div>
            </div>
          </div>
        </Panel>
      </Collapse>
    </div>
  );
};



export default Convert;
