import React, { useMemo, useState } from 'react';
import Chat from '@/components/chat/Chat';
import PageLayout from '../layout';
import {useLocation} from 'react-router-dom';
import {useQuery} from "@tanstack/react-query";
import {getV1AgentByIdOptions} from "@/client/@tanstack/query.gen";

const ChatPage = () => {
  const agentId = useLocation().pathname.split('/')[4];
  const [agentMap, setAgentMap] = useState({});

  const {data: agentInfoResult} = useQuery({
    ...getV1AgentByIdOptions({
      path: {
        id: agentId
      }
    }),
    enabled: !!agentId,
  })

  const agentInfo = useMemo(() => {
    return agentInfoResult?.data
  }, [agentInfoResult])

  return (
    <PageLayout agentMap={agentMap}>
      <div className="flex-1 h-[100vh] overflow-hidden">
        <Chat setAgentMap={setAgentMap} agentId={agentId} mode="prod" agentInfo={agentInfo}/>
      </div>
    </PageLayout>
  );
};

export default ChatPage;
