import { FC, useState, useMemo, useCallback } from 'react';
import { Checkbox, Select, Collapse, Divider } from 'antd';
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
    checked ? onTtsEnableChange?.(selectedTtsModel) : onTtsEnableChange?.('');
  }, [selectedTtsModel]);

  const handleAsrEnableChange = useCallback((e: CheckboxChangeEvent) => {
    const checked = e.target.checked;
    checked ? onAsrEnableChange?.(selectedAsrModel) : onAsrEnableChange?.('');
  }, [selectedAsrModel]);

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
    <div className="convert-container">
      <Divider />
      <Collapse ghost>
        <Panel
          header={<span className="text-base font-medium">语音与文本转化配置</span>}
          key="1"
        >
          <div className="mb-4">
            <div className="flex items-center mb-2">
              <div className="text-gray-500">
                <ExclamationCircleOutlined className='mr-2' />
                开启文字转语音后，AI回复的信息中将显示语音播放功能
                <br />
                开启语音转文字后，可以使用麦克风进行语音输入
              </div>
            </div>

            <div className={`flex py-4 border-b ${readonly ? '' : 'justify-between items-center'}`}>
              <div className="text-base">文字转语音（TTS）</div>
              {!readonly && <Checkbox
                checked={!!ttsModelId}
                onChange={handleTtsEnableChange}
                disabled={!selectedTtsModel || readonly}
              >
                开启
              </Checkbox>}
              {readonly && <div className='ml-9'>
                <div className="text-base flex-none">{ttsModelId ? "已开启" : "未开启"}</div>
                {!!ttsModelId && <div className="text-sm mt-2 text-black/25">{audioModelList?.filter(model => model.id === ttsModelId)[0]?.alias}</div>}
              </div>}
            </div>

            {!readonly && <div className="py-4">
              <Select
                placeholder="请选择 TTS 模型"
                style={{ width: '100%' }}
                options={ttsModels}
                value={selectedTtsModel || undefined}
                onChange={handleTtsModelChange}
              />
            </div>}

            <div className={`flex py-4 border-b ${readonly ? '' : 'justify-between items-center'}`}>
              <div className="text-base">语音转文字（ASR）</div>
              {!readonly && <Checkbox
                checked={!!asrModelId}
                onChange={handleAsrEnableChange}
                disabled={!selectedAsrModel || readonly}
              >
                开启
              </Checkbox>}
              {readonly && <div className='ml-9'>
                <div className="text-base flex-none">{asrModelId ? "已开启" : "未开启"}</div>
                {!!asrModelId && <div className="text-sm mt-2 text-black/25">{audioModelList?.filter(model => model.id === asrModelId)[0]?.alias}</div>}
              </div>}
            </div>

            {!readonly && <div className="py-4">
              <Select
                placeholder="请选择 ASR 模型"
                style={{ width: '100%' }}
                options={asrModels}
                value={selectedAsrModel || undefined}
                onChange={handleAsrModelChange}
              />
            </div>}
          </div>
        </Panel>
      </Collapse>
    </div>
  );
};

export default Convert;