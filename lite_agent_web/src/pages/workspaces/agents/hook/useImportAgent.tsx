import { useState, useCallback, useRef, useEffect } from 'react';
import { message } from 'antd';
import { useWorkspace } from '@/contexts/workspaceContext';
import ResponseCode from '@/constants/ResponseCode';
import { Agent, KnowledgeBaseMetadata, ModelVOAddAction, ModelVOUpdateAction, postV1AgentImportByToken, ToolVO } from '@/client';
import { EventSourceMessage, fetchEventSource } from '@microsoft/fetch-event-source';
export interface ImportStep {
  key: string;
  title: string;
  status: 'pending' | 'processing' | 'completed' | 'error';
}

export enum ProcessType {
  END = 'end',
  ERROR = 'error',
  MESSAGE = 'message',
}

export interface ImportData {
  mainAgent: Agent;
  subAgentMap: Record<string, Agent>;
  modelMap: Record<string, ModelVOAddAction>;
  toolMap: Record<string, ToolVO>;
  knowledgeBaseMap: Record<string, KnowledgeBaseMetadata>;
  tempDir: string;
  token: string;
}

interface ModelWithKey extends ModelVOAddAction {
  key?: number;
}

export const stepMessages = {
  parse: [
    '【任务】正在解压压缩包...',
    '【完成】压缩包解压完成',
    '【任务】正在解析Json文件配置信息...',
    '【完成】配置信息解析完成。',
    '【注意】请点击下一步，进入配置信息确认。'
  ],
  model: [],
  tool: [],
  knowledge: [],
  agent: [],
  create: [] as string[]
};

export const useImportAgent = (onSuccess: () => void, onBack: () => void) => {
  const workspace = useWorkspace();
  const [currentStep, setCurrentStep] = useState(0);
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [isCreated, setIsCreated] = useState(false);
  const [importError, setImportError] = useState(false);
  const [steps, setSteps] = useState<ImportStep[]>([
    { key: 'file', title: '导入文件', status: 'pending' },
    { key: 'parse', title: '解析文件配置', status: 'pending' },
    { key: 'model', title: '大模型配置', status: 'pending' },
    { key: 'tool', title: '工具配置', status: 'pending' },
    { key: 'knowledge', title: '知识库配置', status: 'pending' },
    { key: 'agent', title: '智能体配置', status: 'pending' },
    { key: 'create', title: '创建配置', status: 'pending' },
  ]);

  // 模型表单弹窗
  const [isFormModalVisible, setIsFormModalVisible] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelWithKey | undefined>(undefined);
  
  // 配置数据状态
  const [importData, setImportData] = useState<ImportData | null>(null);
  const [hasApiKeyWarning, setHasApiKeyWarning] = useState(false);
  const [processingMessages, setProcessingMessages] = useState<string[]>([]);
  const [connecting, setConnecting] = useState(false);
  const errorRetryCountRef = useRef(0);
  const processTipEnableRef = useRef(true);
  
  const processingIntervalRef = useRef<NodeJS.Timeout | null>(null);
  // SSE 消息队列和定时器
  const messageQueueRef = useRef<string[]>([]);
  const sseMessageIntervalRef = useRef<NodeJS.Timeout | null>(null);

  const draggerRef = useRef<any>(null);
  const controllerRef = useRef<AbortController>(new AbortController());

  useEffect(() => {
    const controller = controllerRef.current;
    return () => {
      if (controller) {
        controller.abort();
      }
      // 清理所有定时器
      if (processingIntervalRef.current) {
        clearInterval(processingIntervalRef.current);
      }
      if (sseMessageIntervalRef.current) {
        clearInterval(sseMessageIntervalRef.current);
      }
    };
  }, []);

  const onChangeFile = () => {
    if (draggerRef.current) {
      // ✅ 手动触发 Dragger 内部的文件选择
      const input = draggerRef.current?.upload?.uploader?.fileInput;
      if (input) input.click();
    }
  };

  // 处理文件上传
  const handleFileUpload = useCallback(async (file: File) => {
    if (!file.name.toLowerCase().endsWith('.agent')) {
      message.error('不支持该文件格式，请上传agent格式文件');
      return false;
    }

    setUploadedFile(file);
    
    return false; // 阻止默认上传
  }, []);

  const showFormModal = (e: React.MouseEvent, model: ModelVOAddAction, index: number) => {
    e.stopPropagation();
    const modelWithKey: ModelWithKey = { ...model, key: index };
    setEditingModel(modelWithKey);
    setIsFormModalVisible(true);
  };

  const closeFormModal = () => {
    setIsFormModalVisible(false);
    setEditingModel(undefined);
  };

  const handleSaveModel = useCallback(
    async (values: ModelVOUpdateAction) => {
      if (!importData || !editingModel || editingModel.key === undefined) return;
      
      // 找到要修改的模型在map中的key
      const modelKeys = Object.keys(importData.modelMap);
      const targetKey = modelKeys[editingModel.key];
      
      if (targetKey) {
        // 创建新的modelMap，保持原有的key-value结构
        const newModelMap = {
          ...importData.modelMap,
          [targetKey]: {
            ...importData.modelMap[targetKey],
            ...values,
          }
        };
        
        setImportData(prev => {
          if (!prev) return null;
          return {
            ...prev,
            modelMap: newModelMap,
          };
        });

        setImportError(false);
        
        message.success('模型配置已更新');
      }
      
      setTimeout(() => {
        closeFormModal();
      }, 100);
    },
    [editingModel, importData]
  );

  // 模拟处理过程
  const simulateProcessing = useCallback((stepIndex: number, stepName: string) => {
    const messages = stepMessages[stepName as keyof typeof stepMessages] || [];
    let messageIndex = 0;
    
    setProcessingMessages([]);
    
    const interval = setInterval(() => {
      messageIndex++;
      if (messageIndex < messages.length + 1) {
        setProcessingMessages(prev => {
          return [...prev, messages[messageIndex - 1]];
        });
      } else {
        clearInterval(interval);
        // 更新步骤状态为完成
        setSteps(prev => prev.map((step, index) => 
          index === stepIndex ? { ...step, status: 'completed' } : step
        ));
        setIsProcessing(false);
        if (stepName === 'create') {
          setIsCreated(true);
        }
      }
    }, 1000);

    processingIntervalRef.current = interval;
  }, []);

  // 解析导入数据
  const parseImportData = useCallback((data: ImportData) => {
    setImportData(data);
    setImportError(false);
    // 检查是否有API Key警告
    const hasApiKey = Object.values(data.modelMap).some(model => model.apiKey && model.apiKey.trim() !== '{{<APIKEY>}}');
    setHasApiKeyWarning(!hasApiKey);
  }, []);

  // 开始导入流程
  const startImportProcess = useCallback(async () => {
    if (!uploadedFile) return;

    setIsProcessing(true);

    try {
      const formData = new FormData();
      formData.append('file', uploadedFile);

      const response = await fetch('/v1/agent/import/preview', {
        method: 'POST',
        body: formData,
        headers: {
          'Workspace-id': workspace?.id || '',
          Authorization: `Bearer ${localStorage.getItem('access_token')}`,
        },
      });

      if (!response.ok) {
        throw new Error('上传失败');
      }

      const result = await response.json();

      if (result.code === ResponseCode.S_OK) {
        stepMessages.create = [];
        parseImportData(result.data);
        setCurrentStep(1);
        setSteps(prev => prev.map((step, index) =>
          index === 0 ? { ...step, status: 'completed' } : index === 1 ? { ...step, status: 'processing' } : step
        ));
        simulateProcessing(1, 'parse');
      } else {
        message.error(result.message || '上传失败');
        throw new Error(result.message || '上传失败');
      }
    } catch (error) {
      console.error('Import error:', error);
      message.error('上传失败');
      setIsProcessing(false);
    }
  }, [uploadedFile, workspace, parseImportData, simulateProcessing]);

  // 启动 SSE 消息的定时显示
  const startSSEMessageDisplay = useCallback(() => {
    // 清理之前的定时器
    if (sseMessageIntervalRef.current) {
      clearInterval(sseMessageIntervalRef.current);
    }
    
    // 清空队列和显示的消息
    messageQueueRef.current = [];
    setProcessingMessages([]);
    
    // 启动定时器，每秒显示一条消息
    sseMessageIntervalRef.current = setInterval(() => {
      if (messageQueueRef.current.length > 0) {
        const nextMessage = messageQueueRef.current.shift();
        if (nextMessage) {
          setProcessingMessages(prev => [...prev, nextMessage]);
          // 如果是完成消息，显示后更新状态并清理定时器
          if (nextMessage === '【完成】所有智能体创建完成') {
            setTimeout(() => {
              setSteps(prev => prev.map((step) => {
                return { ...step, status: 'completed' };
              }));
              setIsCreated(true);
              setIsProcessing(false);
              controllerRef.current.abort();
              // 清理定时器
              if (sseMessageIntervalRef.current) {
                clearInterval(sseMessageIntervalRef.current);
                sseMessageIntervalRef.current = null;
              }
            }, 100); // 短暂延迟确保消息已渲染
          }
          if (nextMessage.includes('失败') || nextMessage.includes('错误')) {
            setIsProcessing(false);
            setImportError(true);
            setSteps(prev => prev.map((step, index) => 
              index === prev.length - 1 ? { ...step, status: 'error' } : step
            ));
          }
        } else if (messageQueueRef.current.length === processingMessages.length && !connecting) {
          setIsProcessing(false);
          setImportError(true);
          setProcessingMessages(prev => [...prev, '【错误】连接超时']);
          if (sseMessageIntervalRef.current) {
            clearInterval(sseMessageIntervalRef.current);
          }
        }
      }
    }, 1000);
  }, [connecting, processingMessages]);

  const enqueueSSERequest = (token: string) => {
    const id = Date.now().toString();
    processTipEnableRef.current = true;
    // 开始定时显示消息
    startSSEMessageDisplay();
    setConnecting(true);
    return fetchEventSource(`/v1/agent/import/progress/${token}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Connection: 'keep-alive',
        'Workspace-id': workspace?.id || '',
        Authorization: `Bearer ${localStorage.getItem('access_token')}`,
      },
      openWhenHidden: true,
      signal: controllerRef.current.signal,
      onmessage: (e) => onmessage(e, id),
      onopen: async (response) => {
        console.log('onopen', response);
        if (response.status === 200) {
          setSteps(prev => prev.map((step, index) =>
            index === 6 ? { ...step, status: 'processing' } : step
          ));
          importAgent();
        } else if (response.status !== 200) {
          setIsProcessing(false);
          setConnecting(false);
          console.log('request fail---', response.status);
          response.text()?.then(async (data) => {
            console.log('data----', data);
            if (data && typeof data === 'string') {
              processTipEnableRef.current = false;
              const responseData = JSON.parse(data);
              console.log('response data', responseData);
              message.error(responseData?.message || responseData?.data || '获取进度失败');
            } else {
              message.error('获取进度失败');
            }
          });
        }
      },
      onerror(err) {
        setConnecting(false);
        console.error('Error:', err);
        errorRetryCountRef.current = errorRetryCountRef.current + 1;
        if (errorRetryCountRef.current > 1) {
          setTimeout(() => {
            if (processTipEnableRef.current) {
              message.error('获取进度失败');
            }
          }, 200);
          errorRetryCountRef.current = 0;
          throw err;
        }
      },
      onclose() {
        console.log('onclose----');
        setConnecting(false);
      }
    })
  }

  const onmessage = (event: EventSourceMessage, id: string) => {
    switch (event.event) {
      case ProcessType.MESSAGE:
        handleMessageEvent(event.data, id);
        break;
      default:
        handleMessageEvent(event.data, id);
        break;
    }
  }

  const handleMessageEvent = (message: string, id: string) => {
    // 格式化消息
    const formattedMessage = message === '开始导入工作' ? '【任务】开始导入工作' : message;
    
    // 将消息加入队列，而不是直接显示
    // 定时器会每秒从队列中取出一条消息显示
    messageQueueRef.current.push(formattedMessage);
  }

  const importAgent = async () => {
    if (!importData) return;
    await postV1AgentImportByToken({
      body: importData,
      path: {
        token: importData.token || '',
      },
      headers: { 'Workspace-id': workspace?.id || '' },
    });
  }
  
  // 下一步
  const handleNext = async () => {
    if (currentStep === 0 && !uploadedFile) {
      message.warning('请先上传文件');
      return;
    }

    if (currentStep > 1 && currentStep < 6) {
      setSteps(prev => prev.map((step, index) => 
        index === currentStep ? { ...step, status: 'completed' } : step
      ));
    }

    if (currentStep === 5) {
      setImportError(false);
      setIsCreated(false);
      setSteps(prev => prev.map((step, index) => 
        index === 6 ? { ...step, status: 'pending' } : step
      ));
    }

    if (currentStep === 0) {
      startImportProcess();
    } else if (currentStep < 6) {
      setProcessingMessages([]);
      setCurrentStep(prev => prev + 1);
    } else if (currentStep === 6 && !isCreated) {
      if (!importData) return;
      setIsProcessing(true);
      enqueueSSERequest(importData.token);
    } else if (currentStep === 6 && isCreated) {
      // 完成导入
      message.success('智能体导入完成');
      onSuccess();
    }
  };

  // 上一步
  const handlePrev = useCallback(() => {
    // 如果当前步骤是2，则把processingMessages切换为parse
    if (currentStep === 2) {
      setProcessingMessages(stepMessages.parse);
    }
    if (currentStep > 0) {
      setCurrentStep(prev => prev - 1);
    }
  }, [currentStep]);

  // 取消导入
  const handleCancel = useCallback(() => {
    onBack();
  }, [onBack]);

  const updateImportData = useCallback((type: 'modelMap' | 'toolMap' | 'knowledgeBaseMap' | 'subAgentMap' | 'mainAgent', operate: number, id?: string) => {
    setImportData(prev => {
      if (!prev) return null;
      if (type === 'mainAgent') {
        return {
          ...prev,
          mainAgent: {
            ...prev.mainAgent,
            operate: operate,
          }
        }
      } else {
        if (!id) return prev;
        return {
          ...prev,
          [type]: {
            ...prev[type],
            [id]: {
              ...prev[type][id],
              operate: operate,
            }
          }
        }
      }
      
    });
  }, []);

  return {
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
    simulateProcessing,
    startImportProcess,
    handleNext,
    handlePrev,
    handleCancel,
    
    // 设置方法
    setCurrentStep,
    setSteps,
    setIsProcessing,
    setProcessingMessages,
    onChangeFile, 
    draggerRef,
    updateImportData,
  };
};
