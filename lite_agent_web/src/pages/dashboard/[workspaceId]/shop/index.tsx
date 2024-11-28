import React, {useMemo, useState, MouseEvent, useCallback} from 'react';
import {Tabs, TabsProps, Modal, Image, Card, Empty, Skeleton} from 'antd';
import {EllipsisOutlined} from '@ant-design/icons';
import PageLayout from '../layout';
import {useQuery} from '@tanstack/react-query';
import {getV1AgentListOptions} from '@/client/@tanstack/query.gen';
import {useLocation, useNavigate} from "react-router-dom";
import logo from "@/assets/login/logo.png";
import {buildImageUrl} from "@/utils/buildImageUrl";
import placeholderIcon from "@/assets/dashboard/avatar.png";
import {AgentDTO} from "@/client";

interface IAgentCard {
  agent: AgentDTO,
  onSelectAgent: (agent: AgentDTO, event: MouseEvent<HTMLElement>) => void
}

const AgentCard: React.FC<IAgentCard> = ({agent, onSelectAgent}) => {
  return (
    <Card className="hover:shadow-md transition-shadow">
      <div>
        <div className="flex items-center flex-none">
          <Image
            preview={false}
            src={buildImageUrl(agent.icon!) || placeholderIcon}
            alt={`图标`}
            width={40}
            height={40}
            className="mr-4 rounded"
          />
          <h3 className="text-lg font-semibold m-2 flex-1">{agent.name}</h3>
          <EllipsisOutlined onClick={e => onSelectAgent(agent, e)}
                            style={{fontSize: "24px", color: "#000"}} className="flex-none"/>
        </div>
        <p className="text-gray-500 mt-4 mb-2 line-clamp-3">{agent.description}</p>
      </div>
    </Card>

  )
}

const ShopPage: React.FC = () => {
  const pathname = useLocation().pathname;
  const navigate = useNavigate()
  const workspaceId = pathname?.split('/')?.[2];
  const [openDetail, setOpenDetail] = useState(false);
  const [selectedAgent, setSelectedAgent] = useState<AgentDTO>();
  const [tab, setTab] = useState(0);

  const {data: agentListResult} = useQuery({
    ...getV1AgentListOptions({
      headers: {
        'Workspace-id': workspaceId
      },
      query: {
        tab: tab
      }
    }),
    enabled: !!workspaceId,
  })
  const { data: agentList } = agentListResult || {};

  const onSelectAgent = useCallback((agent: AgentDTO, event: MouseEvent<HTMLElement>) => {
    event.stopPropagation();
    setSelectedAgent(agent);
    setOpenDetail(true);
  }, [])

  const renderAgentCards = () => {
    if (!agentList?.length) {
      return agentList?.length === 0 ? <Empty/> : <Skeleton/>;
    }

    return (
      <div className="flex flex-wrap grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5 mt-4">
        {agentList.map(agent => (
          <AgentCard key={agent.id} agent={agent} onSelectAgent={onSelectAgent}/>
        ))}
      </div>
    );
  };

  const TabChildren = useMemo(() => renderAgentCards(), [agentList]);

  const items: TabsProps['items'] = useMemo(() => [
    {key: '0', label: '全部', children: TabChildren},
    {key: '1', label: '系统', children: TabChildren},
    {key: '2', label: '来自分享', children: TabChildren},
    {key: '3', label: '我的', children: TabChildren},
  ], [TabChildren]);

  const onCancel = () => {
    setSelectedAgent(undefined);
    setOpenDetail(false);
  };

  const onChange = (key: string) => {
    console.log(key);
    setTab(Number(key));
  };

  const onNavigateChatPage = useCallback((event: MouseEvent<HTMLElement>) => {
    event.stopPropagation();
    onCancel();
    navigate(`/dashboard/${workspaceId}/chat/${selectedAgent?.id}`)
  },[navigate, workspaceId, selectedAgent])

  return (
    <PageLayout>
      <div className="space-y-4">
        <div className="flex justify-between items-center">
          <h1 className="text-2xl font-bold">Agents名称</h1>
        </div>

        <div className="flex justify-between items-center">
          <Tabs defaultActiveKey="0" className="flex-grow" items={items} onChange={onChange}/>
        </div>
      </div>
      <Modal
        title="Agent详情"
        closable
        onCancel={onCancel}
        className="!w-[538px]"
        footer={null}
        maskClosable={false}
        open={openDetail}
        centered
      >
        <div className="px-8 pt-10 flex flex-col items-center h-[500px]">
          <img className="w-[120px] h-[120px]" src={buildImageUrl(selectedAgent?.icon || "") || logo}/>
          <div className="font-xs mt-5">{selectedAgent?.name}</div>
          <div className="w-full mt-5 mb-16">
            <div className="font-xs mb-3">描述</div>
            <div className="font-xs text-black/25">{selectedAgent?.description}</div>
          </div>
          <div onClick={onNavigateChatPage}
               className="font-xs text-white w-[164px] h-8 flex items-center justify-center bg-[#1890FF] cursor-pointer">开始聊天
          </div>
        </div>
      </Modal>
    </PageLayout>
  );
};

export default ShopPage;
