import React, { useMemo, useState, MouseEvent, useCallback } from 'react';
import { Skeleton } from 'antd';
import PageLayout from '../layout';
import { useQuery } from '@tanstack/react-query';
import { getV1AgentListOptions } from '@/client/@tanstack/query.gen';
import { useLocation, useNavigate } from 'react-router-dom';
import { AgentDTO } from '@/client';
import '../index.css';
import AgentDetailModal from './components/AgentDetailModal';
import Header from './components/Header';
import AgentCard from './components/AgentCard';
import { FilterAllIcon, FilterSystemIcon, FilterMyIcon } from '@/assets/agent/shop_filter_icons_svg';
import agentsEmpty from '@/assets/agent/agents_empty.svg';

export enum EAgentListType {
  'LOCAL',
  'CLOUD',
} 

const ShopPage: React.FC = () => {
  const pathname = useLocation().pathname;
  const navigate = useNavigate();
  const workspaceId = pathname?.split('/')?.[2];
  const [openDetail, setOpenDetail] = useState(false);
  const [selectedAgent, setSelectedAgent] = useState<AgentDTO>();
  const [tab, setTab] = useState(0);
  const [agentListType, setAgentListType] = useState(EAgentListType.CLOUD);
  const [searchValue, setSearchValue] = useState('');

  const { data: agentListResult, refetch } = useQuery({
    ...getV1AgentListOptions({
      headers: {
        'Workspace-id': workspaceId,
      },
      query: {
        tab: tab,
      },
    }),
    enabled: !!workspaceId,
    retry: false, // 禁用自动重试
  });
  const { data: agentList } = agentListResult || {};

  const onSelectAgent = useCallback((agent: AgentDTO, event: MouseEvent<HTMLElement>) => {
    event.stopPropagation();
    setSelectedAgent(agent);
    setOpenDetail(true);
  }, []);

  const filteredAgentList = useMemo(() => {
    if (!searchValue.trim() || !agentList) return agentList;
    
    return agentList.filter(agent => 
      (agent.name?.toLowerCase().includes(searchValue.toLowerCase()) ||
      agent.description?.toLowerCase().includes(searchValue.toLowerCase()))
    );
  }, [agentList, searchValue]);

  const filterOptions = useMemo(() => [
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
  ], []);

  const EmptyState = ({ text }: { text: string }) => (
    <div className="flex-1 flex items-center justify-center min-h-[400px]">
      <div className="relative w-[265px] h-[265px]">
      <div
          className=""
        >
          <img src={agentsEmpty} alt="agents empty" />
        </div>  
        <div
          className="absolute left-[105px] top-[243px] w-20 text-sm font-medium text-center text-[#1D4A6B]"
        >
          {text}
        </div>
      </div>
    </div>
  );

  const renderAgentList = () => {
    if (filteredAgentList === undefined) return <Skeleton />;
    if (filteredAgentList.length === 0) {
      const emptyText = searchValue
        ? '无搜索结果'
        : agentListType === EAgentListType.LOCAL
        ? '暂无本地Agent'
        : '暂无数据';
      return <EmptyState text={emptyText} />;
    }
    return (
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-2">
        {filteredAgentList.map((agent) => (
          <AgentCard tab={tab} key={agent.id} agent={agent} onSelectAgent={onSelectAgent} />
        ))}
      </div>
    );
  };

  const onCancel = () => {
    setSelectedAgent(undefined);
    setOpenDetail(false);
  };

  const onNavigateChatPage = useCallback(
    (event: MouseEvent<HTMLElement>) => {
      event.stopPropagation();
      onCancel();
      navigate(`/dashboard/${workspaceId}/chat/${selectedAgent?.id}`);
    },
    [navigate, workspaceId, selectedAgent]
  );

  const onSelectCloudType = () => {
    setAgentListType(EAgentListType.CLOUD);
    setTab(0);
  };

  const onSelectLocalType = () => {
    setAgentListType(EAgentListType.LOCAL);
    setTab(4);
  };

  const handleSearch = useCallback((value: string) => {
    setSearchValue(value);
  }, []);

  return (
    <PageLayout>
      <div className="flex flex-col h-full overflow-hidden pr-4 pb-4">
        {/* 顶部 Bar */}
        <Header
          agentListType={agentListType}
          onSelectCloudType={onSelectCloudType}
          onSelectLocalType={onSelectLocalType}
          onSync={refetch}
          searchValue={searchValue}
          onSearch={handleSearch}
        />

        {/* 内容区 */}
        <div className="flex-1 overflow-hidden flex flex-col gap-4">
          {/* Filter Bar（仅云端模式显示），激活态纯白背景，字重 500 */}
          {agentListType === EAgentListType.CLOUD && (
            <div className="flex-none flex items-center gap-2.5">
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
          )}

          {/* Agent 列表 */}
          <div className="flex-1 overflow-y-auto pb-4">
            {renderAgentList()}
          </div>
        </div>
      </div>
      <AgentDetailModal
        open={openDetail}
        agent={selectedAgent}
        onCancel={onCancel}
        onNavigateChatPage={onNavigateChatPage}
      />
    </PageLayout>
  );
};

export default ShopPage;
