import React, { useState, useEffect, useMemo } from 'react';
import { Form, Input, Button, InputNumber, Slider, Modal, Select, message } from 'antd';
import { useDatasetContext } from '@/contexts/datasetContext';
import { deleteV1DatasetById, putV1DatasetById } from '@/client';
import ResponseCode from '@/constants/ResponseCode';
import { useQuery } from '@tanstack/react-query';
import { getV1DatasetByDatasetIdDocumentsOptions } from '@/client/@tanstack/query.gen';
import { Link, useNavigate } from 'react-router-dom';

interface SettingsProps {
  refetch: () => void;
}

const Settings: React.FC<SettingsProps> = ({ refetch }) => {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const { workspaceId, datasetInfo, models } = useDatasetContext();
  const datasetId = useMemo(() => {
    const path = window.location.pathname;
    return path.split('/')[3];
  }, []);

  const { data } = useQuery({
    ...getV1DatasetByDatasetIdDocumentsOptions({
      query: {
        pageNo: 1,
        pageSize: 10,
      },
      path: {
        datasetId,
      },
    }),
    enabled: !!datasetId && !!workspaceId,
  });

  const docsLength = data?.data?.total || 0;

  useEffect(() => {
    const savedModel = datasetInfo?.embeddingModel;
    const modelExists = models?.some((m) => m.type === 'embedding' && m.id === savedModel);

    form.setFieldsValue({
      name: datasetInfo?.name,
      description: datasetInfo?.description,
      model: modelExists ? savedModel : undefined,
      retrievalTopK: datasetInfo?.retrievalTopK,
      retrievalScoreThreshold: datasetInfo?.retrievalScoreThreshold,
    });
  }, [datasetInfo, form, models]);

  const onFinish = async (values: {
    name: string;
    description: string;
    embeddingModel: string;
    llmModelId: string;
    retrievalTopK: number;
    retrievalScoreThreshold: number;
  }) => {
    setLoading(true);
    try {
      const model = models?.find((model) => model.id === values.embeddingModel);
      const res = await putV1DatasetById({
        path: { id: datasetId },
        headers: {
          'Workspace-id': workspaceId!,
        },
        body: {
          name: values.name,
          description: values.description,
          llmModelId: values.llmModelId,
          embeddingModel: values.embeddingModel,
          embeddingModelProvider: model?.name || 'openai',
          retrievalTopK: values.retrievalTopK,
          retrievalScoreThreshold: values.retrievalScoreThreshold,
        },
      });
      if (res.data?.code === ResponseCode.S_OK) {
        message.success('更新成功');
        refetch();
      } else {
        message.error(res.data?.message || '更新失败');
      }
    } catch (error) {
      // Error message
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    try {
      const res = await deleteV1DatasetById({
        path: {
          id: datasetId,
        },
        headers: {
          'Workspace-id': workspaceId!,
        },
      });
      if (res.data?.code === ResponseCode.S_OK) {
        message.success('删除成功');
        navigate(`/workspaces/${datasetInfo?.workspaceId}/datasets`);
      } else {
        message.error(res.data?.message || '删除失败');
      }
      setConfirmDelete(false);
    } catch (error) {
      // Error message
    }
  };

  return (
    <div className="w-1/2">
      <h2 className="text-xl mb-6">知识库设置</h2>

      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={{
          name: datasetInfo?.name,
          description: datasetInfo?.description,
          embeddingModel: datasetInfo?.embeddingModel,
          llmModelId: datasetInfo?.llmModelId,
          retrievalTopK: datasetInfo?.retrievalTopK,
          retrievalScoreThreshold: datasetInfo?.retrievalScoreThreshold,
        }}
      >
        <Form.Item
          name="name"
          label="知识库名称"
          rules={[
            { required: true, message: '请输入知识库名称' },
            { max: 40, message: '名称不能超过40个字符' },
          ]}
        >
          <Input placeholder="知识库名称" />
        </Form.Item>

        <Form.Item name="description" label="描述">
          <Input.TextArea
            placeholder="用简单几句话描述知识库"
            maxLength={200}
            autoSize={{ minRows: 3, maxRows: 5 }}
          />
        </Form.Item>

        <Form.Item name="embeddingModel" label="嵌入模型" required>
          <Select placeholder="请选择嵌入模型" disabled={docsLength > 0}>
            {models
              ?.filter((m) => m.type === 'embedding')
              .map((model) => (
                <Select.Option key={model.id} value={model.id}>
                  {model.alias}
                </Select.Option>
              ))}
            <Select.Option className="p-0" key="new" value={null} onClick={() => console.log('新建模型')}>
              <Link className="w-full h-full block pl-3 leading-10" to={`/workspaces/${workspaceId!}/models`}>
                新建模型
              </Link>
            </Select.Option>
          </Select>
        </Form.Item>

        <Form.Item name="llmModelId" label="摘要模型">
          <Select placeholder="请选择摘要模型">
            {models
              ?.filter((m) => m.type === 'LLM')
              .map((model) => (
                <Select.Option key={model.id} value={model.id}>
                  {model.alias}
                </Select.Option>
              ))}
            <Select.Option className="p-0" key="new" value={undefined} onClick={() => console.log('新建模型')}>
              <Link className="w-full h-full block pl-3 leading-10" to={`/workspaces/${workspaceId!}/models`}>
                新建模型
              </Link>
            </Select.Option>
          </Select>
        </Form.Item>

        <Form.Item name="retrievalTopK" label="搜索结果条数">
          <InputNumber min={1} max={20} />
        </Form.Item>

        <Form.Item name="retrievalScoreThreshold" label="最大向量距离">
          <InputNumber min={0} max={1} />
        </Form.Item>

        <div className="flex justify-center gap-4 mt-20">
          <Button
            danger
            disabled={!datasetInfo?.canDelete}
            type="primary"
            className="w-full py-5"
            onClick={() => setConfirmDelete(true)}
          >
            删除
          </Button>

          <Button
            type="primary"
            disabled={!datasetInfo?.canEdit}
            loading={loading}
            htmlType="submit"
            className="w-full py-5"
          >
            保存
          </Button>
        </div>
      </Form>

      <Modal
        centered={true}
        title="确认删除"
        open={confirmDelete}
        onOk={handleDelete}
        onCancel={() => setConfirmDelete(false)}
        okText="删除"
        cancelText="取消"
        okButtonProps={{ danger: true }}
      >
        <p>删除后，知识库中的内容将无法再被引用，确认删除？</p>
      </Modal>
    </div>
  );
};

export default Settings;
