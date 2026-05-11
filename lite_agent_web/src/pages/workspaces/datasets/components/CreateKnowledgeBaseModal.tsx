import React from 'react';
import { Modal, Form, Input, Select, InputNumber, Button, message } from 'antd';
import { postV1DatasetAdd } from '@/client';
import { useWorkspace } from '@/contexts/workspaceContext';
import { Link } from 'react-router-dom';
import ResponseCode from '@/constants/ResponseCode';
import { getV1ModelListOptions } from '@/client/@tanstack/query.gen';
import { useQuery } from '@tanstack/react-query';

interface CreateKnowledgeBaseModalProps {
  visible: boolean;
  onCancel: () => void;
  refresh: () => void;
}

const CreateKnowledgeBaseModal: React.FC<CreateKnowledgeBaseModalProps> = (props) => {
  const { visible, onCancel, refresh } = props;
  const [form] = Form.useForm();
  const workspace = useWorkspace();

  const { data: modelsData } = useQuery({
    ...getV1ModelListOptions({
      headers: {
        'Workspace-id': workspace?.id!,
      },
      query: {
        pageNo: 0,
        pageSize: 100000000,
      },
    }),
    enabled: !!workspace?.id,
  });
  const models = modelsData?.data?.list || [];

  const handleOk = () => {
    form
      .validateFields()
      .then(async (values) => {
        console.log('Form values:', values);
        const model = models.find((model) => model.id === values.embeddingModel);
        const res = await postV1DatasetAdd({
          headers: {
            'Workspace-id': workspace?.id!,
          },
          body: {
            name: values.name,
            description: values.description,
            retrievalTopK: values.retrievalTopK ||  10,
            retrievalScoreThreshold: values.similarity || 0.5,
            llmModelId: values.llmModelId,
            embeddingModel: values.embeddingModel,
            embeddingModelProvider: model?.name || 'openai',
          },
        });

        if (res?.data?.code === ResponseCode.S_OK) {
          message.success('创建知识库成功');
          await refresh();
          onCancel(); // 提交后关闭模态框
        } else {
          message.error(res?.data?.message || '创建知识库失败');
        }
      })
      .catch((info) => {
        console.log('Validate Failed:', info);
      });
  };

  return (
    <Modal
      centered={true}
      title={<span className="text-[18px] font-medium text-[#1D4A6B]">新建知识库</span>}
      open={visible}
      onCancel={onCancel}
      styles={{
        header: { padding: '16px 24px', marginBottom: 0, borderBottom: 'none' },
        body: { padding: '16px 24px' },
        footer: { padding: '10px 16px', marginTop: 0, borderTop: 'none' },
      }}
      footer={[
        <Button key="back" className="rounded-xl h-10 px-6" onClick={onCancel}>
          取消
        </Button>,
        <Button key="submit" type="primary" className="bg-[#40A5EE] rounded-xl h-10 px-6 border-[#40A5EE]" onClick={handleOk}>
          确认
        </Button>,
      ]}
    >
      <Form form={form} layout="vertical" requiredMark={false}>
        <Form.Item name="name" label={<span className="text-[14px] text-[#383F44] font-medium">知识库名称</span>} rules={[{ required: true, message: '请输入知识库名称' }]}>
          <Input className="h-10 rounded-lg shadow-sm" placeholder="请输入知识库名称" maxLength={40} />
        </Form.Item>
        <Form.Item name="embeddingModel" label={<span className="text-[14px] text-[#383F44] font-medium">嵌入模型</span>} rules={[{ required: true, message: '请选择嵌入模型' }]}>
          <Select className="h-10 rounded-lg shadow-sm" placeholder="请选择嵌入模型">
            {models
              .filter((m) => m.type === 'embedding')
              .filter((m) => m.status === 1)
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
        <Form.Item name="llmModelId" label={<span className="text-[14px] text-[#383F44] font-medium">摘要模型</span>} rules={[{ message: '请选择摘要模型' }]}>
          <Select className="h-10 rounded-lg shadow-sm" placeholder="请选择摘要模型">
            {models
              .filter((m) => m.type === 'LLM')
              .filter((m) => m.status === 1)
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
        <div className="flex gap-4">
          <Form.Item name="retrievalTopK" label={<span className="text-[14px] text-[#383F44] font-medium">搜索结果条数</span>} initialValue={10} className="flex-1">
            <InputNumber min={1} max={20} className="w-full h-10 rounded-lg shadow-sm leading-10" />
          </Form.Item>
          <Form.Item name="similarity" label={<span className="text-[14px] text-[#383F44] font-medium">最大向量距离</span>} initialValue={0.5} className="flex-1">
            <InputNumber min={0} max={1} step={0.1} className="w-full h-10 rounded-lg shadow-sm leading-10" />
          </Form.Item>
        </div>
      </Form>
    </Modal>
  );
};

export default CreateKnowledgeBaseModal;
