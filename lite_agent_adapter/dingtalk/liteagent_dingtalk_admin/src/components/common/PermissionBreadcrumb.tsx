import React from 'react';

interface PermissionBreadcrumbProps {
  selectedDepartment: number | null;
  departmentPath: { deptId: number; name: string }[];
  navigateToBreadcrumb: (index: number) => void;
  getDepartmentName: (deptId: number) => string;
}

const PermissionBreadcrumb: React.FC<PermissionBreadcrumbProps> = ({
  selectedDepartment,
  departmentPath,
  navigateToBreadcrumb,
  getDepartmentName,
}) => {
  if (!selectedDepartment) return null;

  return (
    <div className="flex items-center text-gray-600 dark:text-gray-400 mt-2 space-x-1">
      <button
        onClick={() => navigateToBreadcrumb(-1)}
        className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
      >
        根目录
      </button>
      {departmentPath.map((dept, index) => (
        <div key={dept.deptId} className="flex items-center space-x-1">
          <span>/</span>
          <button
            onClick={() => navigateToBreadcrumb(index)}
            className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
          >
            {dept.name}
          </button>
        </div>
      ))}
      <div className="flex items-center space-x-1">
        <span>/</span>
        <span className="text-black dark:text-white font-medium">
          {getDepartmentName(selectedDepartment)}
        </span>
      </div>
    </div>
  );
};

export default PermissionBreadcrumb;