import React from 'react';
import { Modal, Form, Input, Button } from 'antd';

interface ExternalKnowledgeBaseModalProps {
  visible: boolean;
  onCancel: () => void;
}

const ExternalKnowledgeBaseModal: React.FC<ExternalKnowledgeBaseModalProps> = ({ visible, onCancel }) => {
  const [form] = Form.useForm();

  const handleOk = () => {
    form.validateFields()
      .then(values => {
        console.log('Form values:', values);
        // 在这里处理表单提交逻辑
        onCancel(); // 提交后关闭模态框
      })
      .catch(info => {
        console.log('Validate Failed:', info);
      });
  };

  return (
    <Modal
      centered
      title="新建外部知识库"
      open={visible}
      onCancel={onCancel}
      footer={[
        <Button key="back" onClick={onCancel}>
          取消
        </Button>,
        <Button key="submit" type="primary" onClick={handleOk}>
          确定
        </Button>,
      ]}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="知识库名称"
          rules={[{ required: true, message: '请输入知识库名称' }]}
        >
          <Input placeholder="请输入知识库名称" />
        </Form.Item>
        <Form.Item
          name="apiKey"
          label="知识库API KEY"
          rules={[{ required: true, message: '请输入知识库的API KEY' }]}
        >
          <Input placeholder="请输入知识库的API KEY" />
        </Form.Item>
        <Form.Item
          name="url"
          label="知识库URL"
          rules={[{ required: true, message: '请输入知识库的URL' }]}
        >
          <Input placeholder="请输入知识库的URL" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ExternalKnowledgeBaseModal;