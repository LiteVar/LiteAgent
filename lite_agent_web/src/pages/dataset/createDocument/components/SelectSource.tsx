import React from 'react';
import { Radio, Input, Upload, Button, Form, Space, Card, Typography, message } from 'antd';
import { UploadOutlined, LinkOutlined, EditOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';
import { DocumentSourceType } from '@/types/dataset';
const { Text } = Typography;

interface SelectSourceProps {
  documentData: any;
  setDocumentData: (data: any) => void;
  onNext: () => void;
}

const SelectSource: React.FC<SelectSourceProps> = ({ documentData, setDocumentData, onNext }) => {
  const [form] = Form.useForm();

  const handleSourceChange = (dataSourceType: DocumentSourceType) => {
    form.setFieldValue('dataSourceType', dataSourceType);
    setDocumentData({
      ...documentData,
      dataSourceType,
    });
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    setDocumentData({
      ...documentData,
      ...values,
    });
    onNext();
  };

  const uploadProps: UploadProps = {
    accept: '.doc,.docx,.ppt,.pptx,.pdf,.txt,.md',
    maxCount: 10,
    beforeUpload: (file) => {
      const isLt15M = file.size / 1024 / 1024 < 15;
      if (!isLt15M) {
        message.error('文件大小不能超过15MB!');
      }
      return false;
    },
  };

  return (
    <Form form={form} layout="vertical" initialValues={documentData} className="py-6">
      <Form.Item name="dataSourceType" label="选择数据源" rules={[{ required: true, message: '请选择数据源' }]}>
        <Radio.Group className="w-full">
          <Space direction="horizontal" className="w-full" size="middle">
            <Card
              hoverable
              className={`w-full cursor-pointer ${documentData.dataSourceType === DocumentSourceType.INPUT ? 'border-primary' : ''}`}
              onClick={() => handleSourceChange(DocumentSourceType.INPUT)}
            >
              <Radio value={DocumentSourceType.INPUT}>
                <Space>
                  <EditOutlined />
                  <span className="font-medium">导入已有文本</span>
                </Space>
              </Radio>
            </Card>

            {/* 接口还不支持上传文件 */}
            {/* <Card
              hoverable
              className={`w-full cursor-pointer ${documentData.dataSourceType === DocumentSourceType.FILE ? 'border-primary' : ''}`}
              onClick={() => handleSourceChange(DocumentSourceType.FILE)}
            >
              <Radio value={DocumentSourceType.FILE}>
                <Space>
                  <FileTextOutlined />
                  <span className="font-medium">上传文件</span>
                </Space>
              </Radio>
            </Card> */}

            <Card
              hoverable
              className={`w-full cursor-pointer ${documentData.dataSourceType === DocumentSourceType.HTML ? 'border-primary' : ''}`}
              onClick={() => handleSourceChange(DocumentSourceType.HTML)}
            >
              <Radio value={DocumentSourceType.HTML}>
                <Space>
                  <LinkOutlined />
                  <span className="font-medium">同步自 Web 站点</span>
                </Space>
              </Radio>
            </Card>
          </Space>
        </Radio.Group>
      </Form.Item>

      {documentData.dataSourceType === DocumentSourceType.INPUT && (
        <>
          <Form.Item name="name" label="文档名称" rules={[{ required: true, message: '请输入文档名称' }]}>
            <Input placeholder="请输入文档名称" maxLength={60} />
          </Form.Item>
          <Form.Item name="content" label="文档内容" rules={[{ required: true, message: '请输入文档内容' }]}>
            <Input.TextArea rows={8} placeholder="请输入文档内容" />
          </Form.Item>
        </>
      )}

      {documentData.dataSourceType === DocumentSourceType.FILE && (
        <Form.Item
          name="file"
          label="上传文件"
          rules={[{ required: true, message: '请上传文件' }]}
          extra={
            <Text type="secondary">支持格式：doc/docx、ppt、pptx、pdf、txt、md，单个文件大小不超过15MB</Text>
          }
        >
          <Upload.Dragger {...uploadProps}>
            <p className="ant-upload-drag-icon">
              <UploadOutlined />
            </p>
            <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
          </Upload.Dragger>
        </Form.Item>
      )}

      {documentData.dataSourceType === DocumentSourceType.HTML && (
        <Form.Item
          name="htmlUrl"
          label="网页链接"
          rules={[
            { required: true, message: '请输入网页链接' },
            {
              validator: (_, value) => {
                if (!value) return Promise.resolve();
                const links = value.split('\n').filter((link: string) => link.trim());
                if (links.length > 10) {
                  return Promise.reject('最多支持10个链接');
                }
                const validLinks = links.every((link: string) => {
                  try {
                    new URL(link.trim());
                    return true;
                  } catch {
                    return false;
                  }
                });
                if (!validLinks) {
                  return Promise.reject('请确保每行都是的URL地址格式');
                }
                return Promise.resolve();
              }
            }
          ]}
          extra={
            <Text type="secondary">
              仅支持静态链接，每行一个链接，至多支持10个链接。如果上传数据为空，可能是该链接无法被读取
            </Text>
          }
        >
          <Input.TextArea rows={4} placeholder="请输入网页链接，每行一个" />
        </Form.Item>
      )}

      <div className="mt-8 text-right">
        <Button type="primary" size="large" onClick={handleSubmit}>
          下一步
        </Button>
      </div>
    </Form>
  );
};

export default SelectSource;
