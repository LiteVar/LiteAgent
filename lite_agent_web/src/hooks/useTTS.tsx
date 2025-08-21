import { useState, useCallback, useRef, useEffect } from 'react';
import { message as Toast } from 'antd';
import { TtsStatus } from '@/types/chat';
import { getV1ChatAudioSpeech } from '@/client';

interface UseTTSOptions {
  ttsModelId: string;
  getMessageContent: () => string;
  autoPlay?: boolean;
  playAudio?: boolean;
}

export const useTTS = ({ ttsModelId, getMessageContent, autoPlay = false, playAudio = false }: UseTTSOptions) => {
  const [ttsStatus, setTtsStatus] = useState<TtsStatus>(TtsStatus.Init);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const audioHasPlayedRef = useRef(false);
  const controllerRef = useRef<AbortController | null>(null);

  // 清理函数
  const cleanup = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
      if (audioRef.current.src && audioRef.current.src.startsWith('blob:')) {
        URL.revokeObjectURL(audioRef.current.src);
      }
      audioRef.current = null;
    }
    if (controllerRef.current) {
      controllerRef.current.abort();
      controllerRef.current = null;
    }
    setTtsStatus(TtsStatus.Init);
  }, []);

  // 组件卸载时清理
  useEffect(() => {
    return cleanup;
  }, [cleanup]);

  // 按句子分段函数
  const splitBySentences = useCallback((text: string, maxLen: number) => {
    const segments = [];
    const sentences = text.split(/([。！？；\n.!?;])/);
    let currentSegment = '';

    for (let i = 0; i < sentences.length; i += 2) {
      const sentence = sentences[i] || '';
      const punctuation = sentences[i + 1] || '';
      const fullSentence = sentence + punctuation;

      if ((currentSegment + fullSentence).length <= maxLen) {
        currentSegment += fullSentence;
      } else {
        if (currentSegment.trim()) {
          segments.push(currentSegment.trim());
        }
        currentSegment = fullSentence;
      }
    }

    if (currentSegment.trim()) {
      segments.push(currentSegment.trim());
    }

    return segments.filter((seg) => seg.length > 0);
  }, []);

  // TTS播放函数
  const onTtsClick = useCallback(async () => {
    if (ttsStatus === 'playing') {
      cleanup();
      return;
    }
    if (ttsStatus === 'loading') return;

    setTtsStatus(TtsStatus.Loading);

    const content = getMessageContent()
      .trim()
      .replace(/^```[\s\S]*?```/, '')
      .replace(/```[\s\S]*?```/g, ' 代码块 ');
    const maxLength = 500;
    const segments = splitBySentences(content, maxLength);

    try {
      const playSegment = async (index: number) => {
        if (index >= segments.length) {
          setTtsStatus(TtsStatus.Init);
          return;
        }

        controllerRef.current = new AbortController();

        const res = await getV1ChatAudioSpeech({
          query: {
            modelId: ttsModelId,
            content: segments[index],
            stream: true,
          },
          signal: controllerRef.current.signal,
        });

        if (audioRef.current) {
          return;
        }

        if (res.data && res.data instanceof Blob) {
          const blob = res.data;
          const url = URL.createObjectURL(blob);
          const audio = new Audio(url);
          audioRef.current = audio;

          audio.play();

          audio.onended = () => {
            URL.revokeObjectURL(url);
            audioRef.current = null;
            playSegment(index + 1);
          };

          audio.onerror = (e: any) => {
            console.error('TTS audio error:', e);
            setTtsStatus(TtsStatus.Init);
            audioRef.current = null;
            if (e && e.message === 'signal is aborted without reason') {
              Toast.info('音频播放已取消');
            } else {
              Toast.error('音频播放失败');
            }
          };
        } else {
          setTtsStatus(TtsStatus.Init);
          Toast.error('音频播放失败');
        }
      };

      audioHasPlayedRef.current = true;
      setTtsStatus(TtsStatus.Playing);
      await playSegment(0);
    } catch (e: any) {
      console.error('TTS error:', e);
      setTtsStatus(TtsStatus.Init);
      if (e && e.message === 'signal is aborted without reason') {
        Toast.info('音频播放已取消');
      } else {
        Toast.error('音频播放失败');
      }
    }
  }, [ttsModelId, ttsStatus, getMessageContent, cleanup, splitBySentences]);

  // 自动播放逻辑
  useEffect(() => {
    if (playAudio && ttsModelId && ttsStatus === TtsStatus.Init && !audioHasPlayedRef.current) {
      onTtsClick();
    }
  }, [playAudio, ttsModelId, ttsStatus, onTtsClick]);

  return {
    ttsStatus,
    onTtsClick,
    cleanup,
  };
};