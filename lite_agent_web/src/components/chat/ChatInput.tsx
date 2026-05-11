import React, { useRef, useEffect, useState, useCallback } from 'react';
import { Button, Dropdown } from 'antd';
import { AudioOutlined } from '@ant-design/icons';
import { ChatInputProps, InputMode } from '@/types/chat';
import { useChatFileUpload } from '@/hooks/chat/useChatFileUpload';
import { useChatInputAudio } from './chatInput/useChatInputAudio';
import { useChatInputComposer } from './chatInput/useChatInputComposer';
import { UploadedFilesPreview } from './chatInput/UploadedFilesPreview';
import { ChatInputModeContent } from './chatInput/ChatInputModeContent';

const ChatInput: React.FC<ChatInputProps> = ({ 
  value, 
  mode, 
  agentType,
  agentId,
  onChange, 
  onSend, 
  setAsrLoading,
  asrModelId,
  asrStreamSupported = false // 默认不支持流式
}) => {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const videoInputRef = useRef<HTMLInputElement>(null);
  const [inputMode, setInputMode] = useState<InputMode>(InputMode.NORMAL);
  const [isComposing, setIsComposing] = useState(false);
  // 用于防止长按空格多次触发
  const isSpacePressedRef = useRef(false);
  
  // 使用文件上传 hook
  const {
    uploadedFiles,
    uploading,
    handleFileSelect,
    handleRemoveFile,
    buildFileMessages,
    clearFiles,
    handleUrlUpload,
    detectUrlType,
  } = useChatFileUpload();

  const {
    startRecording,
    stopRecording,
  } = useChatInputAudio({
    agentId,
    asrModelId,
    asrStreamSupported,
    onChange,
    onSend,
    setAsrLoading,
    setInputMode,
  });

  const {
    disabledUpload,
    handlePaste,
    handleSend,
    isReflectionAgent,
    menuItems,
    textareaPlaceholder,
  } = useChatInputComposer({
    value,
    onChange,
    onSend,
    agentType,
    inputMode,
    uploading,
    uploadedFiles,
    buildFileMessages,
    clearFiles,
    handleFileSelect,
    handleUrlUpload,
    detectUrlType,
    textareaRef,
    fileInputRef,
    videoInputRef,
  });

  // 组件卸载时清理所有本地预览URL
  useEffect(() => {
    return () => {
      uploadedFiles.forEach((file) => {
        URL.revokeObjectURL(file.url);
        if (file.thumbnail && file.thumbnail !== file.url) {
          if (file.thumbnail.startsWith('blob:')) {
            URL.revokeObjectURL(file.thumbnail);
          }
        }
      });
    };
  }, [uploadedFiles]);

  const handleVoiceMouseUp = useCallback(() => {
    stopRecording();
    document.removeEventListener('mouseup', handleVoiceMouseUp);
  }, [stopRecording]);

  const handleVoiceMouseDown = useCallback(() => {
    // 绑定全局 mouseup，确保移出区域也能结束
    document.addEventListener('mouseup', handleVoiceMouseUp);
    startRecording();
  }, [handleVoiceMouseUp, startRecording]);

  useEffect(() => {
    if (inputMode !== InputMode.VOICE_INIT && 
      inputMode !== InputMode.VOICE_RECORDING
    ) {
      isSpacePressedRef.current = false;
      return;
    }

    const handleKeyDown = (e: KeyboardEvent) => {
      // 空格键，且不是输入框等元素聚焦时
      if (
        e.code === 'Space' &&
        !isSpacePressedRef.current &&
        !(document.activeElement && ['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName))
      ) {
        isSpacePressedRef.current = true;
        e.preventDefault();
        startRecording();
      }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      if (e.code === 'Space' && isSpacePressedRef.current) {
        isSpacePressedRef.current = false;
        e.preventDefault();
        stopRecording();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
      isSpacePressedRef.current = false;
    };
  }, [inputMode, startRecording, stopRecording]);

  return (
    <div className={`rounded-2xl bg-white p-4 mx-6 shadow-sm border border-white
      ${mode === 'dev' ? 'w-[calc(100%-80px)]' : 'w-[760px]'}`}
    >
      {/* 隐藏的文件输入 */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        multiple
        className="hidden"
        onChange={(e) => handleFileSelect(e, 'image')}
      />
      <input
        ref={videoInputRef}
        type="file"
        accept="video/*"
        className="hidden"
        onChange={(e) => handleFileSelect(e, 'video')}
      />

      <UploadedFilesPreview
        uploadedFiles={uploadedFiles}
        onRemoveFile={handleRemoveFile}
      />

      <ChatInputModeContent
        agentType={agentType}
        inputMode={inputMode}
        isComposing={isComposing}
        onChange={onChange}
        onPaste={handlePaste}
        onSetIsComposing={setIsComposing}
        onSend={() => handleSend('text')}
        onStartRecording={handleVoiceMouseDown}
        onStopRecording={stopRecording}
        textareaPlaceholder={textareaPlaceholder}
        textareaRef={textareaRef}
        value={value}
        uploadedFilesCount={uploadedFiles.length}
      />

      <div className="flex justify-between items-center w-full mt-2">
        <div className="flex items-center gap-2">
          <Dropdown
            menu={{ items: menuItems }}
            trigger={['click']}
            placement="topLeft"
            disabled={disabledUpload}
          >
            <div className={`w-9 h-9 flex items-center justify-center rounded-xl hover:bg-gray-100 transition-colors ${disabledUpload ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}>
              <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M16 3C13.4288 3 10.9154 3.76244 8.77759 5.1909C6.63975 6.61935 4.97351 8.64968 3.98957 11.0251C3.00563 13.4006 2.74819 16.0144 3.2498 18.5362C3.75141 21.0579 4.98953 23.3743 6.80762 25.1924C8.6257 27.0105 10.9421 28.2486 13.4638 28.7502C15.9856 29.2518 18.5995 28.9944 20.9749 28.0104C23.3503 27.0265 25.3807 25.3603 26.8091 23.2224C28.2376 21.0846 29 18.5712 29 16C28.9964 12.5533 27.6256 9.24882 25.1884 6.81163C22.7512 4.37445 19.4467 3.00364 16 3ZM16 27C13.8244 27 11.6977 26.3549 9.88873 25.1462C8.07979 23.9375 6.66989 22.2195 5.83733 20.2095C5.00477 18.1995 4.78693 15.9878 5.21137 13.854C5.63581 11.7202 6.68345 9.7602 8.22183 8.22183C9.76021 6.68345 11.7202 5.6358 13.854 5.21136C15.9878 4.78692 18.1995 5.00476 20.2095 5.83733C22.2195 6.66989 23.9375 8.07979 25.1462 9.88873C26.3549 11.6977 27 13.8244 27 16C26.9967 18.9164 25.8367 21.7123 23.7745 23.7745C21.7123 25.8367 18.9164 26.9967 16 27ZM22 16C22 16.2652 21.8946 16.5196 21.7071 16.7071C21.5196 16.8946 21.2652 17 21 17H17V21C17 21.2652 16.8946 21.5196 16.7071 21.7071C16.5196 21.8946 16.2652 22 16 22C15.7348 22 15.4804 21.8946 15.2929 21.7071C15.1054 21.5196 15 21.2652 15 21V17H11C10.7348 17 10.4804 16.8946 10.2929 16.7071C10.1054 16.5196 10 16.2652 10 16C10 15.7348 10.1054 15.4804 10.2929 15.2929C10.4804 15.1054 10.7348 15 11 15H15V11C15 10.7348 15.1054 10.4804 15.2929 10.2929C15.4804 10.1054 15.7348 10 16 10C16.2652 10 16.5196 10.1054 16.7071 10.2929C16.8946 10.4804 17 10.7348 17 11V15H21C21.2652 15 21.5196 15.1054 21.7071 15.2929C21.8946 15.4804 22 15.7348 22 16Z" fill="#383F44"/>
              </svg>
            </div>
          </Dropdown>
          {(asrModelId && !isReflectionAgent && inputMode === InputMode.NORMAL) && (
            <div
              onClick={() => setInputMode(InputMode.VOICE_INIT)}
              className="w-9 h-9 flex items-center justify-center rounded-xl bg-[#F5F7F9] cursor-pointer hover:bg-gray-100 transition-colors"
            >
              <AudioOutlined className="text-[#58636C]" />
            </div>
          )}
        </div>

        {inputMode === InputMode.NORMAL ? (
          <div className="flex items-center">
            <button
              onClick={() => handleSend('text')}
              disabled={!value.trim() && uploadedFiles.length === 0}
              className={`flex-none border-none w-9 h-9 p-0 flex items-center justify-center bg-transparent transition-transform active:scale-90 ${
                (!value.trim() && uploadedFiles.length === 0) ? 'cursor-not-allowed grayscale' : 'cursor-pointer hover:brightness-110'
              }`}
            >
              <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M16 3C13.4288 3 10.9154 3.76244 8.77759 5.1909C6.63975 6.61935 4.97351 8.64968 3.98957 11.0251C3.00563 13.4006 2.74819 16.0144 3.2498 18.5362C3.75141 21.0579 4.98953 23.3743 6.80762 25.1924C8.6257 27.0105 10.9421 28.2486 13.4638 28.7502C15.9856 29.2518 18.5995 28.9944 20.9749 28.0104C23.3503 27.0265 25.3807 25.3603 26.8091 23.2224C28.2376 21.0846 29 18.5712 29 16C28.9964 12.5533 27.6256 9.24882 25.1884 6.81163C22.7512 4.37445 19.4467 3.00364 16 3ZM20.7075 15.7075C20.6146 15.8005 20.5043 15.8742 20.3829 15.9246C20.2615 15.9749 20.1314 16.0008 20 16.0008C19.8686 16.0008 19.7385 15.9749 19.6171 15.9246C19.4957 15.8742 19.3854 15.8005 19.2925 15.7075L17 13.4137V21C17 21.2652 16.8946 21.5196 16.7071 21.7071C16.5196 21.8946 16.2652 22 16 22C15.7348 22 15.4804 21.8946 15.2929 21.7071C15.1054 21.5196 15 21.2652 15 21V13.4137L12.7075 15.7075C12.5199 15.8951 12.2654 16.0006 12 16.0006C11.7346 16.0006 11.4801 15.8951 11.2925 15.7075C11.1049 15.5199 10.9994 15.2654 10.9994 15C10.9994 14.7346 11.1049 14.4801 11.2925 14.2925L15.2925 10.2925C15.3854 10.1995 15.4957 10.1258 15.6171 10.0754C15.7385 10.0251 15.8686 9.99921 16 9.99921C16.1314 9.99921 16.2615 10.0251 16.3829 10.0754C16.5043 10.1258 16.6146 10.1995 16.7075 10.2925L20.7075 14.2925C20.8005 14.3854 20.8742 14.4957 20.9246 14.6171C20.9749 14.7385 21.0008 14.8686 21.0008 15C21.0008 15.1314 20.9749 15.2615 20.9246 15.3829C20.8742 15.5043 20.8005 15.6146 20.7075 15.7075Z" fill={(!value.trim() && uploadedFiles.length === 0) ? "#E0E3E6" : "#40A5EE"}/>
              </svg>
            </button>
          </div>
        ) : (
          <Button
            type="text"
            onClick={() => setInputMode(InputMode.NORMAL)}
            className="text-[#40A5EE] font-medium"
          >
            键盘输入
          </Button>
        )}
      </div>
    </div>
  );

};

export default ChatInput;
