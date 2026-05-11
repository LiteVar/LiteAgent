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
      title={<span className="text-[#1D4A6B] font-medium text-lg">模型详情</span>}
      open={visible}
      onCancel={onClose}
      footer={null}
      styles={{
        header: { padding: '16px 24px', borderBottom: '1px solid #F2F3F5', marginBottom: 0 },
        body: { padding: '24px' },
      }}
      width={460}
    >
      <Descriptions 
        column={1} 
        size="small" 
        className="[&_.ant-descriptions-item-label]:text-[#7C8B98] [&_.ant-descriptions-item-content]:whitespace-pre-line [&_.ant-descriptions-item-content]:break-all [&_.ant-descriptions-item-content]:text-[#383F44] [&_.ant-descriptions-item-content]:font-medium"
      >
        <Descriptions.Item label="模型名称" key="modelName">{modelInfo?.name}</Descriptions.Item>
        <Descriptions.Item label="连接别名" key="modelAlias">{modelInfo?.alias}</Descriptions.Item>
        <Descriptions.Item label="BaseURL" key="baseUrl">{modelInfo?.baseUrl}</Descriptions.Item>
        <Descriptions.Item label="API Key" key="apiKey">{modelInfo?.apiKey}</Descriptions.Item>
        {modelInfo?.type === 'LLM' && (
          <>
            <Descriptions.Item label="工具调用" key="toolInvoke">{modelInfo?.toolInvoke ? '是' : '否'}</Descriptions.Item>
            <Descriptions.Item label="深度思考" key="deepThink">{modelInfo?.deepThink ? '是' : '否'}</Descriptions.Item>
            <Descriptions.Item label="Auto MultiAgent" key="autoAgent">{modelInfo?.autoAgent ? '是' : '否'}</Descriptions.Item>
            <Descriptions.Item label="视觉理解" key="vision">{modelInfo?.vision ? '是' : '否'}</Descriptions.Item>
            {modelInfo?.maxTokens && <Descriptions.Item label="maxToken" key="maxToken">{modelInfo?.maxTokens}</Descriptions.Item>}
            {modelInfo?.contextWindows &&<Descriptions.Item label="上下文长度" key="contextWindows">{modelInfo?.contextWindows}</Descriptions.Item>}
          </>
        )}
        {(modelInfo?.type === 'tts' || modelInfo?.type === 'asr') && (
          <>
            <Descriptions.Item label="流式输出" key="audioFormat">{modelInfo?.streamable ? '是' : '否'}</Descriptions.Item>
          </>
        )}
      </Descriptions>
    </Modal>
  );
};

export default ModelInfoModal;
