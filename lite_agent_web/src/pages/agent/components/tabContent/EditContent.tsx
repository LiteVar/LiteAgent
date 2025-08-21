import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { message } from 'antd';
import { Agent, AgentDetailVO, DatasetsVO, ModelDTO, ToolDTO, FunctionVO, getV1ToolListWithFunction, Account } from '@/client';
import AgentBaseSet from '../AgentBaseSet';
import ToolsList from '../ToolsList';
import Dataset from '../dataset';
import Convert from '../convert';
import AgentSet from '../agent-set';
import SubAgent from '../sub-agent';
import MultiAgent from '../multi-agent';
import ReadonlyAgentInfo from '../ReadonlyAgentInfo';
import Chat from '@/components/chat/Chat';
import AddToolModal from '../AddToolModal';
import AddDatasetModal from '../modal/add-dataset';
import SequenceModal from '../modal/sequence';
import SubAgentModal from '../modal/sub-agent';
import {
  getV1AgentAdminListOptions,
  getV1DatasetListOptions,
  getV1ModelListOptions,
  getV1ToolListWithFunctionOptions,
  getV1WorkspaceListOptions
} from '@/client/@tanstack/query.gen';
import { useQuery } from '@tanstack/react-query';
import { AgentTypeMode } from '../agent-set';
import { AutoEnableType } from '../multi-agent';
import ResponseCode from "@/constants/ResponseCode";
import { DEFAULT_Max_TOKENS } from '../AdvancedSettingsPopover';
import '../../style/index.css';
import AutoAgentType from '../autoAgentType';
import AutoAgentModelList from '../autoAgentModelList';
import AutoAgentToolsList from '../autoAgentToolList';
import AutoAgentBaseSet from '../autoAgentBaseSet';
import { AgentType } from '@/types/chat';

interface EditContentProps {
  agentInfo: AgentDetailVO;
  agentId: string;
  workspaceId: string;
  visible: boolean;
  setAgentInfo: (info: AgentDetailVO) => void;
  setHasUnsavedChanges: (value: boolean) => void;
}

const EditContent: React.FC<EditContentProps> = (props) => {
  const { agentId, workspaceId, agentInfo, setAgentInfo, setHasUnsavedChanges, visible } = props
  const [toolList, setToolList] = useState<ToolDTO[]>([]);
  const [datasetList, setDatasetList] = useState<DatasetsVO[]>([]);
  const [modelList, setModelList] = useState<ModelDTO[]>([]);
  const [audioModelList, setAudioModelList] = useState<ModelDTO[]>([]);
  const [agentList, setAgentList] = useState<Agent[]>([]);

  const [addToolModalVisible, setAddToolModalVisible] = useState(false);
  const [isShowAddDatasetModal, setIsShowAddDatasetModal] = useState(false);
  const [isShowSequenceModal, setIsShowSequenceModal] = useState(false);
  const [isShowSubAgentModal, setIsShowSubAgentModal] = useState(false);
  const [selectedToolTab, setSelectedToolTab] = useState<string>('0');

  const { data: tools } = useQuery({
    ...getV1ToolListWithFunctionOptions({
      headers: {
        'Workspace-id': workspaceId!,
      },
      query: {
        name: '',
        tab: Number(selectedToolTab),
      },
    }),
    enabled: !!workspaceId,
  });

  const { data: models } = useQuery({
    ...getV1ModelListOptions({
      headers: {
        'Workspace-id': workspaceId!,
      },
      query: {
        pageNo: 0,
        pageSize: 100000000,
      },
    }),
    enabled: !!workspaceId,
  });

  const { data: datasets } = useQuery({
    ...getV1DatasetListOptions({
      query: {
        pageNo: 0,
        pageSize: 10000,
      },
      headers: {
        'Workspace-id': workspaceId!,
      },
    }),
    enabled: !!workspaceId,
  });

  const { data: agents } = useQuery({
    ...getV1AgentAdminListOptions({
      query: {
        tab: 0,
      },
      headers: {
        'Workspace-id': workspaceId!,
      },
    }),
  });

  const toggleTool = useCallback((func: FunctionVO) => {
    // @ts-ignore
    setAgentInfo((prev: AgentDetailVO) => {

      const agentFunList = prev?.agent?.functionList || [];
      const funcList = prev?.functionList || [];

      if (agentFunList.find(t => t.functionId === func.functionId)) {
        const newAgentFunList = agentFunList.filter(t => t.functionId !== func.functionId);
        const newFunList = funcList.filter(t => t.functionId !== func.functionId);

        return {
          ...prev,
          agent: { ...prev?.agent, functionList: newAgentFunList },
          functionList: newFunList
        };

      } else {
        const newAgentFunList = [
          ...agentFunList, {
            functionId: func.functionId,
            mode: 0 as AgentTypeMode,
          }
        ]
        const newFunList = [...funcList, { ...func, mode: 0 }];

        return {
          ...prev,
          agent: { ...prev?.agent, functionList: newAgentFunList },
          functionList: newFunList
        };
      }
    });
    setHasUnsavedChanges(true)
  }, []);

  const refreshToolList = useCallback(async () => {
    try {
      const res = await getV1ToolListWithFunction({
        headers: {
          'Workspace-id': workspaceId!
        },
        query: { tab: 0 }
      });

      if (res?.data?.code === ResponseCode.S_OK) {
        setToolList(addToolNameIconToFunc(res.data?.data || []));
        message.success('新建工具成功');
      } else {
        message.error('获取工具列表失败');
      }
    } catch (error) {
      console.error('刷新工具列表时出错:', error);
      message.error('刷新工具列表时发生错误');
    }
  }, [workspaceId]);

  const toggleSequence = useCallback((func: FunctionVO, removeIndex?: number) => {
    if (removeIndex !== undefined) {
      // @ts-ignore
      setAgentInfo((prev: AgentDetailVO) => {
        const agentSequence = prev?.agent?.sequence || [];

        return {
          ...prev,
          agent: {
            ...prev?.agent,
            sequence: agentSequence.filter((_, index) => index !== removeIndex)
          }
        };
      })
    } else {
      // @ts-ignore
      setAgentInfo((prev: AgentDetailVO) => {
        const agentSequence = prev?.agent?.sequence || [];

        return {
          ...prev,
          agent: {
            ...prev?.agent,
            sequence: [...agentSequence, func.functionId]
          }
        };
      })
    }
  }, []);

  const restoreSequence = useCallback((sequence: string[]) => {
    // @ts-ignore
    setAgentInfo(prev => ({
      ...prev,
      agent: {
        ...prev?.agent,
        sequence: [...sequence]
      }
    }));
  }, []);

  const onChangeMode = useCallback((func: FunctionVO, mode: AgentTypeMode) => {
    // @ts-ignore
    setAgentInfo((prev: AgentDetailVO) => {
      const agentFunList = prev?.agent?.functionList || [];
      const funcList = prev?.functionList || [];

      const newAgentFunList = agentFunList.map(t => {
        if (t.functionId === func.functionId) {
          return {
            ...t,
            mode: mode
          }
        }
        return t;
      });

      const newFuncList = funcList.map(t => {
        if (t.functionId === func.functionId) {
          return {
            ...t,
            mode: mode
          }
        }
        return t;
      });

      return {
        ...prev,
        agent: {
          ...prev?.agent,
          functionList: newAgentFunList
        },
        functionList: newFuncList
      };
    })
  }, []);

  const addToolNameIconToFunc = useCallback((toolList: ToolDTO[]) => {
    return toolList?.map(tool => {
      return {
        ...tool,
        functionList: tool.functionList?.map(func => {
          return {
            ...func,
            toolName: tool.name,
            icon: tool.icon
          };
        }),
      };
    });
  }, []);

  const handleEditTool = () => {
    setAddToolModalVisible(true)
  };

  const goDatasetPage = useCallback((datasetId: string) => {
    window.open(`/dataset/${workspaceId}/${datasetId}`, '_blank');
  }, []);

  const toggleDataset = useCallback((base: DatasetsVO) => {
    setHasUnsavedChanges(true)
    // @ts-ignore
    setAgentInfo((prev: AgentDetailVO) => {
      const datasetList = prev.datasetList || [];

      if (datasetList.find(t => t.id === base.id)) {
        const newDatasetList = datasetList.filter(t => t.id !== base.id);

        return {
          ...prev,
          datasetList: newDatasetList
        };

      } else {
        const newDatasetList = [...datasetList, base];

        return {
          ...prev,
          datasetList: newDatasetList
        }
      }
    })
  }, [])

  const onTtsEnableChange = useCallback((ttsModelId: string) => {
    // @ts-ignore
    setAgentInfo((prev: AgentDetailVO) => {
      return {
        ...prev,
        agent: {
          ...prev?.agent,
          ttsModelId: ttsModelId,
        }
      }
    });

    setHasUnsavedChanges(true);
  }, []);

  const onAsrEnableChange = useCallback((asrModelId: string) => {
    // @ts-ignore
    setAgentInfo((prev: AgentDetailVO) => {
      return {
        ...prev,
        agent: {
          ...prev?.agent,
          asrModelId: asrModelId,
        }
      }
    });

    setHasUnsavedChanges(true);
  }, []);

  const toggleSubAgent = useCallback((subAgentId: string) => {
    // @ts-ignore
    setAgentInfo(prev => {
      const currentAgentType = prev?.agent?.type;
      const subAgentIds = prev?.agent?.subAgentIds || [];
      const index = subAgentIds.indexOf(subAgentId);
      console.log(subAgentIds)
      console.log(index)

      if (index > -1) {
        subAgentIds.splice(index, 1);
      } else {
        if (currentAgentType === AgentType.REFLECTION) {
          message.warning('反思类型不能添加子 agent');
          return { ...prev };
        }

        subAgentIds.push(subAgentId);
      }
      return { ...prev, agent: { ...prev?.agent, subAgentIds } };
    });

    setHasUnsavedChanges(true);
  }, []);

  const onAutoEnableChange = useCallback((autoType: AutoEnableType) => {
    // @ts-ignore
    setAgentInfo((prev: AgentDetailVO) => {
      return {
        ...prev,
        agent: {
          ...prev?.agent,
          auto: autoType,
        }
      }
    });

    setHasUnsavedChanges(true);
  }, []);

  useEffect(() => {
    if (tools?.data) {
      setToolList(addToolNameIconToFunc(tools?.data || []));
    }
    if (models?.data) {
      setModelList(models?.data?.list?.filter(m => m.type === 'LLM') || []);
      setAudioModelList(models?.data?.list || []);
    }
    if (datasets?.data) {
      setDatasetList(datasets?.data?.list || []);
    }
    if (agents?.data) {
      setAgentList(agents?.data || [])
    }
  }, [tools, models, datasets, agents]);

  const checkMoxToken = useCallback(() => {
    const agentLLMId = agentInfo?.agent?.llmModelId;
    if (agentLLMId && modelList.length > 0) {
      const currentModel = modelList.find(model => model.id === agentLLMId);

      if (currentModel) {
        const agentMaxToken = agentInfo.agent?.maxTokens || 0;
        const currentModelMaxToken = currentModel?.maxTokens || DEFAULT_Max_TOKENS;

        // 如果 agentInfo 的 maxToken 大于当前模型的 maxToken，则更正
        if (agentMaxToken > currentModelMaxToken) {
          setAgentInfo({
            ...agentInfo,
            agent: {
              ...agentInfo.agent,
              maxTokens: currentModelMaxToken
            }
          });
        }
      }
    }
  }, [agentInfo, modelList]);

  useEffect(() => {
    checkMoxToken();
  }, [agentInfo, modelList]);

  return (
    <div className={visible ? "h-full flex" : "invisible w-0 h-0 m-0 p-0"}>
      {agentInfo?.canEdit ? (
        <div className="w-1/3 bg-white rounded p-6 flex flex-col overflow-y-auto agent-config"
          style={{
            scrollbarGutter: 'stable'
          }}>
          {!!agentInfo?.agent?.autoAgentFlag && <AutoAgentType agentInfo={agentInfo} />}
          {!!agentInfo?.agent?.autoAgentFlag && <AutoAgentBaseSet
            agentInfo={agentInfo}
            modelList={modelList}
            setAgentInfo={(info: AgentDetailVO) => {
              setAgentInfo(info);
              setHasUnsavedChanges(true);
            }} />}
          {!!agentInfo?.agent?.autoAgentFlag && <AutoAgentModelList workspaceId={workspaceId} />}
          {!!agentInfo?.agent?.autoAgentFlag && <AutoAgentToolsList workspaceId={workspaceId} />}
          {!agentInfo?.agent?.autoAgentFlag && <AgentBaseSet
            agentInfo={agentInfo}
            modelList={modelList}
            setAgentInfo={(info: AgentDetailVO) => {
              setAgentInfo(info);
              setHasUnsavedChanges(true);
            }}
          />}
          {!agentInfo?.agent?.autoAgentFlag && <ToolsList
            agentInfo={agentInfo}
            tools={agentInfo?.functionList || []}
            onAddTool={() => setAddToolModalVisible(true)}
            onSetClick={() => setIsShowSequenceModal(true)}
            onEditTool={handleEditTool}
            onRemoveFn={toggleTool}
            onChangeMode={onChangeMode}
          />}
          {!agentInfo?.agent?.autoAgentFlag && <Dataset
            datasetList={agentInfo?.datasetList || []}
            onAddBase={() => setIsShowAddDatasetModal(true)}
            onEditDataset={goDatasetPage}
            onRemoveDataset={toggleDataset}
          />}
          <Convert
            agentInfo={agentInfo}
            audioModelList={audioModelList}
            onTtsEnableChange={onTtsEnableChange}
            onAsrEnableChange={onAsrEnableChange}
          />
          {!agentInfo?.agent?.autoAgentFlag && <AgentSet
            agentInfo={agentInfo}
            setAgentInfo={(info: AgentDetailVO) => {
              setAgentInfo(info);
              setHasUnsavedChanges(true);
            }}
          />}
          {!agentInfo?.agent?.autoAgentFlag && <SubAgent
            agentInfo={agentInfo}
            agentList={agentList}
            onAddClick={() => setIsShowSubAgentModal(true)}
            toggleSubAgent={toggleSubAgent}
          />}
        </div>
      ) : (
        <div className="w-1/3 mr-6 bg-white rounded p-4 flex flex-col overflow-y-auto">
          <ReadonlyAgentInfo
            audioModelList={audioModelList}
            workspaceId={workspaceId}
            agentInfo={agentInfo}
            agentList={agentList}
            modelList={modelList}
          />
        </div>
      )}
      <div style={{ borderLeft: '1px solid #ddd' }} className="w-2/3 bg-white rounded h-full">
        <Chat mode="dev" agentId={agentId} agentInfo={agentInfo} />
      </div>

      <AddToolModal
        agentInfo={agentInfo}
        visible={addToolModalVisible}
        onCancel={() => setAddToolModalVisible(false)}
        toggleTool={toggleTool}
        toolList={toolList}
        selectedToolTab={selectedToolTab}
        setSelectedToolTab={setSelectedToolTab}
        refreshToolList={refreshToolList}
      />

      <SequenceModal
        agentInfo={agentInfo}
        toggleSequence={toggleSequence}
        restoreSequence={restoreSequence}
        visible={isShowSequenceModal}
        onCancel={() => setIsShowSequenceModal(false)}
      />

      <AddDatasetModal
        agentInfo={agentInfo}
        datasetList={datasetList}
        toggleDataset={toggleDataset}
        visible={isShowAddDatasetModal}
        onCancel={() => setIsShowAddDatasetModal(false)}
      />

      <SubAgentModal
        currentAgentId={agentId}
        agentInfo={agentInfo}
        visible={isShowSubAgentModal}
        onClose={() => setIsShowSubAgentModal(false)}
        agentList={agentList}
        toggleSubAgent={toggleSubAgent}
      />
    </div>
  );
};

export default EditContent;
