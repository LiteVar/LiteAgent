import { useCallback, useEffect, useMemo } from 'react';
import type { MenuProps } from 'antd';
import { AgentType, InputMode } from '@/types/chat';
import { ChatMessageItem } from '@/hooks/chat/useChatSSE';
import { FILE_LIMITS, UploadedFile } from '@/hooks/chat/useChatFileUpload';

interface UseChatInputComposerParams {
  value: string;
  onChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  onSend: (messages: ChatMessageItem[]) => void;
  agentType: AgentType;
  inputMode: InputMode;
  uploading: boolean;
  uploadedFiles: UploadedFile[];
  buildFileMessages: () => ChatMessageItem[];
  clearFiles: () => void;
  handleFileSelect: (e: React.ChangeEvent<HTMLInputElement>, type: 'image' | 'video') => Promise<void>;
  handleUrlUpload: (url: string, type: 'image' | 'video') => Promise<void>;
  detectUrlType: (url: string) => Promise<'image' | 'video' | null>;
  textareaRef: React.RefObject<HTMLTextAreaElement>;
  fileInputRef: React.RefObject<HTMLInputElement>;
  videoInputRef: React.RefObject<HTMLInputElement>;
}

export const useChatInputComposer = ({
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
}: UseChatInputComposerParams) => {
  const isReflectionAgent = useMemo(() => agentType === AgentType.REFLECTION, [agentType]);

  const textareaPlaceholder = useMemo(() => {
    return isReflectionAgent ? '反思 Agent 不能进行聊天对话' : '请输入聊天内容';
  }, [isReflectionAgent]);

  const handleSend = useCallback((type: 'text' | 'imageUrl' | 'videoUrl' = 'text', text?: string) => {
    const messages: ChatMessageItem[] = [];
    const textContent = text || value.trim();

    if (type === 'text' && textContent) {
      messages.push({
        type: 'text',
        message: textContent,
      });
    }

    if (uploadedFiles.length > 0) {
      const fileMessages = buildFileMessages();
      messages.push(...fileMessages);
      clearFiles();
    }

    if (messages.length > 0) {
      onSend(messages);
    }
  }, [buildFileMessages, clearFiles, onSend, uploadedFiles.length, value]);

  const menuItems: MenuProps['items'] = useMemo(() => [
    {
      key: 'upload-video',
      label: '上传视频',
      disabled: uploadedFiles.filter(file => file.type === 'video').length >= FILE_LIMITS.MAX_VIDEOS,
      icon: (
        <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M7.5 5.25L5.25 3.75V6.75L7.5 5.25Z" stroke="#343330" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M10.125 2.25H1.875C1.66789 2.25 1.5 2.41789 1.5 2.625V7.875C1.5 8.08211 1.66789 8.25 1.875 8.25H10.125C10.3321 8.25 10.5 8.08211 10.5 7.875V2.625C10.5 2.41789 10.3321 2.25 10.125 2.25Z" stroke="#343330" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M1.5 9.75H10.5" stroke="#343330" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      ),
      onClick: () => {
        videoInputRef.current?.click();
      },
    },
    {
      key: 'upload-image',
      label: '上传图片',
      disabled: uploadedFiles.filter(file => file.type === 'image').length >= FILE_LIMITS.MAX_IMAGES,
      icon: (
        <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M10.125 2.25H1.875C1.66789 2.25 1.5 2.41789 1.5 2.625V9.375C1.5 9.58211 1.66789 9.75 1.875 9.75H10.125C10.3321 9.75 10.5 9.58211 10.5 9.375V2.625C10.5 2.41789 10.3321 2.25 10.125 2.25Z" stroke="#343330" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M7.3125 5.0625C7.51961 5.0625 7.6875 4.89461 7.6875 4.6875C7.6875 4.48039 7.51961 4.3125 7.3125 4.3125C7.10539 4.3125 6.9375 4.48039 6.9375 4.6875C6.9375 4.89461 7.10539 5.0625 7.3125 5.0625Z" fill="#343330"/>
          <path d="M6.90527 7.68756L8.10949 6.48475C8.17981 6.41448 8.27516 6.375 8.37457 6.375C8.47398 6.375 8.56933 6.41448 8.63965 6.48475L10.5001 8.34662" stroke="#343330" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M1.5 7.90715L4.04719 5.35949C4.08201 5.32463 4.12337 5.29697 4.1689 5.2781C4.21442 5.25923 4.26322 5.24951 4.3125 5.24951C4.36178 5.24951 4.41058 5.25923 4.4561 5.2781C4.50163 5.29697 4.54299 5.32463 4.57781 5.35949L8.96766 9.74981" stroke="#343330" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      ),
      onClick: () => {
        fileInputRef.current?.click();
      },
    },
  ], [fileInputRef, uploadedFiles, videoInputRef]);

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${textareaRef.current.scrollHeight}px`;
    }
  }, [textareaRef, value]);

  useEffect(() => {
    if (agentType === AgentType.REFLECTION && textareaRef.current) {
      onChange({ target: { value: '' } } as React.ChangeEvent<HTMLTextAreaElement>);
    }
  }, [agentType, onChange, textareaRef]);

  const handlePaste = useCallback(async (e: React.ClipboardEvent<HTMLTextAreaElement>) => {
    if (uploading || inputMode !== InputMode.NORMAL) {
      return;
    }

    const clipboardData = e.clipboardData;
    const items = Array.from(clipboardData.items);
    const fileItems = items.filter(item => item.kind === 'file');

    if (fileItems.length > 0) {
      e.preventDefault();

      const imageFiles: File[] = [];
      const videoFiles: File[] = [];

      for (const item of fileItems) {
        const file = item.getAsFile();
        if (!file) continue;

        if (file.type.startsWith('image/')) {
          imageFiles.push(file);
        } else if (file.type.startsWith('video/')) {
          videoFiles.push(file);
        }
      }

      if (imageFiles.length > 0) {
        const dataTransfer = new DataTransfer();
        imageFiles.forEach(file => dataTransfer.items.add(file));
        const fakeEvent = {
          target: { files: dataTransfer.files, value: '' },
        } as React.ChangeEvent<HTMLInputElement>;
        await handleFileSelect(fakeEvent, 'image');
      }

      if (videoFiles.length > 0) {
        const dataTransfer = new DataTransfer();
        videoFiles.forEach(file => dataTransfer.items.add(file));
        const fakeEvent = {
          target: { files: dataTransfer.files, value: '' },
        } as React.ChangeEvent<HTMLInputElement>;
        await handleFileSelect(fakeEvent, 'video');
      }

      return;
    }

    const text = clipboardData.getData('text');
    if (!text) return;

    const urlRegex = /(https?:\/\/[^\s]+)/g;
    const matches = text.match(urlRegex);

    if (!(matches && matches.length > 0)) {
      return;
    }

    e.preventDefault();

    let newText = text;
    const mediaUrls: { url: string; type: 'image' | 'video' }[] = [];

    await Promise.all(matches.map(async (url) => {
      const type = await detectUrlType(url);
      if (type) {
        mediaUrls.push({ url, type });
        newText = newText.replace(url, '');
      }
    }));

    newText = newText.replace(/\s{2,}/g, ' ').trim();

    for (const media of mediaUrls) {
      await handleUrlUpload(media.url, media.type);
    }

    if (!textareaRef.current) {
      return;
    }

    const textarea = textareaRef.current;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const currentValue = textarea.value;

    const nextValue =
      currentValue.substring(0, start) +
      newText +
      currentValue.substring(end);

    onChange({ target: { value: nextValue } } as React.ChangeEvent<HTMLTextAreaElement>);

    setTimeout(() => {
      if (textareaRef.current) {
        const newCursorPos = start + newText.length;
        textareaRef.current.selectionStart = newCursorPos;
        textareaRef.current.selectionEnd = newCursorPos;
      }
    }, 0);
  }, [
    detectUrlType,
    handleFileSelect,
    handleUrlUpload,
    inputMode,
    onChange,
    textareaRef,
    uploading,
  ]);

  const disabledUpload = useMemo(() => {
    return agentType === AgentType.REFLECTION || (
      uploading ||
      (uploadedFiles.filter(file => file.type === 'image').length >= FILE_LIMITS.MAX_IMAGES &&
      uploadedFiles.filter(file => file.type === 'video').length >= FILE_LIMITS.MAX_VIDEOS)
    );
  }, [agentType, uploadedFiles, uploading]);

  return {
    disabledUpload,
    handlePaste,
    handleSend,
    isReflectionAgent,
    menuItems,
    textareaPlaceholder,
  };
};

