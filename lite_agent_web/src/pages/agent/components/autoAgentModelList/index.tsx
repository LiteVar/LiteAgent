import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  List,
  Button,
  Collapse,
  Divider
} from 'antd';
import {
  UpOutlined,
  DownOutlined
} from '@ant-design/icons';

import {
  AgentDetailVO,
  AgentDTO,
} from '@/client';
import agentImg from '@/assets/agent/agent.png';
import { useQuery } from '@tanstack/react-query';
import { getV1ModelListOptions } from '@/client/@tanstack/query.gen';
import { ModelDTO, Account } from '../../../../client';

const { Panel } = Collapse;

interface ToolsListProps {
  readonly?: boolean;
  workspaceId: string;
}

const AutoAgentModelList: React.FC<ToolsListProps> = ({
  workspaceId,
  readonly,
}) => {

  if (!workspaceId) return;

  const [modelList, setModelList] = useState<ModelDTO[]>([]);
  const [showAll, setShowAll] = useState(false);

  const { data: models } = useQuery({
    ...getV1ModelListOptions({
      headers: {
        'Workspace-id': workspaceId!,
      },
      query: {
        pageNo: 0,
        autoAgent: true,
        pageSize: 100000000,
      },
    }),
    enabled: !!workspaceId,
  });

  useEffect(() => {
    if (models?.data) {
      setModelList(models?.data?.list?.filter(m => m.type === 'LLM') || []);
    }
  }, [models]);

  const displayModels = useMemo(() => {
    return showAll ? modelList : modelList.slice(0, 2);
  }, [showAll, modelList]);

  const navigateCreateModelPage = useCallback((event: any) => {
    event.stopPropagation();
    window.open(`/workspaces/${workspaceId}/models`, '_blank')
  }, [workspaceId]);

  return (
    <div>
      <Divider />
      <Collapse ghost>
        <Panel
          header={<span className="text-base font-medium">模型</span>}
          collapsible="header"
          key="1"
        >
          <div className="flex flex-col">
            <div className="text-base text-gray-500 mb-5">在模型库中对模型开启“支持 Auto Multi Agent”后，模型将在此处显示。agent将根据任务自动选择模型。</div>
            {modelList.length === 0 && (
              <div>
                {!readonly && <div>
                  <span>还没添加可用模型，</span>
                  <span className="text-blue-500 cursor-pointer" onClick={navigateCreateModelPage}>前往设置</span>
                </div>}
                {!!readonly && <div>还没添加可用模型</div>}
              </div>
            )}
            {modelList.length > 0 && (
              <List
                className={`flex-grow ${showAll ? 'overflow-y-auto' : 'overflow-hidden'}`}
                style={{ maxHeight: showAll ? '29vh' : 'auto' }}
                dataSource={displayModels}
                renderItem={(model) => (
                  <List.Item
                    key={model.id}
                    className="border rounded p-2 mb-2 hover:bg-gray-100"
                  >
                    <List.Item.Meta
                      avatar={
                        <img
                          src={agentImg}
                          alt={model.name}
                          className="w-10 h-10 rounded"
                        />
                      }
                      title={<div className="pt-2">{model.alias || model.name}</div>}
                    />
                  </List.Item>
                )}
              />
            )}

            {modelList.length > 2 && (
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

export default AutoAgentModelList;
