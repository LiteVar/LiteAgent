import React from 'react';
import { Modal, Descriptions } from 'antd';
import { ModelDTO } from "@/client";

interface ModelInfoModalProps {
  visible: boolean;
  onClose: () => void;
  modelInfo: ModelDTO | undefined;
}

const ModelInfoModal: React.FC<ModelInfoModalProps> = ({ visible, onClose, modelInfo }) => {
  return (
    <Modal
      centered
      title="模型详情"
      open={visible}
      onCancel={onClose}
      footer={null}
    >
      <Descriptions column={1}>
        <Descriptions.Item label="模型名称" key="modelName">{modelInfo?.name}</Descriptions.Item>
        <Descriptions.Item label="连接别名" key="modelName">{modelInfo?.alias}</Descriptions.Item>
        <Descriptions.Item label="BaseURL" key="baseUrl">{modelInfo?.baseUrl}</Descriptions.Item>
        <Descriptions.Item label="API Key" key="apiKey">
          {modelInfo?.canEdit ? modelInfo?.apiKey : '******'}
        </Descriptions.Item>
        {modelInfo?.type === 'LLM' && <Descriptions.Item label="工具调用" key="toolInvoke">{modelInfo?.toolInvoke ? '是' : '否'}</Descriptions.Item>}
        {modelInfo?.type === 'LLM' && <Descriptions.Item label="深度思考" key="deepThink">{modelInfo?.deepThink ? '是' : '否'}</Descriptions.Item>}
        {modelInfo?.type === 'LLM' && <Descriptions.Item label="Auto MultiAgent" key="autoAgent">{modelInfo?.autoAgent ? '是' : '否'}</Descriptions.Item>}
      </Descriptions>
    </Modal>
  );
};

export default ModelInfoModal;
