import React, { useState } from 'react';
import { Form, InputNumber, Input, Button, Space, Typography, Spin, Empty, message } from 'antd';
import { postV1DatasetDocumentsSplitMutation } from '@/client/@tanstack/query.gen';
import { useMutation } from '@tanstack/react-query';
import { LinkOutlined } from '@ant-design/icons';
import { DocumentSourceType } from '@/types/dataset';
interface DataProcessProps {
  documentData: any;
  setDocumentData: (data: any) => void;
  onPrev: () => void;
  onNext: () => void;
}

const DefaultSplitSet = {
  chunkSize: 500,
  separator: '\\n\\n',
  metadata: '',
};

const DataProcess: React.FC<DataProcessProps> = ({ documentData, setDocumentData, onPrev, onNext }) => {
  const [form] = Form.useForm();
  const [previewData, setPreviewData] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  const { mutate: splitDocument } = useMutation({
    ...postV1DatasetDocumentsSplitMutation,
    onSuccess: (data) => {
      setPreviewData(data.data || []);
      setLoading(false);
    },
    onError: (error) => {
      console.error('数据处理失败:', error);
      message.error('数据处理失败');
      setLoading(false);
    },
  });

  const loadPreview = () => {
    setLoading(true);
    const formValues = form.getFieldsValue();
    splitDocument({
      path: {
        documentId: documentData.id,
      },
      headers: {
        'Workspace-id': documentData.workspaceId,
      },
      body: {
        workspaceId: documentData.workspaceId,
        dataSourceType: documentData.dataSourceType,
        chunkSize: formValues.chunkSize || 500,
        separator: formValues.separator || '',
        metadata: formValues.metadata || '',
        name: documentData.dataSourceType === DocumentSourceType.INPUT ? documentData.name : '',
        content: documentData.dataSourceType === DocumentSourceType.INPUT ? documentData.content : '',
        htmlUrl:
          documentData.dataSourceType === DocumentSourceType.HTML
            ? documentData.htmlUrl?.split('\n')?.filter((v: string) => !!v)
            : [],
      },
    });
  };

  const resetForm = () => {
    form.resetFields();
    setPreviewData([]);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setDocumentData({
        ...documentData,
        ...values,
      });
      onNext();
    } catch (error) {
      console.error('验证失败:', error);
    }
  };

  return (
    <div className="flex gap-8 h-full">
      <div className="flex-1" style={{ borderRight: '1px solid #f0f0f0' }}>
        <Typography.Title level={5}>数据处理参数设置</Typography.Title>
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            separator: documentData.separator || DefaultSplitSet.separator,
            chunkSize: documentData.chunkSize || DefaultSplitSet.chunkSize,
            metadata: documentData.metadata || DefaultSplitSet.metadata,
          }}
          labelAlign="left"
          className="mt-8 mr-8"
        >
          <Form.Item name="chunkSize" label="片段长度" className="mb-4">
            <InputNumber min={50} className="w-full" placeholder="请输入片段长度" />
          </Form.Item>
          <Form.Item name="separator" label="分隔符" className="mb-4">
            <Input className="whitespace-pre-wrap" placeholder="请输入分隔符" />
          </Form.Item>
          <Form.Item name="metadata" label="metadata" className="mb-0">
            <Input.TextArea placeholder="可以输入 json 格式的 metadata" rows={4} />
          </Form.Item>
        </Form>
        <Space className="w-full text-center mt-8 justify-between">
          <div>
            <Button
              type="primary"
              size="large"
              onClick={loadPreview}
              disabled={documentData.dataSourceType === 'HTML'}
            >
              预览块
            </Button>
            <Button
              type="default"
              size="large"
              onClick={resetForm}
              className="ml-4"
              disabled={documentData.dataSourceType === 'HTML'}
            >
              重置
            </Button>
          </div>
          <div className="mr-8">
            <Button onClick={onPrev} size="large">
              上一步
            </Button>
            <Button type="primary" onClick={handleSubmit} size="large" className="ml-4">
              下一步
            </Button>
          </div>
        </Space>
      </div>

      <div className="w-1/2">
        <Typography.Title level={5}>
          预览数据<span className="text-gray-400">（至多5个片段）</span>
        </Typography.Title>
        {documentData.dataSourceType === 'HTML' && (
          <div className="mt-8 rounded-lg overflow-y-auto h-15 bg-[#f5f5f5] p-4 flex items-start">
            <LinkOutlined className="text-blue-400 text-xl mt-1" />
            <div className="ml-4">
              <div className="text-lg">{documentData.htmlUrl}</div>
              <Typography.Text>链接内容不支持预览</Typography.Text>
            </div>
          </div>
        )}
        {documentData.dataSourceType !== 'HTML' && (
          <div className="mt-8 rounded-lg overflow-y-auto h-80">
            {loading ? (
              <div className="flex items-center justify-center h-full">
                <Spin tip="加载预览数据中..." />
              </div>
            ) : previewData.length > 0 ? (
              previewData.slice(0, 5).map((chunk, index) => (
                <div key={index} className="bg-gray-50 p-4 mb-4 rounded">
                  <div className="text-gray-400 mb-2">#{index + 1}</div>
                  <div>{chunk || '无内容'}</div>
                </div>
              ))
            ) : (
              <Empty description="暂无预览数据，请点击左侧的“预览块”按钮来加载预览" />
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default DataProcess;
