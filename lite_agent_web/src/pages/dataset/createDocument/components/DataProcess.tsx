import React, { useState, useCallback, useEffect, useMemo, useRef } from 'react';
import { Form, InputNumber, Input, Button, Space, Typography, Spin, Empty, message } from 'antd';
import { postV1DatasetDocumentsSplitMutation } from '@/client/@tanstack/query.gen';
import { getV1FileDatasetMarkdownProgress, getV1FileDatasetMarkdownPreview } from '@/client';
import { useMutation } from '@tanstack/react-query';
import { LinkOutlined } from '@ant-design/icons';
import { DocumentSourceType } from '@/types/dataset';
import PreviewSourceModal from '../../retrievalTest/components/PreviewSourceModal';

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
      return await getV1FileDatasetMarkdownPreview({
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
        separator: formValues.separator || '',
        metadata: formValues.metadata || '',
        name: documentData.dataSourceType === DocumentSourceType.INPUT ? documentData.name : '',
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
      
      const res = await getV1FileDatasetMarkdownProgress({
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
    <div className="flex gap-8 h-full">
      <div className="flex-1" style={{ borderRight: '1px solid #f0f0f0' }}>
        <Typography.Title level={5}>数据处理参数设置</Typography.Title>
        {renderConversionStatus()}
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            separator: documentData.separator || DefaultSplitSet.separator,
            chunkSize: documentData.chunkSize || DefaultSplitSet.chunkSize,
            metadata: documentData.metadata || DefaultSplitSet.metadata,
          }}
          labelAlign="left"
          className="mt-8 mr-8"
        >
          <Form.Item name="chunkSize" label="片段长度" className="mb-4">
            <InputNumber min={50} className="w-full" placeholder="请输入片段长度" />
          </Form.Item>
          <Form.Item name="separator" label="分隔符" className="mb-4">
            <Input className="whitespace-pre-wrap" placeholder="请输入分隔符" />
          </Form.Item>
          <Form.Item name="metadata" label="metadata" className="mb-0">
            <Input.TextArea placeholder="可以输入 JSON 格式的 metadata" rows={4} />
          </Form.Item>
        </Form>
        <Space className="w-full text-center mt-8 justify-between">
          <div>
            <Button
              type="primary"
              size="large"
              onClick={loadPreview}
              disabled={documentData.dataSourceType === 'HTML' || isFileConverting}
            >
              预览块
            </Button>
            <Button
              type="default"
              size="large"
              onClick={resetForm}
              className="ml-4"
              disabled={documentData.dataSourceType === 'HTML'}
            >
              重置
            </Button>
          </div>
          <div className="mr-8">
            <Button onClick={onPrev} size="large">
              上一步
            </Button>
            <Button 
              type="primary" 
              onClick={handleSubmit} 
              size="large" 
              className="ml-4"
              disabled={isFileConverting}
            >
              下一步
            </Button>
          </div>
        </Space>
      </div>

      <div className="w-1/2">
        <Typography.Title level={5}>
          预览数据<span className="text-gray-400">（至多5个片段）</span>
        </Typography.Title>
        {documentData.dataSourceType === 'HTML' && (
          <div className="mt-8 rounded-lg overflow-y-auto h-15 bg-[#f5f5f5] p-4 flex items-start">
            <LinkOutlined className="text-blue-400 text-xl mt-1" />
            <div className="ml-4">
              <div className="text-lg">{documentData.htmlUrl}</div>
              <Typography.Text>链接内容不支持预览</Typography.Text>
            </div>
          </div>
        )}
        {documentData.dataSourceType !== 'HTML' && (
          <div className="mt-8 rounded-lg overflow-y-auto h-80">
            {loading ? (
              <div className="flex items-center justify-center h-full">
                <Spin tip="加载预览数据中..." />
              </div>
            ) : previewData.length > 0 ? (
              previewData.slice(0, 5).map((chunk, index) => (
                <div key={index} className="bg-gray-50 p-4 mb-4 rounded">
                  <div className="text-gray-400 mb-2">#{index + 1}</div>
                  <div>{chunk || '无内容'}</div>
                </div>
              ))
            ) : (
              <Empty description="暂无预览数据，请点击左侧的“预览块”按钮来加载预览" />
            )}
          </div>
        )}
      </div>

      {previewModalVisible && (
        <PreviewSourceModal
          open={previewModalVisible}
          markdown={convertedContent}
          onCancel={() => setPreviewModalVisible(false)}
          title={'查看文档'}
        />
      )}
    </div>
  );
};

export default DataProcess;
