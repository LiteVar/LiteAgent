import { useState, useCallback, useRef, useEffect } from 'react';
import { message as Toast } from 'antd';
import { EventSourceMessage, fetchEventSource } from '@microsoft/fetch-event-source';
import { getAccessToken } from '@/utils/cache';
import { TtsStatus } from '@/types/chat';
import { postV1ChatAudioSpeech } from '@/client';
import { useChatContext } from '@/contexts/ChatContext';

interface UseTTSOptions {
  ttsModelId: string;
  getMessageContent: () => string;
  autoPlay?: boolean;
  playAudio?: boolean;
  supportStream?: boolean; // 是否支持流式
  agentId?: string; // agent ID，用于统计使用
}

const UNSUPPORT_CONVERSION_CODE = 30017;
const TTS_CANCEL_MESSAGE_KEY = 'chat-tts-cancel';

const decodeBase64ToUint8Array = (base64: string) => {
  const cleaned = base64.replace(/^data:[^;]+;base64,/i, '').replace(/\s/g, '');
  const binary = atob(cleaned);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
};

const pcm16ToFloat32 = (pcm: Uint8Array) => {
  const sampleCount = Math.floor(pcm.length / 2);
  const output = new Float32Array(sampleCount);
  const view = new DataView(pcm.buffer, pcm.byteOffset, pcm.byteLength);
  for (let i = 0; i < sampleCount; i++) {
    const s = view.getInt16(i * 2, true);
    output[i] = s / 32768;
  }
  return output;
};

const extractAudioBase64FromSseData = (data: unknown): string | null => {
  if (typeof data !== 'string') return null;
  const text = data.trim();
  if (!text) return null;

  // 兼容 SSE 格式：data:{"audio":"..."}
  const payload = text.startsWith('data:') ? text.slice(5).trim() : text;
  if (!payload || payload === '[DONE]') return null;

  try {
    const parsed = JSON.parse(payload) as { audio?: unknown };
    if (parsed && typeof parsed.audio === 'string') return parsed.audio;
  } catch {
    // 如果不是 JSON，尝试按纯 base64 处理
    return payload;
  }
  return null;
};

const isLikelyMpegAudio = (bytes: Uint8Array) => {
  if (bytes.length < 3) return false;

  // ID3 header
  if (bytes[0] === 0x49 && bytes[1] === 0x44 && bytes[2] === 0x33) {
    return true;
  }

  // MPEG frame sync
  return bytes[0] === 0xff && (bytes[1] & 0xe0) === 0xe0;
};

export const useTTS = ({ 
  ttsModelId, 
  getMessageContent, 
  playAudio = false,
  supportStream = false,
  agentId
}: UseTTSOptions) => {
  const [ttsStatus, setTtsStatus] = useState<TtsStatus>(TtsStatus.Init);
  const { requestMediaPlayback, releaseMediaPlayback } = useChatContext();
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const audioHasPlayedRef = useRef(false);
  const controllerRef = useRef<AbortController | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const workletNodeRef = useRef<AudioWorkletNode | null>(null);
  const streamEndedRef = useRef(false);
  const drainResolverRef = useRef<(() => void) | null>(null);
  const mediaPlaybackIdRef = useRef(`tts-${Math.random().toString(36).slice(2)}`);
  const playbackSessionIdRef = useRef(0);

  const createPlaybackSession = useCallback(() => {
    playbackSessionIdRef.current += 1;
    return playbackSessionIdRef.current;
  }, []);

  const invalidatePlaybackSession = useCallback(() => {
    playbackSessionIdRef.current += 1;
  }, []);

  const isPlaybackSessionActive = useCallback((sessionId: number) => {
    return playbackSessionIdRef.current === sessionId;
  }, []);

  // 清理函数
  const cleanup = useCallback(() => {
    invalidatePlaybackSession();
    if (audioRef.current) {
      audioRef.current.onended = null;
      audioRef.current.onerror = null;
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
      if (audioRef.current.src && audioRef.current.src.startsWith('blob:')) {
        URL.revokeObjectURL(audioRef.current.src);
      }
      audioRef.current = null;
    }
    if (audioContextRef.current) {
      if (workletNodeRef.current) {
        workletNodeRef.current.port.onmessage = null;
        workletNodeRef.current.disconnect();
        workletNodeRef.current = null;
      }
      audioContextRef.current.close();
      audioContextRef.current = null;
    }
    if (controllerRef.current) {
      controllerRef.current.abort();
      controllerRef.current = null;
    }
    streamEndedRef.current = true;
    if (drainResolverRef.current) {
      drainResolverRef.current();
      drainResolverRef.current = null;
    }
    releaseMediaPlayback(mediaPlaybackIdRef.current);
    setTtsStatus(TtsStatus.Init);
  }, [invalidatePlaybackSession, releaseMediaPlayback]);

  const cancelPlayback = useCallback(() => {
    const hasActivePlayback = Boolean(
      controllerRef.current || audioRef.current || audioContextRef.current || workletNodeRef.current,
    );

    cleanup();

    if (hasActivePlayback) {
      Toast.success({
        content: '已取消语音播放',
        key: TTS_CANCEL_MESSAGE_KEY,
        duration: 1.2,
      });
    }
  }, [cleanup]);

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

  // TTS播放函数（流式）
  const playStreamTTS = useCallback(async (segments: string[], sessionId: number) => {
    let pcmStreamUsed = false;

    const ensurePcmPlayer = async () => {
      if (!audioContextRef.current) {
        audioContextRef.current = new AudioContext({ sampleRate: 24000 });
        await audioContextRef.current.audioWorklet.addModule('/pcm-player.worklet.js');
      }
      if (audioContextRef.current.state === 'suspended') {
        await audioContextRef.current.resume();
      }
      if (workletNodeRef.current) return;

      const node = new AudioWorkletNode(audioContextRef.current, 'pcm-player-worklet');
      node.port.onmessage = (event: MessageEvent<{ type?: string }>) => {
        if (event.data?.type === 'drain' && drainResolverRef.current) {
          const resolve = drainResolverRef.current;
          drainResolverRef.current = null;
          resolve();
        }
      };
      node.connect(audioContextRef.current.destination);
      workletNodeRef.current = node;
      workletNodeRef.current.port.postMessage({ type: 'reset' });
    };

    const waitQueueDrain = () =>
      new Promise<void>((resolve) => {
        drainResolverRef.current = resolve;
      });

    const playSegment = async (index: number) => {
      if (!isPlaybackSessionActive(sessionId)) {
        return;
      }

      if (index >= segments.length) {
        streamEndedRef.current = true;
        if (pcmStreamUsed && workletNodeRef.current) {
          workletNodeRef.current.port.postMessage({ type: 'end' });
          await waitQueueDrain();
        }
        if (isPlaybackSessionActive(sessionId)) {
          cleanup();
        }
        return;
      }

      let controller: AbortController | null = null;
      let pcmReadyPromise: Promise<void> | null = null;
      let encodedReadyPromise: Promise<void> | null = null;
      let audioEndedPromise: Promise<void> | null = null;
      let mediaSource: MediaSource | null = null;
      let sourceBuffer: SourceBuffer | null = null;
      const encodedQueue: ArrayBuffer[] = [];
      let encodedDrainResolver: (() => void) | null = null;
      let streamType = 'unknown';
      let streamDone = false;
      let hasChunk = false;
      let errorMessage = '';

      const appendPcmChunk = (pcmBytes: Uint8Array) => {
        const floatChunk = pcm16ToFloat32(pcmBytes);
        const buffer = floatChunk.buffer;
        workletNodeRef.current?.port.postMessage(
          { type: 'append', samples: buffer },
          [buffer],
        );
        hasChunk = true;
        pcmStreamUsed = true;
      };

      const cleanupEncodedAudio = () => {
        if (!audioRef.current) return;
        audioRef.current.onended = null;
        audioRef.current.onerror = null;
        audioRef.current.pause();
        audioRef.current.currentTime = 0;
        if (audioRef.current.src && audioRef.current.src.startsWith('blob:')) {
          URL.revokeObjectURL(audioRef.current.src);
        }
        audioRef.current = null;
      };

      const flushEncodedQueue = () => {
        if (!sourceBuffer || sourceBuffer.updating) return;

        if (encodedQueue.length > 0) {
          sourceBuffer.appendBuffer(encodedQueue.shift()!);
          return;
        }

        if (streamDone && mediaSource?.readyState === 'open') {
          try {
            mediaSource.endOfStream();
          } catch (endError) {
            console.warn('TTS mediaSource endOfStream failed:', endError);
          }
        }

        if (encodedDrainResolver) {
          const resolve = encodedDrainResolver;
          encodedDrainResolver = null;
          resolve();
        }
      };

      const waitEncodedDrain = () =>
        new Promise<void>((resolve) => {
          if (!sourceBuffer || (!sourceBuffer.updating && encodedQueue.length === 0)) {
            resolve();
            return;
          }
          encodedDrainResolver = resolve;
          flushEncodedQueue();
        });

      const waitAudioEnded = async () => {
        const currentAudioEndedPromise = audioEndedPromise;
        if (!currentAudioEndedPromise) return;

        await new Promise<void>((resolve, reject) => {
          const abortHandler = () => reject(new Error('aborted'));

          controller?.signal.addEventListener('abort', abortHandler, { once: true });
          currentAudioEndedPromise
            .then(() => resolve())
            .catch(reject)
            .finally(() => {
              controller?.signal.removeEventListener('abort', abortHandler);
            });
        });
      };

      const ensureEncodedPlayer = () => {
        if (encodedReadyPromise) return encodedReadyPromise;

        encodedReadyPromise = new Promise<void>((resolve, reject) => {
          if (typeof MediaSource === 'undefined' || !MediaSource.isTypeSupported('audio/mpeg')) {
            reject(new Error('当前浏览器不支持流式 MP3 播放'));
            return;
          }

          const audio = new Audio();
          const nextMediaSource = new MediaSource();
          mediaSource = nextMediaSource;
          audio.src = URL.createObjectURL(nextMediaSource);
          audioRef.current = audio;

          audioEndedPromise = new Promise<void>((resolveEnded, rejectEnded) => {
            audio.onended = () => resolveEnded();
            audio.onerror = () => rejectEnded(new Error('音频播放失败'));
          });

          nextMediaSource.addEventListener(
            'sourceopen',
            () => {
              try {
                sourceBuffer = nextMediaSource.addSourceBuffer('audio/mpeg');
                sourceBuffer.mode = 'sequence';
                sourceBuffer.addEventListener('updateend', flushEncodedQueue);
                audio.play().catch((playError) => {
                  console.warn('TTS audio autoplay blocked:', playError);
                });
                resolve();
                flushEncodedQueue();
              } catch (sourceError) {
                reject(
                  sourceError instanceof Error ? sourceError : new Error('音频流初始化失败'),
                );
              }
            },
            { once: true },
          );

          nextMediaSource.addEventListener(
            'error',
            () => reject(new Error('音频流初始化失败')),
            { once: true },
          );
        });

        return encodedReadyPromise;
      };

      try {
        if (!isPlaybackSessionActive(sessionId)) {
          return;
        }

        streamEndedRef.current = false;
        controller = new AbortController();
        controllerRef.current = controller;
        const token = getAccessToken();
        const query = new URLSearchParams({
          modelId: ttsModelId,
          content: segments[index],
        });
        if (agentId) {
          query.set('agentId', agentId);
        }

        await fetchEventSource(`/v1/chat/audio/speech/stream?${query.toString()}`, {
          method: 'POST',
          headers: {
            Accept: 'text/event-stream',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          signal: controller.signal,
          openWhenHidden: true,
          async onopen(response) {
            if (response.ok) return;

            const errorText = await response.text();
            try {
              const errorJson = JSON.parse(errorText) as { message?: unknown };
              errorMessage = String(errorJson?.message || '音频播放失败');
            } catch {
              errorMessage = errorText || '音频播放失败';
            }
            throw new Error(errorMessage);
          },
          onmessage(event: EventSourceMessage) {
            const raw = String(event.data || '').trim();
            if (!raw) return;

            if (raw === '[DONE]') {
              streamDone = true;
              return;
            }

            if (event.event === 'error') {
              try {
                const errObj = JSON.parse(raw) as { message?: unknown };
                errorMessage = String(errObj?.message || '音频播放失败');
              } catch {
                errorMessage = raw || '音频播放失败';
              }
              throw new Error(errorMessage);
            }

            const payload = extractAudioBase64FromSseData(raw);
            if (!payload) return;

            try {
              const audioBytes = decodeBase64ToUint8Array(payload);
              if (audioBytes.length === 0) return;

              if (streamType === 'unknown') {
                streamType = isLikelyMpegAudio(audioBytes) ? 'mpeg' : 'pcm';
              }

              if (streamType === 'mpeg') {
                void ensureEncodedPlayer()
                  .then(() => {
                    encodedQueue.push(audioBytes.slice().buffer);
                    hasChunk = true;
                    flushEncodedQueue();
                  })
                  .catch((playerError) => {
                    errorMessage =
                      playerError instanceof Error ? playerError.message : '音频流初始化失败';
                    controller?.abort();
                  });
                return;
              }

              pcmReadyPromise = pcmReadyPromise || ensurePcmPlayer();
              void pcmReadyPromise
                .then(() => {
                  appendPcmChunk(audioBytes);
                })
                .catch((playerError) => {
                  errorMessage =
                    playerError instanceof Error ? playerError.message : '音频播放失败';
                  controller?.abort();
                });
            } catch (decodeError) {
              console.error('TTS base64 decode error:', decodeError);
            }
          },
          onclose() {
            streamDone = true;
            flushEncodedQueue();
          },
          onerror(error) {
            if (controller?.signal.aborted) {
              return;
            }
            throw error;
          },
        });

        if (controllerRef.current === controller) {
          controllerRef.current = null;
        }

        if (!isPlaybackSessionActive(sessionId)) {
          cleanupEncodedAudio();
          return;
        }

        if (pcmReadyPromise) {
          await pcmReadyPromise;
        }
        if (encodedReadyPromise) {
          await encodedReadyPromise;
        }

        if (!isPlaybackSessionActive(sessionId)) {
          cleanupEncodedAudio();
          return;
        }

        if (!hasChunk) {
          releaseMediaPlayback(mediaPlaybackIdRef.current);
          setTtsStatus(TtsStatus.Init);
          Toast.error(errorMessage || '音频播放失败');
          return;
        }

        if (!streamDone) {
          console.warn('TTS stream closed without explicit done event');
        }

        if (streamType === 'mpeg') {
          await waitEncodedDrain();
          await waitAudioEnded();

          if (!isPlaybackSessionActive(sessionId)) {
            cleanupEncodedAudio();
            return;
          }

          const currentSourceBuffer: SourceBuffer | null = sourceBuffer;
          if (currentSourceBuffer) {
            (currentSourceBuffer as SourceBuffer).removeEventListener('updateend', flushEncodedQueue);
          }
          cleanupEncodedAudio();
        }

        await playSegment(index + 1);
      } catch (e: unknown) {
        console.error('TTS stream error:', e);
        if (controllerRef.current === controller) {
          controllerRef.current = null;
        }
        if (!isPlaybackSessionActive(sessionId)) {
          cleanupEncodedAudio();
          return;
        }
        setTtsStatus(TtsStatus.Init);
        cleanupEncodedAudio();
        if (controller?.signal.aborted) {
          Toast.info('音频播放已取消');
        } else {
          const error = e as Error;
          Toast.error(error?.message || '音频播放失败');
        }
      }
    };

    audioHasPlayedRef.current = true;
    requestMediaPlayback({
      id: mediaPlaybackIdRef.current,
      kind: 'audio',
      stop: cleanup,
    });
    setTtsStatus(TtsStatus.Playing);
    await playSegment(0);
  }, [ttsModelId, agentId, cleanup, isPlaybackSessionActive, releaseMediaPlayback, requestMediaPlayback]);

  // TTS播放函数（非流式）
  const playNonStreamTTS = useCallback(async (segments: string[], sessionId: number) => {
    const playSegment = async (index: number) => {
      if (!isPlaybackSessionActive(sessionId)) {
        return;
      }

      if (index >= segments.length) {
        releaseMediaPlayback(mediaPlaybackIdRef.current);
        setTtsStatus(TtsStatus.Init);
        return;
      }

      const controller = new AbortController();
      controllerRef.current = controller;

      try {
        const res = await postV1ChatAudioSpeech({
          query: {
            agentId,
            modelId: ttsModelId,
            content: segments[index],
          },
          signal: controller.signal,
        });

        if (controllerRef.current === controller) {
          controllerRef.current = null;
        }

        if (!isPlaybackSessionActive(sessionId)) {
          return;
        }

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
            if (!isPlaybackSessionActive(sessionId)) {
              return;
            }
            void playSegment(index + 1);
          };

          audio.onerror = (e: unknown) => {
            console.error('TTS audio error:', e);
            setTtsStatus(TtsStatus.Init);
            audioRef.current = null;
            const error = e as Error;
            if (error && error.message === 'signal is aborted without reason') {
              Toast.info('音频播放已取消');
            } else {
              Toast.error('音频播放失败');
            }
          };
        } else {
          const code = res?.data?.code;
          const message = res?.data?.message;
       
          if (code === UNSUPPORT_CONVERSION_CODE) {
            Toast.error(message || '音频播放失败');
          } else {
            Toast.error('音频播放失败');
          }

          setTtsStatus(TtsStatus.Init);

          return;
        }
      } catch (e: unknown) {
        if (controllerRef.current === controller) {
          controllerRef.current = null;
        }
        if (controller.signal.aborted || !isPlaybackSessionActive(sessionId)) {
          return;
        }

        console.error('TTS non-stream error:', e);
        const error = e as Error;
        if (error?.message) {
          Toast.error(error.message);
        } else {
          Toast.error('音频播放失败');
        }
        setTtsStatus(TtsStatus.Init);
      }
    };

    audioHasPlayedRef.current = true;
    requestMediaPlayback({
      id: mediaPlaybackIdRef.current,
      kind: 'audio',
      stop: cleanup,
    });
    setTtsStatus(TtsStatus.Playing);
    await playSegment(0);
  }, [ttsModelId, agentId, cleanup, isPlaybackSessionActive, releaseMediaPlayback, requestMediaPlayback]);

  // TTS播放函数
  const onTtsClick = useCallback(async () => {
    if (ttsStatus === TtsStatus.Playing || ttsStatus === TtsStatus.Loading) {
      cancelPlayback();
      return;
    }

    setTtsStatus(TtsStatus.Loading);
    const sessionId = createPlaybackSession();

    const content = getMessageContent()
      .trim()
      .replace(/^```[\s\S]*?```/, '')
      .replace(/```[\s\S]*?```/g, ' 代码块 ');
    const maxLength = 500;
    const segments = splitBySentences(content, maxLength);

    try {
      if (supportStream) {
        await playStreamTTS(segments, sessionId);
      } else {
        await playNonStreamTTS(segments, sessionId);
      }
    } catch (e: unknown) {
      console.error('TTS error:', e);
      if (isPlaybackSessionActive(sessionId)) {
        setTtsStatus(TtsStatus.Init);
        Toast.error('音频播放失败');
      }
    }
  }, [
    createPlaybackSession,
    ttsStatus, 
    getMessageContent, 
    cancelPlayback,
    isPlaybackSessionActive,
    splitBySentences, 
    supportStream, 
    playStreamTTS, 
    playNonStreamTTS
  ]);

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
