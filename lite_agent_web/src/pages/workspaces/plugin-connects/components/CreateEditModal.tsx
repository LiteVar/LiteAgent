import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Select, Upload, Image, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { PluginConnect } from '@/types/plugin-connect';
import { getV1PluginList, putV1PluginConnectorById } from '@/client';
import type { Plugin } from '@/client';
import { UploadChangeParam } from 'antd/es/upload/interface';
import { buildImageUrl } from '@/utils/buildImageUrl';
import type { GetProp, UploadFile, UploadProps } from 'antd';
import { beforeUpload, customUploadRequest } from '@/utils/uploadFile';

type FileType = Parameters<GetProp<UploadProps, 'beforeUpload'>>[0];

interface CreateEditModalProps {
  visible: boolean;
  editingConnect?: PluginConnect;
  workspaceId: string;
  onClose: () => void;
  onSuccess: (connectId?: string, basicInfo?: {
    id?: string;
    name: string;
    pluginId: string;
    pluginName?: string;
    icon?: string;
    description?: string;
  }) => void;
}

const CreateEditModal: React.FC<CreateEditModalProps> = ({
  visible,
  editingConnect,
  workspaceId,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [pluginOptions, setPluginOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewImage, setPreviewImage] = useState('');
  const [imageName, setImageName] = useState<string>('');
  const [fileList, setFileList] = useState<UploadFile[]>([]);

  // 加载可用插件列表
  useEffect(() => {
    const loadPlugins = async () => {
      try {
        const response = await getV1PluginList({
          query: { pageNo: 1, pageSize: 100 },
        });
        
        if (response.data?.data) {
          setPluginOptions(
            response.data.data
              .filter((p: Plugin) => p.status === 2)
              .map((p: Plugin) => ({
                label: p.name || '',
                value: p.id || '',
              }))
          );
        }
      } catch (error) {
        console.error('加载插件列表失败', error);
      }
    };

    loadPlugins();
  }, []);

  // 编辑时回填表单
  useEffect(() => {
    if (editingConnect) {
      form.setFieldsValue({
        name: editingConnect.name,
        pluginId: editingConnect.pluginId,
        description: editingConnect.description,
      });
      
      const icon = editingConnect.icon;
      if (icon) {
        const imgUrl = buildImageUrl(icon);
        setFileList([
          {
            uid: '-1',
            name: icon,
            status: 'done',
            url: imgUrl,
            thumbUrl: imgUrl, // 使用 thumbUrl 避免 Upload 组件发送额外请求
            type: 'image/jpeg',
          },
        ]);
        setImageName(icon);
      }
    } else {
      form.resetFields();
      setImageName('');
      setFileList([]);
    }
  }, [editingConnect, form]);

  // 图片预览相关
  const getBase64 = (file: FileType): Promise<string> =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = (error) => reject(error);
    });

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

  // 处理图标上传
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
      setImageName('');
      setFileList([]);
    } else if (info.file.status === 'removed') {
      setImageName('');
      setFileList([]);
    } else {
      setFileList(info.fileList);
    }
  };

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      
      const selectedPlugin = pluginOptions.find(p => p.value === values.pluginId);
      const basicInfo = {
        id: editingConnect?.id,
        name: values.name,
        pluginId: values.pluginId,
        pluginName: selectedPlugin?.label,
        icon: imageName,
        description: values.description,
      };
      
      if (editingConnect) {
        // 编辑模式：直接保存基础信息（不传 data 字段，避免覆盖配置数据）
        await putV1PluginConnectorById({
          path: { id: editingConnect.id },
          body: {
            id: editingConnect.id,
            name: values.name,
            pluginId: values.pluginId,
            icon: imageName,
            description: values.description,
          } as any, // 使用 any 类型避免类型错误
          headers: {
            'Workspace-id': workspaceId,
          },
        });
        message.success('编辑成功');
        onSuccess(); // 不传参数，不打开 ConfigModal
      } else {
        // 新建模式：传递基础信息到父组件，打开 ConfigModal
        onSuccess(undefined, basicInfo);
      }
    } catch (error) {
      console.error('提交失败', error);
      message.error('操作失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  // 检查是否可以编辑插件类型 (状态2-已上线或3-已下线时不能编辑)
  const isPluginTypeDisabled = editingConnect && (editingConnect.status === 2 || editingConnect.status === 3);

  return (
    <Modal
      title={<span className="text-[#1D4A6B] text-[18px] font-medium">{editingConnect ? '编辑插件智连' : '新建插件智连'}</span>}
      open={visible}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={loading}
      width={460}
      centered
      okText="确定"
      cancelText="取消"
      className="custom-modal"
      styles={{
        header: { padding: '16px 24px', marginBottom: 0, borderBottom: '1px solid #f0f0f0' },
        body: { padding: '16px 24px' },
        footer: { padding: '10px 16px', marginTop: 0, borderTop: '1px solid #f0f0f0' }
      }}
    >
      <Form
        form={form}
        layout="vertical"
      >
        <Form.Item
          name="name"
          label={<span className="text-[#383F44] font-medium">智连名称</span>}
          rules={[{ required: true, message: '请输入智连名称' }]}
        >
          <Input placeholder="请输入智连名称" className="rounded-lg h-10" maxLength={20} />
        </Form.Item>

        <Form.Item
          name="pluginId"
          label={<span className="text-[#383F44] font-medium">插件</span>}
          rules={[{ required: true, message: '请选择插件' }]}
        >
          <Select
            placeholder="请选择插件"
            options={pluginOptions}
            disabled={isPluginTypeDisabled}
            className="h-10"
            notFoundContent={
              <div className="text-red-500 text-center py-4">
                请联系管理员，到【插件管理】新增或开启相关插件
              </div>
            }
          />
        </Form.Item>

        <Form.Item label={<span className="text-[#383F44] font-medium">图标</span>}>
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
            {fileList.length >= 1 ? null : (
              <div>
                <PlusOutlined />
                <div className="mt-2">上传图标</div>
              </div>
            )}
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

        <Form.Item name="description" label={<span className="text-[#383F44] font-medium">描述</span>}>
          <Input.TextArea
            placeholder="请简单描述一下智连"
            rows={4}
            maxLength={200}
            showCount
            className="rounded-lg"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CreateEditModal;

