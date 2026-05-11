import React, { useCallback, useMemo, useState } from 'react';
import { Card, Tag, message, Empty, Dropdown, Button } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';
import CreateAgentModal from './components/CreateAgentModal';
import ImportAgentPage from './ImportAgentPage';
import { AgentCreateForm, AgentDTO, postV1AgentAdd } from '@/client';
import { getV1AgentAdminListOptions } from '@/client/@tanstack/query.gen';
import { useQuery } from '@tanstack/react-query';
import { useWorkspace } from '@/contexts/workspaceContext';
import { UserType } from '@/types/User';
import { buildImageUrl } from '@/utils/buildImageUrl';
import Header from '@/components/workspace/Header';
import type { MenuProps } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { FilterAllIcon, FilterMyIcon, FilterSystemIcon } from '@/assets/agent/shop_filter_icons_svg';
import AgentIconSvg from '@/assets/common/agent_icon_svg';

enum CreateAgentType {
  CREATE = 'create',
  IMPORT = 'import',
}

export default function Agents() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [searchValue, setSearchValue] = useState('');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [tab, setTab] = useState<number>(0);
  const workspace = useWorkspace();

  // 检查是否显示导入页面
  const showImportPage = searchParams.get('action') === 'import';

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

  const showModal = () => {
    setIsModalVisible(true);
  };

  const handleBackFromImport = useCallback(() => {
    setSearchParams({});
  }, [setSearchParams]);

  const handleImportSuccess = useCallback(() => {
    refetch();
    setSearchParams({});
  }, [refetch, setSearchParams]);

  const handleCancel = () => {
    setIsModalVisible(false);
  };

  const handleOk = useCallback(
    async (values: AgentCreateForm) => {
      console.log('New Agent:', values);
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

  const showFormModal = useCallback((e: React.MouseEvent) => {
    e.stopPropagation();
    setIsModalVisible(true);
  }, []);

  const onImportFiles = useCallback((e: React.MouseEvent) => {
    e.stopPropagation();
    setSearchParams({ action: 'import' });
  }, [setSearchParams]);

  const createButton = useMemo(() => {
    if (!workspace?.role ||Number(workspace?.role) === UserType.Normal) return null;

    const items: MenuProps['items'] = [
    {
      label: (
        <div onClick={(e) => showFormModal(e)} className="text-[14px]">
          新建Agent
        </div>
      ),
      key: CreateAgentType.CREATE,
    },
    {
      label: (
        <div onClick={(e) => onImportFiles(e)} className="text-[14px]">
          导入
        </div>
      ),
      key: CreateAgentType.IMPORT,
    },
  ];

    return (
      <Dropdown menu={{ items }}>
        <a onClick={(e) => e.preventDefault()}>
          <Button
            className="rounded-xl bg-[#40A5EE] hover:!bg-[#40A5EE]/90 border-none shadow-md shadow-blue-200/50 flex items-center gap-2 h-10"
            icon={<PlusOutlined />}
            iconPosition='start'
            type="primary"
            size="large"
          >
            新建Agent
          </Button>
        </a>
      </Dropdown>
    )
  }, [workspace, onImportFiles, showFormModal]);

  // 如果显示导入页面，直接渲染导入页面
  if (showImportPage) {
    return (
      <ImportAgentPage
        onBack={handleBackFromImport}
        onSuccess={handleImportSuccess}
      />
    );
  }

  return (
    <div className="flex flex-col">
      <Header
        title="Agents管理"
        placeholder="搜索你的Agent"
        searchValue={searchValue}
        onSearchChange={setSearchValue}
        showCreateButton={false}
        onCreateClick={showModal}
        createButton={createButton}
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

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-2 px-4 pb-8 pt-4">
        {filteredAgents?.map((agent) => (
          <Card
            key={agent.id}
            className="bg-white/60 backdrop-blur-sm border-white/80 rounded-xl hover:shadow-lg transition-all cursor-pointer overflow-hidden border"
            bodyStyle={{ padding: '22px 16px' }}
            onClick={() => navigateToAgent(agent.id!)}
          >
            <div className="flex flex-col gap-4">
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2.5 flex-1 min-w-0">
                  <div className="w-10 h-10 flex-shrink-0 bg-white rounded-lg flex items-center justify-center border border-white/80 overflow-hidden shadow-sm">
                    {agent.icon ? (
                      <img
                        src={buildImageUrl(agent.icon!)}
                        alt={agent.name}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <AgentIconSvg seed={agent.id} />
                    )}
                  </div>
                  <h3 className="text-[14px] font-medium text-[#383F44] truncate" title={agent.name}>
                    {agent.name}
                  </h3>
                </div>
                {agent?.status === 0 && (
                  <Tag color="gold" className="m-0 border-none rounded-full px-2 text-[10px]">
                    未发布
                  </Tag>
                )}
              </div>
              
              <div className="space-y-2">
                <p className="text-[12px] text-[#58636C] h-[40px] break-all line-clamp-2 leading-[20px]">
                  {agent.description || '暂无描述'}
                </p>
                
                <div className="flex items-center justify-between mt-auto">
                  {!agent.autoAgentFlag ? (
                    <div className="flex items-center gap-2 text-[12px] text-[#94A0AB]">
                      <span className="w-2 h-2 bg-[#94A0AB] rounded-full" />
                      <span className="truncate max-w-[100px]">{agent.createUser}</span>
                      <span>创建</span>
                    </div>
                  ) : (
                    <div className="text-[12px] text-[#58636C]">
                      类型： Auto Muti Agent
                    </div>
                  )}
                  
                  <Button 
                    className="border-[#40A5EE] text-[#40A5EE] rounded-lg h-8 px-4 text-[12px] hover:bg-[#40A5EE] hover:text-white"
                    onClick={(e) => {
                      e.stopPropagation();
                      navigateToAgent(agent.id!);
                    }}
                  >
                    详情
                  </Button>
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
      
      {filteredAgents.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20">
          <Empty description="暂无数据" />
        </div>
      )}
      <CreateAgentModal visible={isModalVisible} onCancel={handleCancel} onOk={handleOk} />
    </div>
  );
}