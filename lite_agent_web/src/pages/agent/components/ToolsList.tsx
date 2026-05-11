import React, { useState, useMemo, useCallback } from 'react';
import { 
  List, 
  Button, 
  Select, 
  Collapse 
} from 'antd';
import { 
  PlusCircleTwoTone, 
  ExclamationCircleOutlined, 
  UpOutlined, 
  DownOutlined, 
} from '@ant-design/icons';

import { 
  AgentDetailVO, 
  FunctionVO
} from '@/client';
import { AgentTypeMode } from './agent-set';
import ToolIcon from '@/pages/workspaces/tools/components/tool-icon';
import ImgIcon from './img-icon';
import removeImg from '@/assets/agent/remove.png';

const { Panel } = Collapse;

interface ToolsListProps {
  agentInfo: AgentDetailVO;
  tools: FunctionVO[];
  onAddTool: () => void;
  onEditTool: (toolId: string) => void;
  onRemoveFn: (tool: FunctionVO) => void;
  onChangeMode: (tool: FunctionVO, mode: AgentTypeMode) => void;
}

const ToolsList: React.FC<ToolsListProps> = ({
  tools,
  onAddTool,
  onRemoveFn,
  onChangeMode,
}) => {
  const [showAll, setShowAll] = useState(false);

  const displayTools = useMemo(() => {
    return showAll ? tools : tools.slice(0, 2);
  }, [showAll, tools]);

  const toUpperCaseWord = useCallback((word: string | undefined) => {
    return (word || '').toUpperCase();
  }, []);

  return (
    <div className="border-t border-white/20 mt-4">
      <Collapse ghost>
        <Panel
          className='[&_.ant-collapse-content-box]:px-0 [&_.ant-collapse-header]:py-0'
          header={<span className="text-base font-medium text-[#383F44]">工具</span>}
          key="1"
          extra={
            <Button 
              type="link"
              size="middle"
              icon={<PlusCircleTwoTone />} 
              onClick={(e) => {
                e.stopPropagation();
                onAddTool();
              }}
              className="text-[#40A5EE] w-[60px] h-7 text-xs border border-[#40A5EE] rounded-lg hover:text-[#40A5EE]/80 font-medium"
            >
              添加
            </Button>
          }
        >
          <div className="flex flex-col gap-2">
            <div className="flex items-start gap-2 text-xs text-[#7C8B98] leading-relaxed">
              <ExclamationCircleOutlined className="mt-0.5" />
              <span>Agent 在特定场景下可以调用工具，可以更好执行指令</span>
            </div>

            {tools.length > 0 && (
              <List
                className={`overflow-x-hidden ${showAll ? 'max-h-[400px] overflow-y-auto' : ''}`}
                dataSource={displayTools}
                renderItem={(tool) => (
                  <div
                    key={tool.functionId}
                    className="bg-transparent border border-white rounded-xl p-2 mb-2 hover:bg-white transition-colors group"
                  >
                    <div className="flex items-center gap-3">
                      <ToolIcon iconName={tool.icon} />
                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-medium text-[#1D4A6B] break-all line-clamp-2">
                          {`${tool.toolName} ${tool.functionName}${tool.requestMethod ? `-${toUpperCaseWord(tool.requestMethod)}` : ''}`}
                        </div>
                        <div className="text-xs text-[#7C8B98] break-all line-clamp-1 mt-0.5">
                          {tool.functionDesc}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Select
                          variant="borderless"
                          className="text-xs text-[#40A5EE] [&_.ant-select-selection-item]:p-0"
                          prefix={<DownOutlined className="text-xs" />}
                          suffixIcon={false}
                          size="small"
                          style={{ width: 70 }}
                          value={tool.mode}
                          onChange={(val) => onChangeMode(tool, val)}
                          options={[
                            { value: 0, label: '并行' },
                            { value: 1, label: '串行' },
                            { value: 2, label: '拒绝' },
                          ]}
                        />

                        <ImgIcon
                          key="remove"
                          src={removeImg}
                          width={24}
                          className="cursor-pointer"
                          onClick={() => onRemoveFn(tool)}
                        />
                      </div>
                    </div>
                  </div>
                )}
              />
            )}

            {tools.length > 2 && (
              <div className="flex justify-center">
                <Button
                  type="link"
                  size="small"
                  onClick={() => setShowAll(!showAll)}
                  icon={showAll ? <UpOutlined /> : <DownOutlined />}
                  className="text-[#40A5EE] hover:text-[#40A5EE]/80"
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
