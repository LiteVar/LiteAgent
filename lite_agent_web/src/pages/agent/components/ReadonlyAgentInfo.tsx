import React, { useState, useCallback, useMemo } from 'react';
import { Typography, List, Button, Divider } from 'antd';
import { UpOutlined, DownOutlined } from '@ant-design/icons';

import ToolIcon from '@/pages/workspaces/tools/components/tool-icon';
import datasetImg from '@/assets/agent/dataset.png';
import ImgIcon from './img-icon';
import { AgentDetailVO, AgentDTO, ModelDTO } from "@/client";
import { AgentTypeMode } from './agent-set';
import AutoAgentType from './autoAgentType';
import AutoAgentBaseSet from './autoAgentBaseSet';
import AutoAgentModelList from './autoAgentModelList';
import AutoAgentToolsList from './autoAgentToolList';
import Convert from './convert';

const { Text } = Typography;

interface AgentSettingsProps {
  agentInfo: AgentDetailVO;
  workspaceId: string;
  agentList: AgentDTO[];
  modelList: ModelDTO[];
  audioModelList: ModelDTO[];
}

const ReadonlyAgentInfo: React.FC<AgentSettingsProps> = ({ agentInfo, audioModelList, modelList, workspaceId }) => {
  const [showAllTool, setShowAllTool] = useState(false);
  const [showAllDataset, setShowAllDataset] = useState(false);

  const toUpperCaseWord = useCallback((word: string | undefined) => {
    return (word || '').toUpperCase();
  }, []);

  const renderMode = useCallback((mode: AgentTypeMode) => {
    let modeStr = '';
    switch (mode) {
      case 0:
        modeStr = '并行';
        break;

      case 1:
        modeStr = '串行';
        break;

      case 2:
        modeStr = '拒绝';
        break;

      default:
        break;
    }

    return modeStr;
  }, []);

  const renderAgentType = useCallback((type: AgentTypeMode) => {
    let modeStr = '';
    switch (type) {
      case 0:
        modeStr = '普通';
        break;

      case 1:
        modeStr = '分发';
        break;

      case 2:
        modeStr = '反思';
        break;

      default:
        break;
    }

    return modeStr;
  }, []);

  const functionList = useMemo(() => {
    return agentInfo?.functionList ?? [];
  }, [agentInfo]);

  const datasetList = useMemo(() => {
    return agentInfo?.datasetList ?? [];
  }, [agentInfo]);

  const displayTools = useMemo(() => {
    return showAllTool ? functionList : functionList.slice(0, 2);
  }, [showAllTool, functionList]);

  const displayDatasets = useMemo(() => {
    return showAllDataset ? datasetList : datasetList.slice(0, 2);
  }, [showAllDataset, datasetList]);

  return (
    <div>
      {!agentInfo?.agent?.autoAgentFlag && <div>
        <div className="mb-6">
          <Text strong>模型</Text>
          <div className="flex flex-col">
            <p>
              名称：{agentInfo?.agent?.name}
            </p>
            {agentInfo?.model?.name && <p>大模型：{agentInfo?.model?.alias}</p>}
            <ul>
              <li>Temperature：{agentInfo?.agent?.temperature}</li>
              <li>Max Token：{agentInfo?.agent?.maxTokens}</li>
              <li>Top P：{agentInfo?.agent?.topP}</li>
            </ul>
          </div>
        </div>

        <div className="mb-6">
          <Text strong>工具</Text>
          {functionList.length > 0 && (
            <List
              className={`flex-grow ${showAllTool ? 'overflow-y-scroll' : 'overflow-hidden'}`}
              style={{ height: showAllTool ? 190 : 'auto' }}
              dataSource={displayTools}
              renderItem={(tool) => (
                <List.Item className="border rounded p-2 mb-2 flex items-center justify-between">
                  <div className="flex items-center">
                    <ToolIcon iconName={tool.icon} />
                    <div className='ml-4'>
                      <div className="font-semibold">
                        {`${tool.toolName} ${tool.functionName}-${toUpperCaseWord(tool.requestMethod)}`}
                      </div>
                      <div className="text-gray-500 text-sm">{tool.functionDesc}</div>
                    </div>
                  </div>
                  <div className="text-gray-500 text-sm">{renderMode(tool.mode)}</div>
                </List.Item>
              )}
            />
          )}
          {functionList.length > 2 && (
            <div className="flex justify-center mt-2">
              <Button
                type="link"
                onClick={() => setShowAllTool(!showAllTool)}
                icon={showAllTool ? <UpOutlined /> : <DownOutlined />}
                iconPosition="end"
              >
                {showAllTool ? '收起' : '更多'}
              </Button>
            </div>
          )}
          <Divider />
        </div>

        <div className="mb-6">
          <Text strong>知识库</Text>
          {datasetList.length > 0 && (
            <List
              className={`flex-grow ${showAllDataset ? 'overflow-y-scroll' : 'overflow-hidden'}`}
              style={{ height: showAllDataset ? 190 : 'auto' }}
              dataSource={displayDatasets}
              renderItem={(dataset) => (
                <List.Item className="border rounded p-2 mb-2">
                  <List.Item.Meta
                    avatar={<ImgIcon src={datasetImg} />}
                    title={<div className="pt-2">{dataset?.name}</div>}
                  />
                </List.Item>
              )}
            />
          )}
          {datasetList.length > 2 && (
            <div className="flex justify-center mt-2">
              <Button
                type="link"
                onClick={() => setShowAllDataset(!showAllDataset)}
                icon={showAllDataset ? <UpOutlined /> : <DownOutlined />}
                iconPosition="end"
              >
                {showAllDataset ? '收起' : '更多'}
              </Button>
            </div>
          )}
          <Divider />
        </div>

        <div className="mb-6">
          <Text strong>Agent类型：{renderAgentType(agentInfo?.agent?.type)}</Text>
        </div>

        <div className="mb-6">
          <Text strong>执行模式：{renderMode(agentInfo?.agent?.mode)}</Text>
        </div>
      </div>}
      {!!agentInfo?.agent?.autoAgentFlag && <div>
        <AutoAgentType />
        <AutoAgentBaseSet
          agentInfo={agentInfo}
          readonly
          modelList={modelList}
        />
        <AutoAgentModelList readonly workspaceId={workspaceId} />
        <AutoAgentToolsList readonly workspaceId={workspaceId} />
        <Convert
          agentInfo={agentInfo}
          readonly
          audioModelList={audioModelList}
        />
      </div>}
    </div>
  );
};

export default ReadonlyAgentInfo;
