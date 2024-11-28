import React, {useCallback, useEffect} from 'react';
import {Modal, Form, Input, Switch, Button, Popconfirm} from 'antd';
import {isValidHttpUrl} from "@/utils/validUrl";
import {ModelVOAddAction} from "@/client";

interface ModelFormModalProps {
  visible: boolean;
  onCancel: () => void;
  onOk: (values: ModelVOAddAction) => void;
  onDelete?: (id: string) => void;
  initialData?: ModelVOAddAction;
}

const ModelFormModal: React.FC<ModelFormModalProps> = (props) => {
  const {visible, onCancel, onOk, onDelete, initialData} = props;
  const [form] = Form.useForm();
  const isEditing = !!initialData;

  useEffect(() => {
    if (initialData) {
      form.setFieldsValue(initialData);
    } else {
      form.resetFields();
    }
  }, [initialData, form]);

  useEffect(() => {
    if (!visible) {
      form.resetFields();
    }
  }, [visible, form])

  const handleSubmit = useCallback(async () => {
    form.validateFields().then((values) => {
      onOk({...values, id: initialData?.id});
    })
  }, [form, onOk, initialData])

  const handleDelete = () => {
    if (initialData?.id && onDelete) {
      onDelete(initialData.id);
    }
  };

  return (
    <Modal
      centered
      title={isEditing ? "编辑模型" : "新建模型"}
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText="确定"
      cancelText="取消"
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="模型名称"
          rules={[{required: true, message: '请输入模型名称', whitespace: true,}]}
        >
          <Input maxLength={20} placeholder="请输入模型名称"/>
        </Form.Item>

        <Form.Item
          name="baseUrl"
          label="BaseURL"
          rules={[{
            required: true,
            message: '请输入URL',
            whitespace: true,
          }, () => ({
            validator(_, value) {
              if (!value?.trim()) {
                return Promise.resolve();
              }
              if (isValidHttpUrl(value)) {
                return Promise.resolve();
              }
              return Promise.reject(new Error('请输入正确的URL'));
            },
          }),]}
        >
          <Input maxLength={500} placeholder="请输入URL"/>
        </Form.Item>

        <Form.Item
          name="apiKey"
          label="API Key"
          rules={[{required: true, message: '请输入API Key', whitespace: true}]}
        >
          <Input maxLength={60} placeholder="请输入key值"/>
        </Form.Item>

        <Form.Item
          name="maxTokens"
          label="maxToken最大值"
          rules={[{
            pattern: /^[1-9]\d*$/,
            message: '请输入大于0的正整数',
          }]}
        >
          <Input min={1} maxLength={9} type="number" placeholder="请输入maxToken最大值"/>
        </Form.Item>

        <Form.Item name="shareFlag" label="分享设置" valuePropName="checked" extra={
          <span className="ml-2">未开启，开启后其他成员可以查看并使用此模型</span>
        }>
          <Switch checkedChildren="开启" unCheckedChildren="关闭"/>
        </Form.Item>
      </Form>
      {isEditing && onDelete && (
        <Popconfirm
          title="确认删除"
          description="即将删除模型的所有信息，确认删除？"
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

export default ModelFormModal;
