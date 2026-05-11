import React from 'react';
import { AudioOutlined } from '@ant-design/icons';
import { AgentType, InputMode } from '@/types/chat';
import RecordWave from '@/components/record-wave';

interface ChatInputModeContentProps {
  agentType: AgentType;
  inputMode: InputMode;
  isComposing: boolean;
  onChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  onPaste: (e: React.ClipboardEvent<HTMLTextAreaElement>) => void;
  onSetIsComposing: (value: boolean) => void;
  onSend: () => void;
  onStartRecording: () => void;
  onStopRecording: () => void;
  textareaPlaceholder: string;
  textareaRef: React.RefObject<HTMLTextAreaElement>;
  value: string;
  uploadedFilesCount: number;
}

export const ChatInputModeContent: React.FC<ChatInputModeContentProps> = ({
  agentType,
  inputMode,
  isComposing,
  onChange,
  onPaste,
  onSetIsComposing,
  onSend,
  onStartRecording,
  onStopRecording,
  textareaPlaceholder,
  textareaRef,
  value,
  uploadedFilesCount,
}) => {
  return (
    <>
      {inputMode === InputMode.NORMAL && (
        <textarea
          ref={textareaRef}
          value={value}
          onChange={onChange}
          onPaste={onPaste}
          onCompositionStart={() => onSetIsComposing(true)}
          onCompositionEnd={() => onSetIsComposing(false)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey && !isComposing) {
              e.preventDefault();
              if (value.trim() || uploadedFilesCount > 0) {
                onSend();
              }
            }
          }}
          className="border-0 text-base w-full flex-1 outline-none resize-none overflow-y-auto bg-transparent text-[#1D4A6B] placeholder-[#ACB6BE] min-h-[24px] max-h-[240px]"
          placeholder={textareaPlaceholder}
          disabled={agentType === AgentType.REFLECTION}
        />
      )}

      {inputMode === InputMode.VOICE_INIT && (
        <div
          className="flex justify-center items-center mb-4 rounded-xl bg-white border border-[#E0E3E6] h-12 text-[#1D4A6B] text-sm select-none cursor-pointer hover:bg-gray-50 transition-colors shadow-sm"
          onMouseDown={onStartRecording}
          onTouchStart={onStartRecording}
          onTouchEnd={onStopRecording}
        >
          <AudioOutlined className="mr-2 text-[#40A5EE]" />
          <span>按在此处或者空格说话</span>
        </div>
      )}

      {inputMode === InputMode.VOICE_RECORDING && (
        <div className="flex items-center justify-center rounded-xl bg-white border border-[#40A5EE] h-12 text-[#40A5EE] px-4 mb-4 cursor-pointer shadow-md" onMouseUp={onStopRecording}>
          <RecordWave />
        </div>
      )}
    </>
  );
};

