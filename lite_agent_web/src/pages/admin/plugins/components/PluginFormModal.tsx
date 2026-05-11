import { Modal, Form, Input, Upload, Button, message } from 'antd';
import { useState, useEffect } from 'react';
import { UploadOutlined } from '@ant-design/icons';
import type { UploadFile, RcFile, UploadChangeParam } from 'antd/es/upload/interface';
import type { Plugin, PluginVOUpdateAction } from '@/client';
import { buildImageUrl } from '@/utils/buildImageUrl';
import ResponseCode from '@/constants/ResponseCode';
import { beforeUpload, customUploadRequest } from '@/utils/uploadFile';
import { putV1PluginById } from '@/client';

interface PluginFormModalProps {
  open: boolean;
  editingPlugin?: Plugin;
  onClose: () => void;
  onSuccess: () => void;
}

export default function PluginFormModal({ 
  open, 
  editingPlugin, 
  onClose, 
  onSuccess 
}: PluginFormModalProps) {
  const [form] = Form.useForm();
  const [packageFile, setPackageFile] = useState<UploadFile | null>(null);
  const [iconName, setIconName] = useState<string>('');
  const [iconFileList, setIconFileList] = useState<UploadFile[]>([]);
  const [submitting, setSubmitting] = useState(false);

  // 处理图标上传
  const handleImageUpload = async (info: UploadChangeParam) => {
    if (info.file.status === 'done') {
      // info.file.response 就是 customUploadRequest 返回的完整图片 URL
      const imageUrl = info.file.response;
      setIconName(imageUrl);
      
      // 手动设置 fileList，使用 thumbUrl 避免额外请求
      setIconFileList([{
        uid: info.file.uid,
        name: info.file.name,
        status: 'done',
        url: imageUrl,
        thumbUrl: imageUrl,
      }]);
      message.success(`${info.file.name} 上传成功`);
      console.log('图标上传成功，URL:', imageUrl);
    } else if (info.file.status === 'error') {
      setIconName('');
      setIconFileList([]);
      message.error(`${info.file.name} 上传失败`);
    } else if (info.file.status === 'removed') {
      setIconName('');
      setIconFileList([]);
    } else {
      setIconFileList(info.fileList);
    }
  };

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);

      if (editingPlugin) {
        // 更新插件
        const updateData: PluginVOUpdateAction = {
          id: editingPlugin.id!,
          name: values.name.trim(),
          description: values.description?.trim(),
          icon: iconName || undefined,
        };

        const res = await putV1PluginById({
          body: updateData,
          path: { id: editingPlugin.id! }
        });

        if (res?.data?.code === ResponseCode.S_OK) {
          message.success('更新插件成功');
          onSuccess();
          handleClose();
        } else {
          message.error(res?.data?.message || '更新插件失败');
        }
      } else {
        // 新建插件
        if (!packageFile || !packageFile.originFileObj) {
          message.error('请上传插件文件');
          setSubmitting(false);
          return;
        }

        try {
          // 使用 FormData 构建请求
          const formData = new FormData();
          formData.append('name', values.name.trim());
          if (values.description?.trim()) {
            formData.append('description', values.description.trim());
          }
          if (iconName) {
            formData.append('icon', iconName);
          }
          formData.append('file', packageFile.originFileObj);

          // 显示创建插件的 loading 提示
          message.loading({ content: '正在创建插件...', key: 'creatingPlugin', duration: 0 });

          const uploadResponse = await fetch('/v1/plugin/add', {
            method: 'POST',
            body: formData,
            headers: {
              Authorization: `Bearer ${localStorage.getItem('access_token')}`,
            },
          });

          message.destroy('creatingPlugin');

          if (!uploadResponse.ok) {
            throw new Error('创建插件失败');
          }

          const result = await uploadResponse.json();
          
          if (result?.code === ResponseCode.S_OK) {
            message.success('创建插件成功');
            onSuccess();
            handleClose();
          } else {
            message.error(result?.message || '创建插件失败');
          }
        } catch (error) {
          message.destroy('creatingPlugin');
          console.error('创建插件失败:', error);
          message.error('创建插件失败：' + (error instanceof Error ? error.message : '请检查文件大小和网络连接'));
          setSubmitting(false);
          return;
        }
      }
    } catch (error) {
      console.error('提交失败:', error);
      message.error('操作失败：' + (error instanceof Error ? error.message : '未知错误'));
      setSubmitting(false);
    } finally {
      setSubmitting(false);
    }
  };

  // 关闭弹框并重置状态
  const handleClose = () => {
    form.resetFields();
    setPackageFile(null);
    setIconFileList([]);
    setIconName('');
    onClose();
  };

  // 当 open 或 editingPlugin 变化时，更新表单
  useEffect(() => {
    if (open) {
      if (editingPlugin) {
        form.setFieldsValue({
          name: editingPlugin.name,
          description: editingPlugin.description,
        });
        
        // 如果有图标，显示图标
        if (editingPlugin.icon) {
          const imgUrl = buildImageUrl(editingPlugin.icon);
          setIconFileList([
            {
              uid: '-1',
              name: '',
              status: 'done',
              url: imgUrl,
              thumbUrl: imgUrl,
            },
          ]);
          setIconName(editingPlugin.icon);
        } else {
          setIconFileList([]);
          setIconName('');
        }
      } else {
        form.resetFields();
        setIconFileList([]);
        setIconName('');
        setPackageFile(null);
      }
    }
  }, [open, editingPlugin, form]);

  return (
    <Modal
      title={<span className="text-[#1D4A6B] text-[18px] font-medium">{editingPlugin ? '编辑插件' : '新建插件'}</span>}
      open={open}
      onCancel={handleClose}
      onOk={handleSubmit}
      confirmLoading={submitting}
      okText="确定"
      cancelText="取消"
      width={460}
      centered
      className="[&_.ant-modal-content]:rounded-2xl [&_.ant-modal-header]:mb-6 [&_.ant-modal-header]:pb-4 [&_.ant-modal-header]:border-b [&_.ant-modal-header]:border-white/20 [&_.ant-modal-footer]:mt-6"
      okButtonProps={{
        className: 'bg-[#40A5EE] border-[#40A5EE] h-10 px-6 rounded-xl',
      }}
      cancelButtonProps={{
        className: 'h-10 px-6 rounded-xl border-[#E0E3E6] text-[#383F44]',
      }}
    >
      <Form
        form={form}
        layout="vertical"
        autoComplete="off"
        className="[&_.ant-form-item-label_label]:text-[#383F44] [&_.ant-form-item-label_label]:font-medium"
      >
        <Form.Item
          name="name"
          label={
            <span className="flex flex-col">
              插件名称
              <span className="text-[#7C8B98] text-[12px] font-normal leading-tight mt-1">
                ( 名称会向系统用户展示，建议：平台名称 +  智连 / 智能通 )
              </span>
            </span>
          }
          rules={[{ required: true, message: '请输入插件名称' }]}
        >
          <Input placeholder="请输入插件名称" className="h-10 rounded-lg border-[#E0E3E6] hover:border-[#40A5EE] focus:border-[#40A5EE]" disabled={!!editingPlugin} />
        </Form.Item>

        {!editingPlugin && (
          <Form.Item
            label={
              <span className="flex flex-col">
                插件文件
                <span className="text-[#7C8B98] text-[12px] font-normal leading-tight mt-1">
                  ( .tar 或 .tar.gz )
                </span>
              </span>
            }
            required
          >
            <Upload
              accept=".tar,.tar.gz"
              beforeUpload={(file) => {
                // 验证文件类型
                const isValidType = file.name.endsWith('.tar') || file.name.endsWith('.tar.gz');
                if (!isValidType) {
                  message.error('只能上传 .tar 或 .tar.gz 格式的文件');
                  return Upload.LIST_IGNORE;
                }
                
                const uploadFile: UploadFile = {
                  uid: file.uid,
                  name: file.name,
                  status: 'done',
                  originFileObj: file as RcFile,
                };
                setPackageFile(uploadFile);
                return false;
              }}
              onRemove={() => setPackageFile(null)}
              fileList={packageFile ? [packageFile] : []}
              maxCount={1}
              className="w-full"
            >
              <Button icon={<UploadOutlined />} className="w-full h-10 rounded-lg border-[#E0E3E6] text-[#383F44] flex items-center justify-center">
                上传文件
              </Button>
            </Upload>
          </Form.Item>
        )}

        <Form.Item label="图标">
          <Upload
            name="icon"
            maxCount={1}
            accept=".png,.jpg,.jpeg,.svg,.gif,.webp"
            listType="picture"
            customRequest={customUploadRequest}
            beforeUpload={beforeUpload}
            onChange={handleImageUpload}
            fileList={iconFileList}
            className="w-full [&_.ant-upload-list-item]:rounded-lg"
          >
            {iconFileList.length >= 1 ? null : (
              <Button icon={<UploadOutlined />} className="w-full h-10 rounded-lg border-[#E0E3E6] text-[#383F44] flex items-center justify-center">
                上传图标
              </Button>
            )}
          </Upload>
        </Form.Item>

        <Form.Item
          name="description"
          label="描述"
        >
          <Input.TextArea 
            placeholder="请简要描述一下插件" 
            rows={4}
            maxLength={500}
            showCount
            className="rounded-lg border-[#E0E3E6] hover:border-[#40A5EE] focus:border-[#40A5EE]"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}

