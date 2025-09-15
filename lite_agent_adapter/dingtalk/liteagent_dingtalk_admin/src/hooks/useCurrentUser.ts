import { useState, useEffect, useCallback } from 'react';
import { getUserInfo } from '../api';
import { useApi } from './useApi';

interface CurrentUser {
  name?: string;
  userid?: string;
  email?: string;
  avatar?: string;
}

export function useCurrentUser() {
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const api = useApi<any>();

  const fetchCurrentUser = useCallback(async () => {
    try {
      const response = await api.execute(() => getUserInfo());
      
      setCurrentUser(response);
    } catch (error) {
      console.error('获取当前用户信息失败:', error);
    }
  }, [api]);

  useEffect(() => {
    fetchCurrentUser();
  }, []);

  return {
    currentUser,
    loading: api.loading,
    error: api.error,
    refresh: fetchCurrentUser,
  };
}