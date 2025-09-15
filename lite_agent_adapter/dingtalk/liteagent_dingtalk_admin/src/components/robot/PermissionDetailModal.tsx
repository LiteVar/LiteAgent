import { Modal } from '../ui/modal';
import Button from '../ui/button/Button';
import { PermissionWithNames } from '../../hooks/useRobotPermissions';

interface PermissionDetailModalProps {
  isOpen: boolean;
  robotCode: string;
  permission: PermissionWithNames | null;
  onEdit: () => void;
  onClose: () => void;
  onDelete: () => void;
}

export default function PermissionDetailModal({
  isOpen,
  robotCode,
  permission,
  onEdit,
  onClose,
  onDelete,
}: PermissionDetailModalProps) {
  // 判断是否有权限配置
  const hasPermissions =
    permission &&
    ((permission.userList && permission.userList.length > 0) ||
      (permission.departmentList && permission.departmentList.length > 0));

  const handleDelete = () => {
    if (hasPermissions) {
      onDelete();
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} className="w-full max-w-lg">
      <div className="p-6 border-b border-stroke dark:border-strokedark">
        <h3 className="text-lg font-semibold text-black dark:text-white">机器人权限详情</h3>
        <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">机器人代码: {robotCode}</p>
      </div>

      <div className="p-6">
        {!hasPermissions ? (
          <div className="text-center py-8">
            <div className="text-gray-400 mb-4">
              <svg className="mx-auto h-16 w-16" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
                />
              </svg>
            </div>
            <h4 className="text-lg font-medium text-gray-900 dark:text-white mb-2">未配置权限</h4>
            <p className="text-gray-600 dark:text-gray-400">该机器人尚未配置任何使用权限</p>
          </div>
        ) : (
          <div className="space-y-6">
            {/* 权限摘要 */}
            <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
              <h4 className="text-sm font-medium text-gray-900 dark:text-white mb-2">权限摘要</h4>
              <div className="flex items-center space-x-4">
                {permission.departmentList && permission.departmentList.length > 0 && (
                  <div className="flex items-center text-sm text-gray-600 dark:text-gray-400">
                    <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"
                      />
                    </svg>
                    {permission.departmentList.length}个部门
                  </div>
                )}
                {permission.userList && permission.userList.length > 0 && (
                  <div className="flex items-center text-sm text-gray-600 dark:text-gray-400">
                    <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                      />
                    </svg>
                    {permission.userList.length}个用户
                  </div>
                )}
              </div>
            </div>

            {/* 部门列表 */}
            {permission.departmentList && permission.departmentList.length > 0 && (
              <div>
                <h4 className="text-sm font-medium text-gray-900 dark:text-white mb-3">
                  授权部门 ({permission.departmentList.length})
                </h4>
                <div className="space-y-2">
                  {permission.departmentList?.map((dept) => (
                    <div
                      key={`dept-${dept.departmentId}`}
                      className="flex items-center p-3 bg-white dark:bg-boxdark border border-gray-200 dark:border-strokedark rounded-lg"
                    >
                      <div className="w-2 h-2 bg-blue-500 rounded-full mr-3"></div>
                      <span className="text-sm text-gray-900 dark:text-white">{dept.departmentName}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 用户列表 */}
            {permission.userList && permission.userList.length > 0 && (
              <div>
                <h4 className="text-sm font-medium text-gray-900 dark:text-white mb-3">
                  授权用户 ({permission.userList.length})
                </h4>
                <div className="space-y-2">
                  {permission.userList?.map((user) => (
                    <div
                      key={`user-${user.userId}`}
                      className="flex items-center p-3 bg-white dark:bg-boxdark border border-gray-200 dark:border-strokedark rounded-lg"
                    >
                      <div className="w-8 h-8 bg-gray-200 dark:bg-gray-600 rounded-full flex items-center justify-center mr-3">
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
                      </div>
                      <span className="text-sm text-gray-900 dark:text-white">{user.userName}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* 操作按钮 */}
      <div className="flex justify-between items-center p-6 border-t border-stroke dark:border-strokedark">
        <div>
          {hasPermissions && (
            <Button
              variant="outline"
              onClick={handleDelete}
              className="text-red-600 border-red-300 hover:bg-red-50 hover:border-red-400 dark:text-red-400 dark:border-red-600 dark:hover:bg-red-900/20"
            >
              删除权限
            </Button>
          )}
        </div>

        <div className="flex space-x-3">
          <Button variant="outline" onClick={onClose}>
            关闭
          </Button>
          <button
            onClick={onEdit}
            className="inline-flex items-center justify-center gap-2 rounded-lg transition px-5 py-3.5 text-sm bg-brand-500 text-white shadow-theme-xs hover:bg-brand-600 disabled:bg-brand-300 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {hasPermissions ? '编辑权限' : '配置权限'}
          </button>
        </div>
      </div>
    </Modal>
  );
}
