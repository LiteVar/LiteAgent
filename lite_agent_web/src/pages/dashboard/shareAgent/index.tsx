import React, { MouseEvent, useCallback, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import logoIcon from '@/assets/login/logo_svg';
import { useQuery } from '@tanstack/react-query';
import { getV1WorkspaceListOptions, getV1AgentByIdOptions } from '@/client/@tanstack/query.gen';
import { Button, message } from 'antd';

const ShareAgent = () => {
  const location = useLocation();
  const pathname = location.pathname;
  const workspaceId = pathname?.split('/')?.[2];
  const agentId = useLocation().pathname.split('/')[4];
  const navigate = useNavigate();
  const [showError, setShowError] = useState(false);

  const { data: workspaceListResult } = useQuery({
    ...getV1WorkspaceListOptions({}),
  });

  const { data: agentInfoResult } = useQuery({
    ...getV1AgentByIdOptions({
      path: {
        id: agentId,
      },
    }),
    enabled: !!agentId,
  });

  const hasJoinWorkspace = useMemo(() => {
    const hasJoinWorkspace = workspaceListResult?.data?.some((workspace) => workspace.id === workspaceId);
    if (hasJoinWorkspace) {
      setShowError(false);
    }
    return hasJoinWorkspace;
  }, [workspaceListResult?.data, workspaceId]);

  const onNavigateToChat = useCallback(
    (event: MouseEvent<HTMLElement>) => {
      event.stopPropagation();
      if (!agentInfoResult?.data?.agent?.shareFlag) {
        message.error('agent不存在，无法继续对话');
      } else if (hasJoinWorkspace) {
        setShowError(false);
        navigate(`/dashboard/${workspaceId}/chat/${agentId}`);
      } else {
        setShowError(true);
      }
    },
    [workspaceId, agentId, hasJoinWorkspace, agentInfoResult]
  );

  return (
    <div className="w-full flex h-[100vh] items-center justify-center overflow-hidden">
      <div className="flex items-center h-full">
        <div className="max-w-[400px] break-all px-4 flex flex-col items-center justify-center">
          {agentInfoResult?.data?.agent?.icon ? (
            <img className="w-[82px] h-[82px] rounded-md mb-12" src={agentInfoResult?.data?.agent?.icon} />
          ) : (
            <div className="customeSvg w-[82px] h-[82px] rounded-md mb-12 bg-[#F5F5F5] flex items-center justify-center">
              <span className="w-10 h-10 text-black">{logoIcon}</span>
            </div>
          )}
          <div className="text-6 mb-8">{agentInfoResult?.data?.agent?.name}</div>
          <div className="text-base text-[#C2C2C2] mb-24">{agentInfoResult?.data?.agent?.description}</div>
          <Button
            onClick={onNavigateToChat}
            type="primary"
            size="large"
            className="w-[300px] h-[50px] flex items-center justify-center"
          >
            开始聊天
          </Button>
          <div className={`text-[#ff4d4f] text-sm mt-2 ${showError ? 'opacity-100' : 'opacity-0'}`}>
            agent需要加入workspace才能使用，请联系管理员
          </div>
        </div>
      </div>
    </div>
  );
};

export default ShareAgent;
