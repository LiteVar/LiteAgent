import { useRef, useCallback, useState } from 'react';

export const useChatScroll = () => {
  const scrollRef = useRef<HTMLDivElement>(null);
  const thinkScrollRef = useRef<HTMLDivElement>(null);
  const scrollTimerRef = useRef<NodeJS.Timeout>();
  const lastThinkMessage = useRef<HTMLDivElement>(null);
  const [showScrollToBottom, setShowScrollToBottom] = useState(false);

  const scrollToBottom = useCallback(() => {
    if (scrollTimerRef.current) {
      clearTimeout(scrollTimerRef.current);
    }
    scrollTimerRef.current = setTimeout(() => {
      if (scrollRef.current) {
        scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
      }
      if (thinkScrollRef.current) {
        thinkScrollRef.current.scrollTop = thinkScrollRef.current.scrollHeight;
      }
    }, 100);
  }, []);

  const handleScroll = useCallback(() => {
    if (scrollRef.current) {
      const { scrollTop, scrollHeight, clientHeight } = scrollRef.current;
      setShowScrollToBottom(scrollTop + clientHeight < scrollHeight - 100);
    }
  }, []);

  const scrollToThinkMessage = useCallback((expandThink = false) => {
    if (expandThink && lastThinkMessage.current && scrollRef.current) {
      if (scrollRef.current.scrollHeight > scrollRef.current.clientHeight) {
        scrollRef.current.scrollTop = lastThinkMessage.current.offsetTop - scrollRef.current.offsetTop;
      } else {
        scrollToBottom();
      }
    }
  }, [scrollToBottom]);

  const adjustScrollAfterLoadMore = useCallback((oldScrollHeight: number) => {
    setTimeout(() => {
      if (scrollRef.current) {
        const newScrollHeight = scrollRef.current.scrollHeight;
        scrollRef.current.scrollTop = newScrollHeight - oldScrollHeight;
      }
    }, 100);
  }, []);

  return {
    scrollRef,
    thinkScrollRef,
    lastThinkMessage,
    showScrollToBottom,
    scrollToBottom,
    handleScroll,
    scrollToThinkMessage,
    adjustScrollAfterLoadMore,
  };
};