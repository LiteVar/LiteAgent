import React, {useCallback, useMemo, useState} from 'react';
import { Card, message, Empty, Skeleton } from 'antd';
import CreateToolModal from "./components/CreateToolModal";
import ToolInfoModal from "./components/ToolInfoModal";
import { useQuery } from '@tanstack/react-query';
import {getV1ToolListOptions} from "@/client/@tanstack/query.gen";
import {
  postV1ToolAdd,
  ToolDTO,
  deleteV1ToolById,
  ToolProvider,
  putV1ToolUpdate,
  getV1ToolExportById,
  ToolVOAddAction,
  ToolVOUpdateAction,
} from '@/client';
import {useWorkspace} from "@/contexts/workspaceContext";
import {UserType} from "@/types/User";
import ResponseCode from "@/constants/ResponseCode";
import ToolIcon from './components/tool-icon';
import Header from '@/components/workspace/Header';
import FileExportModal from '@/components/workspace/FileExportModal';
import { FilterAllIcon, FilterMyIcon, FilterSystemIcon } from '@/assets/agent/shop_filter_icons_svg';

export default function Tools() {
  const [searchValue, setSearchValue] = useState('');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [isInfoModalVisible, setIsInfoModalVisible] = useState(false);
  const [isExportModalVisible, setIsExportModalVisible] = useState(false);
  const [editingTool, setEditingTool] = useState<ToolDTO | ToolProvider | undefined>(undefined);
  const [exportTool, setExportTool] = useState<ToolDTO | ToolProvider | undefined>(undefined);
  const [toolInfo, setToolInfo] = useState<ToolDTO | undefined>(undefined);
  const [tab, setTab] = useState<number>(0);
  const workspace = useWorkspace()
  const toolListQuery = useMemo(
    () =>
      getV1ToolListOptions({
        query: {
          name: '',
          tab: Number(tab),
        },
        headers: {
          'Workspace-id': workspace?.id || '',
        },
      }),
    [tab, workspace?.id]
  );

  const {data, isLoading, refetch} = useQuery({
    ...toolListQuery,
  })

  const tools = useMemo(() => data?.data || [], [data]);

  const filteredTools:ToolDTO[] = useMemo(() => {
    return tools?.filter(tool =>
      tool?.name?.toLowerCase().includes(searchValue.toLowerCase()) ||
      tool?.description?.toLowerCase().includes(searchValue.toLowerCase())
    );
  }, [tools, searchValue]);

  const handleSearch = useCallback(() => {
    console.log('searchValue', searchValue);
  }, [searchValue]);

  const showEditingToolModal = (tool: ToolDTO | undefined) => {
    setEditingTool(tool);
    setIsModalVisible(true);
  };

  const handleCancel = () => {
    setIsModalVisible(false);
    setEditingTool(undefined);
  };

  const showExportModal = (event: React.MouseEvent, model: ToolDTO | ToolProvider) => {
    event.stopPropagation();
    setIsExportModalVisible(true);
    setExportTool(model);
  };

  const closeExportModal = () => {
    setIsExportModalVisible(false);
    setExportTool(undefined);
  };

  const showInfoModal = (tool: ToolDTO) => {
    setToolInfo(tool);
    setIsInfoModalVisible(true);
  }

  const deleteTool = useCallback(async (toolId: string) => {
    await deleteV1ToolById({
      path: {id: toolId},
      headers: {'Workspace-id': workspace?.id || ''},
    })
    await refetch()
  }, [workspace, refetch])

  const handleCancelInfo = () => {
    setIsInfoModalVisible(false);
    setToolInfo(undefined);
  }

  const handleOk = useCallback(async (id: string, values: ToolVOAddAction | ToolVOUpdateAction) => {
    let res;
    if (id) {
      res = await putV1ToolUpdate({body: {...values as ToolVOUpdateAction, id}, headers: {'Workspace-id': workspace?.id || ''}});
    } else {
      res = await postV1ToolAdd({body: values as ToolVOAddAction, headers: {'Workspace-id': workspace?.id || ''}});
    }
    if (res?.data?.code === ResponseCode.S_OK) {
      const refetchResult = await refetch();
      if (id) {
        const listPayload = refetchResult.data as { data?: ToolDTO[] } | undefined;
        const nextTool = listPayload?.data?.find((t) => t.id === id);
        if (nextTool) {
          setToolInfo((prev) => (prev?.id === id ? { ...prev, ...nextTool } : prev));
        }
      }
      message.success(`${editingTool ? '更新' : '创建'}工具成功`);
      setIsModalVisible(false);
      setEditingTool(undefined);
      return ResponseCode.S_OK;
    } else {
      return res?.data?.code!;
    }
  }, [editingTool, refetch, workspace]);

  const handleDelete = useCallback(async (id: string) => {
    await deleteTool(id);
    message.success('删除工具成功');
    setIsModalVisible(false);
    setEditingTool(undefined);
  }, [deleteTool]);

  const onExportFile = useCallback(async (id: string, checked: boolean) => {
    try {
      const res = await getV1ToolExportById({ 
        path: { id: id },
        query: {
          plainText: checked,
        },
      });

      if (!res.data) {
        message.error('导出工具失败');
        return;
      }
      
      message.success('导出工具成功');
      closeExportModal();

      const text = JSON.stringify(res.data, null, 2);
      const blob = new Blob([text], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${res.data.name}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error('导出工具失败:', error);
      message.error('导出工具失败，请稍后重试');
    }
  }, []);

  const filterOptions = [
    {
      key: 0, label: '全部',
      Icon: FilterAllIcon,
    },
    {
      key: 1, label: '系统',
      Icon: FilterSystemIcon,
    },
    {
      key: 3, label: '我的',
      Icon: FilterMyIcon,
    },
  ];

  return (
    <div className="flex flex-col">
      <Header
        title="工具管理"
        placeholder="搜索你的工具"
        searchValue={searchValue}
        onSearchChange={setSearchValue}
        onSearch={handleSearch}
        showCreateButton={Number(workspace?.role) !== UserType.Normal}
        createButtonText="新建工具"
        onCreateClick={() => showEditingToolModal(undefined)}
      />

      <div className="flex gap-2.5 px-4">
        {filterOptions.map((option) => (
            <button
              key={option.key}
              onClick={() => setTab(option.key)}
              className={`flex items-center gap-1 px-2 py-1.5 rounded-xl cursor-pointer border-none transition-all ${
                tab === option.key
                  ? 'text-[#383F44] shadow-sm bg-white'
                  : 'bg-transparent text-[#58636C] hover:bg-white/50'
              }`}
              style={{
                fontSize: 14,
                fontWeight: 500,
              }}
            >
              <span className="w-5 h-5 flex-none flex items-center justify-center">
                <option.Icon 
                  active={tab === option.key} 
                  color={tab === option.key ? '#383F44' : '#58636C'} 
                />
              </span>
              <span>{option.label}</span>
            </button>
          ))}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-2 px-4 pb-8 pt-4 auto-rows-fr">
        {filteredTools.map(tool => (
          <Card 
            key={tool.id} 
            className="bg-white/60 backdrop-blur-sm border-white/80 rounded-xl hover:shadow-lg transition-all cursor-pointer overflow-hidden border h-full"
            bodyStyle={{ padding: '22px 16px', height: '100%' }}
            onClick={() => showInfoModal(tool)}
          >
            <div className="flex flex-col h-full justify-between gap-4">
              <div>
                <div className="flex items-center gap-2.5">
                  <div className="w-10 h-10 flex-shrink-0 bg-white rounded-lg flex items-center justify-center border border-white/80 overflow-hidden shadow-sm">
                    <ToolIcon iconName={tool.icon} />
                  </div>
                  <h3 className="text-[14px] font-medium text-[#383F44] truncate">{tool.name}</h3>
                </div>
                <p className="text-[12px] text-[#58636C] h-[40px] break-all line-clamp-2 leading-[20px] mt-4">
                  {tool.description || '暂无描述'}
                </p>
              </div>
              <div className="flex items-center gap-2 text-[12px] text-[#94A0AB] mt-auto">
                <span className="w-2 h-2 bg-[#94A0AB] rounded-full flex-none" />
                <span className="truncate flex-1 min-w-0">{tool.createUser} 创建</span>
              </div>
            </div>
          </Card>
        ))}
      </div>

      {filteredTools.length === 0 && !isLoading && (
        <div className="flex flex-col items-center justify-center py-20">
          <Empty description="暂无数据" />
        </div>
      )}
      {isLoading && (
        <div className="px-8 mt-4">
          <Skeleton active />
        </div>
      )}

      <CreateToolModal
        visible={isModalVisible}
        onCancel={handleCancel}
        onOk={handleOk}
        onDelete={handleDelete}
        showExportModal={showExportModal}
        initialData={editingTool}
      />

      <ToolInfoModal
        visible={isInfoModalVisible}
        onClose={handleCancelInfo}
        toolInfo={toolInfo}
        deleteTool={deleteTool}
        showEditingToolModal={showEditingToolModal}
        showExportModal={showExportModal}
      />

      <FileExportModal title="工具" visible={isExportModalVisible && !!exportTool?.id} id={exportTool?.id} onClose={closeExportModal} onOk={onExportFile} />
    </div>
  );
}
