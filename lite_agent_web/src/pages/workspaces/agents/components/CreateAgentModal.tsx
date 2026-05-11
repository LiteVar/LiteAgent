import React, { useCallback, useState } from 'react';
import { Modal, Form, Input, Upload, message, Image } from 'antd';
import type { GetProp, UploadFile, UploadProps } from 'antd';
import { UploadChangeParam } from "antd/es/upload/interface";
import { PlusOutlined } from '@ant-design/icons';

import { validateMaxLength } from '@/utils/validate';
import { beforeUpload, customUploadRequest } from '@/utils/uploadFile';

type FileType = Parameters<GetProp<UploadProps, 'beforeUpload'>>[0];

const { TextArea } = Input;
interface ICreateAgentModalProps {
  visible: boolean;
  onCancel: () => void;
  onOk: (values: any) => void;
}

const getBase64 = (file: FileType): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = (error) => reject(error);
  });

const CreateAgentModal:React.FC<ICreateAgentModalProps> = (props) => {
  const { visible, onCancel, onOk } = props;
  const [form] = Form.useForm();
  const [imageName, setImageName] = useState<string>("");
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewImage, setPreviewImage] = useState('');
  const [fileList, setFileList] = useState<UploadFile[]>([]);

  const handleSubmit = useCallback(async () => {
    try {
      const values = await form.validateFields();
      onOk({ ...values, icon: imageName });
      form.resetFields();
      setImageName("");
      setFileList([]);
    } catch (error) {
      console.error('Validation failed:', error);
    }
  }, [form, imageName, onOk]);

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
          uid: options.file.uid,
          name: options.file.name,
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
        uid: info.file.uid,
        name: info.file.name,
        status: 'done',
        url: imageUrl,
        thumbUrl: imageUrl,
        type: 'image/jpeg',
      }]);
      
    } else if (info.file.status === 'error') {
      setImageName("")
      setFileList([]);
      message.error(`${info.file.name} 上传失败`);
    } else if (info.file.status === 'removed') {
      setImageName("");
      setFileList([]);
    } else {
      setFileList(info.fileList);
    }
  };

  const uploadButton = (
    <div>
      <PlusOutlined />
      <div style={{ marginTop: 8 }}>上传图标</div>
    </div>
  );

  return (
    <Modal
      centered
      title={<span className="text-[18px] font-medium text-[#1D4A6B]">新建Agent</span>}
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText="确认"
      cancelText="取消"
      styles={{
        header: { padding: '16px 24px', marginBottom: 0, borderBottom: 'none' },
        body: { padding: '16px 24px' },
        footer: { padding: '10px 16px', marginTop: 0, borderTop: 'none' },
      }}
      okButtonProps={{ className: 'bg-[#40A5EE] rounded-xl h-10 px-6' }}
      cancelButtonProps={{ className: 'rounded-xl h-10 px-6' }}
    >
      <Form form={form} layout="vertical" requiredMark={false}>
        <Form.Item
          name="name"
          label={<span className="text-[14px] text-[#383F44] font-medium">Agent 名称</span>}
          rules={[
            { required: true, message: '请输入 Agent 名称', whitespace: true },
            { validator: validateMaxLength(20, 'Agent 名称不能超过 20 个字符') }
          ]}
        >
          <Input className="h-10 rounded-lg" maxLength={20} placeholder="请输入Agent名称" />
        </Form.Item>

        <Form.Item label={<span className="text-[14px] text-[#383F44] font-medium">图标</span>} name="icon">
          <Upload
            name="icon"
            maxCount={1}
            accept=".png,.jpg,.jpeg,.svg,.gif,.webp"
            listType="picture-card"
            className="avatar-uploader"
            showUploadList={true}
            customRequest={handleCustomRequest}
            beforeUpload={beforeUpload}
            onChange={handleImageUpload}
            onPreview={handlePreview}
            fileList={fileList}
            isImageUrl={() => true}
          >
            {uploadButton}
          </Upload>
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
        </Form.Item>

        <Form.Item
          name="description"
          label={<span className="text-[14px] text-[#383F44] font-medium">描述</span>}
        >
          <TextArea className="rounded-lg" maxLength={200} rows={4} placeholder="用简单几句话将Agent介绍给用户" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CreateAgentModal;

