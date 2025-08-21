import React, { useMemo, useState, MouseEvent, useCallback } from 'react';
import { Tabs, TabsProps, Empty, Skeleton } from 'antd';
import PageLayout from '../layout';
import { useQuery } from '@tanstack/react-query';
import { getV1AgentListOptions } from '@/client/@tanstack/query.gen';
import { useLocation, useNavigate } from 'react-router-dom';
import { AgentDTO } from '@/client';
import '../index.css';
import AgentDetailModal from './components/AgentDetailModal';
import Header from './components/Header';
import AgentCard from './components/AgentCard';

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
    retry: 1,
    cacheTime: 0,
    onError: (err) => {
      console.error('Agent列表请求失败:', err);
    }
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

  const renderAgentCards = () => {
    if (!filteredAgentList?.length) {
      return filteredAgentList?.length === 0 ? <Empty /> : <Skeleton />;
    }

    return (
      <div className="flex-wrap grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5">
        {filteredAgentList.map((agent) => (
          <AgentCard tab={tab} key={agent.id} agent={agent} onSelectAgent={onSelectAgent} />
        ))}
      </div>
    );
  };

  const TabChildren = useMemo(() => renderAgentCards(), [filteredAgentList]);

  const items: TabsProps['items'] = useMemo(
    () => [
      { key: '0', label: '全部', children: TabChildren },
      { key: '1', label: '系统', children: TabChildren },
      { key: '3', label: '我的', children: TabChildren },
    ],
    [TabChildren]
  );

  const onCancel = () => {
    setSelectedAgent(undefined);
    setOpenDetail(false);
  };

  const onChange = (key: string) => {
    console.log(key);
    setTab(Number(key));
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
      <div className="space-y-4 h-full overflow-hidden">
        <Header
          agentListType={agentListType}
          onSelectCloudType={onSelectCloudType}
          onSelectLocalType={onSelectLocalType}
          onSync={refetch}
          searchValue={searchValue}
          onSearch={handleSearch}
        />
        <div
          className="flex justify-between items-center overflow-hidden no-underline px-6 shopTab"
          style={{ height: 'calc(100% - 16px)' }}
        >
          {agentListType === EAgentListType.CLOUD && (
            <Tabs
              defaultActiveKey="0"
              className="flex-grow agentListTabs"
              items={items}
              onChange={onChange}
            />
          )}
          {agentListType === EAgentListType.LOCAL && (
            <div className="w-full h-full pb-20 overflow-y-auto">
              <div className="flex-wrap grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5">
                {filteredAgentList?.map((agent) => (
                  <AgentCard tab={tab} key={agent.id} agent={agent} onSelectAgent={onSelectAgent} />
                ))}
              </div>
              {filteredAgentList?.length === 0 && (
                <Empty description={searchValue ? '没有找到相关Agent' : '本地还没有创建过Agents'} />
              )}
            </div>
          )}
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
