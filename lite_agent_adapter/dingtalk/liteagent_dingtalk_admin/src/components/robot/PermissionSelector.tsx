import { useState, useEffect, useCallback } from 'react';
import { useUsers } from '../../hooks/useUsers';
import { PermissionWithNames } from '../../hooks/useRobotPermissions';
import LoadingSpinner from '../common/LoadingSpinner';
import DepartmentPanel from '../common/DepartmentPanel';
import UserPanel from '../common/UserPanel';

interface PermissionSelectorProps {
  robotCode: string;
  selectedUserIds: string[];
  selectedDepartmentIds: number[];
  initialPermission?: PermissionWithNames | null;
  onChange: (
    userIds: string[],
    userNames: string[],
    departmentIds: number[],
    departmentNames: string[]
  ) => void;
}


export default function PermissionSelector({
  robotCode,
  selectedUserIds,
  selectedDepartmentIds,
  initialPermission,
  onChange,
}: PermissionSelectorProps) {
  const [localSelectedUsers, setLocalSelectedUsers] = useState<string[]>(selectedUserIds);
  const [localSelectedDepartments, setLocalSelectedDepartments] = useState<number[]>(selectedDepartmentIds);

  const {
    rootDepartments,
    userListItems,
    selectedDepartment,
    departmentPath,
    rootDepartmentUsers,
    loading,
    departmentsLoading,
    switchDepartment,
    navigateToSubDepartment,
    navigateToBreadcrumb,
    getUserName,
    getDepartmentName,
  } = useUsers(robotCode);

  // 同步外部选择状态
  useEffect(() => {
    setLocalSelectedUsers(selectedUserIds);
    setLocalSelectedDepartments(selectedDepartmentIds);
  }, [selectedUserIds, selectedDepartmentIds]);

  // 触发变化回调
  const triggerChange = useCallback(
    (userIds: string[], userNames: string[], departmentIds: number[], departmentNames: string[]) => {
      onChange(userIds, userNames, departmentIds, departmentNames);
    },
    [onChange]
  );


  const getUserNameByInitialPermission = useCallback(
    (userId: string) => {
      return initialPermission?.userList?.filter((u) => u.userId === userId)?.[0]?.userName || '';
    },
    [initialPermission]
  );

  const getDepartmentNameByInitialPermission = useCallback(
    (deptId: number) => {
      return (
        initialPermission?.departmentList?.filter((d) => d.departmentId === deptId)?.[0]?.departmentName || ''
      );
    },
    [initialPermission]
  );

  // 处理部门选择
  const handleDepartmentToggle = useCallback(
    (deptId: number, selected: boolean) => {
      const newSelectedDepartments = selected
        ? [...localSelectedDepartments, deptId]
        : localSelectedDepartments.filter((id) => id !== deptId);

      setLocalSelectedDepartments(newSelectedDepartments);
      triggerChange(
        localSelectedUsers,
        localSelectedUsers.map((id) => {
          return getUserNameByInitialPermission(id) || getUserName(id) || '';
        }),
        newSelectedDepartments,
        newSelectedDepartments.map((id) => {
          return getDepartmentNameByInitialPermission(id) || getDepartmentName(id) || '';
        })
      );
    },
    [
      localSelectedDepartments,
      localSelectedUsers,
      triggerChange,
      getUserName,
      getDepartmentName,
      getUserNameByInitialPermission,
      getDepartmentNameByInitialPermission,
    ]
  );

  // 处理部门点击（显示用户列表）
  const handleDepartmentClick = useCallback(
    (deptId: number) => {
      switchDepartment(deptId);
    },
    [switchDepartment]
  );

  // 处理用户选择
  const handleUserToggle = useCallback(
    (userId: string, selected: boolean) => {
      const newSelectedUsers = selected
        ? [...localSelectedUsers, userId]
        : localSelectedUsers.filter((id) => id !== userId);

      setLocalSelectedUsers(newSelectedUsers);
      triggerChange(
        newSelectedUsers,
        newSelectedUsers.map((id) => getUserNameByInitialPermission(id) || getUserName(id) || ''),
        localSelectedDepartments,
        localSelectedDepartments.map(
          (id) => getDepartmentNameByInitialPermission(id) || getDepartmentName(id) || ''
        )
      );
    },
    [
      localSelectedUsers,
      localSelectedDepartments,
      triggerChange,
      getUserName,
      getDepartmentName,
      getUserNameByInitialPermission,
      getDepartmentNameByInitialPermission,
    ]
  );

  if (departmentsLoading) {
    return <LoadingSpinner message="加载部门数据中..." />;
  }

  return (
    <div className="grid grid-cols-2 gap-6 h-96">
      <DepartmentPanel
        rootDepartments={rootDepartments}
        rootDepartmentUsers={rootDepartmentUsers}
        selectedDepartmentIds={localSelectedDepartments}
        selectedUserIds={localSelectedUsers}
        currentDepartment={selectedDepartment}
        departmentsLoading={departmentsLoading}
        onDepartmentToggle={handleDepartmentToggle}
        onDepartmentClick={handleDepartmentClick}
        onUserToggle={handleUserToggle}
      />

      <UserPanel
        userListItems={userListItems}
        selectedDepartment={selectedDepartment}
        departmentPath={departmentPath}
        selectedUserIds={localSelectedUsers}
        selectedDepartmentIds={localSelectedDepartments}
        loading={loading}
        navigateToBreadcrumb={navigateToBreadcrumb}
        getDepartmentName={(deptId: number) => getDepartmentName(deptId) || ''}
        onUserToggle={handleUserToggle}
        onDepartmentToggle={handleDepartmentToggle}
        onNavigateToSubDepartment={navigateToSubDepartment}
      />
    </div>
  );
}
