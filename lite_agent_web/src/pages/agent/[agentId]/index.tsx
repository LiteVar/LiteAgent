import React, {useCallback, useEffect, useRef, useState} from 'react';
import {message, Modal} from 'antd';
import {useLocation} from "react-router-dom";
import Header from '../components/Header';
import AgentSettings from '../components/AgentSettings';
import ToolsList from '../components/ToolsList';
import AddToolModal from '../components/AddToolModal';
import {
  getV1AgentAdminInfoByIdOptions,
  getV1ModelListOptions,
  getV1ToolListOptions
} from "@/client/@tanstack/query.gen";
import {
  Agent,
  AgentDetailVO,
  AgentUpdateForm,
  deleteV1AgentById,
  postV1ChatInitSession,
  putV1AgentById,
  putV1AgentReleaseById,
  ToolDTO,
  postV1ChatClearSession, ModelDTO
} from "@/client";
import {useQuery} from '@tanstack/react-query';
import ShareModal from "@/pages/agent/components/ShareModal";
import Chat from '@/components/Chat';
import {useHandleBackNavigation} from "@/hooks/useHandleBackNavigation";
import ResponseCode from "@/config/ResponseCode";
import ReadonlyAgentInfo from "@/pages/agent/components/ReadonlyAgentInfo";

const AgentDetailPage: React.FC = () => {
  const agentId = useLocation().pathname.split('/')[2];
  const [agentInfo, setAgentInfo] = useState<AgentDetailVO | null>(null);
  const [toolList, setToolList] = useState<ToolDTO[]>([]);
  const [modelList, setModelList] = useState<ModelDTO[]>([]);
  const [shareModalVisible, setShareModalVisible] = useState(false);
  const [addToolModalVisible, setAddToolModalVisible] = useState(false);
  const [editingName, setEditingName] = useState(false);
  const [selectedToolTab, setSelectedToolTab] = useState<string>("0");
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)
  const initialAgentInfoRef = useRef<AgentDetailVO | null>(null);
  const handleBackNavigation = useHandleBackNavigation()

  const {data: agentData, refetch} = useQuery({
    ...getV1AgentAdminInfoByIdOptions({path: {id: agentId}}),
    enabled: !!agentId,
  })

  const agentDetails = agentData?.data;
  const workspaceId = agentDetails?.agent?.workspaceId;
  const isDataReady = agentData?.code === ResponseCode.S_OK;

  useEffect(() => {
    if (agentData?.code === ResponseCode.FA_HTTP_404) {
      message.error("未找到相关agent")
      handleBackNavigation()
    }
  }, [agentData])

  const {data: tools} = useQuery({
    ...getV1ToolListOptions({
      headers: {
        'Workspace-id': workspaceId!
      },
      query: {
        name: "",
        tab: Number(selectedToolTab)
      }
    }),
    enabled: isDataReady,
  })

  const {data: models} = useQuery({
    ...getV1ModelListOptions({
      headers: {
        'Workspace-id': workspaceId!
      },
      query: {
        pageNo: 0,
        pageSize: 100000000
      }
    }),
    enabled: isDataReady,
  })

  useEffect(() => {
    if (isDataReady) {
      setAgentInfo({
        ...agentDetails,
        agent: setDefaultAgentProperty(agentDetails?.agent!),
      });
      // todo 对比应该有两种，1.当前编辑表单是否变化 2.当前保存的草稿和已发布的版本是否一致
      initialAgentInfoRef.current = agentDetails!;
    }
  }, [agentData]);

  const setDefaultAgentProperty = useCallback((agent: Agent) => {
    if (!agent.temperature) {
      agent.temperature = 0.0;
    }
    if (!agent.topP) {
      agent.topP = 1.0;
    }
    return agent;
  }, []);

  useEffect(() => {
    if (tools?.data) {
      setToolList(tools?.data || []);
    }
    if (models?.data) {
      setModelList(models?.data?.list || []);
    }
  }, [tools, models]);

  const handleSave = useCallback(async () => {
    // @ts-ignore
    const agentForm: AgentUpdateForm = {
      ...agentInfo?.agent,
    }
    console.log("Saving agentInfo:", agentForm);
    const res = await putV1AgentById({
      headers: {
        'Workspace-id': workspaceId!
      },
      path: {
        id: agentId
      },
      body: {
        ...agentForm,
      }
    })

    setEditingName(false);
    if (res?.data?.code === ResponseCode.S_OK) {
      message.success("保存成功");
      setHasUnsavedChanges(false)
      await refetch()
    } else {
      message.error("保存失败");
    }

  }, [agentInfo, workspaceId, agentId, refetch]);

  const handlePublish = useCallback(async () => {
    const res = await putV1AgentReleaseById({
      path: {id: agentId},
      headers: {
        'Workspace-id': workspaceId!
      }
    })
    if (res?.data?.code === ResponseCode.S_OK) {
      await refetch()
      message.success("发布成功")
    } else {
      message.error(res?.data?.message)
    }
  }, [agentId, workspaceId, refetch]);

  const handleShare = useCallback(() => {
    setShareModalVisible(true)
  }, [])

  const handleDeleteAgent = useCallback(async () => {
    Modal.confirm({
      title: '删除',
      content: '即将删除该Agent的所有信息，确认删除？',
      onOk: async () => {
        await deleteV1AgentById({
          headers: {
            'Workspace-id': workspaceId!
          },
          path: {
            id: agentId
          }
        })
        message.success("删除成功");
        handleBackNavigation()
      }
    })
  }, [workspaceId, agentId])

  const toggleTool = useCallback((tool: ToolDTO) => {
    // @ts-ignore
    setAgentInfo(prev => {
      if (prev?.agent?.toolIds?.length) {
        const toolId = tool.id;
        const toolInfo = toolList.find(t => t.id === toolId);
        const selectedToolIds = [...prev.agent.toolIds];
        let selectedTools = [...(prev.toolList || [])];
        const toolIdIndex = prev.agent.toolIds.indexOf(toolId!);
        if (toolIdIndex > -1) {
          selectedToolIds.splice(toolIdIndex, 1);
          selectedTools = selectedTools.filter(t => t.id !== toolId)
        } else {
          selectedToolIds.push(toolId!);
          // @ts-ignore
          selectedTools.push(toolInfo!);
        }
        return {...prev, agent: {...prev?.agent, toolIds: selectedToolIds}, toolList: selectedTools};
      } else {
        return {...prev, agent: {...prev?.agent, toolIds: [tool.id!]}, toolList: [tool]};
      }
    });
    setHasUnsavedChanges(true)
  }, [toolList]);

  const handleEditTool = () => {
    setAddToolModalVisible(true)
  };

  const handleDeleteTool = useCallback((toolId: string) => {
    setAgentInfo(prev => {
      const toolIds = prev?.agent?.toolIds?.filter(id => id !== toolId);
      const toolList = prev?.toolList?.filter(t => t.id !== toolId);
      return {...prev, agent: {...prev?.agent, toolIds: toolIds}, toolList: toolList};
    });
    setHasUnsavedChanges(true)
  }, []);

  if (!agentInfo) return <div>Loading...</div>;

  return (
    <div className="flex flex-col h-[100vh] overflow-hidden bg-gray-100">
      <Header
        className={'flex-none'}
        agentInfo={agentInfo}
        editingName={editingName}
        onEdit={() => setEditingName(true)}
        onSave={handleSave}
        handlePublish={handlePublish}
        handleShare={handleShare}
        handleDelete={handleDeleteAgent}
        onCancel={() => setEditingName(false)}
        hasUnsavedChanges={hasUnsavedChanges}
        setHasUnsavedChanges={setHasUnsavedChanges}
        setAgentName={(name) => setAgentInfo({...agentInfo, agent: {...agentInfo.agent, name}})}
      />
      <main className="flex-grow flex p-6 flex-1 overflow-hidden">
        {agentInfo?.canEdit &&
          <div className="w-1/3 mr-6 bg-white rounded p-4 flex flex-col">
            <AgentSettings agentInfo={agentInfo} modelList={modelList} setAgentInfo={
              (agentInfo: AgentDetailVO) => {
                setAgentInfo(agentInfo)
                setHasUnsavedChanges(true)
              }}/>
            <ToolsList
              tools={agentInfo?.toolList || []}
              onAddTool={() => setAddToolModalVisible(true)}
              onEditTool={handleEditTool}
              onDeleteTool={handleDeleteTool}
            />
          </div>
        }
        {!agentInfo?.canEdit &&
          <div className="w-1/3 mr-6 bg-white rounded p-4 flex flex-col">
            <ReadonlyAgentInfo agentInfo={agentInfo}/>
          </div>
        }
        <div className="w-2/3 bg-white rounded h-full">
          <Chat mode="dev" agentId={agentId} agentInfo={agentInfo} />
        </div>
      </main>

      <AddToolModal
        agentInfo={agentInfo}
        visible={addToolModalVisible}
        onCancel={() => setAddToolModalVisible(false)}
        toggleTool={toggleTool}
        toolList={toolList}
        selectedToolTab={selectedToolTab}
        setSelectedToolTab={setSelectedToolTab}
      />

      <ShareModal
        visible={shareModalVisible}
        onClose={() => setShareModalVisible(false)}
        agentInfo={agentInfo}
      />
    </div>
  );
};

export default AgentDetailPage;
