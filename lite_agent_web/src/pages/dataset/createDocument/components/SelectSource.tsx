import React, { useState, useCallback, useEffect } from 'react';
import { Input, Upload, Button, Form, message } from 'antd';
import type { UploadProps, UploadFile } from 'antd';
import { DocumentSourceType } from '@/types/dataset';
import fileImg from '@/assets/dataset/file.png';

interface SelectSourceProps {
  documentData: any;
  setDocumentData: (data: any) => void;
  onNext: () => void;
  onFileUpload?: (file: File) => Promise<{ fileId: string; fileName: string }>;
}

const labelClass = 'text-sm font-medium text-[#383F44]';

function FormFieldLabel({ text, required }: { text: string; required?: boolean }) {
  return (
    <span className={labelClass}>
      {required && <span className="text-[#CC2D3A] mr-0.5">*</span>}
      {text}
      <span className="ml-0.5">:</span>
    </span>
  );
}

const SelectSource: React.FC<SelectSourceProps> = ({ documentData, setDocumentData, onNext, onFileUpload }) => {
  const [form] = Form.useForm();

  const [fileList, setFileList] = useState<UploadFile[]>(() => {
    if (documentData?.fileId && documentData?.fileName) {
      return [{ uid: documentData.fileId, name: documentData.fileName, status: 'done' } as UploadFile];
    }
    return [];
  });
  const [uploading, setUploading] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // 回到此步骤时，若已有上传文件则恢复 form 字段值以通过 required 校验
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (documentData?.fileId && documentData?.fileName) {
      form.setFieldsValue({ file: documentData.fileName });
    }
  }, []);

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

      if (documentData.dataSourceType === DocumentSourceType.FILE) {
        if (selectedFile && onFileUpload) {
          // 用户重新选择了文件，上传新文件
          setUploading(true);
          try {
            const uploadResult = await onFileUpload(selectedFile);
            fileData = uploadResult;
            setUploading(false);
          } catch (uploadError) {
            setUploading(false);
            setFileList([]);
            setSelectedFile(null);
            form.setFieldsValue({ file: undefined });
            return;
          }
        } else if (documentData.fileId) {
          // 文件已上传过，直接复用
          fileData = { fileId: documentData.fileId, fileName: documentData.fileName };        
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
    onRemove: async () => {
      setFileList([]);
      setSelectedFile(null);
      form.setFieldsValue({ file: undefined });
      // 同步清空 documentData 中的文件信息，避免复用旧文件
      setDocumentData({ ...documentData, fileId: undefined, fileName: undefined });
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

  const sourceOptions: { type: DocumentSourceType; title: string }[] = [
    { type: DocumentSourceType.FILE, title: '文档' },
    { type: DocumentSourceType.INPUT, title: '纯文本' },
    { type: DocumentSourceType.HTML, title: 'Web 站点' },
  ];

  return (
    <div className="h-full bg-white rounded-2xl p-4 flex flex-col gap-4">
      <Form
        form={form}
        layout="vertical"
        initialValues={documentData}
        className="flex flex-col gap-4 w-full"
        requiredMark={false}
      >
        <Form.Item label={<FormFieldLabel text="选择数据源" required />} className="!mb-0">
          <Form.Item name="dataSourceType" noStyle rules={[{ required: true, message: '请选择数据源' }]}>
            <Input type="hidden" />
          </Form.Item>
          <div className="flex flex-row flex-wrap gap-2 mt-2" role="radiogroup" aria-label="选择数据源">
            {sourceOptions.map(({ type, title }) => {
              const selected = documentData.dataSourceType === type;
              return (
                <button
                  key={type}
                  type="button"
                  role="radio"
                  aria-checked={selected}
                  onClick={() => handleSourceChange(type)}
                  className={[
                    'flex flex-none flex-row items-center justify-start gap-[10px] px-4 py-3 rounded-xl',
                    'border border-solid bg-white text-left text-black/[0.85]',
                    'shadow-none [appearance:none] [-webkit-appearance:none]',
                    'transition-[border-color] duration-150',
                    'hover:border-[#40A5EE]',
                    'focus:outline-none focus-visible:ring-2 focus-visible:ring-[#40A5EE]/25 focus-visible:ring-offset-0',
                    'active:shadow-none active:translate-y-0',
                    selected ? 'border-[#40A5EE]' : 'border-[#E0E3E6]'
                  ].join(' ')}
                >
                  <span
                    className={[
                      'relative box-border inline-flex h-4 w-4 shrink-0 flex-none items-center justify-center rounded-full border border-solid',
                      selected
                        ? 'border-[#40A5EE] bg-[#ECF6FD]'
                        : 'border-[#E0E3E6] bg-white',
                    ].join(' ')}
                    aria-hidden
                  >
                    {selected ? (
                      <span className="h-2 w-2 shrink-0 rounded-full bg-[#40A5EE]" />
                    ) : null}
                  </span>
                  <span className="text-sm font-normal leading-[1.57142857]">{title}</span>
                </button>
              );
            })}
          </div>
        </Form.Item>

        <div className="flex flex-col gap-4">
          {documentData.dataSourceType === DocumentSourceType.INPUT && (
            <div className="flex flex-col gap-4">
              <Form.Item 
                name="name" 
                className="!mb-0"
                label={<FormFieldLabel text="文档名称" required />} 
                rules={[
                  { required: true, message: '请输入文档名称' }, 
                  { validator: (_, value) => !value || value.trim() ? Promise.resolve() : Promise.reject(new Error('请输入文档名称')) }
                ]} 
              >
                <Input placeholder="请输入文档名称" maxLength={60} className="!rounded-xl !border-[#E0E3E6] !h-11 !text-sm" />
              </Form.Item>
              <Form.Item name="content" label={<FormFieldLabel text="文档内容" required />} rules={[{ required: true, message: '请输入文档内容' }]} className="!mb-0">
                <Input.TextArea rows={10} placeholder="请输入文档内容" className="!rounded-xl !border-[#E0E3E6] !text-sm !p-3" />
              </Form.Item>
            </div>
          )}

          {documentData.dataSourceType === DocumentSourceType.FILE && (
            <div className="flex flex-col gap-2">
              <FormFieldLabel text="上传文档" required />
              <Form.Item
                name="file"
                rules={[{ required: true, message: '请上传文件' }]}
                className="!mb-0"
              >
                <Upload.Dragger
                  {...uploadProps}
                  disabled={uploading}
                >
                  <div className="flex flex-col items-center gap-5 py-0">
                    <div className="w-13 h-13 flex items-center justify-center text-black/45">
                      <img src={fileImg} className="w-12" />
                    </div>
                    <div className="flex flex-col items-center gap-1 max-w-[395px] text-center">
                      <p className="text-base text-black/[0.85] leading-tight m-0">拖拽文件或点击此区域进行上传</p>
                      <p className="text-sm text-black/45 leading-[1.57] m-0">支持上传文档格式：doc/docx, pdf, txt, md（最大 15MB）</p>
                    </div>
                  </div>
                </Upload.Dragger>
              </Form.Item>
            </div>
          )}

          {documentData.dataSourceType === DocumentSourceType.HTML && (
            <div className="flex flex-col gap-4">
              <Form.Item 
                name="name" 
                className="!mb-0"
                label={<FormFieldLabel text="文档名称" required />} 
                rules={[
                  { required: true, message: '请输入文档名称' }, 
                  { validator: (_, value) => !value || value.trim() ? Promise.resolve() : Promise.reject(new Error('请输入文档名称')) }
                ]}
              >
                <Input placeholder="请输入文档名称" maxLength={60} className="!rounded-xl !border-[#E0E3E6] !h-11 !text-sm" />
              </Form.Item>
              <Form.Item
                name="htmlUrl"
                label={<FormFieldLabel text="网页链接" required />}
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
                className="!mb-0"
                extra={
                  <div className="mt-2 p-3 border-[#E0E3E6] rounded-xl text-xs text-black/45 flex items-start gap-2">
                    {/* <div className="w-1.5 h-1.5 bg-[#40A5EE] rounded-full mt-1 shrink-0" /> */}
                    仅支持静态链接，每行一个链接，至多支持10个链接。如果上传数据为空，可能是该链接无法被读取。
                  </div>
                }
              >
                <Input.TextArea rows={6} placeholder="请输入网页链接，每行一个" className="!rounded-xl !border-[#E0E3E6] !text-sm !p-3" />
            </Form.Item>
          </div>
          )}
        </div>

        <div className="flex justify-end items-center gap-2 pt-2.5">
          <Button
            type="primary"
            onClick={handleSubmit}
            loading={uploading}
            className="!h-8 !min-w-[80px] !rounded-lg !px-4 !text-sm !font-normal !bg-[#40A5EE] hover:!bg-[#40A5EE]/90 !border-[#40A5EE] !shadow-none"
          >
            下一步
          </Button>
        </div>
      </Form>
    </div>
  );
};

export default SelectSource;
