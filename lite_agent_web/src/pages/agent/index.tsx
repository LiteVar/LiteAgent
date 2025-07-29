import React, { useCallback, useEffect, useRef, useState } from 'react';
import { message, Modal } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import ResponseCode from '@/constants/ResponseCode';
import Header from './components/Header';
import SideMenu from './components/SideMenu';
import SettingContent from './components/tabContent/SettingContent';
import EditContent from './components/tabContent/EditContent';
import ApiContent from './components/tabContent/ApiContent';
import LogsContent from './components/tabContent/LogsContent';

import { useHandleBackNavigation } from '@/hooks/useHandleBackNavigation';
import { getV1AgentAdminInfoByIdOptions } from '@/client/@tanstack/query.gen';
import {
  Agent,
  AgentDetailVO,
  AgentUpdateForm,
  deleteV1AgentById,
  putV1AgentById,
  putV1AgentReleaseById,
} from '@/client';
import { DEFAULT_Max_TOKENS } from './components/AdvancedSettingsPopover';

const AgentDetailPage: React.FC = () => {
  const agentId = useLocation().pathname.split('/')[2];
  const navigate = useNavigate();
  const [agentInfo, setAgentInfo] = useState<AgentDetailVO | null>(null);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const initialAgentInfoRef = useRef<AgentDetailVO | null>(null);
  const handleBackNavigation = useHandleBackNavigation();
  const [showMaxTokenWarning, setShowMaxTokenWarning] = useState(false);
  const [selectedTab, setSelectedTab] = useState(() => {
    const params = new URLSearchParams(window.location.search);
    return params.get('tab') || 'edit';
  });
  const [settingAgent, setSettingAgent] = useState<Agent | null>(null);

  const { data: agentData, refetch } = useQuery({
    ...getV1AgentAdminInfoByIdOptions({ path: { id: agentId } }),
    enabled: !!agentId,
  });

  const agentDetails = agentData?.data;
  const workspaceId = agentDetails?.agent?.workspaceId;
  const isDataReady = agentData?.code === ResponseCode.S_OK;

  useEffect(() => {
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      if (!hasUnsavedChanges) return;
      // 自定义提示文本（某些浏览器不再显示该文本，但仍可拦截
      const confirmationMessage = '你确定要离开此页面吗？未保存的数据将会丢失。';

      // 标准方式（多数现代浏览器会忽略 return 的字符串内容，但仍阻止关闭）
      event.preventDefault(); // 标准做法
      event.returnValue = confirmationMessage; // 兼容旧版浏览器
      return confirmationMessage; // 某些浏览器可能还需要这个
    }

    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    }

  }, [hasUnsavedChanges]);

  useEffect(() => {
    if (agentData?.code === ResponseCode.FA_HTTP_404) {
      message.error('未找到相关agent');
      handleBackNavigation();
    }
  }, [agentData, handleBackNavigation]);

  useEffect(() => {
    if (isDataReady) {
      setAgentInfo({
        ...agentDetails,
        agent: setDefaultAgentProperty(agentDetails?.agent!),
      });
      setSettingAgent({
        name: agentDetails?.agent?.name,
        icon: agentDetails?.agent?.icon,
        description: agentDetails?.agent?.description,
      });
      initialAgentInfoRef.current = agentDetails!;
    }
  }, [agentData]);

  const checkMaxTokenWarning = useCallback(() => {
    const agentMaxToken = agentInfo?.agent?.maxTokens;
    if (!agentMaxToken || !agentInfo?.model) return;

    const modelMaxToken = agentInfo?.model?.maxTokens || DEFAULT_Max_TOKENS;
    if (agentMaxToken > modelMaxToken) {
      setShowMaxTokenWarning(true);
    }
  }, [agentInfo]);

  useEffect(() => {
    checkMaxTokenWarning();
  }, [agentInfo]);

  const setDefaultAgentProperty = useCallback((agent: Agent) => {
    if (!agent.temperature) {
      agent.temperature = 0.0;
    }
    if (!agent.topP) {
      agent.topP = 1.0;
    }
    return agent;
  }, []);

  const filterDatasetIds = useCallback(() => {
    const datasetIds: string[] = [];
    const datasetList = agentInfo?.datasetList || [];

    datasetList.forEach((dataset) => {
      if (dataset.id) {
        datasetIds.push(dataset.id);
      }
    });

    return datasetIds;
  }, [agentInfo]);

  const checkModelSelected = useCallback((): boolean => {
    const llmId = agentInfo?.agent?.llmModelId || '';
    if (!llmId) {
      message.warning('请选择模型');
      return false;
    }
    return true;
  }, [agentInfo]);

  const handleSave = useCallback(async () => {
    if (!checkModelSelected()) {
      return;
    }
    // @ts-ignore
    const agentForm: AgentUpdateForm = {
      ...agentInfo?.agent,
      datasetIds: filterDatasetIds(),
      ...settingAgent,
    };
    console.log('Saving agentInfo:', agentForm);

    const res = await putV1AgentById({
      headers: {
        'Workspace-id': workspaceId!,
      },
      path: {
        id: agentId,
      },
      body: {
        ...agentForm,
      },
    });

    if (res?.data?.code === ResponseCode.S_OK) {
      message.success('保存成功');
      setHasUnsavedChanges(false);
      setShowMaxTokenWarning(false);
      await refetch();
    } else {
      message.error(res?.data?.message);
    }
  }, [agentInfo, workspaceId, agentId, refetch, settingAgent, checkModelSelected]);

  const handlePublish = useCallback(async () => {
    if (!checkModelSelected()) {
      return;
    }

    const res = await putV1AgentReleaseById({
      path: { id: agentId },
      headers: {
        'Workspace-id': workspaceId!,
      },
    });
    if (res?.data?.code === ResponseCode.S_OK) {
      await refetch();
      message.success('发布成功');
    } else {
      message.error(res?.data?.message);
    }
  }, [agentId, workspaceId, refetch, checkModelSelected]);

  const goAgentListPage = useCallback(() => {
    navigate(`/workspaces/${workspaceId}/agents`);
  }, [workspaceId]);

  const handleDeleteAgent = useCallback(async () => {
    Modal.confirm({
      title: '删除',
      centered: true,
      content: '即将删除该Agent的所有信息，确认删除？',
      onOk: async () => {
        await deleteV1AgentById({
          headers: {
            'Workspace-id': workspaceId!,
          },
          path: {
            id: agentId,
          },
        });
        message.success('删除成功');
        goAgentListPage();
      },
    });
  }, [workspaceId, agentId, goAgentListPage]);

  const handleTabChange = (key: string) => {
    setSelectedTab(key);
    navigate(`/agent/${agentId}?tab=${key}`);
  };

  if (!agentInfo) return <div>Loading...</div>;

  return (
    <div className="flex flex-col h-[100vh] overflow-hidden bg-gray-100">
      <Header
        className={'flex-none'}
        agentInfo={agentInfo}
        onSave={handleSave}
        handlePublish={handlePublish}
        handleDelete={handleDeleteAgent}
        hasUnsavedChanges={hasUnsavedChanges}
        showMaxTokenWarning={showMaxTokenWarning}
      />
      <main
        style={{ borderTop: '1px solid #ddd' }}
        className="flex flex-1 overflow-hidden bg-white"
      >
        <SideMenu canEdit={agentInfo.canEdit!} selectedTab={selectedTab} onTabChange={handleTabChange} />

        <div className="flex-1 h-full overflow-hidden">
          <EditContent
            visible={selectedTab === 'edit'}
            agentInfo={agentInfo}
            agentId={agentId}
            workspaceId={workspaceId!}
            setAgentInfo={setAgentInfo}
            setHasUnsavedChanges={setHasUnsavedChanges}
          />
          <ApiContent visible={selectedTab === 'api'} agentInfo={agentInfo} agentId={agentId} workspaceId={workspaceId!} />
          <LogsContent visible={selectedTab === 'logs'} agentId={agentId!} />
          <SettingContent visible={selectedTab === 'setting'} settingAgent={settingAgent!} setSettingAgent={setSettingAgent} />
        </div>
      </main>
    </div>
  );
};

export default AgentDetailPage;
