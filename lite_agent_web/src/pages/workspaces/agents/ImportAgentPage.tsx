import React, { useEffect } from 'react';
import { Upload, Button, Table, Typography, Card, Progress, Space, Dropdown, MenuProps } from 'antd';
import { InboxOutlined, FileZipOutlined, CheckCircleOutlined, ExclamationCircleOutlined, LoadingOutlined, DownOutlined, CloseCircleOutlined } from '@ant-design/icons';
import ModelFormModal from '../models/components/ModelFormModal';
import { ModelVOAddAction, ModelVOUpdateAction } from '@/client';
import { useImportAgent } from './hook/useImportAgent';
import { useBlocker } from 'react-router-dom';

const { Dragger } = Upload;
const { Text } = Typography;

interface ImportAgentPageProps {
  onBack: () => void;
  onSuccess: () => void;
}

// usePrompt hook for Data Router
function usePrompt(when: boolean, message: string) {
  const blocker = useBlocker(when);

  useEffect(() => {
    if (blocker.state === "blocked") {
      const confirm = window.confirm(message);
      if (confirm) {
        blocker.proceed();
      } else {
        blocker.reset();
      }
    }
  }, [blocker, message]);
}

const ImportAgentPage: React.FC<ImportAgentPageProps> = ({ onSuccess, onBack }) => {

  const {
    // 状态
    currentStep,
    uploadedFile,
    isProcessing,
    steps,
    isFormModalVisible,
    editingModel,
    importData,
    hasApiKeyWarning,
    processingMessages,
    importError,
    isCreated,
    
    // 方法
    handleFileUpload,
    showFormModal,
    closeFormModal,
    handleSaveModel,
    handleNext,
    handlePrev,
    handleCancel,
    onChangeFile,
    draggerRef,
    updateImportData
  } = useImportAgent(onSuccess, onBack);

  // 拦截离开 - 使用 usePrompt hook
  usePrompt((currentStep > 0 && currentStep < 6) || (currentStep === 6 && !isCreated && !importError), "你有未保存的更改，确定要离开吗？");

  const items: MenuProps['items'] = [
    {
      key: 'insert',
      label: '新建',
      danger: false,
    },
    {
      key: 'update',
      label: '覆盖',
      danger: false,
    },
    {
      key: 'skip',
      label: '跳过',
      danger: false,
    },
  ];

  // 渲染步骤内容
  const renderStepContent = () => {
    switch (currentStep) {
      case 0: // 导入文件
        return (
          <div className="space-y-4">
            <div className='pb-5 '>请选择要导入的智能体文件，上传到此处</div>
            <Card className={`border-2 border-dashed border-gray-300 ${uploadedFile ? 'visible' : 'hidden h-0'}`}>
              <div className="flex items-center space-x-3">
                <FileZipOutlined className="text-2xl text-gray-400" />
                <div className="flex-1">
                  <div className='text-base font-bold'>{uploadedFile?.name}</div>
                </div>
                <Button type="link" onClick={onChangeFile}>
                  更换文件
                </Button>
              </div>
            </Card>

            <Dragger
              ref={draggerRef}
              accept=".agent"
              beforeUpload={handleFileUpload}
              showUploadList={false}
              multiple={false}
              className={`border-2 mt-5 border-gray-300 ${uploadedFile ? 'hidden h-0' : 'visible'}`}
            >
              <p className="ant-upload-drag-icon">
                <InboxOutlined className="text-4xl text-blue-500" />
              </p>
              <p className="ant-upload-text text-blue-500">
                拖拽文件或者点击此区域进行上传
              </p>
              <p className="ant-upload-hint">
                支持上传文档格式：agent格式文档
              </p>
            </Dragger>
          </div>
        );

      case 1: // 解析文件配置
        return (
          <div className="space-y-4">
            <Text className="text-lg font-semibold">解析文件配置</Text>
            <div className="space-y-2">
              {processingMessages.map((msg, index) => (
                <div key={index} className="text-sm">
                  {msg}
                </div>
              ))}
            </div>
          </div>
        );

      case 2: // 大模型配置
        const hasSameModel = importData?.modelMap && Object.values(importData.modelMap).some((model: any) => !!model.similarId);
        return (
          <div className="space-y-4">
            <Text className="text-lg font-semibold">大模型列表</Text>

            {hasSameModel && (
              <div className="bg-yellow-50 border border-yellow-200 rounded p-3">
                <div className="flex items-start space-x-2">
                  <ExclamationCircleOutlined className="text-yellow-600 mt-0.5" />
                  <div>
                    <Text className="text-yellow-800">
                      【注意】平台上已存在相同的模型配置。当前默认导入方式为 创建新模型，您可切换至 覆盖现有配置。
                    </Text>
                  </div>
                </div>
              </div>
            )}
            
            {importData?.modelMap && (
              <Table
                dataSource={Object.values(importData.modelMap).map((model, index) => ({
                  ...model,
                  key: index,
                }))}
                columns={[
                  { title: '模型名称', dataIndex: 'name', key: 'name' },
                  { title: '模型别名', dataIndex: 'alias', key: 'alias' },
                  { title: '模型类型', dataIndex: 'type', key: 'type' },
                  { title: 'URL', dataIndex: 'baseUrl', key: 'baseUrl' },
                  {
                    title: 'API Key', 
                    dataIndex: 'apiKey', 
                    key: 'apiKey', 
                    render: (key: string | undefined) => key ? `${key.substring(0, 8)}...` : '未配置' 
                  },
                  {
                    title: '平台状态',
                    key: 'platformStatus', 
                    render: (_: any, record: any, index: number) => (
                      <div>
                        {!record?.similarId && <span>-</span>}
                        {!!record?.similarId && (<div className='flex items-center'>
                          <div>已有该模型</div>
                          <Dropdown
                            menu={{ items, onClick: (e) => {
                              const modelKeys = Object.keys(importData.modelMap);
                              const targetKey = modelKeys[index];
                              updateImportData('modelMap', e.key === 'insert' ? 0 : e.key === 'update' ? 1 : 2, targetKey);
                            } }} 
                            trigger={['click']}
                          >
                            <a className='ml-2' onClick={(e) => e.preventDefault()}>
                                {record?.operate == 0 ? '新建' : record?.operate === 1 ? '覆盖' : '跳过'}
                                <DownOutlined className='ml-1' />
                            </a>
                          </Dropdown>
                        </div>)}
                      </div>
                    ) 
                  },
                  {
                    title: '操作', 
                    key: 'action', 
                    render: (_: any, record: any, index: number) => (
                      <Button type="link" onClick={(e) => showFormModal(e, record, index)}>编辑</Button>
                    ) 
                  },
                ]}
                pagination={false}
                size="small"
              />
            )}

            {/* 大模型API Key警告 */}
            {(hasApiKeyWarning && importData?.modelMap && Object.values(importData.modelMap).length > 0) && (
              <div className="bg-yellow-50 border border-yellow-200 rounded p-3">
                <div className="flex items-start space-x-2">
                  <ExclamationCircleOutlined className="text-yellow-600 mt-0.5" />
                  <div>
                    <Text className="text-yellow-800">
                      【注意】如果大模型中未包含API Key，大模型将无法启用，可点击编辑进行配置或者在智能体创建完成后，前往设置→模型管理，手动补充您的授权密钥。
                    </Text>
                  </div>
                </div>
              </div>
            )}
          </div>
        );

      case 3: // 工具配置
        const hasSameTool = importData?.toolMap && Object.values(importData.toolMap).some((tool: any) => !!tool.similarId);
        return (
          <div className="space-y-4">
            <Text className="text-lg font-semibold">工具列表</Text>

            {hasSameTool && (
              <div className="bg-yellow-50 border border-yellow-200 rounded p-3">
                <div className="flex items-start space-x-2">
                  <ExclamationCircleOutlined className="text-yellow-600 mt-0.5" />
                  <div>
                    <Text className="text-yellow-800">
                      【注意】平台上已存在相同的工具配置。当前默认导入方式为 创建新工具，您可切换至 覆盖现有配置。
                    </Text>
                  </div>
                </div>
              </div>
            )}

            {importData?.toolMap && (
              <Table
                dataSource={Object.values(importData.toolMap).map((tool, index) => ({
                  ...tool,
                  key: index,
                }))}
                columns={[
                  { title: '工具名称', dataIndex: 'name', key: 'name' },
                  {
                    title: '平台状态',
                    key: 'platformStatus', 
                    render: (_: any, record: any, index: number) => (
                      <div>
                        {!record?.similarId && <span>-</span>}
                        {!!record?.similarId && (<div className='flex items-center'>
                          <div>已有该工具</div>
                          <Dropdown
                            menu={{ items, onClick: (e) => {
                              const toolKeys = Object.keys(importData.toolMap);
                              const targetKey = toolKeys[index];
                              updateImportData('toolMap', e.key === 'insert' ? 0 : e.key === 'update' ? 1 : 2, targetKey);
                            } }} 
                            trigger={['click']}
                          >
                            <a className='ml-2' onClick={(e) => e.preventDefault()}>
                                {record?.operate == 0 ? '新建' : record?.operate === 1 ? '覆盖' : '跳过'}
                                <DownOutlined className='ml-1' />
                            </a>
                          </Dropdown>
                        </div>)}
                      </div>
                    ) 
                  },
                ]}
                pagination={false}
                size="small"
              />
            )}

            {(hasApiKeyWarning && importData?.toolMap && Object.values(importData.toolMap).length > 0) && <div className="bg-yellow-50 border border-yellow-200 rounded p-3">
                <div className="flex items-start space-x-2">
                  <ExclamationCircleOutlined className="text-yellow-600 mt-0.5" />
                  <div>
                    <Text className="text-yellow-800">
                      【注意】如果外部工具中未包含API Key，可能无法启用，在智能体创建完成后，前往设置→工具管理，手动补充您的授权密钥。
                    </Text>
                  </div>
                </div>
              </div>}
          </div>
        );

      case 4: // 知识库配置
        const hasSameKnowledgeBase = importData?.knowledgeBaseMap && Object.values(importData.knowledgeBaseMap).some((kb: any) => !!kb.similarId);
        return (
          <div className="space-y-4">
            <Text className="text-lg font-semibold">知识库列表</Text>

            {hasSameKnowledgeBase && (
              <div className="bg-yellow-50 border border-yellow-200 rounded p-3">
                <div className="flex items-start space-x-2">
                  <ExclamationCircleOutlined className="text-yellow-600 mt-0.5" />
                  <div>
                    <Text className="text-yellow-800">
                      【注意】平台上已存在相同的知识库配置。当前默认导入方式为 创建新知识库，您可切换至 覆盖现有配置。
                    </Text>
                  </div>
                </div>
              </div>
            )}
            
            {importData?.knowledgeBaseMap && (
              <Table
                dataSource={Object.entries(importData.knowledgeBaseMap).map(([id, kb], index) => ({
                  key: index,
                  id,
                  name: kb.metadata.name,
                  description: kb.metadata.description,
                  fileCount: Object.keys(kb.documents).length,
                  embeddingModelId: kb.metadata.embeddingModelId,
                  operate: kb.operate,
                  similarId: kb.similarId,
                }))}
                columns={[
                  { title: '知识库名称', dataIndex: 'name', key: 'name' },
                  { title: '描述', dataIndex: 'description', key: 'description' },
                  { title: '文件数量', dataIndex: 'fileCount', key: 'fileCount', render: (count) => `${count}个` },
                  { title: '向量化模型', dataIndex: 'embeddingModelId', key: 'embeddingModelId', render: (id) => importData?.modelMap[id]?.alias || '未配置' },
                  {
                    title: '平台状态',
                    key: 'platformStatus', 
                    render: (_: any, record: any, index: number) => (
                      <div>
                        {!record?.similarId && <span>-</span>}
                        {!!record?.similarId && (<div className='flex items-center'>
                          <div>已有该知识库</div>
                          <Dropdown
                            menu={{ items, onClick: (e) => {
                              const knowledgeBaseKeys = Object.keys(importData.knowledgeBaseMap);
                              const targetKey = knowledgeBaseKeys[index];
                              updateImportData('knowledgeBaseMap', e.key === 'insert' ? 0 : e.key === 'update' ? 1 : 2, targetKey);
                            } }} 
                            trigger={['click']}
                          >
                            <a className='ml-2' onClick={(e) => e.preventDefault()}>
                                {record?.operate == 0 ? '新建' : record?.operate === 1 ? '覆盖' : '跳过'}
                                <DownOutlined className='ml-1' />
                            </a>
                          </Dropdown>
                        </div>)}
                      </div>
                    ) 
                  },
                ]}
                pagination={false}
                size="small"
              />
            )}

            {(hasApiKeyWarning && importData?.knowledgeBaseMap && Object.values(importData.knowledgeBaseMap).length > 0) && <div className="mt-5 bg-yellow-50 border border-yellow-200 rounded p-3">
              <div className="flex items-start space-x-2">
                <ExclamationCircleOutlined className="text-yellow-600 mt-0.5" />
                <div>
                  <Text className="text-yellow-800">
                    【注意】如果在大模型配置中，embadding模型未配置，则知识库创建后，文件将无法进行向量化，您可以在智能体创建后，在知识库管理-embadding设置中选择一个模型进行向量化设置
                  </Text>
                </div>
              </div>
            </div>}

          </div>
        );

      case 5: // 智能体配置
        const hasSamAgent = ((importData?.mainAgent && importData.mainAgent.similarId) || (importData?.subAgentMap && Object.values(importData.subAgentMap).some((subAgent: any) => !!subAgent.similarId)));
        return (
          <div className="space-y-4">
            <Text className="text-lg font-semibold">智能体列表</Text>

            {hasSamAgent && (
              <div className="bg-yellow-50 border border-yellow-200 rounded p-3">
                <div className="flex items-start space-x-2">
                  <ExclamationCircleOutlined className="text-yellow-600 mt-0.5" />
                  <div>
                    <Text className="text-yellow-800">
                      【注意】平台上已存在相同的智能体配置。当前默认导入方式为 创建新智能体，您可切换至 覆盖现有配置。
                    </Text>
                  </div>
                </div>
              </div>
            )}
            
            {importData && (
              <div className="space-y-4">
                {/* 主智能体 */}
                <Text className="font-semibold">主智能体</Text>
                <div className="p-4 bg-blue-50 rounded border">
                  <div className="flex items-center space-x-2 mb-2">
                    <Text className="font-semibold text-blue-800">{importData.mainAgent.name}</Text>
                    <Text className="text-sm text-blue-600">({importData.mainAgent.type})</Text>
                  </div>
                  <div className="space-y-1">
                    <div><strong>描述：</strong>{importData.mainAgent.description || '无'}</div>
                    <div><strong>提示词：</strong>{importData.mainAgent.prompt || '无'}</div>
                    <div><strong>模型：</strong>{importData.modelMap[importData.mainAgent.modelId]?.alias || '未配置'}</div>
                    <div><strong>子智能体数量：</strong>{importData.mainAgent.subAgentIds?.length || 0}个</div>
                    <div><strong>工具数量：</strong>{importData.mainAgent.functionList?.length || 0}个</div>
                    <div><strong>知识库数量：</strong>{importData.mainAgent.knowledgeBaseIds?.length || 0}个</div>
                    <div>
                      {!importData.mainAgent?.similarId && <span>-</span>}
                      {!!importData.mainAgent?.similarId && (<div className='flex items-center'>
                      <div>已有该智能体</div>
                      <Dropdown
                        menu={{ items, onClick: (e) => {
                          updateImportData('mainAgent', e.key === 'insert' ? 0 : e.key === 'update' ? 1 : 2);
                        } }} 
                        trigger={['click']}
                      >
                        <a className='ml-2' onClick={(e) => e.preventDefault()}>
                            {importData.mainAgent?.operate == 0 ? '新建' : importData.mainAgent?.operate === 1 ? '覆盖' : '跳过'}
                            <DownOutlined className='ml-1' />
                        </a>
                      </Dropdown>
                      </div>)}
                    </div>
                  </div>
                </div>

                {/* 子智能体 */}
                {importData.mainAgent.subAgentIds && importData.mainAgent.subAgentIds.length > 0 && (
                  <div className="space-y-2">
                    <Text className="font-semibold">子智能体</Text>
                    {Object.values(importData.subAgentMap).map((subAgent, index) => {
                      const subAgentKeys = Object.keys(importData.subAgentMap);
                      const targetKey = subAgentKeys[index];
                      return (
                        <div key={subAgent.id} className="p-3 bg-gray-50 rounded border">
                          <div className="flex items-center space-x-2 mb-1">
                            <Text className="font-medium">{subAgent.name}</Text>
                            <Text className="text-sm text-gray-600">({subAgent.type})</Text>
                          </div>
                          <div className="space-y-1 text-sm">
                            <div><strong>描述：</strong>{subAgent.description || '无'}</div>
                            <div><strong>提示词：</strong>{subAgent.prompt || '无'}</div>
                            <div><strong>模型：</strong>{importData.modelMap[subAgent.modelId]?.alias || '未配置'}</div>
                            <div>
                              {!subAgent?.similarId && <span>-</span>}
                              {!!subAgent?.similarId && (<div className='flex items-center'>
                              <div>已有该智能体</div>
                              <Dropdown
                                menu={{ items, onClick: (e) => {
                                  updateImportData('subAgentMap', e.key === 'insert' ? 0 : e.key === 'update' ? 1 : 2, targetKey);
                                } }} 
                                trigger={['click']}
                              >
                                <a className='ml-2' onClick={(e) => e.preventDefault()}>
                                    {subAgent?.operate == 0 ? '新建' : subAgent?.operate === 1 ? '覆盖' : '跳过'}
                                    <DownOutlined className='ml-1' />
                                </a>
                              </Dropdown>
                              </div>)}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            )}
          </div>
        );

      case 6: // 创建配置
        return (
          <div className="space-y-4">
            <Text className="text-lg font-semibold">创建配置</Text>
            
            <div className="space-y-2">
              {processingMessages.map((msg, index) => (
                <div key={index} className={`text-sm ${(msg.includes('失败') || msg.includes('错误')) ? 'text-red-600' : ''}`}>
                  {msg}
                </div>
              ))}
            </div>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 顶部导航 */}
      <div className="bg-white border-b px-6 py-4">
        <div className="flex items-center space-x-4">
          <Text className="text-xl font-semibold">导入智能体</Text>
        </div>
      </div>

      <div className="flex h-[calc(100vh-80px)]">
        {/* 左侧步骤导航 */}
        <div className="w-64 bg-white border-r p-6">
          <div className="space-y-2">
            {steps.map((step, index) => (
              <div
                key={step.key}
                className={`p-3 rounded text-sm ${
                  index === currentStep
                    ? 'bg-gray-100 text-blue-600'
                    : step.status === 'completed'
                    ? 'text-green-600'
                    : 'text-gray-500'
                }`}
              >
                <div className="flex items-center space-x-2">
                  {step.status === 'completed' && <CheckCircleOutlined />}
                  {step.status === 'processing' && <LoadingOutlined />}
                  {step.status === 'error' && <CloseCircleOutlined className="text-red-600" />}
                  <span>{step.title}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 右侧内容区域 */}
        <div className="flex-1 p-6 flex flex-col">
          <div className="bg-white rounded-lg p-6 overflow-y-auto flex-1">
            {renderStepContent()}
          </div>
          {/* 底部按钮 */}
          <div className="bg-white border-t px-6 py-4 flex-none">
            <div className="flex justify-end space-x-2">
              {(currentStep > 0 && currentStep <= 6 && !isCreated) && (
                <Button 
                  onClick={handlePrev} 
                  disabled={isProcessing}
                  className="flex items-center"
                >
                  上一步
                </Button>
              )}
              {currentStep === 0 && (
                <Button 
                  onClick={handleCancel} 
                  disabled={isProcessing}
                  className="flex items-center"
                >
                  取消
                </Button>
              )}
              {(!importError || currentStep !== 6) && <Button
                type="primary"
                onClick={handleNext}
                disabled={isProcessing}
                className="flex items-center"
              >
                {currentStep === 6 ? isCreated ? '完成' : '开始' : '下一步'}
              </Button>}
              {importError && currentStep === 6 && <Button
                type="primary"
                onClick={onSuccess}
                disabled={isProcessing}
                className="flex items-center"
              >
                关闭
              </Button>}
            </div>
          </div>
        </div>
      </div>

      <ModelFormModal
        visible={isFormModalVisible}
        disabledModelRule={true}
        onCancel={closeFormModal}
        onOk={(values) => handleSaveModel(values as ModelVOUpdateAction)}
        initialData={editingModel}
      />
    </div>
  );
};

export default ImportAgentPage;
