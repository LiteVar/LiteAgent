import React, { useState, useCallback, useMemo } from 'react';
import { Modal, Tabs, Button, TabsProps, Collapse, message } from 'antd';

import { AgentDetailVO, FunctionVO, ToolDTO, postV1ToolAdd } from "@/client";
import ToolIcon from '@/pages/workspaces/tools/components/tool-icon';
import CreateToolModal from '@/pages/workspaces/tools/components/CreateToolModal';
import ResponseCode from "@/constants/ResponseCode";
import '../style/index.css';

interface AddToolModalProps {
  visible: boolean;
  agentInfo: AgentDetailVO;
  toolList: ToolDTO[];
  onCancel: () => void;
  toggleTool: (tool: FunctionVO) => void;
  selectedToolTab: string;
  setSelectedToolTab: (key: string) => void;
  refreshToolList: () => Promise<void>
}

const TAB_ALL = '0';

const AddToolModal: React.FC<AddToolModalProps> = ({ 
  visible, 
  agentInfo, 
  toolList, 
  onCancel, 
  toggleTool, 
  setSelectedToolTab,
  refreshToolList
}) => {

  const [isCreateToolModalVisible, setIsCreateToolModalVisible] = useState(false);
  const [currentTabKey, setCurrentTabKey] = useState(TAB_ALL);
  
  const items: TabsProps['items'] = [
    {
      key: TAB_ALL,
      label: '全部',
    },
    {
      key: '1',
      label: '系统',
    },
    {
      key: '3',
      label: '我的',
    },
  ];

  const onChange = (key: string) => {
    setCurrentTabKey(key);
    setSelectedToolTab(key);
  };

  const onCreateOKClick = useCallback(async (_: string, values: any) => {
    const workspaceId = agentInfo.agent?.workspaceId || '';
    const  res = await postV1ToolAdd({
      body: values, 
      headers: { 'Workspace-id': workspaceId }
    });
    
    if (res?.data?.code === ResponseCode.S_OK) {
      setIsCreateToolModalVisible(false);
      setCurrentTabKey(TAB_ALL);
      refreshToolList();

      return true;
    } else {
      message.error('解析失败');
      return false;
    }
  }, [agentInfo]);

  const isToolSelected = useCallback((toolId: string) => {
    return agentInfo?.agent?.functionList?.find(
      (func) => func.functionId === toolId) 
        ? true 
        : false;
  }, [agentInfo]);

  const toUpperCaseWord = useCallback((word: string | undefined) => {
    return word ? `-${word.toUpperCase()}` : '';
  }, []);

  const collapseItems = useMemo(() => {
    return toolList.map((tool) => {
      return {
        key: tool.id,
        label: (     
          <div className="flex pb-4">
            <div style={{ width: '40px', flexShrink: 0 }}>
              <ToolIcon iconName={tool.icon} />
            </div>
            <div className='flex-1 ml-4'>
              <div className=" text-[16px] text-[#333]">
                {tool.name}
              </div>
              <div className="text-[14px] text-[#c2c2c2]">
                {tool.description}
              </div>
            </div>   
          </div>            
        ),
        children: (
          <div 
            key={tool.id}
            className='pl-6 pb-4' 
            style={{ borderBottom: '1px solid #eee' }}
          >
            {tool.functionList?.map((func) => (
              <div 
                key={func.functionId} 
                className='flex items-center justify-between'
              >
                <div>
                  <div className="text-[16px] text-[#333]">
                    {`${func.functionName}${toUpperCaseWord(func.requestMethod)}`}
                  </div>
                  <div className="text-[14px] text-[#c2c2c2]">
                    {func.functionDesc}
                  </div>
                </div>
                <div>
                  <Button 
                    variant="filled"                 
                    color={isToolSelected(func.functionId!) ? 'danger' : 'primary'}
                    onClick={() => toggleTool(func)} 
                  >
                    {isToolSelected(func.functionId!) ? '移除' : '添加'}
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )
      }
    });
  }, [toolList, agentInfo]);

  return (
    <Modal
      centered
      title="添加工具"
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={600}
      styles={{body : {
        display: 'flex', 
        flexDirection: 'column', 
        maxHeight: '70vh' 
      }}}
    >
       <div className="flex justify-between items-center mb-4 toolTab">
        <Tabs 
          defaultActiveKey="0" 
          className="flex-grow" 
          items={items} 
          onChange={onChange} 
          activeKey={currentTabKey}
        />
        <Button 
          type="primary" 
          className="mt-[-16px]"
          onClick={() => setIsCreateToolModalVisible(true)} 
        >
          新建工具
        </Button>
      </div>
      <div className="flex-grow overflow-y-auto p-2">
        <Collapse 
          defaultActiveKey={['1']} 
          expandIconPosition="end" 
          ghost 
          items={collapseItems} 
        />
      </div>

      <CreateToolModal
        visible={isCreateToolModalVisible}
        onCancel={() => setIsCreateToolModalVisible(false)} 
        onOk={onCreateOKClick} 
      />
    </Modal>
  );
};

export default AddToolModal;
