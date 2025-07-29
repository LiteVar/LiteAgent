import React, { useState, useEffect, useCallback } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Button,
  Popconfirm,
  Upload,
  Image,
  message,
  Switch
} from 'antd';
import { ExclamationCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { ToolSchemaType } from '@/types/Tool';
import { UploadChangeParam } from 'antd/es/upload/interface';
import { buildImageUrl } from '@/utils/buildImageUrl';

import type { GetProp, UploadFile, UploadProps } from 'antd';
import { beforeUpload, onUploadAction } from '@/utils/uploadFile';
import ResponseCode from '@/constants/ResponseCode';

type FileType = Parameters<GetProp<UploadProps, 'beforeUpload'>>[0];

const { TextArea } = Input;
const NO_SELECT = 'no-select';
const requiresAuth = [ToolSchemaType.OPEN_API3, ToolSchemaType.JSON_RPC];

interface ToolModalProps {
  visible: boolean;
  onCancel: () => void;
  onOk: (id: string, values: any) => Promise<number>;
  onDelete?: (id: string) => void;
  initialData?: any;
}

const CreateToolModal: React.FC<ToolModalProps> = (props) => {
  const { visible, onCancel, onOk, onDelete, initialData } = props;
  const [form] = Form.useForm();
  const [isEditing, setIsEditing] = useState(false);

  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewImage, setPreviewImage] = useState('');
  const [imageName, setImageName] = useState<string>('');
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [selectedSchemaType, setSelectedSchemaType] = useState<ToolSchemaType | undefined>(undefined);

  const resetFormAndState = useCallback(() => {
    form.resetFields();
    setSelectedSchemaType(ToolSchemaType.OPEN_API3);
    form.setFieldValue('schemaType', ToolSchemaType.OPEN_API3);
    setImageName('');
    setFileList([]);
    setIsEditing(false);
  }, [form]);

  const getSchemaPlaceholder = (schemaType: ToolSchemaType) => {
    switch (schemaType) {
      case ToolSchemaType.OPEN_API3:
        return '请输入 OpenAPI3 文稿（支持 YAML / JSON）';
      case ToolSchemaType.JSON_RPC:
        return '请输入 OpenRPC 文稿（仅支持 JSON 格式）';
      case ToolSchemaType.OPEN_MODBUS:
        return '请输入 OpenModbus JSON 格式文稿（如寄存器配置等）';
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
        if (values.apiKeyType === NO_SELECT) {
          values.apiKeyType = undefined;
        }

        const code = await onOk(initialData?.id, { ...values, icon: imageName });

        if (code === ResponseCode.S_OK) {
          form.resetFields();
          setImageName('');
          setPreviewImage('');
          setFileList([]);
        } else if (code === 1000) {
          form.setFields([
            {
              name: 'schemaStr',
              errors: ['Schema 文稿解析失败，请检查格式是否正确']
            }
          ]);
        } else if (code === 20005) {
          form.setFields([
            {
              name: 'name',
              errors: ['工具名称已存在，请更换名称']
            }
          ]);

        }else {
          message.error('操作失败，请稍后重试');
        }
      });
    } catch (error) {
      console.error('Validation failed:', error);
    }
  }, [form, onOk, initialData, imageName]);

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

  const handleImageUpload = async (info: UploadChangeParam) => {
    setFileList(info.fileList);

    if (info.file.status === 'done') {
      setImageName(info.file.xhr.responseURL.split('=')[1]);
      await message.success(`${info.file.name} 上传成功`);
    } else if (info.file.status === 'error') {
      setImageName('');
      setFileList([]);
      message.error(`${info.file.name} 上传失败`);
    } else if (info.file.status === 'removed') {
      setImageName('');
      setFileList([]);
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

      const toolIcon = initialData?.icon;
      if (toolIcon) {
        const imgUrl = buildImageUrl(toolIcon);

        setFileList([
          {
            uid: '-1',
            name: toolIcon,
            status: 'done',
            url: imgUrl,
          },
        ]);

        setImageName(toolIcon);
      }
    } else {
      resetFormAndState();
    }
  }, [initialData, form]);

  const uploadButton = (
    <div>
      <PlusOutlined />
      <div style={{ marginTop: 8 }}>上传图标</div>
    </div>
  );

  return (
    <Modal
      centered
      title={isEditing ? '编辑工具' : '新建工具'}
      open={visible}
      onCancel={onCancelClick}
      onOk={handleSubmit}
      maskClosable={false}
      okText="确定"
      cancelText="取消"
      width={800}
    >
      <Form form={form} layout="vertical" className="-mb-8 max-h-full overflow-y-auto [&_.ant-form-item]:mb-4">
        <div style={{ display: 'flex', gap: '16px' }}>
          <Form.Item label="图标" name="icon" style={{ flex: '0 0 50px' }}>
            <Upload
              name="icon"
              maxCount={1}
              accept=".png,.jpg,.jpeg,.svg,.gif,.webp"
              listType="picture-card"
              className="avatar-uploader"
              showUploadList={true}
              action={onUploadAction}
              beforeUpload={beforeUpload}
              onChange={handleImageUpload}
              onPreview={handlePreview}
              fileList={fileList}
            >
              {fileList.length >= 1 ? null : uploadButton}
            </Upload>
          </Form.Item>

          <Form.Item
            name="name"
            label="工具名称"
            rules={[{ required: true, message: '请输入工具名称', whitespace: true }]}
            style={{ flex: 1 }}
          >
            <Input maxLength={20} placeholder="请输入工具名称" />
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

        <Form.Item
            name="description"
            label="描述"
          >
            <TextArea rows={3} maxLength={200} placeholder="用简单几句话将工具介绍给用户" />
          </Form.Item>

        <Form.Item label={<span className="font-bold">Schema</span>}>
          <Form.Item
            label="类型"
            name="schemaType"
            style={{ display: 'inline-block', width: 'calc(30% - 8px)', marginRight: '8px' }}
            rules={[{ required: true, message: '请选择协议类型' }]}
          >
            <Select placeholder="这里显示协议类型" 
                onChange={(value) => {
                  setSelectedSchemaType(value);
                  form.setFields([
                    {
                      name: 'schemaStr',
                      errors: []
                    }
                  ]);
                }}>
              <Select.Option value={ToolSchemaType.OPEN_API3}>OpenAPI3(YAML/JSON)</Select.Option>
              <Select.Option value={ToolSchemaType.JSON_RPC}>OpenRPC(JSON)</Select.Option>
              <Select.Option value={ToolSchemaType.OPEN_MODBUS}>OpenModbus(JSON)</Select.Option>
              <Select.Option value={ToolSchemaType.OPEN_TOOL}>第三方open tool</Select.Option>
              <Select.Option value={ToolSchemaType.MCP}>MCP(SSE)</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            label="文稿"
            name="schemaStr"
            style={{ display: 'inline-block', width: 'calc(70%)' }}
            rules={[{ required: true, message: getSchemaPlaceholder(selectedSchemaType!), whitespace: true }]}
          >
            <TextArea rows={8} maxLength={20000} placeholder={getSchemaPlaceholder(selectedSchemaType!)} />
          </Form.Item>
        </Form.Item>

        {requiresAuth.includes(selectedSchemaType!) && (
          <Form.Item label={<span className="font-bold">API Key</span>}>
            <Form.Item
              label="认证类型"
              name="apiKeyType"
              style={{ display: 'inline-block', width: 'calc(30% - 8px)', marginRight: '8px' }}
            >
              <Select placeholder="这里显示Key类型" defaultValue={NO_SELECT}>
                <Select.Option value={NO_SELECT}>暂不选择</Select.Option>
                <Select.Option value="Basic">Basic</Select.Option>
                <Select.Option value="Bearer">Bearer</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item label="Key值" name="apiKey" style={{ display: 'inline-block', width: 'calc(70%)' }}>
              <Input maxLength={150} placeholder="请输入API Key" />
            </Form.Item>
          </Form.Item>
        )}

        <Form.Item name="autoAgent" label="是否支持Auto Multi Agent使用" valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
      {isEditing && onDelete && initialData?.canDelete && (
        <Popconfirm
          title="确认删除"
          icon={<ExclamationCircleOutlined style={{ color: 'red' }} />}
          description="即将删除工具的所有信息，确认删除？"
          onConfirm={handleDelete}
          okText="确认"
          cancelText="取消"
        >
          <Button danger className='bottom-[20px] float-left absolute'>
            删除
          </Button>
        </Popconfirm>
      )}
    </Modal>
  );
};

export default CreateToolModal;
