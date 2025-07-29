import React from 'react';
import { AgentDetailVO } from '@/client';
import Setting from './tabContent/SettingContent';
import EditContent from './tabContent/EditContent';

interface TabContentProps {
  selectedTab: string;
  agentInfo: AgentDetailVO;
  agentId: string;
  modelList: any[];
  setAgentInfo: (info: AgentDetailVO) => void;
  setHasUnsavedChanges: (value: boolean) => void;
  onAddTool: () => void;
  onSetToolModal: () => void;
  onEditTool: () => void;
  onDeleteTool: (id: string) => void;
  onAddBase: () => void;
  onSetSubAgent: () => void;
  settingAgent: any;
  setSettingAgent: (agent: any) => void;
}

const TabContent: React.FC<TabContentProps> = ({
  selectedTab,
  agentInfo,
  agentId,
  modelList,
  setAgentInfo,
  setHasUnsavedChanges,
  onAddTool,
  onSetToolModal,
  onEditTool,
  onDeleteTool,
  onAddBase,
  onSetSubAgent,
  settingAgent,
  setSettingAgent,
}) => {
  if (selectedTab === 'edit') {
    return (
      <EditContent
        agentInfo={agentInfo}
        agentId={agentId}
        modelList={modelList}
        setAgentInfo={setAgentInfo}
        setHasUnsavedChanges={setHasUnsavedChanges}
        onAddTool={onAddTool}
        onSetToolModal={onSetToolModal}
        onEditTool={onEditTool}
        onDeleteTool={onDeleteTool}
        onAddBase={onAddBase}
        onSetSubAgent={onSetSubAgent}
      />
    );
  }

  if (selectedTab === 'api') {
    return <div className="w-1/3 bg-white rounded p-6 flex flex-col">api docs component</div>;
  }

  if (selectedTab === 'logs') {
    return <div className="w-1/3 bg-white rounded p-6 flex flex-col">logs component</div>;
  }

  if (selectedTab === 'setting') {
    return (
      <div className="w-1/3 bg-white rounded p-6 flex flex-col">
        <Setting settingAgent={settingAgent} setSettingAgent={setSettingAgent} />
      </div>
    );
  }

  return null;
};

export default TabContent; 