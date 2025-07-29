import React, { useEffect, useMemo } from 'react';
import { Form, Input, Button, message } from 'antd';
import { postV1AgentGenerateApiKeyById } from '@/client';
import ResponseCode from '@/constants/ResponseCode';
import { CopyOutlined } from '@ant-design/icons';
import { copyToClipboard } from '@/utils/clipboard';

interface ApiContentProps {
  agentId: string;
  workspaceId: string;
  visible: boolean;
  agentInfo: any;
}

const ApiContent: React.FC<ApiContentProps> = (props) => {
  const [form] = Form.useForm();
  const { agentId, agentInfo, visible } = props
  const apiInfo = useMemo(() => agentInfo?.apiKeyList?.[0], [agentInfo]);

  useEffect(() => {
    form.setFieldsValue({
      apiUrl: apiInfo?.apiUrl || '还未存在API URL，请先生成API Key',
      apiKey: apiInfo?.apiKey || '还未存在API Key，请先生成API Key',
    });
  }, [apiInfo, form]);

  const resetApiKey = async () => {
    try {
      const res = await postV1AgentGenerateApiKeyById({
        headers: {
          'Workspace-id': agentInfo?.agent?.workspaceId!,
        },
        path: {
          id: agentId!,
        },
      });
      if (res.data?.code === ResponseCode.S_OK) {
        form.setFieldsValue({
          apiKey: res.data.data?.apiKey,
          apiUrl: res.data.data?.apiUrl,
        });
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

  if (!visible) {
    return <div className="invisible w-0 h-0 m-0 p-0" />;
  }

  return (
    <div className="w-1/2 p-6 px-8">
      <h2 className="text-xl mb-6">Agent后端服务</h2>

      <Form
        form={form}
        layout="vertical"
        initialValues={{
          apiUrl: apiInfo?.apiUrl || '还未存在API URL，请先生成API Key',
          apiKey: apiInfo?.apiKey || '还未存在API Key，请先生成API Key',
        }}
      >
        <Form.Item label="API URL" name="apiUrl">
          <Input readOnly addonAfter={<CopyOutlined onClick={() => handleCopy('apiUrl')} />} />
        </Form.Item>
        <div className="flex gap-4">
          <Form.Item label="API Key" name="apiKey" className='flex-1'>
            <Input readOnly addonAfter={<CopyOutlined onClick={() => handleCopy('apiKey')} />} />
          </Form.Item>
          <Button type="primary" className='mt-8' size="large" onClick={resetApiKey}>
            重新生成API Key
          </Button>
        </div>
      </Form>
    </div>
  );
}


export default ApiContent;
