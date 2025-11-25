import React, { useState, useCallback, useEffect } from 'react';
import { Modal, Form, Input, Switch, Button, Popconfirm, Radio, Select } from 'antd';
import { isValidHttpUrl } from '@/utils/validUrl';
import { ModelVOAddAction, getV1ModelProviders } from '@/client';
import { InfoCircleOutlined } from '@ant-design/icons';

interface ModelFormModalProps {
  visible: boolean;
  disabledModelRule?: boolean;
  onCancel: () => void;
  onOk: (values: ModelVOAddAction) => void;
  showExportModal?: (event: any, record: any) => void;
  onDelete?: (id: string) => void;
  initialData?: ModelVOAddAction;
}

const ModelFormModal: React.FC<ModelFormModalProps> = (props) => {
  const { visible, onCancel, onOk, onDelete, initialData } = props;
  const [form] = Form.useForm();
  const isEditing = !!initialData;

  const [ttsVendors, setTtsVendors] = useState<{ label: string, value: string }[]>([]);
  const [loadingVendors, setLoadingVendors] = useState(false);

  const handleSubmit = useCallback(async () => {
    form.validateFields().then((values) => {
      const submitValues = { ...values, id: initialData?.id };

      if (submitValues.type === 'tts') {
        // 当类型为 tts 时，且是 prmeasure，设置 responseFormat 为 pcm
        if (submitValues.baseUrl) {
          if (submitValues.baseUrl.includes('prmeasure')) {
            submitValues.responseFormat = 'pcm';
          } else {
            submitValues.responseFormat = 'wav';
          }
        }
      }

      onOk(submitValues);
    });
  }, [form, onOk, initialData, isEditing]);

  const handleDelete = () => {
    if (initialData?.id && onDelete) {
      onDelete(initialData.id);
    }
  };

  const getModelProviders = useCallback(async () => {
    const res = await getV1ModelProviders({});
    const providers = (res.data?.data || {}) as Record<string, string>;
    const modelProviders = Object.keys(providers).map((key) => ({
      value: key,
      label: providers[key]
    }));
    setTtsVendors(modelProviders);
    // setLoadingVendors(false);
  }, []);

  useEffect(() => {
    if (initialData) {
      // form.setFieldsValue(initialData);
      // 处理 TTS 的 fieldMapping 字段
      const patch = { ...initialData };
      if (
        patch.type === 'tts' &&
        typeof patch.fieldMapping === 'string'
      ) {
        try {
          const obj = JSON.parse(patch.fieldMapping);
          // patch.fieldMapping = obj.input || '';
        } catch {
          // 解析失败则保持原样
        }
      }
      form.setFieldsValue(patch);
    } else {
      form.resetFields();
    }
  }, [initialData, form]);

  useEffect(() => {
    if (!visible) {
      form.resetFields();
    }
  }, [visible, form]);

  // 获取模型提供商
  useEffect(() => {
    visible && getModelProviders();
  }, [visible]);

  const toolTipNode = () => {
    return (
      <div className='text-black/65 text-xs p-1 leading-5'>
        <div className='pb-2 border-0 border-b border-solid border-b-black/5'>大模型配置说明</div>
        <div className='pt-2'>
          <div>•  模型供应商 (provider)</div>
          <div>指定服务商，如 openai、dashscope 等。默认为 "openai"。</div>
        </div>
        <div className='pt-4'>
          <div>•  响应格式(responseFormat)</div>
          <div>指定音频输出格式，如 wav、mp3 等。默认为 "wav"。</div>
        </div>
        <div className='pt-4'>
          <div>•  字段映射(fieldMapping)</div>
          <div>用于兼容非 OpenAI 标准的接口参数。</div>
        </div>
        <div className='pt-4'>{`例如，一些模型的文本参数是 "tts_text" 而不是标准的 "input"，可按以下 JSON 格式配置以实现兼容： {"input": "tts_text" }`}</div>
      </div>
    )
  }

  return (
    <Modal
      zIndex={10}
      centered
      title={isEditing ? '编辑模型' : '新建模型'}
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText="确定"
      cancelText="取消"
      maskClosable={false}
    >
      <div style={{ maxHeight: '70vh', overflowY: 'auto', paddingRight: 8 }}>
        <Form form={form} layout="vertical">
          <Form.Item
            name="type"
            label="模型类型"
            rules={[{ required: true, message: '请输入模型类型', whitespace: true }]}
            tooltip={{ title: toolTipNode, color: '#fff', placement: 'right', icon: <InfoCircleOutlined /> }}
          >
            <Radio.Group>
              <Radio value="LLM">LLM</Radio>
              <Radio value="embedding">Text Embedding</Radio>
              <Radio value="tts">TTS</Radio>
              <Radio value="asr">ASR</Radio>
            </Radio.Group>
          </Form.Item>

          <Form.Item
            name="name"
            label="模型名称"
            extra="这是模型的官方名称，用于系统识别其能力和计费标准。"
            rules={[{ required: true, message: '请输入模型名称', whitespace: true }]}
          >
            <Input maxLength={100} placeholder="请输入模型名称" />
          </Form.Item>

          <Form.Item
            name="alias"
            label="连接别名"
            extra="同个模型的名称通过不同API Key可以有多个，所以设置一个易于区分的名称。"
            rules={[{ required: true, message: '请输入连接别名', whitespace: true }]}
          >
            <Input maxLength={100} placeholder="请输入连接别名" />
          </Form.Item>

          {/* TTS专属字段 */}
          <Form.Item
            noStyle
            shouldUpdate={(prev, curr) => prev.type !== curr.type}
          >
            {({ getFieldValue }) =>
              getFieldValue('type') === 'tts' && (
                <>
                  <Form.Item
                    name="provider"
                    label="模型提供商"
                    rules={[{ message: '请选择模型提供商' }]}
                  >
                    <Select
                      placeholder="请选择模型提供商"
                      options={ttsVendors}
                      loading={loadingVendors}
                    />
                  </Form.Item>
                  <Form.Item
                    name="fieldMapping"
                    label="字段映射"
                    rules={[{ message: '请输入字段映射', whitespace: true }]}
                  >
                    <Input placeholder="请输入字段映射" />
                  </Form.Item>
                </>
              )
            }
          </Form.Item>

          <Form.Item
            name="baseUrl"
            label="BaseURL"
            rules={props.disabledModelRule ? [] : [
              {
                required: true,
                message: '请输入URL',
                whitespace: true,
              },
              () => ({
                validator(_, value) {
                  if (!value?.trim()) {
                    return Promise.resolve();
                  }
                  if (isValidHttpUrl(value)) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('请输入正确的URL'));
                },
              }),
            ]}
          >
            <Input maxLength={500} placeholder="请输入URL" />
          </Form.Item>

          <Form.Item
            name="apiKey"
            label="API Key"
            rules={props.disabledModelRule ? [] : [{ required: true, message: '请输入API Key', whitespace: true }]}
          >
            <Input maxLength={100} placeholder="请输入key值" />
          </Form.Item>

          <Form.Item
            name="maxTokens"
            label="maxToken最大值"
            rules={[
              {
                pattern: /^[1-9]\d*$/,
                message: '请输入大于0的正整数',
              },
            ]}
          >
            <Input min={1} maxLength={9} type="number" placeholder="请输入maxToken最大值" />
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prevValues, currentValues) => prevValues.type !== currentValues.type}
          >
            {({ getFieldValue }) =>
              getFieldValue('type') === 'LLM' && (
                <div className="flex justify-between gap-4">
                  <Form.Item
                    name="autoAgent"
                    label="支持auto agent使用"
                    valuePropName="checked"
                    className="flex-1 mb-0"
                  >
                    <Switch />
                  </Form.Item>
                  <Form.Item
                    name="toolInvoke"
                    label="支持工具调用"
                    valuePropName="checked"
                    className="flex-1 mb-0"
                    initialValue={true}
                  >
                    <Switch defaultChecked />
                  </Form.Item>
                  <Form.Item
                    name="deepThink"
                    label="支持深度思考"
                    valuePropName="checked"
                    className="flex-1 mb-0"
                  >
                    <Switch />
                  </Form.Item>
                </div>
              )
            }
          </Form.Item>
        </Form>
      </div>
      {isEditing && onDelete && (
        <Popconfirm
          title="确认删除"
          description="即将删除模型的所有信息，确认删除？"
          onConfirm={handleDelete}
          okText="确认"
          cancelText="取消"
        >
          <Button danger className="bottom-[20px] float-left absolute">
            删除
          </Button>
        </Popconfirm>
      )}
      {!!initialData && !!props.showExportModal && (
        <Button className={`bottom-[20px] float-left absolute ${isEditing && onDelete ? 'left-[120px]' : ''}`} onClick={event => props.showExportModal?.(event, initialData as any)} key={`edit-${initialData?.id}`}>
          导出
        </Button>
      )}
    </Modal>
  );
};

export default ModelFormModal;
