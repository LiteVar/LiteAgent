import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Modal, Form, Input, Select, Button, Popconfirm, Upload, Image, message, Switch } from 'antd';
import { ExclamationCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { ToolSchemaType } from '@/types/Tool';
import { UploadChangeParam } from 'antd/es/upload/interface';
import { buildImageUrl } from '@/utils/buildImageUrl';
import OpenToolSpecForm from './OpenToolSpecForm';

import type { GetProp, UploadFile, UploadProps } from 'antd';
import { beforeUpload, customUploadRequest } from '@/utils/uploadFile';
import ResponseCode from '@/constants/ResponseCode';
import type { ToolVOAddAction, ToolVOUpdateAction } from '@/client';

type FileType = Parameters<GetProp<UploadProps, 'beforeUpload'>>[0];

const { TextArea } = Input;
const NO_SELECT = 'no-select';
const requiresAuth = [ToolSchemaType.OPEN_API3, ToolSchemaType.JSON_RPC];

interface ToolModalProps {
  visible: boolean;
  onCancel: () => void;
  onOk: (id: string, values: ToolVOAddAction | ToolVOUpdateAction) => Promise<number>;
  onDelete?: (id: string) => void;
  showExportModal?: (event: React.MouseEvent, record: any) => void;
  initialData?: any;
}

const CreateToolModal: React.FC<ToolModalProps> = (props) => {
  const { visible, onCancel, onOk, onDelete, showExportModal, initialData } = props;
  const [form] = Form.useForm();
  const [isEditing, setIsEditing] = useState(false);

  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewImage, setPreviewImage] = useState('');
  const [imageName, setImageName] = useState<string>('');
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [selectedSchemaType, setSelectedSchemaType] = useState<ToolSchemaType | undefined>(undefined);
  const originalTypeRef = useRef<ToolSchemaType | undefined>(undefined);
  const originalSchemaStrRef = useRef<string>('');
  const hasShownSchemaWarningRef = useRef(false);

  const SCHEMA_MAX_WARN_LENGTH = 50000;

  const resetFormAndState = useCallback(() => {
    form.resetFields();
    setSelectedSchemaType(ToolSchemaType.OPEN_API3);
    form.setFieldValue('schemaType', ToolSchemaType.OPEN_API3);
    setImageName('');
    setFileList([]);
    setIsEditing(false);
    hasShownSchemaWarningRef.current = false;
  }, [form]);

  const handleSchemaChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const value = e.target.value;
    if (value.length > SCHEMA_MAX_WARN_LENGTH && !hasShownSchemaWarningRef.current) {
      hasShownSchemaWarningRef.current = true;
      Modal.confirm({
        title: 'Schema 文稿过长',
        content: (
          <span style={{ color: '#D97706' }}>
            Schema 文稿定义过长可能导致 Token 消耗过大或超出模型上下文限制。建议删除不必要的注释、描述，或使用 JSON 压缩。
          </span>
        ),
        okText: '知道了',
        cancelButtonProps: { style: { display: 'none' } },
      });
    } else if (value.length <= SCHEMA_MAX_WARN_LENGTH) {
      hasShownSchemaWarningRef.current = false;
    }
  };

  const getSchemaPlaceholder = (schemaType: ToolSchemaType) => {
    switch (schemaType) {
      case ToolSchemaType.OPEN_API3:
        return '请输入 OpenAPI3 文稿（支持 YAML / JSON）';
      case ToolSchemaType.JSON_RPC:
        return '请输入 OpenRPC 文稿（仅支持 JSON 格式）';
      case ToolSchemaType.OPEN_TOOL:
        return '请输入 OpenTool 文稿（第三方插件工具定义）';
      case ToolSchemaType.MCP:
        return '请输入 MCP 协议描述（如 {"name": "mcp server name", "baseUrl": "mcp server url", "sseEndpoint": "mcp server endpoint"}）';
      default:
        return '请输入 schema 文稿';
    }
  };

  const handleSubmit = useCallback(async () => {
    try {
      form.validateFields().then(async (values) => {

        if (values.name?.length > 20) {
          message.error('工具名称不能超过 20 个字符');
          return;
        }

        if (values.apiKeyType === NO_SELECT) {
          values.apiKeyType = undefined;
        }

        // 对于 OPEN_TOOL_SPEC 类型，确保 schemaStr 字段是最新的
        if (selectedSchemaType === ToolSchemaType.OPEN_TOOL_SPEC) {
          const openToolSpecData = {
            origin: values.origin || 'server',
            apiKey: values.apiKey || '',
            serverUrl: values.serverUrl || '',
            schema: values.schema || ''
          };
          values.schemaStr = JSON.stringify(openToolSpecData);
        }

        const code = await onOk(initialData?.id, { ...values, icon: imageName } as ToolVOAddAction | ToolVOUpdateAction);

        if (code === ResponseCode.S_OK) {
          form.resetFields();
          setImageName('');
          setPreviewImage('');
          setFileList([]);
        } else if (code === 1000) {
          form.setFields([
            {
              name: 'schemaStr',
              errors: ['Schema 文稿解析失败，请检查格式是否正确'],
            },
          ]);
        } else if (code === 20005) {
          form.setFields([
            {
              name: 'name',
              errors: ['工具名称已存在，请更换名称'],
            },
          ]);
        } else {
          message.error('操作失败，请稍后重试');
        }
      });
    } catch (error) {
      console.error('Validation failed:', error);
    }
  }, [form, onOk, initialData, imageName, selectedSchemaType]);

  const handleDelete = () => {
    if (initialData?.id && onDelete) {
      onDelete(initialData.id);
    }
  };

  const onCancelClick = useCallback(() => {
    resetFormAndState();
    onCancel();
  }, [resetFormAndState, onCancel]);

  const getBase64 = (file: FileType): Promise<string> =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = (error) => reject(error);
    });

  const handlePreview = async (file: UploadFile) => {
    if (!file.url && !file.preview) {
      file.preview = await getBase64(file.originFileObj as FileType);
    }

    setPreviewImage(file.url || (file.preview as string));
    setPreviewOpen(true);
  };

  const handleCustomRequest = async (options: any) => {
    await customUploadRequest({
      ...options,
      onSuccess: (data: any) => {
        options.onSuccess?.(data);
        
        const imageUrl = data;
        setImageName(imageUrl);
        
        setFileList([{
          uid: '-1',
          name: 'icon.jpg',
          status: 'done',
          url: imageUrl,
          thumbUrl: imageUrl,
          type: 'image/jpeg',
        }]);
      },
    });
  };

  const handleImageUpload = async (info: UploadChangeParam) => {
    if (info.file.status === 'done') {
      // info.file.response 就是 customUploadRequest 返回的完整图片 URL
      const imageUrl = info.file.response;
      setImageName(imageUrl);
      
      // 手动设置 fileList，使用 thumbUrl 避免额外请求
      setFileList([{
        uid: '-1',
        name: 'icon.jpg',
        status: 'done',
        url: imageUrl,
        thumbUrl: imageUrl,
        type: 'image/jpeg',
      }]);
    } else if (info.file.status === 'error') {
      setImageName('');
      setFileList([]);
    } else if (info.file.status === 'removed') {
      setImageName('');
      setFileList([]);
    } else {
      setFileList(info.fileList);
    }
  };

  useEffect(() => {
    if (initialData?.id) {
      const formData = { ...initialData };
      if (!formData.apiKeyType) {
        formData.apiKeyType = NO_SELECT;
      }
      form.setFieldsValue(formData);
      setIsEditing(true);
      setSelectedSchemaType(formData.schemaType);

      // 记录初始类型与原始文稿
      originalTypeRef.current = formData.schemaType;
      originalSchemaStrRef.current = formData.schemaStr || '';

      if (formData.schemaType === ToolSchemaType.OPEN_TOOL_SPEC) {
        const openToolSpecData = JSON.parse(formData.schemaStr);
        form.setFieldValue('origin', openToolSpecData.origin);
        form.setFieldValue('apiKey', openToolSpecData.apiKey);
        form.setFieldValue('serverUrl', openToolSpecData.serverUrl);
        form.setFieldValue('schema', openToolSpecData.schema);
      }

      const toolIcon = initialData?.icon;
      if (toolIcon) {
        const imgUrl = buildImageUrl(toolIcon);

        setFileList([
          {
            uid: '-1',
            name: 'icon.jpg',
            status: 'done',
            url: imgUrl,
            thumbUrl: imgUrl, // 使用 thumbUrl 避免 Upload 组件发送额外请求
            type: 'image/jpeg',
          },
        ]);

        setImageName(toolIcon);
      }
    } else {
      resetFormAndState();
      originalTypeRef.current = undefined;
      originalSchemaStrRef.current = '';
    }
  }, [initialData, form, resetFormAndState]);

  const uploadButton = (
    <div>
      <PlusOutlined />
      <div style={{ marginTop: 8 }}>上传图标</div>
    </div>
  );

  return (
    <Modal
      zIndex={100}
      centered
      title={<span className="text-[18px] font-medium text-[#1D4A6B]">{isEditing ? '编辑工具' : '新建工具'}</span>}
      open={visible}
      destroyOnClose
      onCancel={onCancelClick}
      onOk={handleSubmit}
      maskClosable={false}
      okText="确认"
      cancelText="取消"
      width={800}
      className="createToolModal max-w-[60vh] [&_.ant-modal-body]:max-h-[70vh] [&_.ant-modal-body]:overflow-auto"
      styles={{
        header: { padding: '16px 24px', marginBottom: 0, borderBottom: 'none' },
        body: { padding: '16px 24px' },
        footer: { padding: '10px 16px', marginTop: 0, borderTop: 'none' },
      }}
      okButtonProps={{ className: 'bg-[#40A5EE] rounded-xl h-10 px-6 border-[#40A5EE]' }}
      cancelButtonProps={{ className: 'rounded-xl h-10 px-6' }}
    >
      <Form
        form={form}
        layout="vertical"
        requiredMark={false}
        className="-mb-8 max-h-full overflow-y-auto [&_.ant-form-item]:mb-4"
      >
        <div style={{ display: 'flex', gap: '16px' }}>
          <Form.Item label={<span className="text-[14px] text-[#383F44] font-medium">图标</span>} name="icon" style={{ flex: '0 0 50px' }}>
            <Upload
              name="icon"
              maxCount={1}
              accept=".png,.jpg,.jpeg,.svg,.gif,.webp"
              listType="picture-card"
              className="avatar-uploader shadow-sm"
              showUploadList={true}
              customRequest={handleCustomRequest}
              beforeUpload={beforeUpload}
              onChange={handleImageUpload}
              isImageUrl={() => true}
              onPreview={handlePreview}
              fileList={fileList}
            >
              {fileList.length >= 1 ? null : uploadButton}
            </Upload>
          </Form.Item>

          <Form.Item
            name="name"
            label={<span className="text-[14px] text-[#383F44] font-medium">工具名称</span>}
            rules={[{ required: true, message: '请输入工具名称', whitespace: true }]}
            style={{ flex: 1 }}
          >
            <Input className="h-10 rounded-lg shadow-sm" maxLength={50} placeholder="请输入工具名称" />
          </Form.Item>
        </div>

        {previewImage && (
          <Image
            wrapperStyle={{ display: 'none' }}
            preview={{
              visible: previewOpen,
              onVisibleChange: (visible) => setPreviewOpen(visible),
              afterOpenChange: (visible) => !visible && setPreviewImage(''),
            }}
            src={previewImage}
          />
        )}

        <Form.Item name="description" label={<span className="text-[14px] text-[#383F44] font-medium">描述</span>}>
          <TextArea className="rounded-lg shadow-sm" rows={3} maxLength={200} placeholder="用简单几句话将工具介绍给用户" />
        </Form.Item>

        <div className="bg-[#F2F3F5] rounded-xl p-4 mb-4">
          <h4 className="text-[14px] font-bold text-[#383F44] mb-4">Schema</h4>
          <div className="flex gap-4">
            <Form.Item
              label={<span className="text-[12px] text-[#7C8B98]">类型</span>}
              name="schemaType"
              style={{ flex: '0 0 200px' }}
              rules={[{ required: true, message: '请选择协议类型' }]}
            >
              <Select
                className="rounded-lg"
                placeholder="这里显示协议类型"
                onChange={(value) => {
                  setSelectedSchemaType(value);
                  hasShownSchemaWarningRef.current = false;
                  form.setFields([
                    {
                      name: 'schemaStr',
                      errors: [],
                    },
                  ]);

                  // 如果切回到初始类型，恢复为原始文稿
                  if (value === originalTypeRef.current) {
                    const originalStr = originalSchemaStrRef.current || '';
                    form.setFieldValue('schemaStr', originalStr);
                    if (value === ToolSchemaType.OPEN_TOOL_SPEC && originalStr) {
                      try {
                        const openToolSpecData = JSON.parse(originalStr);
                        form.setFieldValue('origin', openToolSpecData.origin);
                        form.setFieldValue('apiKey', openToolSpecData.apiKey);
                        form.setFieldValue('serverUrl', openToolSpecData.serverUrl);
                        form.setFieldValue('schema', openToolSpecData.schema);
                      } catch {
                        // ignore JSON parse error and keep fields as-is
                      }
                    }
                    return;
                  }

                  // 当切换到 OPEN_TOOL_SPEC 类型时，设置默认的 schemaStr
                  if (value === ToolSchemaType.OPEN_TOOL_SPEC) {
                    const defaultData = {
                      origin: 'server',
                      apiKey: '',
                      serverUrl: '',
                      schema: ''
                    };
                    form.setFieldValue('schemaStr', JSON.stringify(defaultData));
                  } else {
                    // 切换到其他类型时，清空 schemaStr
                    form.setFieldValue('schemaStr', '');
                  }
                }}
              >
                <Select.Option value={ToolSchemaType.OPEN_API3}>OpenAPI3(YAML/JSON)</Select.Option>
                <Select.Option value={ToolSchemaType.JSON_RPC}>OpenRPC(JSON)</Select.Option>
                <Select.Option value={ToolSchemaType.OPEN_TOOL}>第三方OpenTool</Select.Option>
                <Select.Option value={ToolSchemaType.MCP}>MCP(HTTP)</Select.Option>
                <Select.Option value={ToolSchemaType.OPEN_TOOL_SPEC}>OpenTool Spec</Select.Option>
              </Select>
            </Form.Item>
            {selectedSchemaType === ToolSchemaType.OPEN_TOOL_SPEC ? (
              <div style={{ flex: 1 }}>
                <OpenToolSpecForm form={form} onSchemaChange={handleSchemaChange} />
              </div>
            ) : (
              <Form.Item
                label={<span className="text-[12px] text-[#7C8B98]">文稿</span>}
                name="schemaStr"
                style={{ flex: 1 }}
                rules={[{ required: true, message: getSchemaPlaceholder(selectedSchemaType!), whitespace: true }]}
              >
                <TextArea 
                  className="rounded-lg font-mono text-[12px]"
                  rows={8}
                  placeholder={getSchemaPlaceholder(selectedSchemaType!)} 
                  onChange={handleSchemaChange}
                />
              </Form.Item>
            )}
          </div>
        </div>

        {requiresAuth.includes(selectedSchemaType!) && (
          <div className="bg-[#F2F3F5] rounded-xl p-4 mb-4">
            <h4 className="text-[14px] font-bold text-[#383F44] mb-4">API Key</h4>
            <div className="flex gap-4">
              <Form.Item
                label={<span className="text-[12px] text-[#7C8B98]">认证类型</span>}
                name="apiKeyType"
                style={{ flex: '0 0 200px' }}
              >
                <Select className="rounded-lg" placeholder="这里显示Key类型" defaultValue={NO_SELECT}>
                  <Select.Option value={NO_SELECT}>暂不选择</Select.Option>
                  <Select.Option value="Basic">Basic</Select.Option>
                  <Select.Option value="Bearer">Bearer</Select.Option>
                </Select>
              </Form.Item>
              <Form.Item label={<span className="text-[12px] text-[#7C8B98]">Key值</span>} name="apiKey" style={{ flex: 1 }}>
                <Input className="h-10 rounded-lg" maxLength={150} placeholder="请输入API Key" />
              </Form.Item>
            </div>
          </div>
        )}

        <div className="flex items-center py-4 mb-4">
          <span className="text-[#383f44] text-[14px] leading-[22px] font-bold mr-4">支持Auto Agent使用</span>
          <Form.Item
            name="autoAgent"
            valuePropName="checked"
            noStyle
          >
            <Switch size='small' />
          </Form.Item>
        </div>
      </Form>
      {isEditing && onDelete && initialData?.canDelete && (
        <Popconfirm
          title="确认删除"
          icon={<ExclamationCircleOutlined style={{ color: 'red' }} />}
          description="即将删除工具的所有信息，确认删除？"
          onConfirm={handleDelete}
          okText="确认"
          cancelText="取消"
          okButtonProps={{ danger: true, className: 'bg-[#CC2D3A] border-[#CC2D3A]' }}
        >
          <Button danger className="bottom-[20px] left-[24px] absolute rounded-xl h-10 px-6">
            删除
          </Button>
        </Popconfirm>
      )}
      {isEditing && showExportModal && initialData?.canEdit && (
        <Button
          className={`bottom-[20px] absolute rounded-xl h-10 px-6 border-[#E0E3E6] text-[#383F44] ${isEditing && onDelete && initialData?.canDelete ? 'left-[120px]' : 'left-[24px]'}`}
          onClick={(event) => showExportModal(event, initialData)}
          key={`export-${initialData?.id}`}
        >
          导出
        </Button>
      )}
    </Modal>
  );
};

export default CreateToolModal;
