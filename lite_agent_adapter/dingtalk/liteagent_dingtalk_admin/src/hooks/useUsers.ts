import { useState, useEffect, useCallback } from 'react';
import { 
  getVersionInfo,
  getVersionInfo2
} from '../api';
import { ListUserSimpleResponse, PageResult } from '../api/types.gen';
import { useApi } from './useApi';

export type DeptBaseResponse = {
  autoAddUser?: boolean;
  createDeptGroup?: boolean;
  deptId?: number;
  ext?: string;
  fromUnionOrg?: boolean;
  name?: string;
  parentId?: number;
  sourceIdentifier?: string;
  tags?: string;
  userId?: string;
  type: 'user' | 'department';
};
// 简化的根部门类型，不需要树结构
export interface RootDepartment extends DeptBaseResponse {
  // 移除 children 属性，左侧只显示根部门列表
}

// 面包屑导航项目
export interface DepartmentBreadcrumb {
  deptId: number;
  name: string;
}

// 用户列表项目联合类型
export type UserListItem = 
  | (ListUserSimpleResponse & { type: 'user' })
  | (DeptBaseResponse & { type: 'department'; hasSubDepartments: boolean; });

export function useUsers(robotCode: string) {
  const [users, setUsers] = useState<ListUserSimpleResponse[]>([]);
  const [departments, setDepartments] = useState<DeptBaseResponse[]>([]);
  const [rootDepartments, setRootDepartments] = useState<RootDepartment[]>([]);
  const [rootDepartmentUsers, setRootDepartmentUsers] = useState<ListUserSimpleResponse[]>([]);
  const [selectedDepartment, setSelectedDepartment] = useState<number | null>(null);
  // 面包屑路径状态
  const [departmentPath, setDepartmentPath] = useState<DepartmentBreadcrumb[]>([]);
  // 用户列表项（包含用户和子部门）
  const [userListItems, setUserListItems] = useState<UserListItem[]>([]);
  const [pagination, setPagination] = useState({
    cursor: 0,
    size: 20,
    hasMore: false,
  });
  console.log('departmentPath', departmentPath);
  
  // 缓存用户和部门数据，用于快速获取名称
  const [cacheUsers, setCacheUsers] = useState<Map<string, ListUserSimpleResponse>>(new Map());
  const [cacheDepartments, setCacheDepartments] = useState<Map<number, DeptBaseResponse>>(new Map());

  const fetchUsersApi = useApi<PageResult>();
  const fetchDepartmentsApi = useApi<DeptBaseResponse[]>();

  // 检查部门是否有子部门
  const hasSubDepartments = useCallback((dept: DeptBaseResponse): boolean => {
    return !!(dept.ext && dept.ext.trim() !== '{}' && dept.ext.trim() !== '');
  }, []);

  // 将子部门转换为用户列表项
  const convertDepartmentToUserItem = useCallback((dept: DeptBaseResponse): UserListItem => {
    return {
      ...dept,
      type: 'department',
      hasSubDepartments: hasSubDepartments(dept)
    };
  }, [hasSubDepartments]);

  // 构建根部门列表（扁平结构，不需要树形结构）
  const buildRootDepartmentList = useCallback((deptList: DeptBaseResponse[]): RootDepartment[] => {
    // 当fetchDepartments不传deptId时，返回的都是根级别数据，直接过滤部门类型即可
    return deptList
      .filter(dept => dept.type === 'department')
      .map(dept => ({
        ...dept
      }));
  }, []);


  const fetchUsers = useCallback(async (params?: {
    deptId?: number;
    cursor?: number;
    size?: number;
  }) => {
    const deptId = params?.deptId || selectedDepartment || 1;
    const requestData = {
      robotCode,
      cursor: params?.cursor || 0,
      deptId,
      size: params?.size || pagination.size,
    };

    try {
      // 并行获取用户列表和子部门列表
      const [userData, subDepartmentsData] = await Promise.all([
        fetchUsersApi.execute(() => getVersionInfo({ body: requestData })),
        fetchDepartmentsApi.execute(() => getVersionInfo2({ query: { deptId, robotCode } }))
      ]);

      // 处理用户数据
      const userItems: UserListItem[] = userData?.list ? userData.list.map((user: ListUserSimpleResponse) => ({
        ...user,
        type: 'user' as const
      })) : [];

      // 更新用户缓存
      if (userData?.list) {
        setCacheUsers(prevCache => {
          const newCache = new Map(prevCache);
          userData.list.forEach((user: ListUserSimpleResponse) => {
            if (user.userid) {
              newCache.set(user.userid, user);
            }
          });
          return newCache;
        });
      }

      // 处理子部门数据（只有当cursor为0时才加载子部门）
      const departmentItems: UserListItem[] = [];
      if ((params?.cursor === 0 || !params?.cursor) && subDepartmentsData) {
        departmentItems.push(...subDepartmentsData.map(convertDepartmentToUserItem));
        
        // 更新部门缓存
        setCacheDepartments(prevCache => {
          const newCache = new Map(prevCache);
          subDepartmentsData.forEach((dept: DeptBaseResponse) => {
            if (dept.deptId !== undefined) {
              newCache.set(dept.deptId, dept);
            }
          });
          return newCache;
        });
      }

      // 合并用户和子部门数据
      const allItems = [...departmentItems, ...userItems];

      if (params?.cursor === 0 || !params?.cursor) {
        setUsers(userData?.list || []);
        setUserListItems(allItems);
      } else {
        setUsers(prev => [...prev, ...(userData?.list || [])]);
        setUserListItems(prev => [...prev, ...userItems]);
      }
      
      setPagination({
        cursor: userData?.nextCursor || 0,
        size: requestData.size,
        hasMore: userData?.hasMore || false,
      });
    } catch (error) {
      console.error('获取用户列表失败:', error);
    }
  }, [selectedDepartment, pagination.size, fetchUsersApi, fetchDepartmentsApi, convertDepartmentToUserItem]);

  const fetchDepartments = useCallback(async (deptId?: number) => {
    try {
      // 当 deptId 为空时，获取根部门和根用户
      const data = await fetchDepartmentsApi.execute(() => 
        getVersionInfo2({ query: { deptId: deptId || undefined, robotCode } }))

      if (data) {
        const rootDepartmentList = buildRootDepartmentList(data);
        const rootUserList = data.filter((item: DeptBaseResponse) => item.type === 'user');

        setDepartments(data);
        setRootDepartments(rootDepartmentList);
        setRootDepartmentUsers(rootUserList);
        
        // 更新部门缓存
        setCacheDepartments(prevCache => {
          const newCache = new Map(prevCache);
          data.forEach((dept: DeptBaseResponse) => {
            if (dept.deptId !== undefined) {
              newCache.set(dept.deptId, dept);
            }
          });
          return newCache;
        });

        setCacheUsers(prevCache => {
          const newCache = new Map(prevCache);
          rootUserList.forEach((user: ListUserSimpleResponse) => {
            if (user.userid) {
              newCache.set(user.userid, user);
            }
          });
          return newCache;
        });
        
        // 如果还没有选择部门，选择第一个根部门
        if (!selectedDepartment && rootDepartmentList.length > 0) {
          const firstDept = rootDepartmentList[0];
          if (firstDept.deptId) {
            setSelectedDepartment(firstDept.deptId);
            // 获取第一个部门的用户
            await fetchUsers({ deptId: firstDept.deptId, cursor: 0 });
          }
        }
      }
    } catch (error) {
      console.error('获取部门列表失败:', error);
    }
  }, [fetchDepartmentsApi, buildRootDepartmentList, selectedDepartment, fetchUsers]);

  const switchDepartment = useCallback(async (deptId: number) => {
    if (deptId !== selectedDepartment) {
      setDepartmentPath([]);
      setSelectedDepartment(deptId);
      setUsers([]); // 清空当前用户列表
      setUserListItems([]); // 清空混合列表
      await fetchUsers({ deptId, cursor: 0 });
    }
  }, [selectedDepartment, fetchUsers]);

  // 导航到子部门
  const navigateToSubDepartment = useCallback(async (dept: DeptBaseResponse) => {
    if (!dept.deptId) return;

    // 更新面包屑路径
    const currentDept = departments.find(d => d.deptId === selectedDepartment);
    if (currentDept && currentDept.deptId) {
      const newPath: DepartmentBreadcrumb[] = [
        ...departmentPath,
        { deptId: currentDept.deptId, name: currentDept.name || `部门${currentDept.deptId}` }
      ];
      setDepartmentPath(newPath);
    }

    // 切换到子部门
    setSelectedDepartment(dept.deptId);
    setUsers([]);
    setUserListItems([]);
    await fetchUsers({ deptId: dept.deptId, cursor: 0 });
  }, [selectedDepartment, departments, departmentPath, fetchUsers]);

  // 通过面包屑导航到上级部门
  const navigateToBreadcrumb = useCallback(async (targetIndex: number) => {
    if (targetIndex === -1) {
      // 返回到根部门
      setDepartmentPath([]);
      if (rootDepartments.length > 0 && rootDepartments[0].deptId) {
        await switchDepartment(rootDepartments[0].deptId);
      }
    } else if (targetIndex < departmentPath.length) {
      // 返回到指定的面包屑位置
      const targetDept = departmentPath[targetIndex];
      const newPath = departmentPath.slice(0, targetIndex);
      setDepartmentPath(newPath);
      await switchDepartment(targetDept.deptId);
    }
  }, [departmentPath, rootDepartments, switchDepartment]);

  const searchUsers = useCallback(async () => {
    if (selectedDepartment) {
      await fetchUsers({ deptId: selectedDepartment, cursor: 0 });
    }
  }, [fetchUsers, selectedDepartment]);

  const loadMoreUsers = useCallback(async () => {
    if (pagination.hasMore && !fetchUsersApi.loading && selectedDepartment) {
      await fetchUsers({ deptId: selectedDepartment, cursor: pagination.cursor });
    }
  }, [pagination.hasMore, pagination.cursor, fetchUsersApi.loading, fetchUsers, selectedDepartment]);

  // 从缓存获取用户名称
  const getUserName = useCallback((userId: string) => {
    const user = cacheUsers.get(userId);
    return user?.name;
  }, [cacheUsers]);

  // 从缓存获取部门名称
  const getDepartmentName = useCallback((deptId: number) => {
    const dept = cacheDepartments.get(deptId);
    return dept?.name;
  }, [cacheDepartments]);

  // 初始化时获取根部门和根用户列表（deptId 为空）
  useEffect(() => {
    fetchDepartments(); // 不传 deptId，获取根级数据
  }, []);

  return {
    users,
    departments,
    rootDepartments,
    selectedDepartment,
    departmentPath,
    userListItems,
    rootDepartmentUsers,
    pagination,
    loading: fetchUsersApi.loading,
    departmentsLoading: fetchDepartmentsApi.loading,
    error: fetchUsersApi.error || fetchDepartmentsApi.error,
    fetchUsers,
    fetchDepartments,
    switchDepartment,
    navigateToSubDepartment,
    navigateToBreadcrumb,
    searchUsers,
    loadMoreUsers,
    getDepartmentName,
    getUserName,
    hasSubDepartments,
    cacheUsers,
    cacheDepartments,
  };
}