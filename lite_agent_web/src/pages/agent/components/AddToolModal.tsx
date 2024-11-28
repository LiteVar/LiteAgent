import React, {useCallback} from 'react';
import {Modal, Tabs, List, Button, TabsProps} from 'antd';
import {AgentDetailVO, ToolDTO} from "@/client";
import placeholderIcon from '@/assets/dashboard/avatar.png'

interface AddToolModalProps {
  visible: boolean;
  agentInfo: AgentDetailVO;
  toolList: ToolDTO[];
  onCancel: () => void;
  toggleTool: (tool: ToolDTO) => void;
  selectedToolTab: string;
  setSelectedToolTab: (key: string) => void;
}

const AddToolModal: React.FC<AddToolModalProps> = (props) => {
  const { visible, agentInfo, toolList, onCancel, toggleTool, setSelectedToolTab } = props;
  const items: TabsProps['items'] = [
    {
      key: '0',
      label: '全部',
    },
    {
      key: '1',
      label: '系统',
    },
    {
      key: '2',
      label: '来自分享',
    },
    {
      key: '3',
      label: '我的',
    },
  ];

  const onChange = (key: string) => {
    setSelectedToolTab(key);
  };

  const isToolSelected = useCallback((toolId: string) => {
    return agentInfo?.agent?.toolIds?.includes(toolId);
  }, [agentInfo]);

  return (
    <Modal
      centered
      title="添加工具"
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={600}
      bodyStyle={{ display: 'flex', flexDirection: 'column', maxHeight: '70vh' }}
    >
      <Tabs defaultActiveKey="0" className="flex-grow" items={items} onChange={onChange} />
      <div className="flex-grow overflow-y-auto p-2">
        <List
          dataSource={toolList}
          renderItem={item => (
            <List.Item
              key={item.id}
              actions={[
                <Button key={item.id} type="primary" onClick={() => toggleTool(item)} danger={isToolSelected(item.id!)}>
                  {isToolSelected(item.id!) ? '移出' : '添加'}
                </Button>
              ]}
            >
              <List.Item.Meta
                avatar={<img src={placeholderIcon} alt=""/>}
                title={item.name}
                description={item.description}
              />
            </List.Item>
          )}
        />
      </div>
    </Modal>
  );
};

export default AddToolModal;
