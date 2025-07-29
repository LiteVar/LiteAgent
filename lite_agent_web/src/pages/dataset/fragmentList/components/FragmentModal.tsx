import React, { useEffect } from 'react';
import { Modal, Form, Input, Button, message } from 'antd';
import { DocumentSegment } from '@/client';
import { marked } from 'marked';
import { linkRenderer } from '@/utils/markdownRenderer';
const { TextArea } = Input;

interface FragmentModalProps {
  open: boolean;
  onCancel: () => void;
  onSubmit: (values: { content: string; metadata: string }) => Promise<void>;
  loading: boolean;
  fragmentIndex?: string | null;
  initialValues?: DocumentSegment;
  mode: 'create' | 'edit' | 'read';
}

const renderer = new marked.Renderer();
renderer.link = linkRenderer;

marked.setOptions({
  gfm: true,
  renderer: renderer,
});

const FragmentModal: React.FC<FragmentModalProps> = ({
  open,
  onCancel,
  onSubmit,
  loading,
  fragmentIndex,
  initialValues,
  mode,
}) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (open) {
      if ((mode === 'edit' || mode === 'read') && initialValues) {
        form.setFieldsValue(initialValues);
      } else if (mode === 'create') {
        form.setFieldsValue({ content: '', metadata: '' });
      } else {
        form.resetFields();
      }
    } else {
      form.resetFields();
    }
  }, [open, initialValues, form, mode]);

  const handleSubmit = async (values: { content: string; metadata: string }) => {
    try {
      await onSubmit(values);
      form.resetFields();
    } catch (error) {
      console.error(error);
    }
  };

  return (
    <Modal
      centered
      title={mode === 'create' ? '新建片段' : mode === 'edit' ? '编辑片段' : `片段_${fragmentIndex}预览`}
      open={open}
      onCancel={onCancel}
      footer={null}
      maskClosable={false}
      destroyOnClose
      width={mode === 'read' ? 800 : 600}
    >
      {mode === 'read' ? (
        <div className="max-h-[60vh] overflow-y-auto">
          <div
            className={`prose markdown w-full overflow-hidden `}
            dangerouslySetInnerHTML={{
              __html: marked.parse(initialValues?.content || ''),
            }}
          ></div>
        </div>
      ) : (
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          preserve={false}
          initialValues={initialValues}
        >
          <Form.Item name="content" label="片段内容" rules={[{ required: true, message: '请输入片段内容' }]}>
            <TextArea rows={6} placeholder="输入片段内容" showCount />
          </Form.Item>

          <Form.Item name="metadata" label="元数据 (JSON格式)">
            <TextArea rows={4} placeholder="可选，非必填（json格式）" />
          </Form.Item>

          <Form.Item className=" mb-0">
            <div className="flex justify-between">
              <Button
                onClick={() => {
                  const content = form.getFieldValue('content');
                  Modal.info({
                    title: '预览',
                    width: 800,
                    content: (
                      <div
                        className={`prose markdown w-full max-h-[60vh] overflow-y-auto`}
                        dangerouslySetInnerHTML={{
                          __html: marked.parse(content || ''),
                        }}
                      ></div>
                    ),
                  });
                }}
              >
                预览
              </Button>

              <div>
                <Button onClick={onCancel} className="mr-2">
                  取消
                </Button>
                <Button type="primary" htmlType="submit" loading={loading}>
                  {mode === 'create' ? '创建' : '保存'}
                </Button>
              </div>
            </div>
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
};

export default FragmentModal;
