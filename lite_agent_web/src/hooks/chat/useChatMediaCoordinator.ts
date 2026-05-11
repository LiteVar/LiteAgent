import { useCallback, useRef } from 'react';

export type ChatMediaKind = 'audio' | 'video';

export interface ChatActiveMediaController {
  id: string;
  kind: ChatMediaKind;
  stop: () => void;
}

export const useChatMediaCoordinator = () => {
  const activeMediaRef = useRef<ChatActiveMediaController | null>(null);

  const requestMediaPlayback = useCallback((controller: ChatActiveMediaController) => {
    const currentActiveMedia = activeMediaRef.current;

    if (currentActiveMedia && currentActiveMedia.id !== controller.id) {
      currentActiveMedia.stop();
    }

    activeMediaRef.current = controller;
  }, []);

  const releaseMediaPlayback = useCallback((id: string) => {
    if (activeMediaRef.current?.id === id) {
      activeMediaRef.current = null;
    }
  }, []);

  const stopActiveMediaPlayback = useCallback(() => {
    if (!activeMediaRef.current) {
      return;
    }

    const currentActiveMedia = activeMediaRef.current;
    activeMediaRef.current = null;
    currentActiveMedia.stop();
  }, []);

  return {
    requestMediaPlayback,
    releaseMediaPlayback,
    stopActiveMediaPlayback,
  };
};

