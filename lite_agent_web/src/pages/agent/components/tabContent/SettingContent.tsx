import React, { useCallback, useEffect, useState } from 'react';
import { Form, Input, Upload, message, Image } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { UploadChangeParam } from 'antd/es/upload/interface';
import { Agent } from '@/client';
import type { GetProp, UploadFile, UploadProps } from 'antd';
import { buildImageUrl } from '@/utils/buildImageUrl';
import { validateMaxLength } from '@/utils/validate';
import { beforeUpload, onUploadAction } from '@/utils/uploadFile';

type FileType = Parameters<GetProp<UploadProps, 'beforeUpload'>>[0];
const { TextArea } = Input;

interface ISettingProps {
  settingAgent?: Agent;
  visible: boolean;
  setSettingAgent: (agentData: any) => void;
}

const getBase64 = (file: FileType): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = (error) => reject(error);
  });

const SettingContent: React.FC<ISettingProps> = ({ settingAgent, setSettingAgent, visible }) => {
  const [form] = Form.useForm();
  const [imageName, setImageName] = useState<string>('');
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewImage, setPreviewImage] = useState('');
  const [fileList, setFileList] = useState<UploadFile[]>([]);

  const onValuesChange = useCallback(() => {
    const agentData = form.getFieldsValue();
    setSettingAgent?.({ name: agentData.name, icon: imageName, description: agentData.description });
  }, [imageName, form, setSettingAgent]);

  const handlePreview = async (file: UploadFile) => {
    if (!file.url && !file.preview) {
      file.preview = await getBase64(file.originFileObj as FileType);
    }
    setPreviewImage(file.url || (file.preview as string));
    setPreviewOpen(true);
  };

  const handleImageUpload = async (info: UploadChangeParam) => {
    setFileList(info.fileList);

    if (info.file.status === 'done') {
      const newImageName = info.file.xhr.responseURL.split('=')[1];
      setImageName(newImageName);
      setSettingAgent?.({
        ...form.getFieldsValue(),
        icon: newImageName,
      });
      await message.success(`${info.file.name} 上传成功`);
    } else if (info.file.status === 'error' || info.file.status === 'removed') {
      setImageName('');
      setSettingAgent?.({
        ...form.getFieldsValue(),
        icon: '',
      });
      if (info.file.status === 'error') {
        message.error(`${info.file.name} 上传失败`);
      }
    }
  };

  const uploadButton = (
    <div>
      <PlusOutlined />
      <div style={{ marginTop: 8 }}>上传图标</div>
    </div>
  );

  useEffect(() => {
    if (settingAgent) {
      const { name, description, icon } = settingAgent;
      form.setFieldsValue({ name, description });

      if (icon) {
        const imgUrl = buildImageUrl(icon);
        setFileList([
          {
            uid: '-1',
            name: icon,
            status: 'done',
            url: imgUrl,
          },
        ]);
        setImageName(icon);
      }
    }
  }, [settingAgent, form]);

  if (!visible) {
    return <div className="invisible w-0 h-0 m-0 p-0" />;
  }

  return (
    <div className="w-1/2 p-6">
      <h2 className="text-lg font-medium mb-6">Agent 设置</h2>
      <Form form={form} layout="vertical" onValuesChange={() => onValuesChange()}>
        <Form.Item
          name="name"
          label="Agent名称"
          rules={[
            { required: true, message: '请输入Agent名称', whitespace: true },
            { validator: validateMaxLength(20, 'Agent 名称不能超过 20 个字符') }
          ]}
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
            beforeUpload={beforeUpload}
            onChange={handleImageUpload}
            onPreview={handlePreview}
            fileList={fileList}
          >
            {uploadButton}
          </Upload>
        </Form.Item>
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

        <Form.Item name="description" label="描述">
          <TextArea maxLength={200} rows={4} placeholder="用简单几句话将Agent介绍给用户" />
        </Form.Item>
      </Form>
    </div>
  );
};

export default SettingContent;
