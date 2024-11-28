import React, {useCallback, useMemo, useState} from 'react';
import {Input, Button, Tabs, Card, Tag, message, Image, TabsProps, Empty} from 'antd';
import { SearchOutlined, PlusOutlined } from '@ant-design/icons';
import {useNavigate} from "react-router-dom";
import CreateAgentModal from './components/CreateAgentModal';
import {AgentCreateForm, AgentDTO, postV1AgentAdd} from "@/client";
import { getV1AgentAdminListOptions } from '@/client/@tanstack/query.gen';
import { useQuery } from '@tanstack/react-query';
import placeholderIcon from '@/assets/dashboard/avatar.png'
import {useWorkspace} from "@/contexts/workspaceContext";
import {UserType} from "@/types/User";
import {buildImageUrl} from "@/utils/buildImageUrl";

export default function Agents() {
  const navigate = useNavigate();
  const [searchValue, setSearchValue] = useState('');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [tab, setTab] = useState<number>(0);
  const workspace = useWorkspace()

  const { data: agentListResult, refetch } = useQuery({
    ...getV1AgentAdminListOptions({
      query: {
        tab: tab
      },
      headers: {
        'Workspace-id': workspace?.id || '',
      },
    }),
  })

  const filteredAgents:AgentDTO[] = useMemo(() => {
    return agentListResult?.data?.filter(agent =>
      agent?.name?.toLowerCase().includes(searchValue.toLowerCase()) ||
      agent?.description?.toLowerCase().includes(searchValue.toLowerCase())
    ) || [];
  }, [agentListResult?.data, searchValue])

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
    console.log(key);
    setTab(Number(key));
  };

  const showModal = () => {
    setIsModalVisible(true);
  };

  const handleCancel = () => {
    setIsModalVisible(false);
  };

  const handleOk = useCallback(async (values: AgentCreateForm) => {
    console.log('New Agent:', values);
    // 这里可以添加创建Agent的逻辑
    const res = await postV1AgentAdd({body: values, headers: {"Workspace-id": workspace?.id || ""}});
    if (res.error) {
      message.error('agent创建失败');
    } else {
      message.success('agent创建成功');
      refetch();
    }
    setIsModalVisible(false);
  }, [refetch, workspace]);

  const navigateToAgent = (agentId: string) => {
    navigate(`/agent/${agentId}`);
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">Agents管理</h1>
        <Input
          placeholder="搜索"
          prefix={<SearchOutlined />}
          style={{ width: 300 }}
          value={searchValue}
          onChange={(e) => setSearchValue(e.target.value)}
        />
        {Number(workspace?.role) !== UserType.Normal &&
          <Button type="primary" icon={<PlusOutlined />} onClick={showModal}>新建Agent</Button>
        }
      </div>

      <div className="flex justify-between items-center">
        <Tabs defaultActiveKey="0" className="flex-grow" items={items} onChange={onChange} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredAgents?.map(agent => (
          <Card key={agent.id} className="hover:shadow-md transition-shadow" onClick={() => navigateToAgent(agent.id!)}>
            <div className="flex items-start justify-between">
              <div>
                <div className="flex items-center">
                  <Image
                    preview={false}
                    src={buildImageUrl(agent.icon!) || placeholderIcon}
                    alt={`图标`}
                    width={40}
                    height={40}
                    className="mr-4 rounded"
                  />
                  <h3 className="text-lg font-semibold m-2">{agent.name}</h3>
                </div>
                <p className="text-gray-500 mt-4 mb-2 line-clamp-3">{agent.description}</p>
              </div>
              <div>
                {agent?.shareTip && (
                  <Tag color="blue" className="!mt-2">已分享</Tag>
                )}
                {agent?.status === 0 && (
                  <Tag color="gold" className="!mt-2">未发布</Tag>
                )}
              </div>
            </div>
          </Card>
        ))}
      </div>
      {filteredAgents.length === 0 && (
        <Empty description="暂无数据" />
      )}
      <CreateAgentModal
        visible={isModalVisible}
        onCancel={handleCancel}
        onOk={handleOk}
      />

    </div>
  );
}
