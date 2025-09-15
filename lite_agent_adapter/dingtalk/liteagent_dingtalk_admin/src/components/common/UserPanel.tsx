import React from 'react';
import { UserListItem } from '../../hooks/useUsers';
import PermissionBreadcrumb from './PermissionBreadcrumb';
import UserListItemComponent from './UserListItem';
import LoadingSpinner from './LoadingSpinner';

interface EmptyStateProps {
  selectedDepartment: number | null;
}

const EmptyState: React.FC<EmptyStateProps> = ({ selectedDepartment }) => {
  if (!selectedDepartment) {
    return (
      <div className="text-center py-8">
        <div className="text-gray-400 mb-2">
          <svg className="mx-auto h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1}
              d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"
            />
          </svg>
        </div>
        <p className="text-sm text-gray-600 dark:text-gray-400">请点击左侧部门查看用户</p>
      </div>
    );
  }

  return (
    <div className="text-center py-8">
      <p className="text-sm text-gray-600 dark:text-gray-400">该部门暂无用户</p>
    </div>
  );
};

interface UserPanelProps {
  userListItems: UserListItem[];
  selectedDepartment: number | null;
  departmentPath: { deptId: number; name: string }[];
  selectedUserIds: string[];
  selectedDepartmentIds: number[];
  loading: boolean;
  navigateToBreadcrumb: (index: number) => void;
  getDepartmentName: (deptId: number) => string;
  onUserToggle: (userId: string, selected: boolean) => void;
  onDepartmentToggle: (deptId: number, selected: boolean) => void;
  onNavigateToSubDepartment: (user: UserListItem) => void;
}

const UserPanel: React.FC<UserPanelProps> = ({
  userListItems,
  selectedDepartment,
  departmentPath,
  selectedUserIds,
  selectedDepartmentIds,
  loading,
  navigateToBreadcrumb,
  getDepartmentName,
  onUserToggle,
  onDepartmentToggle,
  onNavigateToSubDepartment,
}) => {
  return (
    <div className="border border-stroke dark:border-strokedark rounded-lg">
      <div className="p-4 border-b border-stroke dark:border-strokedark">
        <h4 className="text-sm font-semibold text-black dark:text-white">选择用户</h4>

        <PermissionBreadcrumb
          selectedDepartment={selectedDepartment}
          departmentPath={departmentPath}
          navigateToBreadcrumb={navigateToBreadcrumb}
          getDepartmentName={getDepartmentName}
        />

        <p className="text-xs text-gray-600 dark:text-gray-400 mt-1">
          {selectedDepartment ? '点击子部门右侧箭头进入下级，单击选择项目' : '点击左侧部门查看用户列表'}
        </p>
      </div>
      <div className="p-4 h-80 overflow-y-auto">
        {loading ? (
          <LoadingSpinner size="sm" message="" />
        ) : !selectedDepartment || userListItems.length === 0 ? (
          <EmptyState selectedDepartment={selectedDepartment} />
        ) : (
          <div className="space-y-2">
            {userListItems.map((user: UserListItem) => {
              const isSelected =
                user.type === 'user'
                  ? selectedUserIds.includes((user as any).userid || '')
                  : selectedDepartmentIds.includes((user as any).deptId || 0);

              const handleToggle = (id: string | number, selected: boolean) => {
                if (user.type === 'user') {
                  onUserToggle(id as string, selected);
                } else {
                  onDepartmentToggle(id as number, selected);
                }
              };

              return (
                <UserListItemComponent
                  key={(user as any).userid || (user as any).deptId}
                  user={user}
                  isSelected={isSelected}
                  onToggle={handleToggle}
                  onNavigateToSubDepartment={onNavigateToSubDepartment}
                />
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default UserPanel;