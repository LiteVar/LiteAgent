import React, { useState, useMemo, useCallback, useEffect, useRef } from 'react';
import SelectSource from './components/SelectSource';
import DataProcess from './components/DataProcess';
import UploadData from './components/UploadData';
import Header from './components/Header';
import { DatasetDocument, deleteV1FileById } from '@/client';
import { useDatasetContext } from '@/contexts/datasetContext';
import { DocumentSourceType } from '@/types/dataset';
import { message } from 'antd';

const CreateDocument = () => {
  const { workspaceId } = useDatasetContext();
  const datasetId = useMemo(() => {
    const path = window.location.pathname;
    return path.split('/')[3];
  }, []);

  const [currentStep, setCurrentStep] = useState(0);
  const [documentData, setDocumentData] = useState<DatasetDocument | null>({
    workspaceId,
    datasetId,
    dataSourceType: DocumentSourceType.FILE,
  });

  // 使用 ref 保存最新的 fileId，避免闭包问题
  const fileIdRef = useRef<string | undefined>();
  // 追踪上传的fileId，用于判断文件是否改变
  const uploadedFileIdRef = useRef<string | undefined>();

  // 文件上传处理函数
  const handleFileUpload = useCallback(async (file: File): Promise<{ fileId: string; fileName: string }> => {
    // 如果存在旧的 fileId，先删除
    if (documentData?.fileId) {
      try {
        await deleteV1FileById({ path: { id: documentData.fileId } });
      } catch (error) {
        console.error('删除旧文件失败:', error);
      }
    }

    // 上传新文件
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await fetch(`/v1/file/dataset/upload?datasetId=${datasetId}`, {
      method: 'POST',
      body: formData,
      headers: {
        Authorization: `Bearer ${localStorage.getItem('access_token')}`, 
      },
    });

    if (!response.ok) {
      message.error('上传失败');
      throw new Error('上传失败');
    }

    const result = await response.json();

    if (result.code === 30014) {
      message.warning(result.message || '文件已存在');
      throw new Error(result.message || '文件已存在');
    }

    const fileId = result.data;

    if (fileId) {
      // 更新 documentData
      const updatedData = {
        ...documentData,
        fileId,
        fileName: file.name,
      };
      setDocumentData(updatedData);
      uploadedFileIdRef.current = fileId;
      message.success('文件上传成功');
      return { fileId, fileName: file.name };
    } else {
      message.error('未获取到文件ID');
      throw new Error('未获取到文件ID');
    }
  }, [documentData, datasetId]);

  const onDataProcessPrev = useCallback(async () => { 
    // 不再自动删除文件，因为用户可能没有重新选择文件
    setCurrentStep(0);
  }, []);

  // 新增：专门用于返回按钮的清理函数
  const handleBackWithCleanup = useCallback(async () => {
    if (documentData?.fileId) {
      try {
        await deleteV1FileById({ path: { id: documentData.fileId } });
        setDocumentData(prev => prev ? { ...prev, fileId: undefined } : null);
        uploadedFileIdRef.current = undefined;
      } catch (error) {
        console.error('删除文件失败:', error);
      }
    }
  }, [documentData]);

  // 流程完成后清空 fileId
  const handleComplete = useCallback(() => {
    setDocumentData(prev => prev ? { ...prev, fileId: undefined } : null);
    fileIdRef.current = undefined;
    uploadedFileIdRef.current = undefined;
  }, []);

  const steps = [
    {
      title: '选择文档/文本',
      content: (
        <SelectSource
          documentData={documentData}
          setDocumentData={setDocumentData}
          onNext={() => setCurrentStep(1)}
          onFileUpload={handleFileUpload}
        />
      ),
    },
    {
      title: '数据处理',
      content: (
        <DataProcess
          documentData={documentData}
          setDocumentData={setDocumentData}
          onPrev={onDataProcessPrev}
          onNext={() => setCurrentStep(2)}
        />
      ),
    },
    {
      title: '上传数据',
      content: <UploadData 
        documentData={documentData} 
        onPrev={() => setCurrentStep(1)} 
        onComplete={handleComplete}
      />,
    },
  ];

  // 每次 documentData 变化时，更新 ref
  useEffect(() => {
    fileIdRef.current = documentData?.fileId;
  }, [documentData?.fileId]);

  // 组件卸载时清理已上传的文件
  useEffect(() => {
    return () => {
      // 组件卸载时，如果存在 fileId，删除文件
      if (fileIdRef.current) {
        deleteV1FileById({ path: { id: fileIdRef.current } })
          .catch(error => {
            console.error('组件卸载时删除文件失败:', error);
          });
      }
    };
  }, []); 

  return (
    <div className="flex flex-col h-full p-2 pt-4">
      <Header 
        currentStep={currentStep} 
        steps={steps} 
        documentData={documentData} 
        onBackWithCleanup={handleBackWithCleanup} 
      />
      <div className="flex-1 px-8 border-0 border-t border-t-gray-200 border-solid">
        <div className="steps-content h-full">{steps[currentStep].content}</div>
      </div>
    </div>
  );
};

export default CreateDocument;
