import React, {useCallback, useMemo, useState} from 'react';
import { Input, Button, Tabs, Card, Tag, message, Empty, Skeleton, Image } from 'antd';
import type { TabsProps } from 'antd';
import { SearchOutlined, PlusOutlined } from '@ant-design/icons';
import CreateToolModal from "./components/CreateToolModal";
import ToolInfoModal from "./components/ToolInfoModal";
import {useQuery} from '@tanstack/react-query'
import {getV1ToolListOptions} from "@/client/@tanstack/query.gen";
import {postV1ToolAdd, ToolDTO, deleteV1ToolById, ToolProvider, putV1ToolUpdate} from '@/client';
import placeholderIcon from '@/assets/dashboard/avatar.png'
import {useWorkspace} from "@/contexts/workspaceContext";
import {UserType} from "@/types/User";
import ResponseCode from "@/config/ResponseCode";

export default function Tools() {
  const [searchValue, setSearchValue] = useState('');
  const [toolNameValue, setToolNameValue] = useState('');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [isInfoModalVisible, setIsInfoModalVisible] = useState(false);
  const [editingTool, setEditingTool] = useState<ToolDTO | ToolProvider | undefined>(undefined);
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
    setToolNameValue(searchValue);
  }, [searchValue]);

  const showEditingToolModal = (tool: ToolDTO | undefined) => {
    setEditingTool(tool);
    setIsModalVisible(true);
  };

  const handleCancel = () => {
    setIsModalVisible(false);
    setEditingTool(undefined);
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

  const handleOk = useCallback(async (id:string, values: any) => {
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
      return true;
    } else {
      message.error(res?.data?.message || '操作失败');
      return false;
    }
  }, [editingTool, refetch, workspace]);

  const handleDelete = useCallback(async (id: string) => {
    await deleteTool(id);
    message.success('删除工具成功');
    setIsModalVisible(false);
    setEditingTool(undefined);
  }, [deleteTool]);

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
      key: '2',
      label: '来自分享',
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
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">工具管理</h1>
        <Input
          placeholder="搜索"
          style={{ width: 300 }}
          value={searchValue}
          onChange={(e) => setSearchValue(e.target.value)}
          prefix={<SearchOutlined onClick={handleSearch} />}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
        />
        {Number(workspace?.role) !== UserType.Normal &&
          <Button type="primary" icon={<PlusOutlined />} onClick={() => showEditingToolModal(undefined)}>新建工具</Button>
        }
      </div>

      <div className="flex justify-between items-center">
        <Tabs defaultActiveKey="0" className="flex-grow" items={items} onChange={onChange} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredTools.map(tool => (
          <Card key={tool.id} className="hover:shadow-md transition-shadow" onClick={() => showInfoModal(tool)}>
            <div className="flex items-start justify-between">
              <div>
                <div className="flex items-center">
                  <Image
                    src={placeholderIcon}
                    preview={false}
                    alt={`${tool.name} 图标`}
                    width={40}
                    height={40}
                    className="mr-4 rounded"
                  />
                  <h3 className="text-lg font-semibold m-2">{tool.name}</h3>
                </div>
                <p className="text-gray-500 mt-4 mb-2 line-clamp-3">{tool.description}</p>
              </div>
              {tool.shareTip && (
                <Tag color="blue" className="!mt-2">已分享</Tag>
              )}
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
        initialData={editingTool}
      />

      <ToolInfoModal
        visible={isInfoModalVisible}
        onClose={handleCancelInfo}
        toolInfo={toolInfo}
        deleteTool={deleteTool}
        showEditingToolModal={showEditingToolModal}
      />

    </div>
  );
}
