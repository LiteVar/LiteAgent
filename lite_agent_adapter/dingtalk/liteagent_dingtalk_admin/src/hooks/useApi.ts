import { useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';

interface ApiState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

interface UseApiOptions {
  onSuccess?: (data: unknown) => void;
  onError?: (error: string) => void;
}

export function useApi<T = unknown>(options?: UseApiOptions) {
  const { logout } = useAuth();
  const [state, setState] = useState<ApiState<T>>({
    data: null,
    loading: false,
    error: null,
  });

  const execute = useCallback(
    async (apiCall: () => Promise<unknown>) => {
      setState(prev => ({ ...prev, loading: true, error: null }));
      
      try {
        const response = await apiCall();
        const data = (response as any)?.data?.data || (response as any)?.data || response;
        
        setState({
          data,
          loading: false,
          error: null,
        });
        
        if (options?.onSuccess) {
          options.onSuccess(data);
        }

        if (data?.error?.code === 401) {
          logout();
        }
        
        return data;
      } catch (error: unknown) {
        const errorMessage = (error as Error)?.message || '请求失败';
        
        setState({
          data: null,
          loading: false,
          error: errorMessage,
        });
        
        if (options?.onError) {
          options.onError(errorMessage);
        }
        
        throw error;
      }
    },
    [options]
  );

  const reset = useCallback(() => {
    setState({
      data: null,
      loading: false,
      error: null,
    });
  }, []);

  return {
    ...state,
    execute,
    reset,
  };
}