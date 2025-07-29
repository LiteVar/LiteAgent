import React, { useEffect } from 'react';
import { Form, Input, Button, message } from 'antd';
import { useDatasetContext } from '@/contexts/datasetContext';
import { getV1DatasetByIdApiKeyGenerate } from '@/client';
import ResponseCode from '@/constants/ResponseCode';
import { CopyOutlined } from '@ant-design/icons';
import { copyToClipboard } from '@/utils/clipboard';

const Apis = ({ refetch }: { refetch: () => void }) => {
  const [form] = Form.useForm();
  const { workspaceId, datasetInfo } = useDatasetContext();
  const canEdit = datasetInfo?.canEdit;

  useEffect(() => {
    form.setFieldsValue({
      apiUrl: datasetInfo?.apiUrl || '还未存在API URL，请先生成API Key',
      apiKey: datasetInfo?.apiKey || '还未存在API Key，请先生成API Key',
    });
  }, [datasetInfo, form]);

  const resetApiKey = async () => {
    try {
      const res = await getV1DatasetByIdApiKeyGenerate({
        headers: {
          'Workspace-id': workspaceId!,
        },
        path: {
          id: datasetInfo?.id!,
        },
      });
      if (res.data?.code === ResponseCode.S_OK) {
        form.setFieldsValue({
          apiKey: res.data.data?.apiKey,
          apiUrl: res.data.data?.apiUrl,
        });
        refetch();
        message.success('API Key 生成成功');
      }
    } catch (error) {
      console.error(error);
      message.error('API Key 生成失败');
    }
  };

  const handleCopy = (content: 'apiKey' | 'apiUrl') => {
    const val = form.getFieldValue(content);
    copyToClipboard(val);
    message.success('复制成功');
  };

  return (
    <div className="w-1/2">
      <h2 className="text-xl mb-6">知识库后端服务</h2>

      <Form
        form={form}
        layout="vertical"
        initialValues={{
          apiUrl: datasetInfo?.apiUrl || '还未存在API URL，请先生成API Key',
          apiKey: datasetInfo?.apiKey || '还未存在API Key，请先生成API Key',
        }}
      >
        <Form.Item label="API URL" name="apiUrl">
          <Input readOnly addonAfter={<CopyOutlined onClick={() => handleCopy('apiUrl')} />} />
        </Form.Item>
        <div className="flex gap-4">
          <Form.Item label="API Key" name="apiKey" className="flex-1">
            <Input readOnly addonAfter={<CopyOutlined onClick={() => handleCopy('apiKey')} />} />
          </Form.Item>
          {canEdit && (
            <Button type="primary" className="mt-8" size="large" onClick={resetApiKey}>
              重新生成API Key
            </Button>
          )}
        </div>
      </Form>
    </div>
  );
};

export default Apis;
