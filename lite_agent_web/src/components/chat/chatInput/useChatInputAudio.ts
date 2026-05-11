import { useCallback, useRef } from 'react';
import { message } from 'antd';
import { EventSourceMessage, fetchEventSource } from '@microsoft/fetch-event-source';
import { getAccessToken } from '@/utils/cache';
import ResponseCode from '@/constants/ResponseCode';
import { useChatContext } from '@/contexts/ChatContext';
import { InputMode } from '@/types/chat';
import { ChatMessageItem } from '@/hooks/chat/useChatSSE';
import {
  shouldAutoSendStreamAsrText,
  UNSUPPORT_CONVERSION_CODE,
  VOICE_CONVERSION_FAIL,
} from './utils';

interface UseChatInputAudioParams {
  agentId: string;
  asrModelId: string;
  asrStreamSupported: boolean;
  onChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  onSend: (messages: ChatMessageItem[]) => void;
  setAsrLoading: (value: boolean) => void;
  setInputMode: React.Dispatch<React.SetStateAction<InputMode>>;
}

export const useChatInputAudio = ({
  agentId,
  asrModelId,
  asrStreamSupported,
  onChange,
  onSend,
  setAsrLoading,
  setInputMode,
}: UseChatInputAudioParams) => {
  const { stopActiveMediaPlayback } = useChatContext();
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);

  const handleConvertAudio = useCallback(async () => {
    try {
      setAsrLoading(true);

      if (audioChunksRef.current.length === 0) {
        throw new Error('未获取到录音文件');
      }

      const audioType =
        mediaRecorderRef.current?.mimeType ||
        audioChunksRef.current[0]?.type ||
        'audio/webm';
      const audioBlob = new Blob(audioChunksRef.current, { type: audioType });

      if (!audioBlob.size) {
        throw new Error('录音文件为空');
      }

      if (asrStreamSupported) {
        onChange({
          target: { value: '' },
        } as React.ChangeEvent<HTMLTextAreaElement>);
        setInputMode(InputMode.NORMAL);
      }

      const audioExtension = audioType.includes('mp4')
        ? 'm4a'
        : audioType.includes('mpeg')
          ? 'mp3'
          : audioType.includes('ogg')
            ? 'ogg'
            : 'webm';

      const formData = new FormData();
      formData.append('audio', audioBlob, `recording.${audioExtension}`);

      const token = getAccessToken();
      const textChunks: string[] = [];
      let hasError = false;
      let errorMessage = '';

      const controller = new AbortController();
      const timeoutId = setTimeout(() => {
        controller.abort();
        hasError = true;
        errorMessage = '请求超时';
      }, 1000 * 60);

      try {
        if (asrStreamSupported) {
          let streamDone = false;
          await fetchEventSource(`/v1/chat/audio/transcriptions/stream?modelId=${asrModelId}&agentId=${agentId}`, {
            method: 'POST',
            body: formData,
            headers: {
              Authorization: `Bearer ${token}`,
            },
            signal: controller.signal,
            openWhenHidden: true,
            async onopen(response) {
              if (response.ok) return;
              let currentMessage = VOICE_CONVERSION_FAIL;
              const errorText = await response.text();
              try {
                const errorJson = JSON.parse(errorText);
                currentMessage = errorJson?.message || currentMessage;
              } catch {
                currentMessage = errorText || currentMessage;
              }
              hasError = true;
              errorMessage = currentMessage;
              throw new Error(currentMessage);
            },
            onmessage(event: EventSourceMessage) {
              const raw = String(event.data || '').trim();
              if (!raw) return;

              if (event.event === 'init') return;

              if (raw === '[DONE]') {
                streamDone = true;
                controller.abort();
                return;
              }

              if (event.event === 'error') {
                hasError = true;
                try {
                  const errObj = JSON.parse(raw) as { message?: unknown };
                  errorMessage = String(errObj?.message || VOICE_CONVERSION_FAIL);
                } catch {
                  errorMessage = raw || VOICE_CONVERSION_FAIL;
                }
                throw new Error(errorMessage);
              }

              try {
                const parsed = JSON.parse(raw) as { text?: unknown; data?: unknown };
                const nextText =
                  typeof parsed?.text === 'string'
                    ? parsed.text
                    : typeof parsed?.data === 'string'
                      ? parsed.data
                      : '';
                if (nextText) {
                  textChunks.push(nextText);
                  onChange({
                    target: { value: textChunks.join('') },
                  } as React.ChangeEvent<HTMLTextAreaElement>);
                }
              } catch {
                textChunks.push(raw);
                onChange({
                  target: { value: textChunks.join('') },
                } as React.ChangeEvent<HTMLTextAreaElement>);
              }
            },
            onerror(err) {
              if (streamDone || controller.signal.aborted) {
                return;
              }
              if (!hasError) {
                hasError = true;
                errorMessage = err instanceof Error ? err.message : '网络异常，语音转化失败';
              }
              throw err;
            },
          });
          clearTimeout(timeoutId);
        } else {
          const response = await fetch(`/v1/chat/audio/transcriptions?modelId=${asrModelId}&agentId=${agentId}`, {
            method: 'POST',
            body: formData,
            headers: {
              Authorization: `Bearer ${token}`,
            },
            signal: controller.signal,
          });

          clearTimeout(timeoutId);

          if (!response.ok) {
            const errorText = await response.text();
            hasError = true;
            try {
              const errorJson = JSON.parse(errorText);
              errorMessage = errorJson.message || VOICE_CONVERSION_FAIL;
            } catch {
              errorMessage = errorText || VOICE_CONVERSION_FAIL;
            }
            throw new Error(errorMessage);
          }

          const result = await response.json();
          const code = result?.code;
          const currentMessage = result?.message;

          let text = '';
          if (code === ResponseCode.S_OK && result.data) {
            if (typeof result.data === 'string') {
              text = result.data;
            } else if (Array.isArray(result.data)) {
              text = result.data.join('');
            }
          } else {
            hasError = true;
            if (code === UNSUPPORT_CONVERSION_CODE) {
              errorMessage = currentMessage || VOICE_CONVERSION_FAIL;
            } else {
              errorMessage = VOICE_CONVERSION_FAIL;
            }
          }

          if (typeof text === 'string' && text.trim()) {
            textChunks.push(text);
          }
        }
      } catch (error) {
        clearTimeout(timeoutId);
        console.error('ASR error:', error);
        if (!hasError) {
          hasError = true;
          errorMessage = error instanceof Error ? error.message : '网络异常，语音转化失败';
        }
      }

      if (hasError) {
        message.error(errorMessage);
        setAsrLoading(false);
        setInputMode(asrStreamSupported ? InputMode.NORMAL : InputMode.VOICE_INIT);
        return;
      }

      const text = textChunks.join('');
      const finalText = text.trim();

      if (typeof text === 'string' && finalText) {
        onChange({
          target: { value: finalText },
        } as React.ChangeEvent<HTMLTextAreaElement>);

        if (asrStreamSupported && !shouldAutoSendStreamAsrText(finalText)) {
          setAsrLoading(false);
          setInputMode(InputMode.NORMAL);
          return;
        }

        if (asrStreamSupported) {
          setInputMode(InputMode.NORMAL);
        } else {
          setInputMode(InputMode.VOICE_INIT);
        }

        setTimeout(() => {
          setAsrLoading(false);
          onSend([{
            type: 'text',
            message: finalText,
          }]);
        }, 1000);
      } else {
        message.warning('未检测到有效语音内容');
        setAsrLoading(false);
        setInputMode(asrStreamSupported ? InputMode.NORMAL : InputMode.VOICE_INIT);
      }
    } catch (e) {
      console.log(e);
      const errorMsg = e instanceof Error ? e.message : '网络异常，语音转化失败';
      message.error(errorMsg);
      setAsrLoading(false);
      setInputMode(asrStreamSupported ? InputMode.NORMAL : InputMode.VOICE_INIT);
    }
  }, [agentId, asrModelId, asrStreamSupported, onChange, onSend, setAsrLoading, setInputMode]);

  const startRecording = useCallback(async () => {
    try {
      stopActiveMediaPlayback();
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
        void handleConvertAudio();
      };

      mediaRecorder.start();
    } catch (err) {
      console.error('录音错误', err);
      message.warning('无法访问麦克风，请检查麦克风权限');
    }
  }, [handleConvertAudio, setInputMode, stopActiveMediaPlayback]);

  const stopRecording = useCallback(() => {
    if (asrStreamSupported) {
      setInputMode(InputMode.NORMAL);
    } else {
      setInputMode(InputMode.VOICE_INIT);
    }

    if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'recording') {
      mediaRecorderRef.current.stop();
      mediaRecorderRef.current.stream.getTracks().forEach((track) => track.stop());
    }
  }, [asrStreamSupported, setInputMode]);

  return {
    mediaRecorderRef,
    audioChunksRef,
    startRecording,
    stopRecording,
  };
};

