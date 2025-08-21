import { useState, useCallback, ChangeEvent } from 'react';

export const useChatInput = () => {
  const [value, setValue] = useState('');
  const [asrLoading, setAsrLoading] = useState(false);

  const onInputChange = useCallback((e: ChangeEvent<HTMLTextAreaElement>) => {
    setValue(e.target.value);
  }, []);

  const clearInput = useCallback(() => {
    setValue('');
  }, []);

  return {
    value,
    setValue,
    asrLoading,
    setAsrLoading,
    onInputChange,
    clearInput,
  };
};