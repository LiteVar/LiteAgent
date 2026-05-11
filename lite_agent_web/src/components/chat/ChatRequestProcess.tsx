import { FC, useMemo, useRef, useCallback, useEffect } from 'react';
import { TaskMessageType } from '../../types/Message';
import { AgentMessage } from '@/types/chat';
import { Image } from 'antd';
import MessageActions from './MessageActions';
import { useChatContext } from '@/contexts/ChatContext';

/** contentList 中每个条目的类型 */
interface ContentListItem {
  type: 'text' | 'imageUrl' | 'videoUrl';
  message: string;
}

export interface ChatRequestProcessProps {
  requestProcessMessages: AgentMessage;
  isHovered: boolean;
  setIsHovered: (isHovered: boolean) => void;
  onRetry: () => void;
  onStop: (taskId: string) => void;
  onCopy: (event: React.MouseEvent<HTMLSpanElement>, text?: string) => void;
  copied: boolean;
  isLastMessage?: boolean;
}

const ChatVideoPlayer: FC<{ src: string; className?: string }> = ({ src, className }) => {
  const { requestMediaPlayback, releaseMediaPlayback } = useChatContext();
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const mediaPlaybackIdRef = useRef(`video-${Math.random().toString(36).slice(2)}`);

  const stopPlayback = useCallback(() => {
    if (videoRef.current) {
      videoRef.current.pause();
    }
  }, []);

  const handlePlay = useCallback(() => {
    requestMediaPlayback({
      id: mediaPlaybackIdRef.current,
      kind: 'video',
      stop: stopPlayback,
    });
  }, [requestMediaPlayback, stopPlayback]);

  const handleStop = useCallback(() => {
    releaseMediaPlayback(mediaPlaybackIdRef.current);
  }, [releaseMediaPlayback]);

  useEffect(() => {
    const mediaPlaybackId = mediaPlaybackIdRef.current;

    return () => {
      stopPlayback();
      releaseMediaPlayback(mediaPlaybackId);
    };
  }, [releaseMediaPlayback, stopPlayback]);

  return (
    <video
      ref={videoRef}
      className={className}
      src={src}
      controls
      onPlay={handlePlay}
      onPause={handleStop}
      onEnded={handleStop}
    />
  );
};

const ChatRequestProcess: FC<ChatRequestProcessProps> = ({ requestProcessMessages, isHovered, setIsHovered, onRetry, onStop, onCopy, copied, isLastMessage }) => {

  if (requestProcessMessages.type === TaskMessageType.TEXT) {
    return (
      <div className="mb-8" onMouseEnter={() => setIsHovered(true)} onMouseLeave={() => setIsHovered(false)}>
        <div className="min-h-8 text-message flex w-full items-start flex-row-reverse gap-2 whitespace-normal break-words">
          <div
            className="relative max-w-[70%] break-all whitespace-pre-wrap break-words rounded-2xl px-5 py-2.5 bg-white"
          >
            {requestProcessMessages.content}
          </div>
        </div>
        <div className="mt-2 flex justify-end">
          <MessageActions
            taskId={requestProcessMessages.taskId}
            responding={requestProcessMessages.responding}
            onCopy={onCopy}
            show={isLastMessage || isHovered || !!requestProcessMessages.responding}
            copied={copied}
            onRetry={onRetry}
            onStop={onStop}
            retryDisabled
          />
        </div>
      </div>
    );
  } else if (requestProcessMessages.type === TaskMessageType.CONTENT_LIST) {
    return (
      <ContentListRenderer
        requestProcessMessages={requestProcessMessages}
        isHovered={isHovered}
        setIsHovered={setIsHovered}
        onRetry={onRetry}
        onStop={onStop}
        onCopy={onCopy}
        copied={copied}
        isLastMessage={isLastMessage}
      />
    );
  } else if (requestProcessMessages.type === TaskMessageType.IMAGE_URL) {
    return (
      <div className="mb-8" onMouseEnter={() => setIsHovered(true)} onMouseLeave={() => setIsHovered(false)}>
        <div className="min-h-8 text-message flex w-full items-start flex-row-reverse gap-2 whitespace-normal break-words">
          <div className="relative max-w-[70%] whitespace-pre-wrap break-words py-2.5">
            <Image
              preview={true}
              alt="chat image"
              src={requestProcessMessages.content}
              className="object-cover max-w-[300px] max-h-[300px]"
            />
          </div>
        </div>
        <div className="mt-2 flex justify-end">
          <MessageActions
            taskId={requestProcessMessages.taskId}
            responding={requestProcessMessages.responding}
            onCopy={(event) => onCopy(event, requestProcessMessages.content)}
            show={isLastMessage || isHovered || !!requestProcessMessages.responding}
            copied={copied}
            onRetry={onRetry}
            onStop={onStop}
            retryDisabled
          />
        </div>
      </div>
    );
  } else if (requestProcessMessages.type === TaskMessageType.VIDEO_URL) {
    return (
      <div className="mb-8" onMouseEnter={() => setIsHovered(true)} onMouseLeave={() => setIsHovered(false)}>
        <div className="min-h-8 text-message flex w-full items-start flex-row-reverse gap-2 whitespace-normal break-words">
          <div className="relative whitespace-pre-wrap break-words py-2.5">
            <ChatVideoPlayer className="max-w-[300px] max-h-[400px]" src={requestProcessMessages.content} />
          </div>
        </div>
        <div className="mt-2 flex justify-end">
          <MessageActions
            taskId={requestProcessMessages.taskId}
            responding={requestProcessMessages.responding}
            onCopy={(event) => onCopy(event, requestProcessMessages.content)}
            show={isLastMessage || isHovered || !!requestProcessMessages.responding}
            copied={copied}
            onRetry={onRetry}
            onStop={onStop}
            retryDisabled
          />
        </div>
      </div>
    );
  }


};

/** ContentList 渲染子组件 */
const ContentListRenderer: FC<ChatRequestProcessProps> = ({
  requestProcessMessages,
  isHovered,
  setIsHovered,
  onRetry,
  onStop,
  onCopy,
  copied,
  isLastMessage,
}) => {
  const contentList: ContentListItem[] = useMemo(() => {
    if (!Array.isArray(requestProcessMessages.content)) return [];
    return requestProcessMessages.content;
  }, [requestProcessMessages.content]);

  // 分离不同类型的内容
  const { textItems, imageItems, videoItems } = useMemo(() => {
    const textItems: ContentListItem[] = [];
    const imageItems: ContentListItem[] = [];
    const videoItems: ContentListItem[] = [];
    contentList.forEach((item) => {
      if (item.type === 'text') textItems.push(item);
      else if (item.type === 'imageUrl') imageItems.push(item);
      else if (item.type === 'videoUrl') videoItems.push(item);
    });
    return { textItems, imageItems, videoItems };
  }, [contentList]);

  // 拼接所有文本用于复制
  const copyText = useMemo(() => {
    return contentList.map((item) => item.message).join('\n');
  }, [contentList]);

  return (
    <div className="mb-8" onMouseEnter={() => setIsHovered(true)} onMouseLeave={() => setIsHovered(false)}>
      <div className="min-h-8 text-message flex w-full items-start flex-row-reverse gap-2 whitespace-normal break-words">
        <div className="relative max-w-[70%] flex flex-col items-end gap-4">
          {/* 文字内容 */}
          {textItems.map((item, index) => (
            <div
              key={`text-${index}`}
              className="whitespace-pre-wrap word-break break-all break-words rounded-2xl px-5 py-2.5 bg-white"
            >
              {item.message}
            </div>
          ))}

          {/* 图片内容 - 多张图以网格列表展示 */}
          {imageItems.length > 0 && (
            <div
              className={`grid gap-1 ${
                imageItems.length === 1
                  ? 'grid-cols-1'
                  : imageItems.length === 2
                    ? 'grid-cols-2'
                    : 'grid-cols-3'
              }`}
            >
              <Image.PreviewGroup>
                {imageItems.map((item, index) => (
                  <div
                    key={`img-${index}`}
                    style={{
                      width: imageItems.length === 1 ? '300px' : '100px',
                      height: imageItems.length === 1 ? '300px' : '100px',
                    }}
                    className="overflow-hidden rounded-lg"
                  >
                    <Image
                      preview={true}
                      alt={`chat image ${index + 1}`}
                      src={item.message}
                      className="object-cover w-full h-full"
                      width={imageItems.length === 1 ? 300 : 100}
                      height={imageItems.length === 1 ? 300 : 100}
                      style={{ objectFit: 'cover' }}
                    />
                  </div>
                ))}
              </Image.PreviewGroup>
            </div>
          )}

          {/* 视频内容 */}
          {videoItems.map((item, index) => (
            <div key={`video-${index}`} className="whitespace-pre-wrap break-words py-1">
              <ChatVideoPlayer
                className="max-w-[300px] max-h-[400px] rounded-lg"
                src={item.message}
              />
            </div>
          ))}
        </div>
      </div>
      <div className="mt-2 flex justify-end">
        <MessageActions
          taskId={requestProcessMessages.taskId}
          responding={requestProcessMessages.responding}
          onCopy={(event) => onCopy(event, copyText)}
          show={isLastMessage || isHovered || !!requestProcessMessages.responding}
          copied={copied}
          onRetry={onRetry}
          onStop={onStop}
          retryDisabled
        />
      </div>
    </div>
  );
};

export default ChatRequestProcess;
