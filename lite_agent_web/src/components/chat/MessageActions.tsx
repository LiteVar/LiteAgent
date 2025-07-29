import React from 'react';
import { 
  CheckOutlined, 
  CopyOutlined, 
  SoundOutlined, 
  LoadingOutlined,
  PauseCircleOutlined,
} from '@ant-design/icons';
import { Space, Tooltip } from 'antd';

// export type TtsStatus = 'init' | 'loading' | 'playing';
export enum TtsStatus {
  Init = 'init',
  Loading = 'loading',
  Playing = 'playing',
}

interface MessageActionsProps {
  onCopy: () => void;
  onRetry: () => void;
  show: boolean;
  copied: boolean;
  retryDisabled?: boolean;
  showRetry?: boolean;
  isAssistant?: boolean;
  ttsModelId?: string;
  ttsStatus?: TtsStatus; 
  onTtsClick?: () => void; 
}

const MessageActions: React.FC<MessageActionsProps> = (props) => {
  const { 
    onCopy, 
    show, 
    copied, 
    onRetry, 
    retryDisabled, 
    showRetry, 
    isAssistant,
    ttsModelId,
    ttsStatus = TtsStatus.Init,
    onTtsClick, 
  } = props;

  // 渲染TTS按钮
  const renderTtsButton = () => {
    if (!isAssistant || !ttsModelId) return null;
    if (ttsStatus === TtsStatus.Loading) {
      return (
        <Tooltip title="语音生成中..." placement="bottom">
          <LoadingOutlined className="cursor-default" />
        </Tooltip>
      );
    }
    if (ttsStatus === TtsStatus.Playing) {
      return (
        <Tooltip title="停止播放" placement="bottom">
          <PauseCircleOutlined className="cursor-pointer" onClick={onTtsClick} />
        </Tooltip>
      );
    }
    // init
    return (
      <Tooltip title="播放语音" placement="bottom">
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
        {/* {showRetry && (
          <Tooltip title={retryDisabled ? '' : '重试'} placement="bottom">
            <span
              onClick={() => !retryDisabled && onRetry()}
              className={`inline-flex items-center justify-center w-7 h-7 hover:bg-gray-100 hover:rounded 
                  ${retryDisabled ? 'cursor-not-allowed' : 'cursor-pointer'}`}
            >
              <SyncOutlined />
            </span>
          </Tooltip>
        )} */}
      </Space>
    </div>
  );
};

export default MessageActions;
