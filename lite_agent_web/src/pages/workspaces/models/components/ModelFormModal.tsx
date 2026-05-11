import React, { useState, useCallback, useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Switch, Button, Checkbox, Select } from 'antd';
import { isValidHttpUrl } from '@/utils/validUrl';
import { ModelVOAddAction, getV1ModelProviders } from '@/client';
import { InfoCircleOutlined } from '@ant-design/icons';

interface ModelFormModalProps {
  visible: boolean;
  disabledModelRule?: boolean;
  modelVisible?: boolean;
  onCancel: () => void;
  onOk: (values: ModelVOAddAction) => void;
  showExportModal?: (event: any, record: any) => void;
  onDelete?: (id: string) => void;
  initialData?: ModelVOAddAction;
}

const ModelFormModal: React.FC<ModelFormModalProps> = (props) => {
  const { visible, onCancel, onOk, onDelete, initialData, modelVisible } = props;
  const [form] = Form.useForm();
  const isEditing = !!initialData;

  const selectedType = Form.useWatch('type', form);

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

      if (submitValues.type === 'asr') {
        submitValues.responseFormat = 'TEXT';
      }

      onOk(submitValues);
    });
  }, [form, onOk, initialData, isEditing]);

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
          <div>{`指定服务商，如 openai、dashscope 等。默认为 "openai"。`}</div>
        </div>
        <div className='pt-4'>
          <div>•  响应格式(responseFormat)</div>
          <div>{`指定音频输出格式，如 wav、mp3 等。默认为 "wav"。`}</div>
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
      zIndex={100}
      centered
      title={isEditing ? '编辑模型' : '新建模型'}
      open={visible}
      onCancel={onCancel}
      footer={[
        <div key="footer" className="flex justify-between items-center w-full">
          <div className="flex gap-2">
            {!!initialData && !!props.showExportModal && (
              <Button 
                className="rounded-xl h-[40px] px-6"
                onClick={event => props.showExportModal?.(event, initialData as any)}
              >
                导出
              </Button>
            )}
          </div>
          <div className="flex gap-2">
            <Button onClick={onCancel} className="rounded-xl h-[40px] px-6">
              取消
            </Button>
            <Button type="primary" onClick={handleSubmit} className="rounded-xl h-[40px] px-6 bg-[#40A5EE]">
              确定
            </Button>
          </div>
        </div>
      ]}
      maskClosable={false}
      styles={{
        header: { padding: '16px 24px', borderBottom: '1px solid #F2F3F5', marginBottom: 0 },
        body: { padding: '24px' },
        footer: { padding: '10px 16px', borderTop: '1px solid #F2F3F5', marginTop: 0 },
      }}
      width={520}
    >
      <div style={{ maxHeight: '60vh', overflowY: 'auto', paddingRight: 8 }}>
        <Form 
          form={form} 
          layout="vertical"
          className="[&_.ant-form-item-label_label]:text-[#383F44] [&_.ant-form-item-label_label]:font-medium"
        >
          <Form.Item
            label="模型类型"
            required
            tooltip={{ title: toolTipNode, color: '#fff', placement: 'right', icon: <InfoCircleOutlined className="text-[#7C8B98]" /> }}
          >
            {/* 使用 hidden input 让 Form 仍然校验 type 字段；Checkbox 本身不直接绑定到 Form，避免点击两次才生效的问题 */}
            <Form.Item name="type" rules={[{ required: true, message: '请输入模型类型', whitespace: true }]} noStyle>
              <Input type="hidden" />
            </Form.Item>

            <div className="modelGroup flex flex-wrap gap-x-6 gap-y-2 [&_.ant-checkbox-wrapper]:!m-0 [&_.ant-checkbox-inner]:rounded-sm">
              <Checkbox
                checked={selectedType === 'LLM'}
                onChange={() => form.setFieldsValue({ type: 'LLM' })}
                className="!text-[#383F44]"
              >
                LLM
              </Checkbox>
              <Checkbox
                checked={selectedType === 'embedding'}
                onChange={() => form.setFieldsValue({ type: 'embedding' })}
                className="!text-[#383F44]"
              >
                Text Embedding
              </Checkbox>
              <Checkbox
                checked={selectedType === 'tts'}
                onChange={() => form.setFieldsValue({ type: 'tts' })}
                className="!text-[#383F44]"
              >
                TTS
              </Checkbox>
              <Checkbox
                checked={selectedType === 'asr'}
                onChange={() => form.setFieldsValue({ type: 'asr' })}
                className="!text-[#383F44]"
              >
                ASR
              </Checkbox>
            </div>
          </Form.Item>

          <Form.Item
            name="name"
            label="模型名称"
            extra={<span className="text-[#7C8B98] text-xs">这是模型的官方名称，用于系统识别其能力和计费标准。</span>}
            rules={[{ required: true, message: '请输入模型名称', whitespace: true }]}
          >
            <Input maxLength={100} placeholder="请输入模型名称" className="rounded-lg h-10 border-[#E0E3E6]" />
          </Form.Item>

          {!!modelVisible && <Form.Item 
            name="llmModelId" 
            label="模型可见性" 
            rules={[{ required: true }]}
            initialValue="system"
          >
            <Select disabled value="system" className="[&_.ant-select-selector]:rounded-lg [&_.ant-select-selector]:h-10 [&_.ant-select-selection-item]:flex [&_.ant-select-selection-item]:items-center">
              <Select.Option value="system">
                <div className="text-[#7C8B98]">系统</div>
              </Select.Option>
            </Select>
          </Form.Item>}

          <Form.Item
            name="alias"
            label="连接别名"
            extra={<span className="text-[#7C8B98] text-xs">同个模型的名称通过不同API Key可以有多个，所以设置一个易于区分的名称。</span>}
            rules={[{ required: true, message: '请输入连接别名', whitespace: true }]}
          >
            <Input maxLength={100} placeholder="请输入连接别名" className="rounded-lg h-10 border-[#E0E3E6]" />
          </Form.Item>

          {/* TTS专属字段 */}
          <Form.Item
            noStyle
            shouldUpdate={(prev, curr) => prev.type !== curr.type}
          >
            {({ getFieldValue }) =>
              ['tts', 'asr'].includes(getFieldValue('type')) && (
                <Form.Item
                  name="provider"
                  label="模型提供商"
                  rules={[{ message: '请选择模型提供商' }]}
                >
                  <Select
                    placeholder="请选择模型提供商"
                    options={ttsVendors}
                    loading={loadingVendors}
                    className="[&_.ant-select-selector]:rounded-lg [&_.ant-select-selector]:h-10"
                  />
                </Form.Item>
              )
            }
          </Form.Item>

          {/* TTS 字段映射 */}
          <Form.Item
            noStyle
            shouldUpdate={(prev, curr) => prev.type !== curr.type}
          >
            {({ getFieldValue }) =>
              getFieldValue('type') === 'tts' && (
                <Form.Item
                  name="fieldMapping"
                  label="字段映射"
                  rules={[{ message: '请输入字段映射', whitespace: true }]}
                >
                  <Input placeholder="请输入字段映射" className="rounded-lg h-10 border-[#E0E3E6]" />
                </Form.Item>
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
            <Input maxLength={500} placeholder="请输入URL" className="rounded-lg h-10 border-[#E0E3E6]" />
          </Form.Item>

          <Form.Item
            name="apiKey"
            label="API Key"
            rules={props.disabledModelRule ? [] : [{ required: true, message: '请输入API Key', whitespace: true }]}
          >
            <Input maxLength={100} placeholder="请输入key值" className="rounded-lg h-10 border-[#E0E3E6]" />
          </Form.Item>

          <Form.Item
            name="maxTokens"
            label="maxToken最大值"
            rules={[
              {
                type: 'number',
                min: 1,
                max: 2147483647,
                message: '请输入1-2147483647之间的整数',
              },
            ]}
          >
            <InputNumber
              className="rounded-lg h-10 border-[#E0E3E6] [&_.ant-input-number-input]:h-10"
              style={{ width: '100%' }}
              min={1}
              max={2147483647}
              placeholder="请输入maxToken最大值"
              precision={0}
            />
          </Form.Item>

          <Form.Item
            name="contextWindows"
            label="上下文长度"
            rules={[
              {
                type: 'number',
                min: 1,
                max: 2147483647,
                message: '请输入1-2147483647之间的整数',
              },
            ]}
          >
            <InputNumber
              className="rounded-lg h-10 border-[#E0E3E6] [&_.ant-input-number-input]:h-10"
              style={{ width: '100%' }}
              min={1}
              max={2147483647}
              placeholder="请输入上下文长度最大值"
              precision={0}
            />
          </Form.Item>

           {/* TTS 和 ASR 专属字段,是否支持流式输出 */}
           <Form.Item
            noStyle
            shouldUpdate={(prev, curr) => prev.type !== curr.type}
          >
            {({ getFieldValue }) =>
              ['tts', 'asr'].includes(getFieldValue('type')) && (
                <Form.Item
                  name="streamable"
                  label="支持流式输出"
                  valuePropName="checked"
                >
                  <Switch />
                </Form.Item>
              )
            }
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prevValues, currentValues) => prevValues.type !== currentValues.type}
          >
            {({ getFieldValue }) =>
              getFieldValue('type') === 'LLM' && (
                <Form.Item
                  label="设置："
                >
                  <div className="flex flex-col gap-2">
                    <div className="flex gap-2">
                      <div className="flex flex-1 items-center justify-between">
                        <span className="text-[#383f44] text-sm leading-[22px] font-normal">支持Auto Agent使用</span>
                        <Form.Item
                          name="autoAgent"
                          valuePropName="checked"
                          noStyle
                        >
                          <Switch size='small' />
                        </Form.Item>
                      </div>
                      <div className="flex flex-1 items-center justify-between">
                        <span className="text-[#383f44] text-sm leading-[22px] font-normal">支持工具使用</span>
                        <Form.Item
                          name="toolInvoke"
                          valuePropName="checked"
                          initialValue={true}
                          noStyle
                        >
                          <Switch size='small' defaultChecked />
                        </Form.Item>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <div className="flex flex-1 items-center justify-between">
                        <span className="text-[#383f44] text-sm leading-[22px] font-normal">支持深度思考</span>
                        <Form.Item
                          name="deepThink"
                          valuePropName="checked"
                          noStyle
                        >
                          <Switch size='small' />
                        </Form.Item>
                      </div>
                      <div className="flex flex-1 items-center justify-between">
                        <span className="text-[#383f44] text-sm leading-[22px] font-normal">支持视觉理解</span>
                        <Form.Item
                          name="vision"
                          valuePropName="checked"
                          noStyle
                        >
                          <Switch size='small' />
                        </Form.Item>
                      </div>
                    </div>
                  </div>
                </Form.Item>
              )
            }
          </Form.Item>
        </Form>
      </div>
    </Modal>
  );
};

export default ModelFormModal;
