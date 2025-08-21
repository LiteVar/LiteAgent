import React from 'react';
import {
  CheckOutlined,
  CopyOutlined,
  SoundOutlined,
  LoadingOutlined,
  PauseCircleOutlined,
} from '@ant-design/icons';
import { Space, Tooltip } from 'antd';
import { MessageActionsProps, TtsStatus } from '@/types/chat';

const MessageActions: React.FC<MessageActionsProps> = (props) => {
  const { onCopy, show, copied, isAssistant, ttsModelId, ttsStatus = TtsStatus.Init, onTtsClick } = props;

  // 渲染TTS按钮
  const renderTtsButton = () => {
    if (!isAssistant || !ttsModelId) return null;
    if (ttsStatus === TtsStatus.Loading) {
      return (
        <Tooltip key="loading" title="语音生成中..." placement="bottom">
          <LoadingOutlined className="cursor-default" />
        </Tooltip>
      );
    }
    if (ttsStatus === TtsStatus.Playing) {
      return (
        <Tooltip key="playing" title="停止播放" placement="bottom">
          <PauseCircleOutlined className="cursor-pointer" onClick={onTtsClick} />
        </Tooltip>
      );
    }
    // init
    return (
      <Tooltip key="init" title="播放语音" placement="bottom">
        <span className="relative cursor-pointer rounded">
          <span className="inline-flex items-center justify-center w-7 h-7 hover:bg-gray-100 hover:rounded">
            <SoundOutlined className="cursor-pointer" onClick={onTtsClick} />
          </span>
        </span>
      </Tooltip>
    );
  };

  return (
    <div className={`flex items-center text-base text-gray-500 ${show ? 'opacity-100' : 'opacity-0'}`}>
      <Space size={20}>
        <Tooltip title="复制" placement="bottom">
          <span onClick={onCopy} className="relative cursor-pointer rounded">
            <span className="inline-flex items-center justify-center w-7 h-7 hover:bg-gray-100 hover:rounded">
              {copied ? <CheckOutlined /> : <CopyOutlined />}
            </span>
          </span>
        </Tooltip>
        {renderTtsButton()}
      </Space>
    </div>
  );
};

export default MessageActions;
