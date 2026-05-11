import { useRef, useCallback, useState, useMemo, useEffect } from 'react';
import { throttle } from 'lodash';

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

  const handleScroll = useMemo(
    () => throttle(() => {
      if (scrollRef.current) {
        const { scrollTop, scrollHeight, clientHeight } = scrollRef.current;
        setShowScrollToBottom(scrollTop + clientHeight < scrollHeight - 100);
      }
    }, 100, { leading: true, trailing: true }),
    []
  );

  useEffect(() => {
    return () => {
      handleScroll.cancel();
      if (scrollTimerRef.current) clearTimeout(scrollTimerRef.current);
    };
  }, [handleScroll]);

  const scrollToThinkMessage = useCallback((expandThink = false) => {
    if (expandThink && lastThinkMessage.current && scrollRef.current) {
      if (scrollRef.current.scrollHeight > scrollRef.current.clientHeight) {
        scrollRef.current.scrollTop = lastThinkMessage.current.offsetTop - scrollRef.current.offsetTop;
      } else {
        scrollToBottom();
      }
    }
  }, [scrollToBottom]);

  const adjustScrollAfterLoadMore = useCallback(() => {
    if (!scrollRef.current) return;
    const el = scrollRef.current;
    const oldScrollHeight = el.scrollHeight;
    const oldScrollTop = el.scrollTop;

    el.style.scrollBehavior = 'auto';

    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        if (scrollRef.current) {
          const newScrollHeight = scrollRef.current.scrollHeight;
          scrollRef.current.scrollTop = oldScrollTop + (newScrollHeight - oldScrollHeight);
          scrollRef.current.style.scrollBehavior = '';
        }
      });
    });
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