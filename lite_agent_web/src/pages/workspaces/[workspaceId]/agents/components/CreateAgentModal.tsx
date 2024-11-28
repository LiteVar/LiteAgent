import React, {useCallback, useState} from 'react';
import { Modal, Form, Input, Upload, message, Image } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import {RcFile, UploadChangeParam} from "antd/es/upload/interface";
import {postV1FileUpload} from "@/client";
import ResponseCode from "@/config/ResponseCode";
import type { GetProp, UploadFile, UploadProps } from 'antd';

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

  const handleSubmit = useCallback(async () => {
    try {
      const values = await form.validateFields();
      onOk({ ...values, icon: imageName });
      form.resetFields();
      setImageName("");
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

  const handleImageUpload = async (info: UploadChangeParam) => {
    console.log(info)
    if (info.file.status === 'done') {
      setImageName(info.file.xhr.responseURL.split('=')[1]);
      await message.success(`${info.file.name} 上传成功`)
    } else if (info.file.status === 'error') {
      setImageName("")
      message.error(`${info.file.name} 上传失败`);
    }
  };

  const onUploadAction = async (file: RcFile) => {
    const response = await postV1FileUpload({
      body: {
        file: file
      }
    });
    if (response.data?.code === ResponseCode.S_OK) {
      return "/v1/file/download?filename=" + response.data.data
    } else {
      return '';
    }
  }

  const uploadButton = (
    <div>
      <PlusOutlined />
      <div style={{ marginTop: 8 }}>上传图标</div>
    </div>
  );

  return (
    <Modal
      centered
      title="新建Agent"
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText="确定"
      cancelText="取消"
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="Agent名称"
          rules={[{ required: true, message: '请输入Agent名称', whitespace: true }]}
        >
          <Input maxLength={20} placeholder="请输入Agent名称" />
        </Form.Item>

        <Form.Item label="图标" name="icon">
          <Upload
            name="icon"
            maxCount={1}
            accept=".png,.jpg,.jpeg,.svg,.gif,.webp"
            listType="picture-card"
            className="avatar-uploader"
            showUploadList={true}
            action={onUploadAction}
            onChange={handleImageUpload}
            onPreview={handlePreview}
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
          label="描述"
        >
          <TextArea maxLength={200} rows={4} placeholder="用简单几句话将Agent介绍给用户" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CreateAgentModal;

