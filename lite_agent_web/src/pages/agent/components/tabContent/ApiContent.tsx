import React, { useCallback, useEffect, useMemo } from 'react';
import { Form, Input, Button, message } from 'antd';
import { postV1AgentGenerateApiKeyById } from '@/client';
import ResponseCode from '@/constants/ResponseCode';
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

  

  const copyIconButton = useCallback((content: 'apiKey' | 'apiUrl') => {

    const handleCopy = (content: 'apiKey' | 'apiUrl') => {
      const val = form.getFieldValue(content);
      copyToClipboard(val);
    };

    return (
      <div onClick={() => handleCopy(content)} className='flex items-center justify-center w-6 h-6 cursor-pointer rounded-full'>
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M9.1875 9.1875H11.8125V2.1875H4.8125V4.8125" stroke="#C7CDD3" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M9.1875 4.8125H2.1875V11.8125H9.1875V4.8125Z" stroke="#C7CDD3" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      </div>
    )
  }, [form]);

  return (
    <div className={visible ? "max-w-[500px] px-4 py-6 bg-white/60 rounded-2xl h-[calc(100%-48px)]" : "invisible w-0 h-0 m-0 p-0 overflow-hidden"}>
      <h2 className="text-xl mt-0 mb-4">Agent后端服务</h2>

      <Form
        form={form}
        layout="vertical"
        initialValues={{
          apiUrl: apiInfo?.apiUrl || '还未存在API URL，请先生成API Key',
          apiKey: apiInfo?.apiKey || '还未存在API Key，请先生成API Key',
        }}
      >
        <Form.Item className='[&_label]:font-bold [&_.ant-input-group-addon]:bg-white' label="API URL" name="apiUrl">
          <Input className='[&_.ant-input]:border-r-0 [&_.ant-input]:border-[#E0E3E6] bg-white rounded-xl overflow-hidden' readOnly addonAfter={copyIconButton('apiUrl')} />
        </Form.Item>
        <div className="mt-4">
          <Form.Item label="API Key" name="apiKey" className='[&_label]:font-bold [&_.ant-input-group-addon]:bg-white'>
            <Input className='[&_.ant-input]:border-r-0 [&_.ant-input]:border-[#E0E3E6] bg-white rounded-xl overflow-hidden' readOnly addonAfter={copyIconButton('apiKey')} />
          </Form.Item>
          <div className='flex justify-end'>
            <Button type="primary" className='bg-transparent border-[#40A5EE] text-[#40A5EE] rounded-xl font-normal' size="large" onClick={resetApiKey}>
              重新生成API Key
            </Button>
          </div>
        </div>
      </Form>
    </div>
  );
}


export default ApiContent;
