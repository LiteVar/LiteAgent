import React from 'react';
import { UserListItem as UserItem } from '../../hooks/useUsers';

interface UserListItemProps {
  user: UserItem;
  isSelected: boolean;
  onToggle: (id: string | number, selected: boolean) => void;
  onNavigateToSubDepartment?: (user: UserItem) => void;
}

const UserIcon: React.FC = () => (
  <svg
    className="w-4 h-4 text-gray-600 dark:text-gray-400"
    fill="none"
    stroke="currentColor"
    viewBox="0 0 24 24"
  >
    <path
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth={2}
      d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
    />
  </svg>
);

const DepartmentIcon: React.FC = () => (
  <svg
    className="w-4 h-4 text-gray-600 dark:text-gray-400"
    fill="none"
    stroke="currentColor"
    viewBox="0 0 24 24"
  >
    <path
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth={2}
      d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"
    />
  </svg>
);

const NavigateIcon: React.FC<{ onClick: (e: React.MouseEvent) => void }> = ({ onClick }) => (
  <svg
    className="w-8 h-8 p-2 text-gray-400 ml-2 cursor-pointer hover:text-blue-500 transition-colors"
    fill="none"
    stroke="currentColor"
    viewBox="0 0 24 24"
    onClick={onClick}
  >
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
  </svg>
);

const UserListItem: React.FC<UserListItemProps> = ({
  user,
  isSelected,
  onToggle,
  onNavigateToSubDepartment,
}) => {
  const handleClick = () => {
    const id = user.type === 'user' ? (user as any).userid : (user as any).deptId;
    onToggle(id, !isSelected);
  };

  const handleNavigate = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (user && 'deptId' in user && onNavigateToSubDepartment) {
      onNavigateToSubDepartment(user);
    }
  };

  return (
    <div
      className={`
        flex items-center p-3 rounded-lg cursor-pointer transition-all duration-200 border
        ${
          isSelected
            ? 'bg-blue-50 border-blue-200 dark:bg-blue-900/20 dark:border-blue-800'
            : 'bg-white dark:bg-boxdark border-gray-200 dark:border-strokedark hover:bg-gray-50 dark:hover:bg-gray-800'
        }
      `}
      onClick={handleClick}
    >
      <input
        type="checkbox"
        checked={isSelected}
        onChange={() => {}}
        className="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 dark:focus:ring-blue-600 dark:ring-offset-gray-800 focus:ring-2 dark:bg-gray-700 dark:border-gray-600 mr-3"
      />
      <div className="w-8 h-8 bg-gray-200 dark:bg-gray-600 rounded-full flex items-center justify-center mr-3">
        {user.type === 'user' ? <UserIcon /> : <DepartmentIcon />}
      </div>
      <span className="flex-1 text-sm text-gray-900 dark:text-white">
        {user.name}
        {user.type === 'department' && (
          <span className="text-xs text-gray-500 dark:text-gray-400 ml-2">
            (子部门，点击右侧箭头进入)
          </span>
        )}
      </span>
      {user.type === 'department' && <NavigateIcon onClick={handleNavigate} />}
    </div>
  );
};

export default UserListItem;