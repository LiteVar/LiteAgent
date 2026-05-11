import React, { useState, useEffect, useMemo } from 'react';
import { Modal, message, Spin, Button, Upload, Input, Form, Image } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { createForm } from '@formily/core';
import { FormProvider, createSchemaField, Field, ISchema } from '@formily/react';
import {
  FormItem,
  FormLayout,
  Input as FormilyInput,
  Password,
  Select as FormilySelect,
  NumberPicker,
  Switch as FormilySwitch,
} from '@formily/antd-v5';
import { PluginConnect } from '@/types/plugin-connect';
import { 
  getV1PluginConnector, 
  getV1PluginByPluginIdSchema, 
  postV1PluginConnector, 
  putV1PluginConnectorById, 
  postV1PluginConnectorByIdStatus,
  getV1PluginConnectorByIdData,
  getV1PluginAgentList,
  PluginAgentVO,
  PluginConnectorVOAddAction,
} from '@/client';
import { useWorkspace } from '@/contexts/workspaceContext';
import { UploadChangeParam } from 'antd/es/upload/interface';
import { buildImageUrl } from '@/utils/buildImageUrl';
import type { GetProp, UploadFile, UploadProps } from 'antd';
import { beforeUpload, customUploadRequest } from '@/utils/uploadFile';
import ResponseCode from '@/constants/ResponseCode';

type FileType = Parameters<GetProp<UploadProps, 'beforeUpload'>>[0];

const Text: React.FC<{ content?: string; mode?: string }> = ({ content, mode }) => {
  if (mode === 'h1') return <h1>{content}</h1>;
  if (mode === 'h2') return <h2>{content}</h2>;
  if (mode === 'h3') return <h3>{content}</h3>;
  return <p>{content}</p>;
};

const SchemaField = createSchemaField({
  components: {
    FormItem,
    Input: FormilyInput,
    'Input.TextArea': FormilyInput.TextArea,
    Password,
    Select: FormilySelect,
    NumberPicker,
    Switch: FormilySwitch,
    Text,
  },
});

interface ConfigModalProps {
  visible: boolean;
  pluginConnectId?: string;
  onClose: () => void;
  onSuccess: () => void;
  basicInfo?: {
    id?: string;
    name: string;
    pluginId: string;
    pluginName?: string;
    icon?: string;
    description?: string;
  };
}

const ConfigModal: React.FC<ConfigModalProps> = ({
  visible,
  pluginConnectId,
  onClose,
  onSuccess,
  basicInfo,
}) => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [onlining, setOnlining] = useState(false);
  const [pluginConnect, setPluginConnect] = useState<PluginConnect | null>(null);
  const [formSchema, setFormSchema] = useState<{
    form?: {
      labelCol?: number;
      wrapperCol?: number;
      labelWidth?: string;
    };
    schema?: {
      type?: string;
      properties?: Record<string, unknown>;
    };
  } | null>(null);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewImage, setPreviewImage] = useState('');
  const [imageName, setImageName] = useState<string>('');
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const workspace = useWorkspace();

  // Formily 表单实例，pluginConnectId 切换时重新创建以隔离状态
  const formilyForm = useMemo(
    () => createForm(),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [pluginConnectId]
  );
  
  // 基础信息状态
  const [editBasicInfo, setEditBasicInfo] = useState({
    name: '',
    description: '',
  });
  
  // 已上线时禁用 Formily 表单
  useEffect(() => {
    if (pluginConnect?.status === 2) {
      formilyForm.setPattern('disabled');
    } else {
      formilyForm.setPattern('editable');
    }
  }, [pluginConnect?.status, formilyForm]);

  // Agent 相关状态
  const [agentList, setAgentList] = useState<PluginAgentVO[]>([]);
  const [agentApiInfo, setAgentApiInfo] = useState<{
    agentId?: string;
    agentApiKey?: string;
    agentBaseUrl?: string;
  }>({});

  // Modal 打开/关闭时重置表单和状态
  useEffect(() => {
    if (!visible) {
      formilyForm.reset();
      setFormSchema(null);
      setPluginConnect(null);
      setAgentApiInfo({});
      setImageName('');
      setFileList([]);
      setPreviewImage('');
      setEditBasicInfo({ name: '', description: '' });
    } else {
      formilyForm.reset();
      setAgentApiInfo({});
    }
  }, [visible, formilyForm]);

  // 加载 Agent 列表
  useEffect(() => {
    if (!visible || !workspace?.id) return;

    const loadAgentList = async () => {
      try {
        const response = await getV1PluginAgentList({
          headers: {
            'Workspace-id': workspace?.id || '',
          },
        });
        
        if (response.data?.data) {
          setAgentList(response.data.data);
        }
      } catch (error) {
        console.error('加载 Agent 列表失败', error);
        message.error('加载 Agent 列表失败');
      }
    };

    loadAgentList();
  }, [visible, workspace?.id]);

  // 加载插件智连详情和表单配置
  useEffect(() => {
    if (!visible || !workspace?.id) return;

    const loadDetail = async () => {
      try {
        setLoading(true);
        formilyForm.reset();
        
        // 判断是新建还是编辑
        const isEdit = !!pluginConnectId;
        
        if (isEdit) {
          // 编辑模式：加载插件智连详情
          const response = await getV1PluginConnector({
            headers: {
              'Workspace-id': workspace?.id || '',
            },
          });
          
          if (response.data?.data) {
            const connectors = response.data.data as PluginConnect[];
            const detail = connectors.find(c => c.id === pluginConnectId);
            
            if (detail) {
              setPluginConnect(detail);
              
              // 初始化基础信息
              setEditBasicInfo({
                name: detail.name,
                description: detail.description || '',
              });
              
              // 设置图标
              const icon = detail.icon || basicInfo?.icon;
              if (icon) {
                const imgUrl = buildImageUrl(icon);
                setFileList([
                  {
                    uid: '-1',
                    name: icon,
                    status: 'done',
                    url: imgUrl,
                    type: 'image/jpeg',
                  },
                ]);
                setImageName(icon);
              }
              
              // 加载插件表单配置
              const schemaResponse = await getV1PluginByPluginIdSchema({
                path: { pluginId: detail.pluginId },
              });
              
              if (schemaResponse.data?.data) {
                setFormSchema(schemaResponse.data.data);
                
                // 调用 getV1PluginConnectorByIdData 接口获取配置数据
                try {
                  const dataResponse = await getV1PluginConnectorByIdData({
                    path: { id: pluginConnectId }
                  });
                  
                  if (dataResponse.data?.code === ResponseCode.S_OK) {
                    const savedData = typeof dataResponse.data.data === 'string' 
                      ? JSON.parse(dataResponse.data.data) 
                      : dataResponse.data.data;
                    
                    // 提取 agent 相关信息
                    const { agentId, agentApiKey, agentBaseUrl, ...otherData } = savedData;
                    if (agentId) {
                      setAgentApiInfo({ agentId, agentApiKey, agentBaseUrl });
                    }
                    
                    // 设置表单值，包括 agentId
                    formilyForm.setValues({
                      ...otherData,
                      agentId,
                    });
                  } else {
                    message.error(dataResponse.data?.message || '加载配置数据失败');
                  }
                } catch (error) {
                  console.error('加载配置数据失败', error);
                }
              }
            }
          }
        } else if (basicInfo) {
          // 新建模式：使用传入的基础信息
          setPluginConnect({
            id: '',
            pluginId: basicInfo.pluginId,
            pluginName: basicInfo.pluginName || '',
            name: basicInfo.name,
            description: basicInfo.description,
            icon: basicInfo.icon,
            status: 0,
          } as PluginConnect);
          
          // 初始化基础信息
          setEditBasicInfo({
            name: basicInfo.name,
            description: basicInfo.description || '',
          });
          
          // 设置图标
          const icon = basicInfo.icon;
          if (icon) {
            const imgUrl = buildImageUrl(icon);
            setFileList([
              {
                uid: '-1',
                name: icon,
                status: 'done',
                url: imgUrl,
                type: 'image/jpeg',
              },
            ]);
            setImageName(icon);
          }
          
          // 加载插件表单配置
          const schemaResponse = await getV1PluginByPluginIdSchema({
            path: { pluginId: basicInfo.pluginId },
          });
          
          if (schemaResponse.data?.data) {
            setFormSchema(schemaResponse.data.data);
          }
        }
      } catch (error) {
        message.error('加载插件配置失败');
        console.error(error);
      } finally {
        setLoading(false);
      }
    };

    loadDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible, pluginConnectId, workspace?.id, basicInfo]);

  // agentList 加载完成后，显式更新 Formily 字段的 options，避免因竞态导致下拉列表为空
  useEffect(() => {
    if (!visible || agentList.length === 0) return;
    formilyForm.query('agentId').take(field => {
      field.setComponentProps({
        options: agentList.map(agent => ({ label: agent.name, value: agent.id })),
      });
    });
  }, [agentList, visible, formilyForm]);

  // 处理 Agent 选择变化
  const handleAgentChange = (agentId: string) => {
    // 从列表中找到选中的 Agent 数据
    const selectedAgentData = agentList.find(agent => agent.id === agentId);
    
    if (selectedAgentData) {
      setAgentApiInfo({
        agentId: selectedAgentData.id,
        agentApiKey: selectedAgentData.apiKey,
        agentBaseUrl: selectedAgentData.apiUrl,
      });
    }
    
    // Form 会自动管理 agentId 字段的值
  };

  // 设置智连上线或下线状态
  const updatePluginConnectorStatus = async (
    connectorId: string,
    status: number
  ): Promise<{ success: boolean; message?: string }> => {
    try {
      const response = await postV1PluginConnectorByIdStatus({
        path: { id: connectorId },
        query: { status },
        headers: {
          'Workspace-id': workspace?.id || '',
        },
      });

      if (response?.data?.code === ResponseCode.S_OK) {
        return { success: true };
      } else {
        return {
          success: false,
          message: response?.data?.message || '状态更新失败',
        };
      }
    } catch (error) {
      console.error('更新智连状态失败', error);
      return {
        success: false,
        message: error instanceof Error ? error.message : '状态更新失败',
      };
    }
  };

  // 配置提交
  const handleSubmit = async (isOnline: boolean) => {
    try {
      // Formily 校验所有字段，校验失败会抛出错误
      await formilyForm.validate();
      const formData = formilyForm.values;

      if (!workspace?.id || !pluginConnect) {
        message.error('工作空间信息或插件智连信息缺失');
        return;
      }

      // 合并 agent 信息和动态表单数据
      const submitData = {
        ...agentApiInfo,
        ...formData,
      };

      if (isOnline) {
        setOnlining(true);
      } else {
        setSaving(true);
      }

      const isEdit = !!pluginConnectId;
      
      if (isEdit) {
        // 编辑模式：更新插件智连
        const editRes = await putV1PluginConnectorById({
          path: { id: pluginConnectId },
          body: {
            id: pluginConnectId,
            name: editBasicInfo.name,
            pluginId: pluginConnect.pluginId,
            description: editBasicInfo.description,
            icon: imageName,
            data: submitData
          },
          headers: {
            'Workspace-id': workspace.id,
          },
        });

        if (editRes?.data?.code === ResponseCode.S_OK) {
          if (!isOnline) {
            message.success('保存成功');
            onSuccess();
            return;
          }
          // 提交后设置为上线状态
          const onlineResult = await updatePluginConnectorStatus(pluginConnectId, 2); // 2-上线

          if (onlineResult.success) {
            message.success('上线成功');
            onSuccess();
          } else {
            message.error(onlineResult.message || '上线失败');
          }
        } else {
          message.error(editRes?.data?.message || '配置失败')
        }
      } else {
        // 新建模式：创建插件智连
        const createResponse = await postV1PluginConnector({
          body: {
            pluginId: pluginConnect.pluginId,
            name: editBasicInfo.name,
            icon: imageName,
            description: editBasicInfo.description,
            data: submitData,
          } as unknown as PluginConnectorVOAddAction,
          headers: {
            'Workspace-id': workspace.id,
          },
        });
        
        const createData = createResponse?.data;
        const newPluginConnectId = createData?.data;

        if (createData && createData.code === ResponseCode.S_OK && newPluginConnectId) {
          if (!isOnline) {
            message.success('保存成功');
            onSuccess();
            return;
          }
          
          // 创建成功后设置为上线状态
          const onlineResult = await updatePluginConnectorStatus(newPluginConnectId, 2); // 2-上线

          if (onlineResult.success) {
            message.success('上线成功');
            onSuccess();
          } else {
            message.warning(onlineResult.message || '上线失败');
          }
        } else {
          message.error(createResponse.data?.message || '配置提交失败');
        }
      }

    } catch (error) {
      console.error('提交失败', error);
      if (error instanceof Error && error.message) {
        message.error(`配置提交失败: ${error.message}`);
      } 
    } finally {
      setSaving(false);
      setOnlining(false);
    }
  };

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

  // Formily 新建模式下 formSchema 变更时重置（formilyForm 初始即为空，无需额外处理）

  // 渲染动态表单字段（基于 Formily SchemaField）
  const renderDynamicFormFields = () => {
    if (!formSchema?.schema?.properties) {
      return (
        <div className="text-center py-8 text-gray-500">
          该插件暂无配置表单
        </div>
      );
    }

    // 预处理：① required 为 true 时强制显示星标；② 为必填项补充中文校验提示，避免 Formily 默认英文 "The field value is required"；③ 禁止浏览器自动填充
    const schemaWithAsterisk: ISchema = {
      ...formSchema.schema,
      type: 'object',
      properties: Object.fromEntries(
        Object.entries(formSchema.schema.properties || {}).map(([key, field]) => {
          const f = field as Record<string, unknown>;
          const title = (f?.title as string) || key;
          const requiredMsg = `请填写${title}`;
          const next = { ...f };

          if (f?.required) {
            if (f['x-decorator-props'] && typeof f['x-decorator-props'] === 'object') {
              const decoratorProps = { ...(f['x-decorator-props'] as Record<string, unknown>) };
              if (decoratorProps.asterisk === false) decoratorProps.asterisk = true;
              next['x-decorator-props'] = decoratorProps;
            }
            const validators = Array.isArray(next['x-validator']) ? [...(next['x-validator'] as unknown[])] : [];
            const hasRequiredRule = validators.some(
              (r: unknown) => typeof r === 'object' && r !== null && (r as Record<string, unknown>).required === true
            );
            if (!hasRequiredRule) {
              validators.unshift({ required: true, message: requiredMsg });
            } else {
              // 已有 required 规则但可能无中文 message，补全
              const list = validators as Array<Record<string, unknown>>;
              for (const r of list) {
                if (r?.required === true && !r.message) r.message = requiredMsg;
              }
            }
            next['x-validator'] = validators;
          }

          // 禁止浏览器自动填充：密码字段用 new-password，其他用 off
          const isPassword = f['x-component'] === 'Password';
          const existingComponentProps = (f['x-component-props'] as Record<string, unknown>) || {};
          next['x-component-props'] = {
            ...existingComponentProps,
            autoComplete: isPassword ? 'new-password' : 'off',
          };

          return [key, next];
        })
      ) as ISchema['properties'],
    };

    // 使用后端返回的 form 布局：vertical 使 label 与输入换行显示；labelCol/wrapperCol 为水平布局用，垂直布局下不传
    const formLayout = formSchema.form || {};
    const labelWidth =
      typeof formLayout.labelWidth === 'number'
        ? formLayout.labelWidth
        : typeof formLayout.labelWidth === 'string' && formLayout.labelWidth !== 'inherit'
          ? Number(formLayout.labelWidth)
          : undefined;
    const layoutProps = {
      layout: 'vertical' as const,
      labelAlign: 'left' as const,
      ...(typeof labelWidth === 'number' && !Number.isNaN(labelWidth) && { labelWidth }),
    };

    // Agent 名称单独用 Field 渲染在顶部；SchemaField 只渲染后端 schema
    return (
      <FormProvider form={formilyForm}>
        <FormLayout {...layoutProps}>
          <Field
            name="agentId"
            title="Agent 名称"
            required
            decorator={[FormItem, { layout: 'vertical', labelAlign: 'left' }]}
            component={[
              FormilySelect,
              {
                placeholder: '请选择 Agent',
                showSearch: true,
                optionFilterProp: 'label',
                options: agentList.map(agent => ({ label: agent.name, value: agent.id })),
                onChange: handleAgentChange,
              },
            ]}
            validator={{ required: true, message: '请选择 Agent 名称' }}
          />
          <SchemaField schema={schemaWithAsterisk} />
        </FormLayout>
      </FormProvider>
    );
  };

  return (
    <Modal
      title={<span className="text-[#1D4A6B] text-[18px] font-medium">插件智连配置</span>}
      open={visible}
      onCancel={onClose}
      footer={null}
      width={1000}
      centered
      destroyOnClose
      styles={{
        header: { padding: '16px 24px', marginBottom: 0, borderBottom: '1px solid #f0f0f0' },
        body: { padding: '24px' },
      }}
    >
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Spin size="large" />
          </div>
        ) : pluginConnect ? (
          <div>
            <div className="grid grid-cols-2 gap-12">

              {/* 左侧 - 基础信息 */}
              <div className="flex flex-col gap-4">
                <h3 className="text-[16px] font-semibold text-[#1D4A6B] pb-2 border-b border-[#f0f0f0] mb-2">基础信息</h3>
                
              <Form layout="vertical" autoComplete="off">
              <Form.Item
                label={<span className="text-[#383F44] font-medium">智连名称</span>}
                className="mb-4"
              >
                <Input 
                  className="h-10 rounded-lg"
                  value={editBasicInfo.name}
                  onChange={(e) => setEditBasicInfo(prev => ({ ...prev, name: e.target.value }))}
                  maxLength={20} 
                />
              </Form.Item>

              <Form.Item
                label={<span className="text-[#383F44] font-medium">插件</span>}
                className="mb-4"
              >
                <Input value={basicInfo?.pluginName || pluginConnect.pluginName} disabled className="rounded-lg h-10" />
              </Form.Item>

              <Form.Item label={<span className="text-[#383F44] font-medium">图标</span>} className="mb-4">
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
                      <div className="mt-2 text-[#7C8B98]">上传图标</div>
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

              <Form.Item label={<span className="text-[#383F44] font-medium">描述</span>}>
                <Input.TextArea
                  value={editBasicInfo.description}
                  onChange={(e) => setEditBasicInfo(prev => ({ ...prev, description: e.target.value }))}
                  rows={4}
                  className="rounded-lg"
                />
              </Form.Item>
              </Form>
            </div>

              {/* 右侧 - 动态配置表单 */}
              <div className="flex flex-col gap-4 border-l border-[#f0f0f0] pl-12">
                <h3 className="text-[16px] font-semibold text-[#1D4A6B] pb-2 border-b border-[#f0f0f0] mb-2">配置</h3>
                <div className="max-h-[550px] overflow-y-auto pr-2 custom-scrollbar">
                  {renderDynamicFormFields()}
                </div>
              </div>
            </div>

            {/* 操作按钮 */}
            <div className="flex justify-end gap-3 pt-4 mt-8 border-t border-[#f0f0f0]">
              <Button size="large" onClick={onClose} className="rounded-lg px-8">
                取消
              </Button>
              <Button
                type="primary"
                size="large"
                loading={saving}
                disabled={onlining}
                onClick={() => handleSubmit(false)}
                className="rounded-lg px-8 bg-[#40A5EE]"
              >
                提交
              </Button>
              {pluginConnect?.status !== 2 && (
                <Button
                  type="primary"
                  size="large"
                  loading={onlining}
                  disabled={saving}
                  onClick={() => handleSubmit(true)}
                  className="rounded-lg px-8 bg-[#40A5EE]"
                >
                  上线
                </Button>
              )}
            </div>
          </div>
        ) : (
        <div className="text-center py-20 text-gray-500">插件智连不存在</div>
      )}
    </Modal>
  );
};

export default ConfigModal;
