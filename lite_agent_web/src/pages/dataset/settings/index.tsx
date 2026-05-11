import React, { useState, useEffect, useMemo } from 'react';
import { Form, Input, Button, InputNumber, Modal, Select, message } from 'antd';
import { useDatasetContext } from '@/contexts/datasetContext';
import { deleteV1DatasetById, ModelDTO, putV1DatasetById } from '@/client';
import ResponseCode from '@/constants/ResponseCode';
import { useQuery } from '@tanstack/react-query';
import { getV1DatasetByDatasetIdDocumentsOptions } from '@/client/@tanstack/query.gen';
import { Link, useNavigate } from 'react-router-dom';

interface SettingsProps {
  currentLLMModel?: ModelDTO;
  refetch: () => void;
}

const Settings: React.FC<SettingsProps> = ({ refetch, currentLLMModel }) => {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const { workspaceId, datasetInfo, models } = useDatasetContext();
  const [llmModelId, setLLMModelId] = useState<string>('');
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

  useEffect(() => {
    if (currentLLMModel?.id) {
      setLLMModelId(currentLLMModel.id);
    }
  }, [currentLLMModel])

  const docsLength = data?.data?.total || 0;

  useEffect(() => {
    const savedModel = datasetInfo?.embeddingModel;
    const modelExists = models?.some((m) => m.type === 'embedding' && m.id === savedModel);

    form.setFieldsValue({
      name: datasetInfo?.name,
      description: datasetInfo?.description,
      model: modelExists ? savedModel : undefined,
      llmModelId: (currentLLMModel?.status === 2) ? '' : datasetInfo?.llmModelId,
      retrievalTopK: datasetInfo?.retrievalTopK,
      retrievalScoreThreshold: datasetInfo?.retrievalScoreThreshold,
    });
  }, [datasetInfo, form, models, currentLLMModel]);

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
        setConfirmDelete(false);
        navigate(`/workspaces/${datasetInfo?.workspaceId}/datasets`);
      } else {
        message.error(res.data?.message || '删除失败');
        setConfirmDelete(false);
      }
    } catch (error) {
      // Error message
    }
  };

  const onChangeLLMModel = (value: string) => {
    setLLMModelId(value);
  };

  return (
    <div className="max-w-lg h-full">
      <div className=" bg-white/40 rounded-2xl px-4 py-6 border border-white/60 shadow-sm h-[calc(100%-48px)]">
        <div className="flex justify-between items-center mt-0 mb-8">
          <div className="text-xl m-0">知识库设置</div>
        </div>
        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          initialValues={{
            name: datasetInfo?.name,
            description: datasetInfo?.description,
            embeddingModel: datasetInfo?.embeddingModel,
            llmModelId: (currentLLMModel?.status === 2) ? '' : currentLLMModel?.id,
            retrievalTopK: datasetInfo?.retrievalTopK,
            retrievalScoreThreshold: datasetInfo?.retrievalScoreThreshold,
          }}
          className="customForm"
        >
          <div className="gap-x-8">
            <div className="space-y-4">
              <Form.Item
                name="name"
                label={<span className="text-gray-600 font-medium">知识库名称</span>}
                rules={[
                  { required: true, message: '请输入知识库名称' },
                  { max: 40, message: '名称不能超过40个字符' },
                ]}
              >
                <Input placeholder="知识库名称" className="!rounded-xl !border-[#E0E3E6] !bg-white/80 !h-11 focus:!bg-white transition-all" />
              </Form.Item>

              <Form.Item name="description" label={<span className="text-gray-600 font-medium">描述</span>}>
                <Input.TextArea
                  placeholder="用简单几句话描述知识库"
                  maxLength={200}
                  autoSize={{ minRows: 4, maxRows: 6 }}
                  className="!rounded-xl !border-[#E0E3E6] !bg-white/80 focus:!bg-white transition-all !p-3"
                />
              </Form.Item>
            </div>

            <div className="space-y-4 mt-4">
              <Form.Item name="embeddingModel" label={<span className="text-gray-600 font-medium">嵌入模型</span>} required>
                <Select 
                  placeholder="请选择嵌入模型" 
                  disabled={docsLength > 0}
                  className="customSelect !h-11 [&_.ant-select-selector]:!rounded-xl [&_.ant-select-selector]:!border-[#E0E3E6] [&_.ant-select-selector]:!bg-gray/80"
                >
                  {models
                    ?.filter((m) => m.type === 'embedding')
                    .map((model) => (
                      <Select.Option key={model.id} value={model.id}>
                        {model.alias}
                      </Select.Option>
                    ))}
                  <Select.Option className="p-0" key="new" value={null}>
                    <Link className="w-full h-full block px-3 leading-9 text-blue-500 hover:bg-blue-50" to={`/workspaces/${workspaceId!}/models`}>
                      + 新建模型
                    </Link>
                  </Select.Option>
                </Select>
              </Form.Item>

              <Form.Item name="llmModelId" label={<span className="text-gray-600 font-medium">摘要模型</span>}>
                <Select 
                  onChange={onChangeLLMModel}
                  value={(currentLLMModel?.status === 2 && currentLLMModel?.id === llmModelId) ? '' : llmModelId}
                  placeholder="请选择摘要模型"
                  className="customSelect !h-11 [&_.ant-select-selector]:!rounded-xl [&_.ant-select-selector]:!border-[#E0E3E6] [&_.ant-select-selector]:!bg-white/80"
                >
                  {models
                    ?.filter((m) => m.type === 'LLM' && m.status === 1)
                    .map((model) => (
                      <Select.Option key={model.id} value={model.id}>
                        {model.alias}
                      </Select.Option>
                    ))}
                  <Select.Option className="p-0" key="new" value={undefined}>
                    <Link className="w-full h-full block px-3 leading-9 text-blue-500 hover:bg-blue-50" to={`/workspaces/${workspaceId!}/models`}>
                      + 新建模型
                    </Link>
                  </Select.Option>
                </Select>
              </Form.Item>

              {(currentLLMModel?.status === 2 && currentLLMModel?.id === llmModelId) && <div className="text-red-500 text-xs mt-2 flex items-center">
                <div className='text-ellipsis overflow-hidden whitespace-nowrap'>{currentLLMModel?.alias || currentLLMModel?.name}</div>
                <div className='flex-none'>，模型已停用，请重新选择并保存，以确保摘要功能正常使用。</div>
              </div>}

              <div>
                <Form.Item name="retrievalTopK" label={<span className="text-gray-600 font-medium text-xs">搜索结果条数</span>}>
                  <InputNumber min={1} max={20} className="w-25 !rounded-xl !border-[#E0E3E6] !bg-white/80 !h-11 flex items-center" />
                </Form.Item>

                <Form.Item name="retrievalScoreThreshold" label={<span className="text-gray-600 font-medium text-xs">最大向量距离</span>}>
                  <InputNumber min={0} max={1} step={0.1} className="w-25 !rounded-xl !border-[#E0E3E6] !bg-white/80 !h-11 flex items-center" />
                </Form.Item>
              </div>
            </div>
          </div>

          <div className="flex justify-end gap-4 mt-2 border-t border-black/5">
            {datasetInfo?.canDelete && (
              <Button
                danger
                size="large"
                className="rounded-xl px-12 flex-1 bg-[#CC2D3A] hover:!bg-[#CC2D3A]/90 text-white border-none"
                onClick={() => setConfirmDelete(true)}
              >
                删除
              </Button>
            )}
            <Button
              type="primary"
              disabled={!datasetInfo?.canEdit}
              loading={loading}
              htmlType="submit"
              size="large"
              className="flex-1 rounded-xl bg-[#40A5EE] hover:!bg-[#40A5EE]/90 border-none shadow-md shadow-blue-200/50 px-12"
            >
              保存
            </Button>
          </div>
        </Form>
      </div>

      <Modal
        centered={true}
        title="确认删除"
        open={confirmDelete}
        onOk={handleDelete}
        onCancel={() => setConfirmDelete(false)}
        okText="确认删除"
        cancelText="取消"
        okButtonProps={{ danger: true, className: 'rounded-lg' }}
        cancelButtonProps={{ className: 'rounded-lg' }}
      >
        <div className="py-4">
          <p className="text-gray-600">删除后，该知识库及其包含的所有文档内容将无法再被 Agent 引用，且无法恢复。确认删除？</p>
        </div>
      </Modal>
    </div>
  );
};

export default Settings;
