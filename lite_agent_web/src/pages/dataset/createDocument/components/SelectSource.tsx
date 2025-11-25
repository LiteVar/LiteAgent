import React, { useState, useCallback } from 'react';
import { Radio, Input, Upload, Button, Form, Space, Card, Typography, message } from 'antd';
import { UploadOutlined, LinkOutlined, EditOutlined, FileTextOutlined } from '@ant-design/icons';
import { deleteV1FileById } from '@/client';
import type { UploadProps, UploadFile } from 'antd';
import { DocumentSourceType } from '@/types/dataset';
const { Text } = Typography;

interface SelectSourceProps {
  documentData: any;
  setDocumentData: (data: any) => void;
  onNext: () => void;
  onFileUpload?: (file: File) => Promise<{ fileId: string; fileName: string }>;
}

const SelectSource: React.FC<SelectSourceProps> = ({ documentData, setDocumentData, onNext, onFileUpload }) => {
  const [form] = Form.useForm();

  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [uploading, setUploading] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const handleSourceChange = useCallback((dataSourceType: DocumentSourceType) => {
    form.setFieldValue('dataSourceType', dataSourceType);
    
    setDocumentData({
      ...documentData,
      dataSourceType,
    });
  }, [documentData, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      
      let fileData = {};
      
      // 如果是文件类型且有选择的文件，先上传文件
      if (documentData.dataSourceType === DocumentSourceType.FILE && selectedFile && onFileUpload) {
        setUploading(true);
        try {
          const uploadResult = await onFileUpload(selectedFile);
          fileData = uploadResult;
          setUploading(false);
        } catch (uploadError) {
          // 上传失败，清空文件列表和选中的文件
          setUploading(false);
          setFileList([]);
          setSelectedFile(null);
          form.setFieldsValue({
            file: undefined,
          });
          // 错误信息已在 handleFileUpload 中显示
          return; // 不继续执行
        }
      }
      
      setDocumentData({
        ...documentData,
        ...values,
        ...fileData,
      });
      onNext();
    } catch (error) {
      console.error('表单验证失败:', error);
    }
  };

  const uploadProps: UploadProps = {
    accept: '.doc,.docx,.pdf,.txt,.md',
    maxCount: 1,
    fileList,
    beforeUpload: (file) => {
      // 验证文件格式
      const allowedExt = ['.doc', '.docx', '.pdf', '.txt', '.md'];
      const fileName = file.name;
      const ext = fileName.slice(fileName.lastIndexOf('.')).toLowerCase();
      
      if (!allowedExt.includes(ext)) {
        message.error(`不支持 ${ext} 格式，仅支持 doc/docx/pdf/txt/md 格式`);
        return Upload.LIST_IGNORE; // 不支持的格式不显示在列表中
      }
      
      // 验证文件大小
      const isLt15M = file.size / 1024 / 1024 < 15;
      if (!isLt15M) {
        message.error('文件大小不能超过 15MB');
        return Upload.LIST_IGNORE; // 超大文件不显示在列表中
      }
      
      return true;
    },
    onChange: info => {
      const newList = info.fileList.filter(f => {
        // 过滤 error 状态的文件
        if (f.status === 'error') return false;
        return true;
      });
      setFileList(newList);

      // 如果列表为空，确保表单中相关字段被清空
      if (newList.length === 0) {
        form.setFieldsValue({
          file: undefined,
        });
        setSelectedFile(null);
      }
    },
     onRemove: async (file) => {
      // 用户移除时，清理表单和组件状态
      setFileList([]);
      setSelectedFile(null);
      form.setFieldsValue({
        file: undefined,
      });
      return true;
    },
    customRequest: (options) => {
      const { file, onSuccess } = options;
      // 只保存文件对象，不立即上传
      const rawFile = file as File;
      setSelectedFile(rawFile);
      
      // 更新文件列表显示
      setFileList([{
        uid: (file as any).uid || Date.now().toString(),
        name: rawFile.name,
        status: 'done',
      } as UploadFile]);
      
      form.setFieldsValue({
        file: rawFile,
      });
      
      onSuccess?.('ok');
    },
  };

  return (
    <Form 
      form={form} 
      layout="vertical" 
      initialValues={documentData}
      className="py-6"
    >
      <Form.Item name="dataSourceType" label="选择数据源" rules={[{ required: true, message: '请选择数据源' }]}>
        <Radio.Group className="w-full">
          <Space direction="horizontal" className="w-full" size="middle">
             <Card
              hoverable
              className={`w-full cursor-pointer ${documentData.dataSourceType === DocumentSourceType.FILE ? 'border-primary' : ''}`}
              onClick={() => handleSourceChange(DocumentSourceType.FILE)}
            >
              <Radio value={DocumentSourceType.FILE}>
                <Space>
                  <FileTextOutlined />
                  <span className="font-medium">导入文档</span>
                </Space>
              </Radio>
            </Card>

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
        <>
          <Form.Item
            name="file"
            label="上传文件"
            rules={[{ required: true, message: '请上传文件' }]}
          >
            <Upload.Dragger {...uploadProps} disabled={uploading}>
              <p className="ant-upload-drag-icon">
                <UploadOutlined />
              </p>
              <p className="text-[#1890ff]">拖拽文件或者点击此区域进行上传</p>
              <p className="text-base text-gray-500">支持上传文档格式：doc/docx, pdf, txt, md</p>
            </Upload.Dragger>
          </Form.Item>
        </>
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
        <Button type="primary" size="large" onClick={handleSubmit} loading={uploading}>
          下一步
        </Button>
      </div>
    </Form>
  );
};

export default SelectSource;
