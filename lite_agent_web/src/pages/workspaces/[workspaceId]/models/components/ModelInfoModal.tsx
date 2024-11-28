import React from 'react';
import { Modal, Descriptions } from 'antd';
import {ModelVOAddAction} from "@/client";

interface ModelInfoModalProps {
  visible: boolean;
  onClose: () => void;
  modelInfo: ModelVOAddAction | undefined;
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
        <Descriptions.Item label="模型名称">{modelInfo?.name}</Descriptions.Item>
        <Descriptions.Item label="BaseURL">{modelInfo?.baseUrl}</Descriptions.Item>
        <Descriptions.Item label="API Key">{modelInfo?.apiKey}</Descriptions.Item>
      </Descriptions>
    </Modal>
  );
};

export default ModelInfoModal;
