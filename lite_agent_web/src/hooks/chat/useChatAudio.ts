import { useRef, useCallback } from 'react';
import { message } from 'antd';
import { AgentDetailVO, getV1ChatAudioSpeech } from '@/client';

interface UseChatAudioProps {
  agentInfo?: AgentDetailVO;
}

export const useChatAudio = ({ agentInfo }: UseChatAudioProps) => {
  const hasPlayedAudioRef = useRef(false);

  const playAudioFromText = useCallback(
    async (text: string) => {
      if (!text) return;
      if (hasPlayedAudioRef.current) return; // 已经执行过，直接返回

      hasPlayedAudioRef.current = true;

      try {
        const modelId = agentInfo?.agent?.ttsModelId || '';

        const res = await getV1ChatAudioSpeech({
          query: {
            modelId,
            content: text,
            stream: true
          },
        });

        console.log('res', res);

        if (res.data && res.data instanceof Blob) {
          const blob = res.data;
          const url = URL.createObjectURL(blob);
          const audio = new Audio(url);
          audio.play();
        } else {
          message.error('音频播放失败');
        }
      } catch (e) {
        message.error('音频播放异常');
      }
    }, [agentInfo]
  );

  const resetAudioFlag = useCallback(() => {
    hasPlayedAudioRef.current = false;
  }, []);

  return {
    hasPlayedAudioRef,
    playAudioFromText,
    resetAudioFlag,
  };
};