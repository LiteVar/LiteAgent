import React, { useState, useMemo, useCallback } from 'react';
import { 
  List, 
  Button, 
  Space, 
  Select, 
  Modal, 
  Dropdown, 
  message, 
  Collapse 
} from 'antd';
import type { MenuProps } from 'antd';
import { 
  PlusCircleTwoTone, 
  ExclamationCircleOutlined, 
  UpOutlined, 
  DownOutlined 
} from '@ant-design/icons';

import { 
  AgentDetailVO, 
  FunctionVO, 
  postV1AgentResetSequenceByAgentId 
} from '@/client';
import { AgentTypeMode } from './agent-set';
import ToolIcon from '@/pages/workspaces/tools/components/tool-icon';
import ImgIcon from './img-icon';
import removeImg from '@/assets/agent/remove.png';
import ResponseCode from '@/constants/ResponseCode';

const { Panel } = Collapse;

interface ToolsListProps {
  agentInfo: AgentDetailVO;
  tools: FunctionVO[];
  onAddTool: () => void;
  onSetClick: () => void;
  onEditTool: (toolId: string) => void;
  onRemoveFn: (tool: FunctionVO) => void;
  onChangeMode: (tool: FunctionVO, mode: AgentTypeMode) => void;
}

const ToolsList: React.FC<ToolsListProps> = ({
  agentInfo,
  tools,
  onAddTool,
  onSetClick,
  onRemoveFn,
  onChangeMode,
}) => {
  const { agent } = agentInfo;
  const agentId = agent?.id || '';
  const workspaceId = agent?.workspaceId || '';
  const [showAll, setShowAll] = useState(false);

  const displayTools = useMemo(() => {
    return showAll ? tools : tools.slice(0, 2);
  }, [showAll, tools]);

  const handleRemove = useCallback(
    (tool: FunctionVO) => {
      Modal.confirm({
        title: '删除确认',
        content: '如果方法序列有这个方法，也将会从方法序列中移除该方法，确认删除 ？',
        okText: '确认',
        cancelText: '取消',
        centered: true,
        onOk: () => onRemoveFn(tool),
      });
    },
    [onRemoveFn]
  );

  const toUpperCaseWord = useCallback((word: string | undefined) => {
    return (word || '').toUpperCase();
  }, []);

  const resetFunc = useCallback(async () => {
    try {
      const res = await postV1AgentResetSequenceByAgentId({
        headers: {
          'Workspace-id': workspaceId,
        },
        path: {
          agentId: agentId,
        },
      });
      if (res.data?.code === ResponseCode.S_OK) {
        message.success('重置成功');
      }
    } catch (err) {
      console.error(err);
      message.error('重置失败');
    }
  }, [workspaceId, agentId]);

  const onResetClick = useCallback(() => {
    Modal.confirm({
      title: '重置方法确认',
      content: '确认重置方法序列吗 ？',
      okText: '确认',
      cancelText: '取消',
      centered: true,
      onOk: resetFunc,
    });
  }, []);

  const manageItems: MenuProps['items'] = [
    {
      label: (
        <span 
          className="text-[14px] text-[#2a82e4]" 
          onClick={onSetClick}
          key="method-setting"
        >
          方法序列设置
        </span>
      ),
      key: '0',
    },
    {
      label: (
        <span 
          className="text-[14px] text-[#2a82e4]" 
          onClick={onResetClick}
          key="method-reset"
        >
          重置方法序列
        </span>
      ),
      key: '1',
    },
  ];

  return (
    <div className="">
      <Collapse ghost>
        <Panel
          header={<span className="text-base font-medium">工具</span>}
          collapsible="header"
          key="1"
          extra={
            <Space size={24}>
              <Dropdown menu={{ items: manageItems }} trigger={['click']}>
                <a className="text-[14px]" onClick={(e) => e.preventDefault()}>
                  <Space>
                    方法序列管理
                    <DownOutlined />
                  </Space>
                </a>
              </Dropdown>
              <Button 
                color="primary" 
                variant="filled" 
                icon={<PlusCircleTwoTone />} 
                onClick={onAddTool}
              >
                添加
              </Button>
            </Space>
          }
        >
          <div className="flex flex-col">
            <div className="flex justify-between items-center mb-4">
              <div className="text-gray-500">
                <ExclamationCircleOutlined className="mr-2" />
                Agent 在特定场景下可以调用工具，可以更好执行指令
              </div>
            </div>
            {tools.length === 0 && <div className=""></div>}

            {tools.length > 0 && (
              <List
                className={`flex-grow ${showAll ? 'overflow-y-scroll' : 'overflow-hidden'}`}
                style={{ height: showAll ? '29vh' : 'auto' }}
                dataSource={displayTools}
                renderItem={(tool) => (
                  <List.Item
                    key={tool.functionId}
                    className="border rounded p-2 mb-2 hover:bg-gray-100"
                    actions={[
                      <Select
                        key="mode-select"
                        variant="borderless"
                        style={{ width: 90 }}
                        value={tool.mode}
                        onChange={(val) => onChangeMode(tool, val)}
                        options={[
                          { value: 0, label: '并行' },
                          { value: 1, label: '串行' },
                          { value: 2, label: '拒绝' },
                        ]}
                      />,
                      <ImgIcon
                        key="remove"
                        src={removeImg}
                        width={24}
                        className="cursor-pointer"
                        onClick={() => handleRemove(tool)}
                      />,
                    ]}
                  >
                    <List.Item.Meta
                      avatar={<ToolIcon iconName={tool.icon} />}
                      title={
                        <div className="line-clamp-1">
                          <span className="mr-1">
                            {`${tool.toolName} ${tool.functionName}-${toUpperCaseWord(tool.requestMethod)}`}
                          </span>
                        </div>
                      }
                      description={<div className="w-full line-clamp-3">{tool.functionDesc}</div>}
                    />
                  </List.Item>
                )}
              />
            )}

            {tools.length > 2 && (
              <div className="flex justify-center mt-2">
                <Button
                  type="link"
                  onClick={() => setShowAll(!showAll)}
                  icon={showAll ? <UpOutlined /> : <DownOutlined />}
                  iconPosition="end"
                >
                  {showAll ? '收起' : '更多'}
                </Button>
              </div>
            )}
          </div>
        </Panel>
      </Collapse>
    </div>
  );
};

export default ToolsList;
