import React from 'react';
import { RootDepartment } from '../../hooks/useUsers';
import LoadingSpinner from './LoadingSpinner';

// 简化的部门项组件，不需要递归树结构
interface DepartmentItemProps {
  department: RootDepartment;
  isSelected: boolean;
  isCurrentDepartment: boolean;
  onDepartmentToggle: (deptId: number, selected: boolean) => void;
  onDepartmentClick: (deptId: number) => void;
}

const DepartmentItem: React.FC<DepartmentItemProps> = ({
  department,
  isSelected,
  isCurrentDepartment,
  onDepartmentToggle,
  onDepartmentClick,
}) => {
  const handleCheckboxChange = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (department.deptId) {
      onDepartmentToggle(department.deptId, !isSelected);
    }
  };

  const handleClick = () => {
    if (department.deptId) {
      onDepartmentClick(department.deptId);
    }
  };

  return (
    <div className="select-none">
      <div
        className={`
          flex items-center py-2 px-3 rounded-lg cursor-pointer transition-all duration-200
          ${
            isCurrentDepartment
              ? 'bg-blue-50 border border-blue-200 dark:bg-blue-900/20 dark:border-blue-800'
              : 'hover:bg-gray-50 dark:hover:bg-gray-800'
          }
        `}
        onClick={handleClick}
      >
        <div className="mr-3 flex items-center" onClick={handleCheckboxChange}>
          <input
            type="checkbox"
            checked={isSelected}
            onChange={() => {}}
            className="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 dark:focus:ring-blue-600 dark:ring-offset-gray-800 focus:ring-2 dark:bg-gray-700 dark:border-gray-600"
          />
        </div>

        <div className="flex items-center flex-1">
          <div
            className={`
            w-2 h-2 rounded-full mr-3 
            ${isCurrentDepartment ? 'bg-blue-500' : 'bg-gray-300 dark:bg-gray-600'}
          `}
          ></div>
          <span className="text-sm font-medium truncate text-black dark:text-white">
            {department.name || `部门${department.deptId}`}
          </span>
        </div>
      </div>
    </div>
  );
};

interface RootUserItemProps {
  user: any;
  isSelected: boolean;
  onUserToggle: (userId: string, selected: boolean) => void;
}

const RootUserItem: React.FC<RootUserItemProps> = ({ user, isSelected, onUserToggle }) => (
  <div className="select-none">
    <div className="flex items-center py-2 px-3 rounded-lg cursor-pointer transition-all duration-200">
      <div
        className="mr-3 flex items-center"
        onClick={() => onUserToggle(user.userid || '', !isSelected)}
      >
        <input
          type="checkbox"
          checked={isSelected}
          onChange={() => {}}
          className="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 dark:focus:ring-blue-600 dark:ring-offset-gray-800 focus:ring-2 dark:bg-gray-700 dark:border-gray-600"
        />
      </div>
      <div className="flex items-center flex-1">
        <span className="text-sm font-medium truncate text-black dark:text-white">
          {user.name}
        </span>
      </div>
    </div>
  </div>
);

interface DepartmentPanelProps {
  rootDepartments: RootDepartment[];
  rootDepartmentUsers: any[];
  selectedDepartmentIds: number[];
  selectedUserIds: string[];
  currentDepartment?: number | null;
  departmentsLoading: boolean;
  onDepartmentToggle: (deptId: number, selected: boolean) => void;
  onDepartmentClick: (deptId: number) => void;
  onUserToggle: (userId: string, selected: boolean) => void;
}

const DepartmentPanel: React.FC<DepartmentPanelProps> = ({
  rootDepartments,
  rootDepartmentUsers,
  selectedDepartmentIds,
  selectedUserIds,
  currentDepartment,
  departmentsLoading,
  onDepartmentToggle,
  onDepartmentClick,
  onUserToggle,
}) => {
  if (departmentsLoading) {
    return <LoadingSpinner message="加载部门数据中..." />;
  }

  return (
    <div className="border border-stroke dark:border-strokedark rounded-lg">
      <div className="p-4 border-b border-stroke dark:border-strokedark">
        <div className="flex items-center justify-between">
          <h4 className="text-sm font-semibold text-black dark:text-white">选择部门</h4>
        </div>
        <p className="text-xs text-gray-600 dark:text-gray-400 mt-1">勾选部门表示该部门所有用户都有权限</p>
      </div>
      <div className="p-4 h-80 overflow-y-auto">
        {rootDepartments.length === 0 && rootDepartmentUsers.length === 0 ? (
          <div className="text-center py-8">
            <p className="text-sm text-gray-600 dark:text-gray-400">暂无部门数据</p>
          </div>
        ) : (
          <div className="space-y-1">
            {/* 显示根部门列表 */}
            {rootDepartments.map((dept) => {
              const isSelected = selectedDepartmentIds.includes(dept.deptId || 0);
              const isCurrentDepartment = dept.deptId === currentDepartment;
              
              return (
                <DepartmentItem
                  key={dept.deptId}
                  department={dept}
                  isSelected={isSelected}
                  isCurrentDepartment={isCurrentDepartment}
                  onDepartmentToggle={onDepartmentToggle}
                  onDepartmentClick={onDepartmentClick}
                />
              );
            })}
            
            {/* 显示根级用户 */}
            {rootDepartmentUsers.map((user) => {
              const isSelected = selectedUserIds.includes(user.userid || '');
              return (
                <RootUserItem
                  key={user.userid}
                  user={user}
                  isSelected={isSelected}
                  onUserToggle={onUserToggle}
                />
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default DepartmentPanel;