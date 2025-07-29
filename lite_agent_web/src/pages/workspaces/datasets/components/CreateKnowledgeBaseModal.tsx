import React, { useState } from 'react';
import { Modal, Form, Input, Select, Slider, InputNumber, Button } from 'antd';
import { postV1DatasetAdd } from '@/client';
import { useWorkspace } from '@/contexts/workspaceContext';
import { ModelDTO } from '@/client';
import { Link } from 'react-router-dom';
interface CreateKnowledgeBaseModalProps {
  visible: boolean;
  onCancel: () => void;
  models: ModelDTO[];
  refresh: () => void;
}

const CreateKnowledgeBaseModal: React.FC<CreateKnowledgeBaseModalProps> = (props) => {
  const { visible, onCancel, models, refresh } = props;
  const [form] = Form.useForm();
  const workspace = useWorkspace();

  const handleOk = () => {
    form
      .validateFields()
      .then(async (values) => {
        console.log('Form values:', values);
        const model = models.find((model) => model.id === values.model);
        await postV1DatasetAdd({
          headers: {
            'Workspace-id': workspace?.id!,
          },
          body: {
            name: values.name,
            description: values.description,
            retrievalTopK: values.retrievalTopK ||  10,
            retrievalScoreThreshold: values.similarity || 0.5,
            llmModelId: values.model,
            embeddingModel: values.model,
            embeddingModelProvider: model?.name || 'openai',
          },
        });
        await refresh();
        onCancel(); // 提交后关闭模态框
      })
      .catch((info) => {
        console.log('Validate Failed:', info);
      });
  };

  return (
    <Modal
      centered={true}
      title="新建知识库"
      open={visible}
      onCancel={onCancel}
      footer={[
        <Button key="back" onClick={onCancel}>
          取消
        </Button>,
        <Button key="submit" type="primary" onClick={handleOk}>
          确定
        </Button>,
      ]}
    >
      <Form form={form} layout="vertical">
        <Form.Item name="name" label="知识库名称" rules={[{ required: true, message: '请输入知识库名称' }]}>
          <Input placeholder="请输入知识库名称" maxLength={40} />
        </Form.Item>
        <Form.Item name="model" label="嵌入模型" rules={[{ required: true, message: '请选择嵌入模型' }]}>
          <Select placeholder="请选择嵌入模型">
            {models
              .filter((m) => m.type === 'embedding')
              .map((model) => (
                <Select.Option key={model.id} value={model.id}>
                  {model.alias}
                </Select.Option>
              ))}
            <Select.Option className="p-0" key="new" value={null} onClick={() => console.log('新建模型')}>
              <Link className="w-full h-full block pl-3 leading-10" to={`/workspaces/${workspace?.id!}/models`}>
                新建模型
              </Link>
            </Select.Option>
          </Select>
        </Form.Item>
        <Form.Item name="retrievalTopK" label="搜索结果条数" initialValue={10}>
          <InputNumber min={1} max={20} className="w-full" />
        </Form.Item>
        <Form.Item name="similarity" label="最大向量距离" initialValue={0.5}>
          <InputNumber min={0} max={1} className="w-full" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CreateKnowledgeBaseModal;
