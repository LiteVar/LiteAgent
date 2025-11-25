import React, { useRef, useEffect, useMemo, useState, useCallback } from 'react';
import { Space, Divider, Button, message } from 'antd';
import { PlusOutlined, AudioOutlined } from '@ant-design/icons';
import sendSvg from '@/assets/dashboard/send-normal.png';
import sendDisableSvg from '@/assets/dashboard/send-disable.png';
import ResponseCode from '@/constants/ResponseCode';
import RecordWave from '../record-wave';
import { ChatInputProps, InputMode, AgentType } from '@/types/chat';
import { getAccessToken } from '@/utils/cache';

interface TranscriptionResponse {
  code: number;
  message: string;
  data: string;
}

const ChatInput: React.FC<ChatInputProps> = ({ 
  value, 
  mode, 
  agentType, 
  onChange, 
  onSend, 
  setAsrLoading,
  asrModelId 
}) => {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const [inputMode, setInputMode] = useState<InputMode>(InputMode.NORMAL);
  const [isComposing, setIsComposing] = useState(false);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  // 用于防止长按空格多次触发
  const isSpacePressedRef = useRef(false);

  const isReflectionAgent = useMemo(() => agentType === AgentType.REFLECTION, [agentType]);

  const textareaPlaceholder = useMemo(() => {
    return isReflectionAgent ? '反思 Agent 不能进行聊天对话' : '请输入聊天内容';
  }, [isReflectionAgent]);

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = textareaRef.current.scrollHeight + 'px';
    }
  }, [value]);

  useEffect(() => {
    if (agentType === AgentType.REFLECTION && textareaRef.current) {
      onChange({ target: { value: '' } } as React.ChangeEvent<HTMLTextAreaElement>);
    }
  }, [agentType, onChange]);

  // 语音转文字
  const handleConvertAudio = useCallback(async () => {

    try {
      if (audioChunksRef.current.length === 0) {
        message.warning('未检测到语音内容');
        setInputMode(InputMode.VOICE_INIT);
        setAsrLoading(false);
        return;
      }
      setAsrLoading(true);

      const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/mp3' });
      
      // 使用 FormData 来上传音频文件
      const formData = new FormData();
      formData.append('audio', audioBlob, 'audio.mp3');
      
      // 获取访问令牌
      const token = getAccessToken();
      
      // 添加 30 秒超时处理
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => {
          reject(new Error('语音转文字请求超时'));
        }, 1000 * 30);
      });

      // 使用 fetch 发送 multipart/form-data 请求
      const apiPromise = fetch(`/v1/chat/audio/transcriptions?modelId=${asrModelId}`, {
        method: 'POST',
        body: formData,
        headers: {
          Authorization: `Bearer ${token}`,
        },
        // 不要手动设置 Content-Type，让浏览器自动设置（包括 boundary）
      }).then(response => response.json());

      const res = await Promise.race([apiPromise, timeoutPromise]) as TranscriptionResponse;

      // fetch 返回的数据结构是 { code, message, data }
      if (res.code !== ResponseCode.S_OK) {
        message.error(res.message || '语音转化失败');
        setInputMode(InputMode.VOICE_INIT);
        setAsrLoading(false);
        return;
      }

      const rawData = res.data || '';
      const parsed = JSON.parse(rawData);
      // 如果解析后有 text 字段且内容则判定为有效语音内容
      if (typeof parsed === 'object' && parsed !== null && typeof parsed.text === 'string') {
        setInputMode(InputMode.VOICE_INIT);

        const text = parsed.text.trim();
        setTimeout(() => {
          setAsrLoading(false);
          onSend('text', text);
        }, 1000);
      } else {
        message.warning('未检测到有效语音内容');
        setAsrLoading(false);
        setInputMode(InputMode.VOICE_INIT);
      }
    } catch (e) {
      console.log(e);
      message.error('网络异常，语音转化失败');
      setAsrLoading(false);
      setInputMode(InputMode.VOICE_INIT);
    }
  }, [asrModelId, onSend, setAsrLoading]);

  // 语音录制
  const startRecording = useCallback(async () => {
    try {
      console.log('开始录音');
      setInputMode(InputMode.VOICE_RECORDING);

      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mediaRecorder = new window.MediaRecorder(stream);
      mediaRecorderRef.current = mediaRecorder;
      audioChunksRef.current = [];

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorder.onstop = () => {
        console.log('录音结束');
        handleConvertAudio();
      };

      mediaRecorder.start();
      
    } catch (err) {
      console.error('录音错误', err);
      message.warning('无法访问麦克风，请检查麦克风权限');
    }
  }, [handleConvertAudio]);

  const stopRecording = useCallback(() => {
    setInputMode(InputMode.VOICE_INIT);

    if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'recording') {
      mediaRecorderRef.current.stop();
      mediaRecorderRef.current.stream.getTracks().forEach((track) => track.stop());
    }
  }, []);

  // 语音输入区域鼠标按下时的处理
  const handleVoiceMouseDown = () => {
    // 绑定全局 mouseup，确保移出区域也能结束
    document.addEventListener('mouseup', handleVoiceMouseUp);
    startRecording();
  };

  // 鼠标松开时的处理
  const handleVoiceMouseUp = () => {
    stopRecording();
    document.removeEventListener('mouseup', handleVoiceMouseUp);
  };

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
    <div className={`rounded-[16px] bg-[#f5f5f5] p-3 m-4 
      ${mode === 'dev' ? 'w-full' : 'w-[806px]'}`}
    >
      {inputMode === InputMode.NORMAL && (
        <textarea
          ref={textareaRef}
          value={value}
          onChange={onChange}
          onCompositionStart={() => setIsComposing(true)}
          onCompositionEnd={() => setIsComposing(false)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey && !isComposing) {
              e.preventDefault();
              if (value.trim()) onSend('text');
            }
          }}
          className="border-0 text-[16px] w-full flex-1 outline-none resize-none overflow-y-auto rounded-[8px]"
          placeholder={textareaPlaceholder}
          style={{ minHeight: '24px', maxHeight: '240px', background: 'transparent' }}
          disabled={agentType === AgentType.REFLECTION}
        />
      )}

      {inputMode === InputMode.VOICE_INIT && (
        <div
          className="flex justify-center items-center mb-5  rounded-[10px] bg-[#dedede] h-10 text-gray-600 text-sm select-none cursor-pointer"
          onMouseDown={handleVoiceMouseDown}
          onTouchStart={startRecording}
          onTouchEnd={stopRecording}
        >
          <span>按在此处或者空格说话</span>
        </div>
      )}

      {inputMode === InputMode.VOICE_RECORDING && (
        <div className="flex items-center rounded-[10px] h-10 text-gray-600 px-4 mb-5 cursor-pointer" onMouseUp={stopRecording}>
          <RecordWave />
        </div>
      )}

      <div className="flex justify-between  w-full">
        <Space size={2} split={<Divider type="vertical" />}>
          <Button 
            icon={<PlusOutlined />} 
            shape="circle"  
            style={{ 
              borderRadius: '30%',
              width: '36px',
              height: '36px'
            }}
          />
        </Space>
        {inputMode === InputMode.NORMAL ? (
          <Space size={12}>
            {(asrModelId && !isReflectionAgent) && (
              <Button 
                icon={<AudioOutlined style={{ fontSize: '16px' }} />} 
                onClick={() => setInputMode(InputMode.VOICE_INIT)} 
                shape="circle" 
                style={{ 
                  borderRadius: '30%',
                  width: '36px',
                  height: '36px'
                }}
              />
            )}
            <button
              onClick={() => onSend('text')}
              disabled={!value.trim()}
              className={`flex-none border-none w-8 h-8 flex items-center justify-center bg-inherit ${
                !value.trim() ? 'cursor-not-allowed' : 'cursor-pointer'
              }`}
            >
              {!value.trim() ? (
                <img src={sendDisableSvg} alt="send" className="w-8" />
              ) : (
                <img src={sendSvg} alt="send" className="w-8" />
              )}
            </button>
          </Space>
        ) : (
          <Button type="primary" ghost onClick={() => setInputMode(InputMode.NORMAL)}>
            键盘
          </Button>
        )}
      </div>
    </div>
  );
};

export default ChatInput;
