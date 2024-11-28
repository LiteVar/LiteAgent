import React from 'react';
import {Typography, List} from 'antd';
import {AgentDetailVO} from "@/client";

const {Text} = Typography;

interface AgentSettingsProps {
  agentInfo: AgentDetailVO;
}

const ReadonlyAgentInfo: React.FC<AgentSettingsProps> = ({agentInfo}) => {
  return (
    <div>
      <div className="mb-6">
        <Text strong>模型</Text>
        <div className="flex flex-col">
          <p>
            名称：{agentInfo?.agent?.name}
          </p>
          {agentInfo?.model?.name && <p>大模型：{agentInfo?.model?.name}</p>}
          <ul>
            <li>Temperature：{agentInfo?.agent?.temperature}</li>
            <li>Max Token：{agentInfo?.agent?.maxTokens}</li>
            <li>Top P：{agentInfo?.agent?.topP}</li>
          </ul>
        </div>
      </div>
      <div className="mb-6">
        <Text strong>工具</Text>
        <List
          className="flex-grow overflow-y-auto"
          dataSource={agentInfo?.toolList}
          renderItem={(tool) => (
            <List.Item className="border rounded p-2 mb-2">
              <List.Item.Meta
                avatar={<div className="w-8 h-8 bg-gray-300 rounded mr-2"></div>}
                title={tool.name}
                description={tool.description}
              />
            </List.Item>
          )}
        />
      </div>
    </div>
  );
};

export default ReadonlyAgentInfo;
