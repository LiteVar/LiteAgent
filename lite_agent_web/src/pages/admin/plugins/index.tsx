import { Button, message, Modal, Popconfirm, Switch, Spin } from 'antd';
import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Header from '@/components/workspace/Header';
import type { Plugin } from '@/client';
import pluginConnectIcon from '@/assets/plugin/plugin_connect.png';
import ResponseCode from '@/constants/ResponseCode';
import PluginFormModal from './components/PluginFormModal';

import { 
  getV1PluginConnector,
  getV1PluginList,
  postV1PluginByIdEnable,
  postV1PluginByIdDisable,
  deleteV1PluginById
} from '@/client';

export default function Plugins() {
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingPlugin, setEditingPlugin] = useState<Plugin | undefined>(undefined);
  const [searchValue, setSearchValue] = useState('');
  const [loadingPlugins, setLoadingPlugins] = useState<Map<string, boolean>>(new Map());
  const [checkingDisable, setCheckingDisable] = useState<string | null>(null);
  const [disableConfirmLoading, setDisableConfirmLoading] = useState(false);
  const [disableConfirm, setDisableConfirm] = useState<{
    plugin: Plugin;
    connectorCount: number;
  } | null>(null);
  const [enableConfirmPlugin, setEnableConfirmPlugin] = useState<Plugin | null>(null);

  // 获取插件列表
  const { data, refetch, isLoading } = useQuery({
    queryKey: ['pluginList'],
    queryFn: async () => {
      const res = await getV1PluginList({});
      return res?.data;
    },
  });

  // 根据搜索值过滤数据
  const filteredPlugins = useMemo(() => {
    let plugins = data?.data || [];
    
    if (searchValue.trim()) {
      const searchLower = searchValue.toLowerCase();
      plugins = plugins.filter(plugin => 
        plugin.name?.toLowerCase().includes(searchLower) ||
        plugin.description?.toLowerCase().includes(searchLower)
      );
    }
    
    return plugins;
  }, [data?.data, searchValue]);

  // 打开新建/编辑弹框
  const showModal = (plugin?: Plugin) => {
    setEditingPlugin(plugin);
    setIsModalVisible(true);
  };

  // 关闭弹框
  const closeModal = () => {
    setIsModalVisible(false);
    setEditingPlugin(undefined);
  };

  // 处理插件操作成功
  const handleSuccess = () => {
    refetch();
  };

  // 删除插件
  const handleDelete = async (id: string) => {
    try {
      const res = await deleteV1PluginById({
        path: { id }
      });

      if (res?.data?.code === ResponseCode.S_OK) {
        message.success('删除插件成功');
        refetch();
      } else {
        message.error(res?.data?.message || '删除插件失败');
      }
    } catch (error) {
      message.error('删除插件失败');
    }
  };

  // 切换插件状态
  const handleToggleStatus = async (plugin: Plugin, enabled: boolean) => {
    setLoadingPlugins(prev => new Map(prev).set(plugin.id!, enabled));
    try {
      const res = enabled
        ? await postV1PluginByIdEnable({ path: { id: plugin.id! } })
        : await postV1PluginByIdDisable({ path: { id: plugin.id! } });

      if (res?.data?.code === ResponseCode.S_OK) {
        message.success(enabled ? '启动插件成功' : '关闭插件成功');
        refetch();
      } else {
        message.error(res?.data?.message || '操作失败');
      }
    } catch (error) {
      message.error('操作失败');
    } finally {
      setLoadingPlugins(prev => {
        const next = new Map(prev);
        next.delete(plugin.id!);
        return next;
      });
    }
  };

  // Switch 切换处理：首次开启时二次确认；关闭时先检查上线中的智连数量
  const handleSwitchChange = async (plugin: Plugin, checked: boolean) => {
    if (checked) {
      if (plugin.status === 0) {
        setEnableConfirmPlugin(plugin);
        return;
      }
      handleToggleStatus(plugin, true);
      return;
    }

    setCheckingDisable(plugin.id!);
    try {
      const res = await getV1PluginConnector({
        query: { pluginId: plugin.id, status: 2 },
      });

      if (res?.data?.code === ResponseCode.S_OK) { 
        const connectors = res?.data?.data ?? [];
        if (connectors.length > 0) {
          setDisableConfirm({ plugin, connectorCount: connectors.length });
        } else {
          await handleToggleStatus(plugin, false);
        }
      } else {
        message.error(res?.data?.message || '操作失败');
      }
    } catch {
      message.error('操作失败');
    } finally {
      setCheckingDisable(null);
    }
  };

  // 确认关闭插件
  const handleConfirmDisable = async () => {
    if (!disableConfirm) return;
    setDisableConfirmLoading(true);
    try {
      await handleToggleStatus(disableConfirm.plugin, false);
      setDisableConfirm(null);
    } finally {
      setDisableConfirmLoading(false);
    }
  };

  return (
    <div className="py-4 h-full flex flex-col overflow-hidden space-y-6">
      <Header
        title="插件管理"
        placeholder="搜索插件名称或描述"
        showCreateButton={true}
        showSearch={false}
        createButtonText="新增插件"
        onCreateClick={() => showModal()}
        onSearchChange={setSearchValue}
      />

      <div className="flex-1 overflow-auto bg-white/60 backdrop-blur-md border border-white/80 rounded-2xl mx-4 mb-4">
        <Spin spinning={isLoading}>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-3 gap-6 p-6">
            {filteredPlugins.map((plugin) => {
              const isCardLoading = loadingPlugins.has(plugin.id!) || checkingDisable === plugin.id;
              return (
                <div
                  key={plugin.id}
                  className="relative bg-white/40 border border-white/60 rounded-xl p-5 hover:bg-white/60 hover:shadow-lg transition-all flex flex-col justify-between"
                >
                  <div className="flex flex-col">
                    {/* 顶部：图标、标题和开关 */}
                    <div className="flex items-center gap-4 mb-2">
                      {/* 插件图标 */}
                      <div
                        className="flex-shrink-0 w-12 h-12 rounded-xl flex items-center justify-center overflow-hidden"
                        style={{
                          background: plugin.icon ? 'transparent' : 'linear-gradient(135deg, #1D4A6B 0%, #3B5EA6 100%)',
                        }}
                      >
                        {plugin.icon ? (
                          <img
                            src={plugin.icon}
                            alt={plugin.name || '插件图标'}
                            className="w-full h-full object-cover rounded-xl"
                          />
                        ) : (
                          <img src={pluginConnectIcon} alt="default icon" className="w-full h-full" />
                        )}
                      </div>

                      {/* 标题和开关 */}
                      <div className="flex-1 min-w-0 flex items-center justify-between gap-2">
                        <h3 className="text-[16px] font-semibold truncate" title={plugin.name}>
                          {plugin.name}
                        </h3>
                        <Switch
                          checked={plugin.status === 2}
                          loading={isCardLoading}
                          onChange={(checked) => handleSwitchChange(plugin, checked)}
                        />
                      </div>
                    </div>

                    {/* 描述 */}
                    <p className="text-[13px] text-[#383F44] m-0 break-all line-clamp-2 leading-relaxed" style={{ minHeight: '2.6em' }} title={plugin.description}>
                      {plugin.description || ''}
                    </p>
                  </div>

                  <div className="flex justify-end gap-2 pt-4 border-t border-white/30">
                    <Popconfirm
                      title="确定删除该插件吗？"
                      onConfirm={() => handleDelete(plugin.id!)}
                      okText="确定"
                      cancelText="取消"
                    >
                      <Button
                        danger
                        style={{ borderColor: '#ff4d4f', color: '#ff4d4f', background: 'transparent' }}
                      >
                        删除
                      </Button>
                    </Popconfirm>
                    <Button
                      onClick={() => showModal(plugin)}
                      style={{ borderColor: '#40A5EE', color: '#40A5EE', background: 'transparent' }}
                    >
                      编辑
                    </Button>
                  </div>

                  {/* 切换状态时的遮罩层 */}
                  {isCardLoading && (
                    <div
                      className="absolute inset-0 flex flex-col items-center justify-center gap-1 rounded-lg bg-black/70 z-10"
                      style={{ pointerEvents: 'all' }}
                    >
                      <Spin size="small" />
                      <span className="text-white text-sm whitespace-nowrap">
                        {plugin.name}, 正在{loadingPlugins.get(plugin.id!) ? '启动' : '关闭'}中...
                      </span>
                    </div>
                  )}
                </div>
            )})}
          </div>

          {/* 空状态 */}
          {!isLoading && filteredPlugins.length === 0 && (
            <div className="flex flex-col items-center justify-center py-20 text-[#7C8B98]">
              <div className="text-lg font-medium mb-2">{searchValue ? '未找到匹配的插件' : '暂无插件'}</div>
              {!searchValue && <div>点击右上角“新增插件”开始创建</div>}
            </div>
          )}
        </Spin>
      </div>

      {/* 新建/编辑插件弹框 */}
      <PluginFormModal
        open={isModalVisible}
        editingPlugin={editingPlugin}
        onClose={closeModal}
        onSuccess={handleSuccess}
      />

      {/* 关闭插件确认弹框 */}
      <Modal
        open={!!disableConfirm}
        title="关闭插件"
        okText="确定"
        cancelText="取消"
        confirmLoading={disableConfirmLoading}
        onOk={handleConfirmDisable}
        onCancel={() => setDisableConfirm(null)}
      >
        <p>
          {disableConfirm?.plugin.name}，存在 {disableConfirm?.connectorCount} 个正在使用的插件智连，请确认是否要关闭。
        </p>
      </Modal>

      {/* 首次开启插件确认弹框 */}
      <Modal
        open={!!enableConfirmPlugin}
        title="开启插件"
        okText="确定"
        cancelText="取消"
        onOk={() => {
          if (enableConfirmPlugin) {
            handleToggleStatus(enableConfirmPlugin, true);
            setEnableConfirmPlugin(null);
          }
        }}
        onCancel={() => setEnableConfirmPlugin(null)}
      >
        <p>插件一旦开启，将无法删除，请确认是否开启？</p>
      </Modal>
    </div>
  );
}

