import React, { useCallback } from 'react';
import { Modal, Button, List, Tooltip } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';

import { AgentDetailVO, AgentDTO } from '@/client';
import { buildImageUrl } from "@/utils/buildImageUrl";
import placeholderIcon from "@/assets/agent/agent.png";
import { AgentType } from '../agent-set';

interface SubAgentModalProps {
  currentAgentId: string;
  agentInfo: AgentDetailVO;
  agentList: AgentDTO[];
  toggleSubAgent: (subAgentId: string) => void;
  visible: boolean;
  onClose: () => void;
}

const SubAgentModal: React.FC<SubAgentModalProps> = ({
  currentAgentId,
  agentInfo,
  agentList = [],
  toggleSubAgent,
  visible,
  onClose
}) => {

  const isSubAgentSelected = useCallback((subAgentId: string) => {
    return agentInfo.agent?.subAgentIds?.includes(subAgentId);
  }, [agentInfo]);

  const renderAgentStatus = useCallback((agent: AgentDTO) => {
    if (agent.autoAgentFlag) {
      return (
        <span key="current" className="text-xs text-gray-400">
          不支持添加
        </span>
      );
    }

    if (agent.id === currentAgentId) {
      return (
        <span key="current" className="text-xs text-gray-400">
          当前 Agent
        </span>
      );
    }

    if (agent.status === 0) {
      return (
        <span key="status" className="text-xs text-gray-400">
          未发布，无法添加
        </span>
      );
    }

    return (
      <Button
        key="status"
        color={isSubAgentSelected(agent.id!) ? 'danger' : 'primary'}
        variant="filled"
        onClick={() => toggleSubAgent(agent.id!)}
      >
        {isSubAgentSelected(agent.id!) ? '移除' : '添加'}
      </Button>
    );
  }, [currentAgentId, isSubAgentSelected, toggleSubAgent]);

  const renderType = useCallback((agent: AgentDTO) => {
    if (agent.autoAgentFlag) return 'Auto Multi Agent';

    switch (agent.type) {
      case AgentType.NORMAL:
        return '普通';
      case AgentType.DISTRIBUTION:
        return '分发';
      case AgentType.REFLECTION:
        return '反思';
      default:
        return '';
    }
  }, []);

  const renderAgentNameWithWarning = useCallback((agent: AgentDTO): JSX.Element => {
    // 检查名称是否只包含英文字母、数字和下划线
    const isNameValid = /^[a-zA-Z0-9_]+$/.test(agent.name ?? '');

    return (
      <div className="flex items-center">
        <span>{agent.name}</span>
        {agent.type === AgentType.DISTRIBUTION && !isNameValid && (
          <Tooltip title="agent 名称含有非英文字符，可能无法识别">
            <ExclamationCircleOutlined
              className="ml-2 text-[#faad14] cursor-pointer"
            />
          </Tooltip>
        )}
      </div>
    );
  }, []);

  return (
    <Modal
      title="添加子 Agent"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={600}
      centered
    >
      <div className="space-y-4 max-h-[80vh] overflow-y-auto">
        <List
          dataSource={agentList}
          renderItem={agent => (
            <List.Item
              key={agent.id}
              className="flex items-center p-4 hover:bg-gray-50"
              actions={[renderAgentStatus(agent)]}
            >
              <List.Item.Meta
                avatar={
                  <img
                    src={buildImageUrl(agent.icon!) || placeholderIcon}
                    alt={agent.name}
                    className="w-10 h-10 rounded"
                  />
                }
                title={agent.name}
                description={
                  <div className="text-xs text-[#c2c2c2] line-clamp-2">
                    <span>类型：{renderType(agent)}</span>
                  </div>
                }
              />
            </List.Item>
          )}
          locale={{
            emptyText: '暂无数据'
          }}
        />
      </div>
    </Modal>
  );
};

export default SubAgentModal;