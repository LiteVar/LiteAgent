import React, { useCallback, useMemo, useState } from 'react';
import { Tabs, Card, Tag, message, Image, TabsProps, Empty } from 'antd';
import { useNavigate } from 'react-router-dom';
import CreateAgentModal from './components/CreateAgentModal';
import { AgentCreateForm, AgentDTO, postV1AgentAdd } from '@/client';
import { getV1AgentAdminListOptions } from '@/client/@tanstack/query.gen';
import { useQuery } from '@tanstack/react-query';
import placeholderIcon from '@/assets/login/logo_black.png';
import { useWorkspace } from '@/contexts/workspaceContext';
import { UserType } from '@/types/User';
import { buildImageUrl } from '@/utils/buildImageUrl';
import Header from '@/components/workspace/Header';

export default function Agents() {
  const navigate = useNavigate();
  const [searchValue, setSearchValue] = useState('');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [tab, setTab] = useState<number>(0);
  const workspace = useWorkspace();

  const { data: agentListResult, refetch } = useQuery({
    ...getV1AgentAdminListOptions({
      query: {
        tab: tab,
      },
      headers: {
        'Workspace-id': workspace?.id || '',
      },
    }),
  });

  const filteredAgents: AgentDTO[] = useMemo(() => {
    return (
      agentListResult?.data?.filter(
        (agent) =>
          agent?.name?.toLowerCase().includes(searchValue.toLowerCase()) ||
          agent?.description?.toLowerCase().includes(searchValue.toLowerCase())
      ) || []
    );
  }, [agentListResult?.data, searchValue]);

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
    setTab(Number(key));
  };

  const showModal = () => {
    setIsModalVisible(true);
  };

  const handleCancel = () => {
    setIsModalVisible(false);
  };

  const handleOk = useCallback(
    async (values: AgentCreateForm) => {
      console.log('New Agent:', values);
      // 这里可以添加创建Agent的逻辑
      const res = await postV1AgentAdd({ body: values, headers: { 'Workspace-id': workspace?.id || '' } });
      if (res.error) {
        message.error('agent创建失败');
      } else {
        message.success('agent创建成功');
        refetch();
      }
      setIsModalVisible(false);
    },
    [refetch, workspace]
  );

  const navigateToAgent = (agentId: string) => {
    navigate(`/agent/${agentId}`);
  };

  return (
    <div className="space-y-4">
      <Header
        title="Agents管理"
        placeholder="搜索你的Agent"
        searchValue={searchValue}
        onSearchChange={setSearchValue}
        showCreateButton={Number(workspace?.role) !== UserType.Normal}
        createButtonText="新建Agent"
        onCreateClick={showModal}
      />

      <div className="flex justify-between items-center px-8">
        <Tabs defaultActiveKey="0" className="flex-grow" items={items} onChange={onChange} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 px-8 pb-8">
        {filteredAgents?.map((agent) => (
          <Card
            key={agent.id}
            className="hover:shadow-md transition-shadow"
            onClick={() => navigateToAgent(agent.id!)}
          >
            <div className="flex">
              <div className='w-[86%]'>
                <div className="flex items-center">
                  <div className='w-10'>
                    {agent.icon && (
                      <Image
                        preview={false}
                        src={buildImageUrl(agent.icon!)}
                        alt={`图标`}
                        width={32}
                        height={32}
                        className="mr-4 rounded"
                      />
                    )}
                    {!agent.icon && (
                      <div className="mr-4 rounded w-8 h-8 flex items-center justify-center bg-[#f5f5f5]">
                        <img
                          src={placeholderIcon}
                          alt={`图标`}
                          width={16}
                          height={16}
                        />
                      </div>
                    )}
                  </div>
                  <h3
                    title={agent.name}
                    className="text-lg font-semibold m-2 truncate"
                  >
                    {agent.name}
                  </h3>
                </div>

              </div>
              <div className=" flex items-center justify-end">
                {agent?.status === 0 && (
                  <Tag color="gold" className="!mt-2">
                    未发布
                  </Tag>
                )}
              </div>
            </div>
            {!!agent.autoAgentFlag && <p className='text-base text-gray-500 my-2'>类型：Auto Multi Agent</p>}
            <p className="text-gray-500 my-2 h-16 line-clamp-3">{agent.description}</p>
            {!agent.autoAgentFlag && <p style={{ marginBottom: 0 }} className='flex items-center text-gray-500 w-fit max-w-full'>
              <span className='w-2 h-2 bg-gray-500 rounded-full mr-2 flex-none'></span>
              <span className='flex-1 line-clamp-1 break-all'>{agent.createUser}</span>
              <span className='ml-2 flex-none'>创建</span>
            </p>}
          </Card>
        ))}
      </div>
      {filteredAgents.length === 0 && <Empty description="暂无数据" />}
      <CreateAgentModal visible={isModalVisible} onCancel={handleCancel} onOk={handleOk} />
    </div>
  );
}
