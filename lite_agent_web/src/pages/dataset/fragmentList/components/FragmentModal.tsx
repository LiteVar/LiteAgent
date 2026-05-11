import React, { useEffect } from 'react';
import { Modal, Form, Input, Button } from 'antd';
import { DocumentSegment } from '@/client';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
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
      title={
        <span className="text-[#1D4A6B] font-semibold">
          {mode === 'create' ? '新建片段' : mode === 'edit' ? '编辑片段' : `片段 ${fragmentIndex} 预览`}
        </span>
      }
      open={open}
      onCancel={onCancel}
      footer={null}
      maskClosable={false}
      destroyOnClose
      width={mode === 'read' ? 800 : 600}
      className="customModal"
    >
      <div className="pt-4">
        {mode === 'read' ? (
          <div className="max-h-[60vh] overflow-y-auto custom-scrollbar">
            <div
              className={`prose fragmentMarkdown overflow-hidden break-all whitespace-pre-wrap bg-gray-50/50 p-6 rounded-2xl border border-black/5`}
              dangerouslySetInnerHTML={{
                __html: DOMPurify.sanitize(marked.parse(initialValues?.content || '') as string),
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
            className="customForm"
          >
            <Form.Item name="content" label={<span className="text-gray-600 font-medium">片段内容</span>} rules={[{ required: true, message: '请输入片段内容' }]}>
              <TextArea 
                rows={8} 
                placeholder="输入片段内容..." 
                showCount 
                className="!rounded-xl !border-[#E0E3E6] !bg-white/80 focus:!bg-white transition-all !p-1"
              />
            </Form.Item>

            <Form.Item name="metadata" label={<span className="text-gray-600 font-medium">元数据 (JSON 格式)</span>}>
              <TextArea 
                rows={4} 
                placeholder='可选，支持 JSON 格式（例如: { "source": "manual" }）' 
                className="!rounded-xl !border-[#E0E3E6] !bg-white/80 focus:!bg-white transition-all !p-3"
              />
            </Form.Item>

            <div className="flex justify-between items-center mt-8 pt-6 border-t border-black/5">
              <Button
                className="rounded-xl border-[#E0E3E6] text-gray-600 hover:!text-blue-500 hover:!border-blue-500"
                onClick={() => {
                  const content = form.getFieldValue('content');
                  Modal.info({
                    title: '实时预览',
                    width: 800,
                    centered: true,
                    okText: '关闭',
                    okButtonProps: { className: 'rounded-lg' },
                    content: (
                      <div className="pt-4 pr-2 max-h-[60vh] overflow-y-auto custom-scrollbar">
                        <div
                          className="prose markdown bg-gray-50 p-6 rounded-xl border border-black/5"
                          dangerouslySetInnerHTML={{
                            __html: DOMPurify.sanitize(marked.parse(content || '') as string),
                          }}
                        ></div>
                      </div>
                    ),
                  });
                }}
              >
                实时预览
              </Button>

              <div className="flex gap-3">
                <Button onClick={onCancel} className="rounded-xl border-[#E0E3E6] text-gray-600 hover:!text-blue-500 hover:!border-blue-500 px-6">
                  取消
                </Button>
                <Button 
                  type="primary" 
                  htmlType="submit" 
                  loading={loading}
                  className="rounded-xl bg-[#40A5EE] hover:!bg-[#40A5EE]/90 border-none shadow-md shadow-blue-200/50 px-8"
                >
                  {mode === 'create' ? '立即创建' : '保存修改'}
                </Button>
              </div>
            </div>
          </Form>
        )}
      </div>
    </Modal>
  );
};

export default FragmentModal;
