import React, {useState, useEffect, useCallback} from 'react';
import {Modal, Form, Input, Select, Switch, Button, Popconfirm} from 'antd';
import {ExclamationCircleOutlined} from '@ant-design/icons';
import {ToolSchemaType} from "@/types/Tool";

const {TextArea} = Input;

interface ToolModalProps {
  visible: boolean;
  onCancel: () => void;
  onOk: (id: string, values: any) => Promise<boolean>;
  onDelete?: (id: string) => void;
  initialData?: any;
}

const CreateToolModal: React.FC<ToolModalProps> = (props) => {
  const {visible, onCancel, onOk, onDelete, initialData} = props;
  const [form] = Form.useForm();
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    if (initialData?.id) {
      form.setFieldsValue(initialData);
      setIsEditing(true);
    } else {
      form.resetFields();
      setIsEditing(false);
    }
  }, [initialData, form]);

  const handleSubmit = useCallback(async () => {
    try {
      form.validateFields().then(async (values) => {
        const isOk = await onOk(initialData?.id, values);
        isOk && form.resetFields();
      });
    } catch (error) {
      console.error('Validation failed:', error);
    }
  }, [form, onOk, initialData]);

  const handleDelete = () => {
    if (initialData?.id && onDelete) {
      onDelete(initialData.id);
    }
  };

  return (
    <Modal
      centered
      title={isEditing ? "编辑工具" : "新建工具"}
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText="确定"
      cancelText="取消"
      width={600}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="工具名称"
          rules={[{required: true, message: '请输入工具名称', whitespace: true}]}
        >
          <Input maxLength={20} placeholder="请输入工具名称"/>
        </Form.Item>

        <Form.Item
          name="description"
          label="描述"
        >
          <TextArea maxLength={300} rows={4} placeholder="用简单几句话将工具介绍给用户"/>
        </Form.Item>

        <Form.Item label="Schema">
          <Form.Item
            label="类型"
            name="schemaType"
            rules={[{required: true, message: '请选择类型'}]}
            style={{display: 'inline-block', width: 'calc(30% - 8px)', marginRight: '8px'}}
          >
            <Select placeholder="这里显示协议类型">
              <Select.Option value={ToolSchemaType.OPEN_API3}>OpenAPI3(YAML/JSON)</Select.Option>
              <Select.Option value={ToolSchemaType.JSON_RPC}>OpenRPC(JSON)</Select.Option>
              <Select.Option value={ToolSchemaType.OPEN_MODBUS}>OpenModbus(JSON)</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            label="文稿"
            name="schemaStr"
            rules={[{required: true, message: '请输入schema文稿'}]}
            style={{display: 'inline-block', width: 'calc(70%)'}}
          >
            <TextArea rows={4} maxLength={20000} placeholder="请输入schema文稿"/>
          </Form.Item>
        </Form.Item>

        <Form.Item label="API Key">
          <Form.Item
            label="认证类型"
            name="apiKeyType"
            style={{display: 'inline-block', width: 'calc(30% - 8px)', marginRight: '8px'}}
          >
            <Select placeholder="这里显示Key类型">
              <Select.Option value="Basic">Basic</Select.Option>
              <Select.Option value="Bearer">Bearer</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            label="Key值"
            name="apiKey"
            style={{display: 'inline-block', width: 'calc(70%)'}}
          >
            <Input maxLength={150} placeholder="请输入API Key"/>
          </Form.Item>
        </Form.Item>

        <Form.Item name="shareFlag" label="分享设置" valuePropName="checked"
                   extra={<span className="ml-2">开启后其他成员可以查看并使用此工具</span>}>
          <Switch checkedChildren="开启" unCheckedChildren="关闭"/>
        </Form.Item>
      </Form>
      {(isEditing && onDelete && initialData?.canDelete) && (
        <Popconfirm
          title="确认删除"
          icon={<ExclamationCircleOutlined style={{color: 'red'}}/>}
          description="即将删除工具的所有信息，确认删除？"
          onConfirm={handleDelete}
          okText="确认"
          cancelText="取消"
        >
          <Button danger style={{float: 'left'}}>删除</Button>
        </Popconfirm>
      )}
    </Modal>
  );
};

export default CreateToolModal;
