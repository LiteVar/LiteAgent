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
  };

  return (
    <div className="max-w-[50%] h-full">
      <div className="bg-white/40 backdrop-blur-sm py-6 px-4 rounded-2xl border border-white/60 shadow-sm h-[calc(100%-48px)]">
        <div className="text-xl mt-0 mb-8">知识库后端服务</div>
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            apiUrl: datasetInfo?.apiUrl || '还未存在API URL，请先生成API Key',
            apiKey: datasetInfo?.apiKey || '还未存在API Key，请先生成API Key',
          }}
          className="customForm"
        >
          <Form.Item label={<span className="text-gray-600 font-medium">API URL</span>} name="apiUrl">
            <Input
              readOnly
              suffix={<CopyOutlined className="text-[E0E3E6] cursor-pointer hover:text-blue-600 p-2" onClick={() => handleCopy('apiUrl')} />}
              className="!rounded-xl !border-[#E0E3E6] !bg-white !backdrop-blur-0 !h-11 transition-all focus:!bg-white focus:!backdrop-blur-0 [&.ant-input-affix-wrapper]:!bg-white [&.ant-input-affix-wrapper]:!backdrop-blur-0 [&.ant-input-affix-wrapper-focused]:!bg-white [&.ant-input-affix-wrapper-focused]:!backdrop-blur-0 [&.ant-input]:!bg-white [&.ant-input]:!backdrop-blur-0 [&.ant-input-focused]:!bg-white [&.ant-input-focused]:!backdrop-blur-0"
            />
          </Form.Item>
          
          <div className="flex flex-col gap-6">
            <Form.Item label={<span className="text-gray-600 font-medium">API Key</span>} name="apiKey" className="!mb-0">
              <Input
                readOnly
                suffix={<CopyOutlined className="text-[E0E3E6] cursor-pointer hover:text-blue-600 p-2" onClick={() => handleCopy('apiKey')} />}
                className="!rounded-xl !border-[#E0E3E6] !bg-white !backdrop-blur-0 !h-11 transition-all focus:!bg-white focus:!backdrop-blur-0 [&.ant-input-affix-wrapper]:!bg-white [&.ant-input-affix-wrapper]:!backdrop-blur-0 [&.ant-input-affix-wrapper-focused]:!bg-white [&.ant-input-affix-wrapper-focused]:!backdrop-blur-0 [&.ant-input]:!bg-white [&.ant-input]:!backdrop-blur-0 [&.ant-input-focused]:!bg-white [&.ant-input-focused]:!backdrop-blur-0"
              />
            </Form.Item>
            
            {canEdit && (
              <div className="flex justify-end">
                <Button 
                  size="large" 
                  onClick={resetApiKey}
                  className="rounded-xl bg-transparent font-normal border-[#40A5EE] text-[#40A5EE] px-8"
                >
                  重新生成 API Key
                </Button>
              </div>
            )}
          </div>
        </Form>
      </div>
    </div>
  );
};

export default Apis;
