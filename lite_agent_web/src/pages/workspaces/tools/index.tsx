import React, {useCallback, useMemo, useState} from 'react';
import { Tabs, Card, message, Empty, Skeleton } from 'antd';
import type { TabsProps } from 'antd';
import CreateToolModal from "./components/CreateToolModal";
import ToolInfoModal from "./components/ToolInfoModal";
import {useQuery} from '@tanstack/react-query'
import {getV1ToolListOptions} from "@/client/@tanstack/query.gen";
import {postV1ToolAdd, ToolDTO, deleteV1ToolById, ToolProvider, putV1ToolUpdate, getV1ToolExportById} from '@/client';
import {useWorkspace} from "@/contexts/workspaceContext";
import {UserType} from "@/types/User";
import ResponseCode from "@/constants/ResponseCode";
import ToolIcon from './components/tool-icon';
import Header from '@/components/workspace/Header';
import FileExportModal from '@/components/workspace/FileExportModal';

export default function Tools() {
  const [searchValue, setSearchValue] = useState('');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [isInfoModalVisible, setIsInfoModalVisible] = useState(false);
  const [isExportModalVisible, setIsExportModalVisible] = useState(false);
  const [editingTool, setEditingTool] = useState<ToolDTO | ToolProvider | undefined>(undefined);
  const [exportTool, setExportTool] = useState<ToolDTO | ToolProvider | undefined>(undefined);
  const [toolInfo, setToolInfo] = useState<ToolDTO | undefined>(undefined);
  const [tab, setTab] = useState('0');
  const workspace = useWorkspace()

  const {data, isLoading, refetch} = useQuery({
    ...getV1ToolListOptions({
      query: {
        name: "", // 这里似乎搜索似乎不需要去更新，前端手动筛选即可。toolNameValue || undefined
        tab: Number(tab)
      },
      headers: {
        'Workspace-id': workspace?.id || '',
      },
    })
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

  const showExportModal = (event: any, model: ToolDTO | ToolProvider) => {
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

  const handleOk = useCallback(async (id: string, values: any) => {
    let res;
    if (id) {
      res = await putV1ToolUpdate({body: {id, ...values}, headers: {'Workspace-id': workspace?.id || ''}});
    } else {
      res = await postV1ToolAdd({body: values, headers: {'Workspace-id': workspace?.id || ''}});
    }
    if (res?.data?.code === ResponseCode.S_OK) {
      await refetch()
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

  const items: TabsProps['items'] = [
    {
      key: '0',
      label: '全部',
    },
    {
      key: '1',
      label: '系统',
    },
    {
      key: '3',
      label: '我的',
    },
  ];

  const onChange = (key: string) => {
    setTab(key);
  };

  return (
    <div className="space-y-4">
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

      <div className="flex justify-between items-center px-8">
        <Tabs defaultActiveKey="0" className="flex-grow" items={items} onChange={onChange} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 px-8 pb-8 auto-rows-fr">
        {filteredTools.map(tool => (
          <Card key={tool.id} className="hover:shadow-md transition-shadow h-full" onClick={() => showInfoModal(tool)}>
            <div className="flex flex-col h-full justify-between">
              <div>
                <div className="flex items-center">
                  <ToolIcon iconName={tool.icon} />
                  <h3 className="text-lg font-semibold m-2">{tool.name}</h3>
                </div>
                <p className="h-10 text-gray-500 my-2 line-clamp-3">
                  {tool.description || ' '}
                </p>
              </div>
              <p style={{ marginBottom: 0 }} className="flex items-center text-gray-500 w-fit max-w-full">
                <span className="w-2 h-2 bg-gray-500 rounded-full mr-2 flex-none"></span>
                <span className="flex-1 line-clamp-1 break-all">{tool.createUser}</span>
                <span className="ml-2 flex-none">创建</span>
              </p>
            </div>
          </Card>
        ))}
      </div>

      {
        filteredTools.length === 0 && !isLoading && (
          <Empty className="mt-10" description="暂无数据" />
        )
      }
      {isLoading &&
        <Skeleton />
      }

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
