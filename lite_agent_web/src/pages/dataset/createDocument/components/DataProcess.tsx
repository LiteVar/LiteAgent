import React, { useState, useCallback, useEffect, useMemo, useRef } from 'react';
import { Form, InputNumber, Input, Button, Typography, Spin, Empty, message } from 'antd';
import { postV1DatasetDocumentsSplitMutation } from '@/client/@tanstack/query.gen';
import { getV2FileMarkdownProgress, getV2FileMarkdownPreview } from '@/client';
import { useMutation } from '@tanstack/react-query';
import { LinkOutlined } from '@ant-design/icons';
import { DocumentSourceType } from '@/types/dataset';
import PreviewSourceModal from '../../retrievalTest/components/PreviewSourceModal';
import noPreviewImg from '@/assets/dataset/no-preview.png';
import { normalizeSeparator } from '@/utils/normalizeSeparator';

interface DataProcessProps {
  documentData: any;
  setDocumentData: (data: any) => void;
  onPrev: () => void;
  onNext: () => void;
}

const DefaultSplitSet = {
  chunkSize: 500,
  separator: '\\n\\n',
  metadata: '',
};

type ConversionStatus = 'PENDING' | 'CONVERTING' | 'COMPLETED' | 'FAILED';

const DataProcess: React.FC<DataProcessProps> = ({ documentData, setDocumentData, onPrev, onNext }) => {
  const [form] = Form.useForm();
  const [previewData, setPreviewData] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [convertedContent, setConvertedContent] = useState<string>('');
  const [conversionStatus, setConversionStatus] = useState<ConversionStatus>('PENDING');
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewModalVisible, setPreviewModalVisible] = useState(false);
  const [conversionProgress, setConversionProgress] = useState<number>(0);
  const pollIntervalRef = useRef<number | null>(null);

  const isFileConverting = useMemo(() => {
    return documentData.dataSourceType === DocumentSourceType.FILE && conversionStatus !== 'COMPLETED';
  }, [documentData.dataSourceType, conversionStatus]);

  const { mutate: splitDocument } = useMutation({
    ...postV1DatasetDocumentsSplitMutation,
    onSuccess: (data) => {
      setPreviewData(data.data || []);
      setLoading(false);
    },
    onError: (error) => {
      console.error('数据处理失败:', error);
      message.error('数据处理失败');
      setLoading(false);
    },
  });

  const { mutate: previewDocument } = useMutation({
    mutationFn: async (params: { fileId: string }) => {
      return await getV2FileMarkdownPreview({
        query: { fileId: params.fileId },
      });
    },
    onSuccess: (res) => {
      const text = res.data?.data || '';
      setConvertedContent(text);
      setDocumentData((prev: any) => ({
        ...prev,
        content: text,
      }));
      setPreviewLoading(false);
      setPreviewModalVisible(true);
    },
    onError: (err) => {
      console.error('获取转换后预览失败', err);
      message.error('获取转换后预览失败');
      setPreviewLoading(false);
    },
  });

  const loadPreview = () => {
    setLoading(true);
    const formValues = form.getFieldsValue();
    splitDocument({
      path: {
        documentId: documentData.id,
      },
      headers: {
        'Workspace-id': documentData.workspaceId,
      },
      body: {
        workspaceId: documentData.workspaceId,
        dataSourceType: documentData.dataSourceType,
        chunkSize: formValues.chunkSize || 500,
        separator: normalizeSeparator(formValues.separator) || '',
        metadata: formValues.metadata || '',
        name: documentData.dataSourceType != DocumentSourceType.FILE ? documentData.name : '',
        content: documentData.dataSourceType === DocumentSourceType.INPUT ? documentData.content : '',
        fileId: documentData.dataSourceType === DocumentSourceType.FILE ? documentData.fileId : '',
        htmlUrl:
          documentData.dataSourceType === DocumentSourceType.HTML
            ? documentData.htmlUrl?.split('\n')?.filter((v: string) => !!v)
            : [],
      },
    });
  };

  const resetForm = () => {
    form.resetFields();
    setPreviewData([]);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const metadata = form.getFieldValue('metadata');
      // 验证 metadata 是否为合法的 JSON 格式
      if (metadata) {
        try {
          JSON.parse(metadata);
        } catch (error) {
          message.error('metadata 必须是合法的 JSON');
          return;
        }
      }

      setDocumentData({
        ...documentData,
        ...values,
      });
      onNext();
    } catch (error) {
      console.error('验证失败:', error);
    }
  };

  // 修改转换进度查询函数
  const fetchFileConversionProgress = useCallback(async (fileId: string) => {
    try {
      
      const res = await getV2FileMarkdownProgress({
        query: { fileId }
      });

      const status = res.data?.data?.status || 'PENDING';
      const progress = res.data?.data?.progress || 0;
      setConversionProgress(progress); // 每次轮询都更新进度

      if (status === 'COMPLETED') {
        setConversionStatus('COMPLETED');
        setConvertedContent(res.data?.data?.detail || '');
        setDocumentData((prev: any) => ({
          ...prev,
          content: res.data?.data?.detail || '',
        }));
      } else if (status === 'FAILED') {
        setConversionStatus('FAILED');
        message.error('文件转换失败');
      } else {
        setConversionStatus('CONVERTING');
      }

      return status as string | undefined;
    } catch (err) {
      setConversionStatus('FAILED');
      message.error('获取转换进度失败');
    }
  }, []);

  const startFileConversionPolling = useCallback((fileId: string) => {
    if (pollIntervalRef.current !== null) return;

    setConversionStatus('CONVERTING');
      
    pollIntervalRef.current = window.setInterval(async () => {
      const status = await fetchFileConversionProgress(fileId);

      if (status === 'COMPLETED' || status === 'FAILED') {
        if (pollIntervalRef.current !== null) {
          window.clearInterval(pollIntervalRef.current);
          pollIntervalRef.current = null;
        }
      }
    }, 1000 * 2); // 每2秒轮询一次
  }, []);

   // 添加文件转换状态展示组件
  const renderConversionStatus = () => {
    if (documentData.dataSourceType !== DocumentSourceType.FILE) {
      return null;
    }

    const statusConfig = {
      PENDING: {
        color: '#999',
        text: '等待转换',
      },
      CONVERTING: {
        color: '#1890ff',
        text: `正在转换文件格式（${conversionProgress}%）`,
      },
      COMPLETED: {
        color: '#000',
        text: `${documentData?.fileName || '文件'} 已经转换为 markdown 格式文档`,
      },
      FAILED: {
        color: '#ff4d4f',
        text: '文件转换失败',
      },
    };

    return (
      <div className="mb-6 mr-8 p-4 bg-gray-50 rounded">
        <div className="flex items-center">
          <div 
            className="w-2 h-2 rounded-full mr-2" 
            style={{ backgroundColor: statusConfig[conversionStatus].color }}
          />
          <Typography.Text style={{ color: statusConfig[conversionStatus].color }}>
            {statusConfig[conversionStatus].text}
          </Typography.Text>
          {conversionStatus === 'COMPLETED' && convertedContent && (
            <Button
              type="link"
              onClick={() => {
                setPreviewLoading(true);
                previewDocument({ fileId: documentData.fileId });
              }}
              loading={previewLoading}
            >
              查看文档
            </Button>
          )}
          {conversionStatus === 'FAILED' && (
            <Button 
              type="link" 
              onClick={() => fetchFileConversionProgress(documentData.fileId)}
            >
              重新转换
            </Button>
          )}
        </div>
      </div>
    );
  };

  useEffect(() => {
    // 如果 fileId 变了，允许重新触发
    if (!documentData.fileId) {
      return;
    }

    if (
      documentData.dataSourceType === DocumentSourceType.FILE &&
      documentData.fileId &&
      !convertedContent 
    ) {
      startFileConversionPolling(documentData.fileId);
    }
  }, [
    documentData.dataSourceType, 
    documentData.fileId, 
    convertedContent, 
  ]);

  useEffect(() => {
    return () => {
      if (pollIntervalRef.current !== null) {
        window.clearInterval(pollIntervalRef.current);
        pollIntervalRef.current = null;
      }
    };
  }, []);

  return (
    <div className="flex flex-col md:flex-row gap-8 h-full">
      {/* Left Panel - Settings */}
      <div className="flex-1 flex flex-col min-w-0 p-4 mr-4 border-r border-white/40 bg-white rounded-2xl">
        <p className="text-lg mt-0 mb-6 flex items-center gap-2">
          数据处理参数设置
        </p>
        
        <div className="flex-1 overflow-y-auto pr-4 custom-scrollbar">
          {renderConversionStatus()}
          
          <div className="bg-white/40 backdrop-blur-sm rounded-2xl border border-white/60 shadow-sm">
            <Form
              form={form}
              layout="vertical"
              initialValues={{
                separator: documentData.separator || DefaultSplitSet.separator,
                chunkSize: documentData.chunkSize || DefaultSplitSet.chunkSize,
                metadata: documentData.metadata || DefaultSplitSet.metadata,
              }}
              className="customForm"
            >
              <Form.Item name="chunkSize" label={<span className="text-gray-600 font-medium">片段长度 :</span>}>
                <InputNumber 
                  min={50} 
                  className="w-full !rounded-xl !border-[#E0E3E6] !bg-white/80 !h-11 flex items-center" 
                  placeholder="请输入片段长度" 
                />
              </Form.Item>
              
              <Form.Item name="separator" label={<span className="text-gray-600 font-medium">分隔符 :</span>}>
                <Input 
                  className="!rounded-xl !border-[#E0E3E6] !bg-white/80 !h-11 focus:!bg-white transition-all whitespace-pre-wrap" 
                  placeholder="请输入分隔符" 
                />
              </Form.Item>
              
              <Form.Item name="metadata" label={<span className="text-gray-600 font-medium">Metadata (JSON) :</span>} className="!mb-0">
                <Input.TextArea 
                  placeholder="可以输入 JSON 格式的 metadata" 
                  rows={4} 
                  className="!rounded-xl !border-[#E0E3E6] !bg-white/80 focus:!bg-white transition-all !p-3"
                />
              </Form.Item>
            </Form>
          </div>
          
          {/* 放到同一个滚动容器里：让按钮栏紧贴文本域而不是被顶到底部 */}
          <div className="mt-4 pt-4 border-t border-black/5 flex items-center justify-between">
            <div className="flex gap-3">
              <Button
                type="primary"
                size="large"
                onClick={loadPreview}
                disabled={documentData.dataSourceType === 'HTML' || isFileConverting}
                className="rounded-xl bg-white text-[#40A5EE] border-[#40A5EE] px-6"
              >
                预览块
              </Button>
              <Button
                size="large"
                onClick={resetForm}
                disabled={documentData.dataSourceType === 'HTML'}
                className="rounded-xl border-[#E0E3E6] text-gray-600 hover:!text-blue-500 hover:!border-blue-500 px-6"
              >
                重置
              </Button>
            </div>
            
            <div className="flex gap-3">
              <Button 
                onClick={onPrev} 
                size="large"
                className="rounded-xl border-[#E0E3E6] text-gray-600 hover:!text-blue-500 hover:!border-blue-500 px-6"
              >
                上一步
              </Button>
              <Button 
                type="primary" 
                onClick={handleSubmit} 
                size="large" 
                disabled={isFileConverting}
                className="rounded-xl bg-[#40A5EE] text-white border-[#40A5EE] px-6"
              >
                下一步
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Right Panel - Preview */}
      <div className="w-full md:w-[50%] flex flex-col min-w-0 bg-white p-4 rounded-2xl">
        <p className="text-lg mt-0 mb-6 flex items-center gap-2">
          预览数据（至多5个片段）
        </p>

        <div className="flex-1 min-h-[400px] bg-white/40 backdrop-blur-sm rounded-2xl border border-white/60 shadow-sm overflow-hidden flex flex-col">
          {documentData.dataSourceType === 'HTML' ? (
            <div className="p-8 flex flex-col items-center justify-center h-full text-center">
              <div className="w-16 h-16 bg-blue-50 rounded-2xl flex items-center justify-center text-blue-500 text-2xl mb-4">
                <LinkOutlined />
              </div>
              <div className="text-base font-bold text-gray-700 mb-2 truncate w-full px-4">{documentData.htmlUrl}</div>
              <p className="text-sm text-gray-400">网页链接内容目前不支持实时预览</p>
            </div>
          ) : (
            <div className="flex-1 overflow-y-auto custom-scrollbar">
              {loading ? (
                <div className="flex flex-col items-center justify-center h-full gap-4">
                  <Spin size="large" />
                  <span className="text-sm text-gray-400">正在切分数据...</span>
                </div>
              ) : previewData.length > 0 ? (
                <div className="space-y-4">
                  {previewData.slice(0, 5).map((chunk, index) => (
                    <div key={index} className="p-4 rounded-xl bg-[#F2F3F5] border border-white shadow-sm animate-in fade-in slide-in-from-right-4" style={{ animationDelay: `${index * 50}ms` }}>
                      <div className="text-[12px] mb-2 w-fit text-[#383F44]">
                        #{index + 1}
                      </div>
                      <div className="line-clamp-4 break-all text-sm text-[#383F44] leading-relaxed break-words">
                        {chunk || '无内容'}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center h-full opacity-40">
                  <img src={noPreviewImg} alt="no preview" className="w-[160px] h-[160px] object-contain" />
                  <span className="text-sm text-gray-400">暂无预览数据，请点击左侧的“预览块”按钮来加载预览</span>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {previewModalVisible && (
        <PreviewSourceModal
          open={previewModalVisible}
          markdown={convertedContent}
          onCancel={() => setPreviewModalVisible(false)}
          title={'预览原始文档'}
        />
      )}
    </div>
  );
};

export default DataProcess;
