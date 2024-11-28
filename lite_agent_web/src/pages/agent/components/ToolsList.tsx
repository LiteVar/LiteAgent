import React from 'react';
import { List, Button, Typography, Space, Tooltip } from 'antd';
import { PlusOutlined, SettingOutlined, DeleteOutlined, QuestionCircleOutlined, InfoCircleOutlined } from '@ant-design/icons';
import {ToolDTO} from "@/client";
import placeholderIcon from '@/assets/dashboard/avatar.png'

const { Text } = Typography;

interface ToolsListProps {
  tools: ToolDTO[];
  onAddTool: () => void;
  onEditTool: (toolId: string) => void;
  onDeleteTool: (toolId: string) => void;
}

const ToolsList: React.FC<ToolsListProps> = ({ tools, onAddTool, onEditTool, onDeleteTool }) => {
  return (
    <div className="flex-grow overflow-hidden">
      <div className="flex flex-col h-full">
        <div className="flex justify-between items-center mb-4">
          <Space>
            <Text strong>工具</Text>
            <Tooltip title="agent在特定场景下可以调用工具，可以更好执行指令">
              <QuestionCircleOutlined className="text-blue-600"/>
            </Tooltip>
          </Space>
          <Button icon={<PlusOutlined />} onClick={onAddTool}>添加</Button>
        </div>
        <List
          className="flex-grow overflow-y-auto"
          dataSource={tools}
          renderItem={(tool) => (
            <List.Item
              className="border rounded p-2 mb-2"
              actions={[
                <Button key="edit" icon={<SettingOutlined />} onClick={() => onEditTool(tool?.id!)}/>,
                <Button key="delete" icon={<DeleteOutlined />} onClick={() => onDeleteTool(tool?.id!)}/>,
              ]}
            >
              <List.Item.Meta
                avatar={<img src={placeholderIcon} alt=""/>}
                title={<div className="line-clamp-1">
                  <span className="mr-1">{tool.name}</span>
                  {!tool.canRead && <Tooltip placement={"bottom"} title={"工具失效，无法使用"} open={true}>
                    <InfoCircleOutlined className="text-yellow-600" />
                  </Tooltip>}
                </div>}
                description={<div className="w-full line-clamp-3">
                  {tool.description}
                </div>}
              />
            </List.Item>
          )}
        />
      </div>
    </div>
  );
};

export default ToolsList;
