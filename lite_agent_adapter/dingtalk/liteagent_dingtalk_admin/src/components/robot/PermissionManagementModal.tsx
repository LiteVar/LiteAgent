import { useState, useEffect, useCallback } from 'react';
import { Modal } from '../ui/modal';
import Button from '../ui/button/Button';
import { RobotPermissionsDto } from '../../api/types.gen';
import { PermissionWithNames } from '../../hooks/useRobotPermissions';
import PermissionSelector from './PermissionSelector';

interface PermissionManagementModalProps {
  isOpen: boolean;
  robotCode: string;
  initialPermission?: PermissionWithNames | null;
  onSave: (permission: RobotPermissionsDto) => void;
  onCancel: () => void;
  loading: boolean;
}

export default function PermissionManagementModal({
  isOpen,
  robotCode,
  initialPermission,
  onSave,
  onCancel,
  loading,
}: PermissionManagementModalProps) {
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);
  const [selectedUserNames, setSelectedUserNames] = useState<string[]>([]);
  const [selectedDepartmentIds, setSelectedDepartmentIds] = useState<number[]>([]);
  const [selectedDepartmentNames, setSelectedDepartmentNames] = useState<string[]>([]);
  const [validationError, setValidationError] = useState<string>('');

  // 初始化或重置表单数据
  useEffect(() => {
    if (isOpen) {
      if (initialPermission) {
        // 支持新API结构（优先）和旧API结构
        const userIds = initialPermission.userList?.map(u => u.userId || '') || initialPermission.userIdList || [];
        const userNames = initialPermission.userList?.map(u => u.userName || '') || initialPermission.userNameList || [];
        const deptIds = initialPermission.departmentList?.map(d => d.departmentId || 0) || initialPermission.departmentIdList || [];
        const deptNames = initialPermission.departmentList?.map(d => d.departmentName || '') || initialPermission.departmentNameList || [];
        
        setSelectedUserIds(userIds.filter(id => id));
        setSelectedUserNames(userNames.filter(name => name));
        setSelectedDepartmentIds(deptIds.filter(id => id));
        setSelectedDepartmentNames(deptNames.filter(name => name));
      } else {
        setSelectedUserIds([]);
        setSelectedUserNames([]);
        setSelectedDepartmentIds([]);
        setSelectedDepartmentNames([]);
      }
      setValidationError('');
    }
  }, [isOpen, initialPermission]);

  // 处理权限选择变化
  const handlePermissionChange = (userIds: string[], userNames: string[], departmentIds: number[], departmentNames: string[]) => {
    setSelectedUserIds(userIds);
    setSelectedUserNames(userNames);
    setSelectedDepartmentIds(departmentIds);
    setSelectedDepartmentNames(departmentNames);

    // 清除验证错误
    if (validationError && (userIds.length > 0 || departmentIds.length > 0)) {
      setValidationError('');
    }
  };

  // 表单验证
  const validateForm = (): boolean => {
    if (selectedUserIds.length === 0 && selectedDepartmentIds.length === 0) {
      setValidationError('请至少选择一个用户或部门');
      return false;
    }
    return true;
  };

  // 处理保存
  const handleSave = () => {
    if (!validateForm()) {
      return;
    }

    const permissionData: RobotPermissionsDto = {
      robotCode,
      userList: selectedUserIds.length > 0 
        ? selectedUserIds.map((userId, index) => ({
            userId,
            userName: selectedUserNames[index] || ''
          }))
        : undefined,
      departmentList: selectedDepartmentIds.length > 0 
        ? selectedDepartmentIds.map((departmentId, index) => ({
            departmentId,
            departmentName: selectedDepartmentNames[index] || ''
          }))
        : undefined,
    };

    // 如果是编辑模式，需要包含权限ID
    if (initialPermission?.id) {
      permissionData.id = initialPermission.id;
    }

    onSave(permissionData);
  };

  // 处理取消
  const handleCancel = () => {
    setValidationError('');
    onCancel();
  };

  // 获取选择摘要
  const getSelectionSummary = useCallback(() => {
    const parts: string[] = [];
    if (selectedDepartmentIds?.length > 0) {
      parts.push(`${selectedDepartmentIds.length}个部门`);
    }
    if (selectedUserIds.length > 0) {
      parts.push(`${selectedUserIds.length}个用户`);
    }

    if (parts.length === 0) {
      return '未选择任何权限';
    }

    return `已选择: ${parts.join(', ')}`;
  }, [selectedDepartmentIds, selectedUserIds]);

  // 判断是否为编辑模式
  const isEditMode = !!initialPermission;

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleCancel}
      showCloseButton={false}
      className="w-full max-w-6xl max-h-[90vh] overflow-y-auto"
    >
      <div className="p-6 border-b border-stroke dark:border-strokedark">
        <h3 className="text-lg font-semibold text-black dark:text-white">
          {isEditMode ? '编辑机器人权限' : '配置机器人权限'}
        </h3>
        <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">机器人代码: {robotCode}</p>
      </div>

      <div className="p-6">
        {/* 选择摘要 */}
        <div className="mb-6 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium text-gray-900 dark:text-white">权限配置摘要</h4>
              <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">{getSelectionSummary()}</p>
            </div>
            {(selectedUserIds.length > 0 || selectedDepartmentIds?.length > 0) && (
              <div className="flex items-center text-green-600 dark:text-green-400">
                <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-sm">已配置</span>
              </div>
            )}
          </div>
        </div>

        {/* 验证错误提示 */}
        {validationError && (
          <div className="mb-6 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
            <div className="flex items-center">
              <svg
                className="w-5 h-5 text-red-500 mr-2"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <span className="text-sm text-red-700 dark:text-red-400">{validationError}</span>
            </div>
          </div>
        )}

        {/* 权限选择器 */}
        <div className="mb-6">
          <PermissionSelector
            robotCode={robotCode}
            selectedUserIds={selectedUserIds}
            selectedDepartmentIds={selectedDepartmentIds}
            initialPermission={initialPermission}
            onChange={handlePermissionChange}
          />
        </div>
      </div>

      {/* 操作按钮 */}
      <div className="flex justify-end space-x-4 p-6 border-t border-stroke dark:border-strokedark">
        <Button variant="outline" onClick={handleCancel} disabled={loading}>
          取消
        </Button>
        <button
          onClick={handleSave}
          disabled={loading}
          className="inline-flex items-center justify-center gap-2 rounded-lg transition px-5 py-3.5 text-sm bg-brand-500 text-white shadow-theme-xs hover:bg-brand-600 disabled:bg-brand-300 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {loading ? (
            <>
              <div className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-solid border-white border-r-transparent"></div>
              保存中...
            </>
          ) : isEditMode ? (
            '更新权限'
          ) : (
            '保存权限'
          )}
        </button>
      </div>
    </Modal>
  );
}
