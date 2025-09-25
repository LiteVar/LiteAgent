import { useState } from "react";
import PageMeta from "../components/common/PageMeta";
import Button from "../components/ui/button/Button";
import { message } from "../components/ui/message";
import { useRobots } from "../hooks/useRobots";
import { useRobotPermissions, PermissionWithNames } from "../hooks/useRobotPermissions";
import { AgentRobotRefDto, RobotPermissionsDto } from "../api/types.gen";
import RobotModal from "../components/robot/RobotModal";
import DeleteConfirmModal from "../components/robot/DeleteConfirmModal";
import PermissionDetailModal from "../components/robot/PermissionDetailModal";
import PermissionManagementModal from "../components/robot/PermissionManagementModal";
import RobotTable from "../components/robot/RobotTable";
import SearchForm from "../components/robot/SearchForm";
import Pagination from "../components/robot/Pagination";

export default function RobotManagement() {
  const {
    robots,
    pagination,
    loading,
    error,
    creating,
    updating,
    deleting,
    deleteRobot,
    createRobot,
    updateRobot,
    fetchRobots,
  } = useRobots();

  const {
    fetchPermissions,
    createPermission,
    updatePermission,
    deletePermission,
    creating: creatingPermission,
    updating: updatingPermission,
    clearPermissionCache,
  } = useRobotPermissions();


  const [searchTerm, setSearchTerm] = useState("");
  const [editingRobot, setEditingRobot] = useState<AgentRobotRefDto | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null);
  
  // 权限相关状态
  const [showPermissionDetail, setShowPermissionDetail] = useState(false);
  const [showPermissionManagement, setShowPermissionManagement] = useState(false);
  const [currentRobotCode, setCurrentRobotCode] = useState<string>("");
  const [currentPermission, setCurrentPermission] = useState<PermissionWithNames | null>(null);

  const handleSearch = (searchValue: string) => {
    setSearchTerm(searchValue);
    fetchRobots({ search: searchValue, pageNum: 1 });
  };

  const handleDelete = async (robotCode: string) => {
    const result = await deleteRobot(robotCode);
    if (result.success) {
      message.success(result.message);
      setShowDeleteConfirm(null);
    } else {
      message.error(result.message);
    }
  };

  const handleEdit = (robot: AgentRobotRefDto) => {
    setEditingRobot(robot);
    setShowModal(true);
  };

  const handleAdd = () => {
    setEditingRobot(null);
    setShowModal(true);
  };

  const handleSave = async (robotData: AgentRobotRefDto) => {
    let result;
    if (editingRobot?.id) {
      result = await updateRobot({ ...robotData, id: editingRobot.id });
    } else {
      result = await createRobot(robotData);
    }
    
    if (result.success) {
      message.success(result.message);
      setShowModal(false);
      setEditingRobot(null);
    } else {
      message.error(result.message);
    }
    fetchRobots();
  };

  const handlePageChange = (newPage: number) => {
    fetchRobots({ 
      pageNum: newPage, 
      pageSize: pagination.pageSize,
      search: searchTerm 
    });
  };

  // 权限详情处理
  const handleViewPermissions = async (robotPermissionsDTO: RobotPermissionsDto) => {
    setCurrentRobotCode(robotPermissionsDTO.robotCode);
    setCurrentPermission(robotPermissionsDTO as PermissionWithNames);
    setShowPermissionDetail(true);
  };

  // 权限管理处理
  const handleManagePermissions = async (robotCode: string) => {
    setCurrentRobotCode(robotCode);
    const permission = await fetchPermissions(robotCode);
    setCurrentPermission(permission);
    setShowPermissionManagement(true);
  };

  // 编辑权限（从详情弹框跳转到管理弹框）
  const handleEditPermissions = () => {
    setShowPermissionDetail(false);
    setShowPermissionManagement(true);
  };

  // 权限保存处理
  const handleSavePermissions = async (permissionData: RobotPermissionsDto) => {
    let result;
    
    if (currentPermission?.id) {
      // 更新权限
      result = await updatePermission(permissionData);
    } else {
      // 创建权限
      result = await createPermission(permissionData);
    }
    
    if (result.success) {
      message.success(result.message);
      setShowPermissionManagement(false);
      setCurrentPermission(null);
      setCurrentRobotCode("");
      // 清除权限缓存，确保数据一致性
      clearPermissionCache(permissionData.robotCode);
      fetchRobots();
    } else {
      message.error(result.message);
    }
  };

  // 权限删除处理
  const handleDeletePermissions = async () => {
    if (currentRobotCode) {
      const result = await deletePermission(currentPermission?.id || "", currentRobotCode);
      if (result.success) {
        message.success(result.message);
        setShowPermissionDetail(false);
        setCurrentPermission(null);
        setCurrentRobotCode("");
        fetchRobots();
      } else {
        message.error(result.message);
      }
    }
  };

  // 关闭权限弹框
  const handleClosePermissionModals = () => {
    setShowPermissionDetail(false);
    setShowPermissionManagement(false);
    setCurrentPermission(null);
    setCurrentRobotCode("");
  };

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="text-danger mb-4">❌ 加载失败</div>
          <p className="text-gray-600 dark:text-gray-400 mb-4">{error}</p>
          <Button onClick={() => fetchRobots()}>
            重新加载
          </Button>
        </div>
      </div>
    );
  }

  return (
    <>
      <PageMeta
        title="机器人管理 | LiteAgent DingTalk Admin"
        description="管理钉钉机器人与 LiteAgent Agent 的绑定关系"
      />
      <div className="grid grid-cols-12 gap-4 md:gap-6">
        <div className="col-span-12">
          <div className="rounded-lg border border-stroke bg-white p-6 shadow-default dark:border-strokedark dark:bg-boxdark">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-title-md2 font-semibold text-black dark:text-white">
                机器人管理
              </h2>
              <Button 
                onClick={handleAdd}
                disabled={creating}
                className="inline-flex items-center justify-center rounded-md bg-primary px-6 py-3 text-center font-medium text-white hover:bg-opacity-90 disabled:opacity-50"
              >
                {creating ? "添加中..." : "添加机器人"}
              </Button>
            </div>

            <div className="mb-6">
              <SearchForm 
                onSearch={handleSearch}
                loading={loading}
                initialValue={searchTerm}
              />
            </div>
            
            <RobotTable
              robots={robots}
              loading={loading}
              updating={updating}
              deleting={deleting}
              onEdit={handleEdit}
              onDelete={(robotCode) => setShowDeleteConfirm(robotCode)}
              onViewPermissions={handleViewPermissions}
              onManagePermissions={handleManagePermissions}
            />
            
            <Pagination
              currentPage={pagination.pageNum}
              totalSize={pagination.totalSize}
              pageSize={pagination.pageSize}
              loading={loading}
              onPageChange={handlePageChange}
            />
          </div>
        </div>
      </div>
      
      <RobotModal
        isOpen={showModal}
        robot={editingRobot}
        onSave={handleSave}
        onCancel={() => {
          setShowModal(false);
          setEditingRobot(null);
        }}
        loading={creating || updating}
      />
      
      <DeleteConfirmModal
        isOpen={!!showDeleteConfirm}
        robotCode={showDeleteConfirm || ""}
        onConfirm={() => handleDelete(showDeleteConfirm!)}
        onCancel={() => setShowDeleteConfirm(null)}
        loading={deleting}
      />
      
      {/* 权限详情弹框 */}
      <PermissionDetailModal
        isOpen={showPermissionDetail}
        robotCode={currentRobotCode}
        permission={currentPermission}
        onEdit={handleEditPermissions}
        onClose={handleClosePermissionModals}
        onDelete={handleDeletePermissions}
      />
      
      {/* 权限管理弹框 */}
      <PermissionManagementModal
        isOpen={showPermissionManagement}
        robotCode={currentRobotCode}
        initialPermission={currentPermission}
        onSave={handleSavePermissions}
        onCancel={handleClosePermissionModals}
        loading={creatingPermission || updatingPermission}
      />
    </>
  );
}