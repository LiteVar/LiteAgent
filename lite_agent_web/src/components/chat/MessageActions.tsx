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
import SendStopIcon from '@/assets/dashboard/send-stop.png';

const MessageActions: React.FC<MessageActionsProps> = (props) => {
  const { onCopy, show, copied, isAssistant, ttsModelId, ttsStatus = TtsStatus.Init, onTtsClick, onStop, taskId, responding } = props;

  // 渲染TTS按钮
  const renderTtsButton = () => {
    if (!isAssistant || !ttsModelId) return null;
    if (ttsStatus === TtsStatus.Loading) {
      return (
        <Tooltip key="loading" title="点击取消语音生成" placement="bottom">
          <span onClick={onTtsClick} className="relative cursor-pointer rounded">
            <span className="inline-flex items-center justify-center w-7 h-7 hover:bg-gray-100 hover:rounded">
              <LoadingOutlined />
            </span>
          </span>
        </Tooltip>
      );
    }
    if (ttsStatus === TtsStatus.Playing) {
      return (
        <Tooltip key="playing" title="停止播放" placement="bottom">
          <span onClick={onTtsClick} className="relative cursor-pointer rounded">
            <span className="inline-flex items-center justify-center w-7 h-7 hover:bg-gray-100 hover:rounded">
              <PauseCircleOutlined />
            </span>
          </span>
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
    <div className={`flex items-center text-base text-gray-500 ${(show) ? 'opacity-100' : 'opacity-0'}`}>
      <Space size={0}>
        <Tooltip title="复制" placement="bottom">
          <span onClick={onCopy} className="relative cursor-pointer rounded">
            <span className="inline-flex items-center justify-center w-7 h-7 hover:bg-gray-100 hover:rounded">
              {copied ? <CheckOutlined /> : <CopyOutlined />}
            </span>
          </span>
        </Tooltip>
        {!!onStop && !!taskId && !!responding && <Tooltip title="暂停" placement="bottom">
          <span onClick={() => onStop(taskId!)} className="relative cursor-pointer rounded">
            <span className="inline-flex items-center justify-center w-7 h-7 hover:bg-gray-100 hover:rounded">
              <img src={SendStopIcon} alt="暂停" className="w-4 h-4" />
            </span>
          </span>
        </Tooltip>}
        {renderTtsButton()}
      </Space>
    </div>
  );
};

export default MessageActions;
