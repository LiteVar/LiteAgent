import React, { useCallback, useState } from 'react';
import { Skeleton, Modal, message } from 'antd';
import { useWorkspace } from '@/contexts/workspaceContext';
import { PaginationConfig } from 'antd/es/pagination';
import Header from '@/components/workspace/Header';
import PluginConnectList from './components/PluginConnectList';
import CreateEditModal from './components/CreateEditModal';
import ConfigModal from './components/ConfigModal';
import ResponseCode from '@/constants/ResponseCode';
import { UserType } from "@/types/User";

import { 
  getV1PluginConnector, // 智连列表
  postV1PluginConnectorByIdStatus, // 设置智连上下线
  deleteV1PluginConnectorById // 删除智连
} from '@/client';
import type { PluginConnect } from '@/types/plugin-connect';


const PluginConnectsPage: React.FC = () => {
  const [searchValue, setSearchValue] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [showCreateModal, setShowCreateModal] = useState<boolean>(false);
  const [showConfigModal, setShowConfigModal] = useState<boolean>(false);
  const [editingConnect, setEditingConnect] = useState<PluginConnect | undefined>();
  const [configuringConnectId, setConfiguringConnectId] = useState<string | undefined>(undefined);
  const [configuringBasicInfo, setConfiguringBasicInfo] = useState<{
    id?: string;
    name: string;
    pluginId: string;
    pluginName?: string;
    icon?: string;
    description?: string;
  } | undefined>(undefined);
  const [pluginConnects, setPluginConnects] = useState<PluginConnect[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const workspace = useWorkspace();
  const [pagination, setPagination] = useState<PaginationConfig>({
    current: 1,
    pageSize: 12,
  });

  // 加载插件智连列表
  const loadPluginConnects = useCallback(async () => {
    if (!workspace?.id) return;
    
    setIsLoading(true);
    try {
      const response = await getV1PluginConnector({
        headers: {
          'Workspace-id': workspace.id,
        },
      });
      
      if (response.data?.data) {
        const connectors = response.data.data as PluginConnect[];
        // 客户端过滤和分页
        const filtered = searchTerm 
          ? connectors.filter(item => 
              item.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
              item.pluginName?.toLowerCase().includes(searchTerm.toLowerCase())
            )
          : connectors;
        
        const start = (pagination.current! - 1) * pagination.pageSize!;
        const end = start + pagination.pageSize!;
        setPluginConnects(filtered.slice(start, end));
      }
    } catch (error) {
      message.error('加载插件智连列表失败');
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  }, [workspace?.id, pagination.current, pagination.pageSize, searchTerm]);

  // 初始加载
  React.useEffect(() => {
    loadPluginConnects();
  }, [loadPluginConnects]);

  // 搜索处理
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const handleSearch = useCallback(() => {
    setSearchTerm(searchValue);
    setPagination((prev) => ({
      ...prev,
      current: 1,
      pageSize: 12,
    }));
  }, [searchValue]);

  // 打开配置弹框
  const handleConfig = useCallback((id: string) => {
    setConfiguringConnectId(id);
    setShowConfigModal(true);
  }, []);

  // 编辑
  const handleEdit = useCallback((connect: PluginConnect) => {
    setEditingConnect(connect);
    setShowCreateModal(true);
  }, []);

  // 下线
  const handleOffline = useCallback(
    (id: string) => {
      Modal.confirm({
        centered: true,
        title: '下线插件智连',
        content: '下线后，Agent 将从绑定端下架，用户不可见，确认下线？',
        onOk: async () => {
          if (!workspace?.id) return;
          try {
            await postV1PluginConnectorByIdStatus({
              path: { id },
              query: { status: 3 }, // 3-下线
              headers: {
                'Workspace-id': workspace.id,
              },
            });
            message.success('下线成功');
            loadPluginConnects();
          } catch (error) {
            message.error('下线失败');
            console.error(error);
          }
        },
      });
    },
    [workspace?.id, loadPluginConnects]
  );

  // 上线
  const handleOnline = useCallback(async (id: string) => {
    if (!workspace?.id) return;
    try {
      const res = await postV1PluginConnectorByIdStatus({
        path: { id },
        query: { status: 2 }, // 2-上线
        headers: {
          'Workspace-id': workspace.id,
        },
      });

      if (res?.data?.code === ResponseCode.S_OK) {
        message.success('上线成功');
        loadPluginConnects();
      } else {
        message.error(res?.data?.message || '上线失败');
      }
    } catch (error) {
      message.error('上线失败');
      console.error(error);
    }
  }, [workspace?.id, loadPluginConnects]);

  // 删除
  const handleDelete = useCallback(
    (id: string) => {
      Modal.confirm({
        centered: true,
        title: '删除插件智连',
        content: '删除后，插件智连的配置将无法恢复，确认删除？',
        onOk: async () => {
          if (!workspace?.id) return;
          try {
            const response = await deleteV1PluginConnectorById({
              path: { id },
              headers: {
                'Workspace-id': workspace.id,
              },
            });

            console.log('response:', response);

            if (response.data.code === 200) {
              message.success('删除成功');
              loadPluginConnects();
            } else {
              message.error('删除失败');
            }
          } catch (error) {
            message.error('删除失败');
            console.error(error);
          }
        },
      });
    },
    [workspace?.id, loadPluginConnects]
  );

  // 打开数据分析页面
  const handleAnalytics = useCallback((id: string) => {
    const connect = pluginConnects.find(c => c.id === id);
    const name = connect?.name || '插件智连';
    window.open(`/plugin-connect-analytics/${id}?name=${encodeURIComponent(name)}`, '_blank');
  }, [pluginConnects]);

  // 关闭新增/编辑弹框
  const handleCloseModal = useCallback(() => {
    setShowCreateModal(false);
    setEditingConnect(undefined);
  }, []);

  // 新增/编辑基础信息完成
  const handleSaveSuccess = useCallback(
    (connectId?: string, basicInfo?: {
      id?: string;
      name: string;
      pluginId: string;
      pluginName?: string;
      icon?: string;
      description?: string;
    }) => {
      setShowCreateModal(false);
      setEditingConnect(undefined);
      
      // 只在新建时打开配置弹框
      if (basicInfo) {
        setConfiguringConnectId(connectId);
        setConfiguringBasicInfo(basicInfo);
        setShowConfigModal(true);
      } else {
        // 编辑模式：刷新列表
        loadPluginConnects();
      }
    },
    [loadPluginConnects]
  );

  // 关闭配置弹框
  const handleCloseConfigModal = useCallback(() => {
    setShowConfigModal(false);
    setConfiguringConnectId(undefined);
    setConfiguringBasicInfo(undefined);
  }, []);

  // 配置提交成功
  const handleConfigSuccess = useCallback(() => {
    setShowConfigModal(false);
    setConfiguringConnectId(undefined);
    setConfiguringBasicInfo(undefined);
    loadPluginConnects();
  }, [loadPluginConnects]);

  return (
    <div className="flex flex-col h-full overflow-hidden">
      <Header
        title="插件智连"
        placeholder="搜索你的智连"
        searchValue={searchValue}
        onSearchChange={setSearchValue}
        onSearch={handleSearch}
        createButtonText="新建智连"
        showCreateButton={Number(workspace?.role) !== UserType.Normal}
        onCreateClick={() => setShowCreateModal(true)}
      />

      <div className="flex-1 overflow-y-auto px-4 pb-8 mt-4">
        {isLoading ? (
          <Skeleton active className="bg-white/60 backdrop-blur-md p-8 rounded-2xl border border-white/80" />
        ) : (
          <PluginConnectList
            pluginConnects={pluginConnects}
            onConfig={handleConfig}
            onEdit={handleEdit}
            onOffline={handleOffline}
            onOnline={handleOnline}
            onDelete={handleDelete}
            onAnalytics={handleAnalytics}
          />
        )}
      </div>

      {showCreateModal && (
        <CreateEditModal
          visible={showCreateModal}
          editingConnect={editingConnect}
          workspaceId={workspace?.id!}
          onClose={handleCloseModal}
          onSuccess={handleSaveSuccess}
        />
      )}

      {showConfigModal && (
        <ConfigModal
          visible={showConfigModal}
          pluginConnectId={configuringConnectId}
          basicInfo={configuringBasicInfo}
          onClose={handleCloseConfigModal}
          onSuccess={handleConfigSuccess}
        />
      )}
    </div>
  );
};

export default PluginConnectsPage;

