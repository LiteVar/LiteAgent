import { useState, useEffect, useCallback } from 'react';
import { 
  listStarredAgents,
  createRobotPermission,
  updateAgentPermission,
  deleteAgentPermission
} from '../api';
import { AgentRobotRefDto } from '../api/types.gen';
import { useApi } from './useApi';

interface ApiResult {
  success: boolean;
  message: string;
}

export function useRobots() {
  const [robots, setRobots] = useState<AgentRobotRefDto[]>([]);
  const [pagination, setPagination] = useState({
    pageNum: 1,
    pageSize: 10,
    totalSize: 0,
  });

  const fetchApi = useApi<any>();
  const createApi = useApi();
  const updateApi = useApi();
  const deleteApi = useApi();

  const fetchRobots = useCallback(async (params?: {
    pageNum?: number;
    pageSize?: number;
    search?: string;
  }) => {
    const queryParams = {
      pageNum: params?.pageNum || pagination.pageNum,
      pageSize: params?.pageSize || pagination.pageSize,
      search: params?.search || '',
    };

    try {
      const data = await fetchApi.execute(() => listStarredAgents({ query: queryParams }))

      if (data?.contentData) {
        setRobots(data.contentData);
        setPagination({
          pageNum: data.pageNum || 1,
          pageSize: data.pageSize || 10,
          totalSize: data.totalSize || 0,
        });
      }
    } catch (error) {
      console.error('获取机器人列表失败:', error);
    }
  }, [pagination.pageNum, pagination.pageSize, fetchApi]);

  const createRobot = useCallback(async (robotData: AgentRobotRefDto): Promise<ApiResult> => {
    try {
      const res = await createApi.execute(() => createRobotPermission({ body: robotData }))
      
      // 检查响应中是否包含错误信息
      if (res?.error) {
        return { success: false, message: res.error.msg || '创建机器人绑定失败' };
      }
      
      // 刷新列表
      await fetchRobots();
      return { success: true, message: '创建机器人绑定成功' };
    } catch (error) {
      console.error('创建机器人绑定失败:', error);
      const errorMessage = createApi.error || '创建机器人绑定失败';
      return { success: false, message: errorMessage };
    }
  }, [createApi, fetchRobots]);

  const updateRobot = useCallback(async (robotData: AgentRobotRefDto): Promise<ApiResult> => {
    try {
      const res = await updateApi.execute(() => updateAgentPermission({ body: robotData }))
      
      // 检查响应中是否包含错误信息
      if (res?.error) {
        return { success: false, message: res.error.msg || '更新机器人绑定失败' };
      }
      
      // 刷新列表
      await fetchRobots();
      return { success: true, message: '更新机器人绑定成功' };
    } catch (error) {
      console.error('更新机器人绑定失败:', error);
      const errorMessage = updateApi.error || '更新机器人绑定失败';
      return { success: false, message: errorMessage };
    }
  }, [updateApi, fetchRobots]);

  const deleteRobot = useCallback(async (id: string): Promise<ApiResult> => {
    try {
      const res = await deleteApi.execute(() => deleteAgentPermission({ path: { id: id } }))
      
      // 检查响应中是否包含错误信息
      if (res?.error) {
        return { success: false, message: res.error.msg || '删除机器人绑定失败' };
      }
      
      // 刷新列表
      await fetchRobots();
      return { success: true, message: '删除机器人绑定成功' };
    } catch (error) {
      console.error('删除机器人绑定失败:', error);
      const errorMessage = deleteApi.error || '删除机器人绑定失败';
      return { success: false, message: errorMessage };
    }
  }, [deleteApi, fetchRobots]);

  useEffect(() => {
    fetchRobots();
  }, []);

  return {
    robots,
    pagination,
    loading: fetchApi.loading,
    error: fetchApi.error,
    creating: createApi.loading,
    updating: updateApi.loading,
    deleting: deleteApi.loading,
    fetchRobots,
    createRobot,
    updateRobot,
    deleteRobot,
  };
}