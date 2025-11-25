import React, { useState, useEffect } from 'react';
import { Form, Input, Radio, Button, message } from 'antd';
import type { FormInstance, RadioChangeEvent } from 'antd';
import { getV1ToolLoadOpenToolSchema } from '@/client/services.gen';
import ResponseCode from '@/constants/ResponseCode';
import { isJSON } from '@/utils/isJson';

const { TextArea } = Input;

interface OpenToolSpecFormProps {
  form: FormInstance;
  onValuesChange?: (changedValues: Record<string, unknown>, allValues: Record<string, unknown>) => void;
}

interface OpenToolSpecData {
  origin: 'server' | 'input';
  apiKey: string;
  serverUrl: string;
  schema: string;
}

const OpenToolSpecForm: React.FC<OpenToolSpecFormProps> = ({ form, onValuesChange }) => {
  const [loading, setLoading] = useState(false);
  
  // 从表单字段获取当前的数据来源
  const dataSource = Form.useWatch('origin', form) || 'server';

  // 从 schemaStr 解析数据并填充表单
  useEffect(() => {
    const schemaStr = form.getFieldValue('schemaStr');
    if (schemaStr && typeof schemaStr === 'string') {
      try {
        const parsedData: OpenToolSpecData = JSON.parse(schemaStr);

        // 设置表单字段值
        form.setFieldsValue({
          origin: parsedData.origin || 'server',
          apiKey: parsedData.apiKey || '',
          serverUrl: parsedData.serverUrl || '',
          schema: parsedData.schema || '',
        });
      } catch (error) {
        console.error('解析 schemaStr 失败:', error);
        // 如果解析失败，使用默认值
        form.setFieldValue('origin', 'server');
      }
    }
  }, [form]);

  // 更新 schemaStr 字段
  const updateSchemaStr = (updateServerUrlFromSchema?: boolean) => {
    const formValues = form.getFieldsValue();

    if (updateServerUrlFromSchema && !!formValues.schema && isJSON(formValues.schema)) {
      const schema = JSON.parse(formValues.schema || '{}');
      const serverUrl = schema.server?.url;
      if (serverUrl) {
        form.setFieldValue('serverUrl', serverUrl);
      }
    }

    const openToolSpecData: OpenToolSpecData = {
      origin: formValues.origin || 'server',
      apiKey: formValues.apiKey || '',
      serverUrl: formValues.serverUrl || '',
      schema: formValues.schema || '', // 无论哪种模式，都使用 schema 字段
    };

    const schemaStr = JSON.stringify(openToolSpecData);
    form.setFieldValue('schemaStr', schemaStr);

    return schemaStr;
  };

  // 处理数据来源切换
  const handleDataSourceChange = (e: RadioChangeEvent) => {
    const value = e.target.value as 'server' | 'input';

    // 设置 origin 字段
    form.setFieldValue('origin', value);

    // 更新 schemaStr
    setTimeout(() => {
      const schemaStr = updateSchemaStr();
      // 触发 onValuesChange 回调
      if (onValuesChange) {
        onValuesChange({ schemaStr }, form.getFieldsValue());
      }
    }, 0);
  };

  // 从服务器获取 schema
  const handleLoadSchema = async () => {
    try {
      const apiKey = form.getFieldValue('apiKey');
      const serverUrl = form.getFieldValue('serverUrl');

      if (!serverUrl) {
        message.error('请先填写服务器地址');
        return;
      }

      setLoading(true);

      const response = await getV1ToolLoadOpenToolSchema({
        query: {
          apiKey,
          host: serverUrl,
        },
      });

      if (response.data?.code === ResponseCode.S_OK) {
        form.setFieldValue('schema', response.data.data);
        message.success('Schema 获取成功');

        // 更新 schemaStr
        setTimeout(() => {
          const schemaStr = updateSchemaStr();
          // 触发 onValuesChange 回调
          if (onValuesChange) {
            onValuesChange({ schemaStr }, form.getFieldsValue());
          }
        }, 0);
      } else {
        message.error('获取 Schema 失败，请检查服务器地址和 API Key');
      }
    } catch (error) {
      console.error('Load schema error:', error);
      message.error('获取 Schema 失败，请检查网络连接和服务器状态');
    } finally {
      setLoading(false);
    }
  };

  // 处理表单字段变化
  const handleFieldChange = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>, name?: string) => {
    event.stopPropagation();
    setTimeout(() => {
      const schemaStr = updateSchemaStr(name === 'schema' ? true : undefined);
      if (onValuesChange) {
        onValuesChange({ schemaStr }, form.getFieldsValue());
      }
    }, 0);
  };

  return (
    <>
      {/* 数据来源选择 */}
      <Form.Item
        label="数据来源"
        name="origin"
        rules={[{ required: true, message: '请选择数据来源' }]}
        initialValue="server"
      >
        <Radio.Group onChange={handleDataSourceChange}>
          <Radio value="server">从服务器获取</Radio>
          <Radio value="input">手动输入</Radio>
        </Radio.Group>
      </Form.Item>

      {/* 从服务器获取模式 */}
      {dataSource === 'server' && (
        <>
          <Form.Item
            label="API Key"
            name="apiKey"
          >
            <Input
              placeholder="请输入 API Key"
              maxLength={150}
              prefix="Bearer"
              onChange={handleFieldChange}
            />
          </Form.Item>

          <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-end' }}>
            <Form.Item
              label="服务器地址"
              name="serverUrl"
              style={{ flex: 1 }}
              rules={[{ required: true, message: '请输入服务器地址' }]}
            >
              <Input placeholder="请输入服务器地址" onChange={handleFieldChange} />
            </Form.Item>

            <Form.Item style={{ marginBottom: 24 }}>
              <Button type="primary" onClick={handleLoadSchema} loading={loading}>
                获取
              </Button>
            </Form.Item>
          </div>

          {/* 获取到的 schema 显示区域 */}
          <Form.Item
            label="获取到的文稿内容"
            name="schema"
            rules={[{ required: true, message: '获取服务器 schema 后，这里显示内容' }]}
          >
            <TextArea rows={4} placeholder="获取服务器地址后，这里显示内容" readOnly />
          </Form.Item>
        </>
      )}

      {/* 手动输入模式 */}
      {dataSource === 'input' && (
        <>
          <Form.Item
            label="API Key"
            name="apiKey"
          >
            <Input
              placeholder="请输入 API Key"
              maxLength={150}
              prefix="Bearer"
              onChange={handleFieldChange}
            />
          </Form.Item>
          <Form.Item
            label="服务器地址"
            name="serverUrl"
            style={{ flex: 1 }}
            rules={[{ required: true, message: '请输入服务器地址' }]}
          >
            <Input placeholder="请输入服务器地址" onChange={handleFieldChange} />
          </Form.Item>
          <Form.Item
            label="请输入文稿内容"
            name="schema"
            rules={[
              { required: true, message: '请输入 OpenTool Spec 文稿（OpenTool Spec 定义）', whitespace: true },
            ]}
          >
          <TextArea
            rows={8}
            maxLength={20000}
            placeholder="请输入 OpenTool Spec 文稿（OpenTool Spec 定义）"
            onChange={e => handleFieldChange(e, 'schema')}
          />
        </Form.Item>
        </>
      )}
    </>
  );
};

export default OpenToolSpecForm;
