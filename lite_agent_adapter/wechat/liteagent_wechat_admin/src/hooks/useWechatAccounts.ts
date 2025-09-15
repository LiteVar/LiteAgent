import { useState, useEffect, useCallback } from 'react';
import { 
  listStarredAgents,
  createAgentWxRef,
  updateAgentWxRef,
  deleteAgentWxRef
} from '../api';
import { AgentWxRefDto, PageResultAgentWxRefDto } from '../api/types.gen';
import { useApi } from './useApi';

interface ApiResult {
  success: boolean;
  message: string;
}

export function useWechatAccounts() {
  const [wechatAccounts, setWechatAccounts] = useState<AgentWxRefDto[]>([]);
  const [pagination, setPagination] = useState({
    pageNum: 1,
    pageSize: 10,
    totalSize: 0,
  });

  const fetchApi = useApi<PageResultAgentWxRefDto>();
  const createApi = useApi();
  const updateApi = useApi();
  const deleteApi = useApi();

  const fetchWechatAccounts = useCallback(async (params?: {
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
        setWechatAccounts(data.contentData);
        setPagination({
          pageNum: data.pageNum || 1,
          pageSize: data.pageSize || 10,
          totalSize: data.totalSize || 0,
        });
      }
    } catch (error) {
      console.error('获取公众号列表失败:', error);
    }
  }, [pagination.pageNum, pagination.pageSize, fetchApi]);

  const createWechatAccount = useCallback(async (wechatAccountData: AgentWxRefDto): Promise<ApiResult> => {
    try {
      const res = await createApi.execute(() => createAgentWxRef({ body: wechatAccountData }))
      
      // 检查响应中是否包含错误信息
      if (res?.error) {
        return { success: false, message: res.error.msg || '创建公众号绑定失败' };
      }
      
      // 刷新列表
      await fetchWechatAccounts();
      return { success: true, message: '创建公众号绑定成功' };
    } catch (error) {
      console.error('创建公众号绑定失败:', error);
      const errorMessage = createApi.error || '创建公众号绑定失败';
      return { success: false, message: errorMessage };
    }
  }, [createApi, fetchWechatAccounts]);

  const updateWechatAccount = useCallback(async (wechatAccountData: AgentWxRefDto): Promise<ApiResult> => {
    try {
      const res = await updateApi.execute(() => updateAgentWxRef({ body: wechatAccountData }))
      
      // 检查响应中是否包含错误信息
      if (res?.error) {
        return { success: false, message: res.error.msg || '更新公众号绑定失败' };
      }
      
      // 刷新列表
      await fetchWechatAccounts();
      return { success: true, message: '更新公众号绑定成功' };
    } catch (error) {
      console.error('更新公众号绑定失败:', error);
      const errorMessage = updateApi.error || '更新公众号绑定失败';
      return { success: false, message: errorMessage };
    }
  }, [updateApi, fetchWechatAccounts]);

  const deleteWechatAccount = useCallback(async (id: string): Promise<ApiResult> => {
    try {
      const res = await deleteApi.execute(() => deleteAgentWxRef({ query: { Id: id } }))
      
      // 检查响应中是否包含错误信息
      if (res?.error) {
        return { success: false, message: res.error.msg || '删除公众号绑定失败' };
      }
      
      // 刷新列表
      await fetchWechatAccounts();
      return { success: true, message: '删除公众号绑定成功' };
    } catch (error) {
      console.error('删除公众号绑定失败:', error);
      const errorMessage = deleteApi.error || '删除公众号绑定失败';
      return { success: false, message: errorMessage };
    }
  }, [deleteApi, fetchWechatAccounts]);

  useEffect(() => {
    fetchWechatAccounts();
  }, []);

  return {
    wechatAccounts,
    pagination,
    loading: fetchApi.loading,
    error: fetchApi.error,
    creating: createApi.loading,
    updating: updateApi.loading,
    deleting: deleteApi.loading,
    fetchWechatAccounts,
    createWechatAccount,
    updateWechatAccount,
    deleteWechatAccount,
  };
}