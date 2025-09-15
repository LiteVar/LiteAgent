import { useState, useEffect, useCallback } from 'react';
import { 
  getVersionInfo1,
  getUserInfo
} from '../api';
import { useApi } from './useApi';

interface SystemStats {
  robots: number;
  agents: number;
  users: number;
  permissions: number;
}

export function useSystem() {
  const [version, setVersion] = useState<string>('');
  const [userInfo, setUserInfo] = useState<any>(null);
  const [stats, setStats] = useState<SystemStats>({
    robots: 0,
    agents: 0,
    users: 0,
    permissions: 0,
  });

  const versionApi = useApi<string>();
  const userInfoApi = useApi<any>();

  const fetchVersion = useCallback(async () => {
    try {
      const data = await versionApi.execute(() => getVersionInfo1())
      if (data) {
        setVersion(data?.msg);
      }
    } catch (error) {
      console.error('获取版本信息失败:', error);
    }
  }, [versionApi]);

  const fetchUserInfo = useCallback(async () => {
    try {
      const data = await userInfoApi.execute(() => getUserInfo()
      );
      if (data) {
        setUserInfo(data);
      }
    } catch (error) {
      console.error('获取用户信息失败:', error);
    }
  }, [userInfoApi]);

  const fetchStats = useCallback(async () => {
    // TODO: 这里应该调用实际的统计API，目前使用模拟数据
    try {
      // 模拟从多个API获取统计数据
      setStats({
        robots: 12,
        agents: 8,
        users: 156,
        permissions: 23,
      });
    } catch (error) {
      console.error('获取统计数据失败:', error);
    }
  }, []);

  const refreshAll = useCallback(async () => {
    await Promise.all([
      fetchVersion(),
      fetchUserInfo(),
      fetchStats(),
    ]);
  }, [fetchVersion, fetchUserInfo, fetchStats]);

  useEffect(() => {
    refreshAll();
  }, []);

  return {
    version,
    userInfo,
    stats,
    loading: versionApi.loading || userInfoApi.loading,
    error: versionApi.error || userInfoApi.error,
    fetchVersion,
    fetchUserInfo,
    fetchStats,
    refreshAll,
  };
}