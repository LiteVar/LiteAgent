import { useState, useCallback } from 'react';
import { 
  getAgentPermissions,
  createRobotPermission,
  updateAgentPermission,
  deleteAgentPermission
} from '../api';
import { RobotPermissionsDto } from '../api/types.gen';
import { useApi } from './useApi';

interface ApiResult {
  success: boolean;
  message: string;
}

// 扩展权限接口，兼容新的API结构
export interface PermissionWithNames extends RobotPermissionsDto {
  // 兼容旧版API结构
  userIdList?: string[];
  userNameList?: string[];
  departmentIdList?: number[];
  departmentNameList?: string[];
}

export function useRobotPermissions() {
  const [permissionsCache, setPermissionsCache] = useState<Map<string, PermissionWithNames>>(new Map());
  
  const fetchApi = useApi<RobotPermissionsDto>();
  const createApi = useApi();
  const updateApi = useApi();
  const deleteApi = useApi();
  
  // 获取机器人权限信息
  const fetchPermissions = useCallback(async (robotCode: string): Promise<PermissionWithNames | null> => {
    // 先检查缓存
    if (permissionsCache.has(robotCode)) {
      return permissionsCache.get(robotCode)!;
    }

    try {
      const data = await fetchApi.execute(() => getAgentPermissions({ path: { robotCode } }));
      
      if (data) {
        // 更新缓存
        setPermissionsCache(prev => new Map(prev).set(robotCode, data));
        return data;
      }
      
      return null;
    } catch (error) {
      console.error('获取机器人权限失败:', error);
      return null;
    }
  }, [fetchApi, permissionsCache]);

  // 创建机器人权限
  const createPermission = useCallback(async (permission: RobotPermissionsDto): Promise<ApiResult> => {
    try {
      const res = await createApi.execute(() => createRobotPermission({ body: permission }));
      
      // 检查响应中是否包含错误信息
      if (res?.error) {
        return { success: false, message: res.error.msg || '创建机器人权限失败' };
      }
      
      // 清除缓存，确保数据一致性
      setPermissionsCache(prev => {
        const newCache = new Map(prev);
        newCache.delete(permission.robotCode);
        return newCache;
      });
      
      return { success: true, message: '创建机器人权限成功' };
    } catch (error) {
      console.error('创建机器人权限失败:', error);
      const errorMessage = createApi.error || '创建机器人权限失败';
      return { success: false, message: errorMessage };
    }
  }, [createApi]);

  // 更新机器人权限
  const updatePermission = useCallback(async (permission: RobotPermissionsDto): Promise<ApiResult> => {
    try {
      const res = await updateApi.execute(() => updateAgentPermission({ body: permission }));
      
      // 检查响应中是否包含错误信息
      if (res?.error) {
        return { success: false, message: res.error.msg || '更新机器人权限失败' };
      }
      
      // 清除缓存，确保数据一致性
      setPermissionsCache(prev => {
        const newCache = new Map(prev);
        newCache.delete(permission.robotCode);
        return newCache;
      });
      
      return { success: true, message: '更新机器人权限成功' };
    } catch (error) {
      console.error('更新机器人权限失败:', error);
      const errorMessage = updateApi.error || '更新机器人权限失败';
      return { success: false, message: errorMessage };
    }
  }, [updateApi]);

  // 删除机器人权限
  const deletePermission = useCallback(async (id: string, robotCode: string): Promise<ApiResult> => {
    try {
      const res = await deleteApi.execute(() => deleteAgentPermission({ path: { id: id } }));
      
      // 检查响应中是否包含错误信息
      if (res?.error) {
        return { success: false, message: res.error.msg || '删除机器人权限失败' };
      }
      
      // 从缓存中移除
      setPermissionsCache(prev => {
        const newCache = new Map(prev);
        newCache.delete(robotCode);
        return newCache;
      });
      
      return { success: true, message: '删除机器人权限成功' };
    } catch (error) {
      console.error('删除机器人权限失败:', error);
      const errorMessage = deleteApi.error || '删除机器人权限失败';
      return { success: false, message: errorMessage };
    }
  }, [deleteApi]);

  // 获取权限摘要信息
  const getPermissionSummary = useCallback((permission: PermissionWithNames | null): string => {
    if (!permission) {
      return '未配置权限';
    }

    // 支持新版API结构（优先）
    const deptCount = permission.departmentList?.length || permission.departmentIdList?.length || 0;
    const userCount = permission.userList?.length || permission.userIdList?.length || 0;

    if (deptCount === 0 && userCount === 0) {
      return '未配置权限';
    }

    const parts: string[] = [];
    if (deptCount > 0) {
      parts.push(`${deptCount}个部门`);
    }
    if (userCount > 0) {
      parts.push(`${userCount}个用户`);
    }

    return parts.join(', ');
  }, []);

  // 清除指定机器人的权限缓存
  const clearPermissionCache = useCallback((robotCode: string) => {
    setPermissionsCache(prev => {
      const newCache = new Map(prev);
      newCache.delete(robotCode);
      return newCache;
    });
  }, []);

  return {
    // 数据
    permissionsCache,
    
    // 状态
    loading: fetchApi.loading,
    creating: createApi.loading,
    updating: updateApi.loading,
    deleting: deleteApi.loading,
    error: fetchApi.error || createApi.error || updateApi.error || deleteApi.error,
    
    // 方法
    fetchPermissions,
    createPermission,
    updatePermission,
    deletePermission,
    getPermissionSummary,
    clearPermissionCache,
  };
}
