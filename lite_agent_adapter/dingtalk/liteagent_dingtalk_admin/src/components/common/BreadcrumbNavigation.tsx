import React from 'react';
import { DepartmentBreadcrumb } from '../../hooks/useUsers';

interface BreadcrumbNavigationProps {
  // 面包屑路径
  departmentPath: DepartmentBreadcrumb[];
  // 当前部门名称
  currentDepartmentName?: string;
  // 点击面包屑项的回调
  onNavigate: (targetIndex: number) => void;
  // 加载状态
  loading?: boolean;
}

const BreadcrumbNavigation: React.FC<BreadcrumbNavigationProps> = ({
  departmentPath,
  currentDepartmentName,
  onNavigate,
  loading = false,
}) => {
  if (loading) {
    return (
      <div className="flex items-center space-x-2 mb-4">
        <div className="h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
        <span className="text-gray-400">/</span>
        <div className="h-4 w-16 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"></div>
      </div>
    );
  }

  // 如果没有面包屑路径，只显示根部门
  if (departmentPath.length === 0) {
    return (
      <div className="flex items-center space-x-2 mb-4">
        <div className="flex items-center space-x-2">
          <svg className="w-4 h-4 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z" />
          </svg>
          <span className="text-lg font-semibold text-black dark:text-white">
            {currentDepartmentName || '根部门'}
          </span>
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center flex-wrap gap-2 mb-4">
      {/* 根部门 */}
      <button
        onClick={() => onNavigate(-1)}
        className="flex items-center space-x-1 px-3 py-2 text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-primary dark:hover:text-primary rounded-md hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z" />
        </svg>
        <span>根部门</span>
      </button>

      {/* 路径分隔符和路径项 */}
      {departmentPath.map((breadcrumb, index) => (
        <React.Fragment key={breadcrumb.deptId}>
          {/* 分隔符 */}
          <svg className="w-4 h-4 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
          
          {/* 面包屑项 */}
          <button
            onClick={() => onNavigate(index)}
            className="px-3 py-2 text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-primary dark:hover:text-primary rounded-md hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
          >
            {breadcrumb.name}
          </button>
        </React.Fragment>
      ))}

      {/* 当前部门 */}
      {currentDepartmentName && (
        <>
          <svg className="w-4 h-4 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
          <span className="px-3 py-2 text-sm font-semibold text-black dark:text-white bg-gray-100 dark:bg-gray-700 rounded-md">
            {currentDepartmentName}
          </span>
        </>
      )}
    </div>
  );
};

export default BreadcrumbNavigation;

