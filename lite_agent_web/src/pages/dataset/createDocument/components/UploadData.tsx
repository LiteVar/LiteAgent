import React, { useMemo, useState } from 'react';
import { Button, Space } from 'antd';
import ResponseCode from '@/constants/ResponseCode';
import { DocumentSourceType } from '@/types/dataset';
import { getAccessToken } from '@/utils/cache';
import { postV1DatasetByDatasetIdDocuments } from '@/client';

interface UploadDataProps {
  documentData: any;
  onPrev: () => void;
  onComplete?: () => void;
}

const UploadData: React.FC<UploadDataProps> = ({ documentData, onPrev, onComplete }) => {
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatus, setUploadStatus] = useState<'idle' | 'uploading' | 'success' | 'error'>('idle');

  const name = useMemo(() => {
    if (documentData.dataSourceType === DocumentSourceType.INPUT) {
      return documentData.name;
    } else if (documentData.dataSourceType === DocumentSourceType.FILE) {
      return documentData?.fileName?.replace(/\.[^.]+$/, '.md') ||  '未知文件名.md';
    } else if (documentData.dataSourceType === DocumentSourceType.HTML) {
      return '链接文档';
    } else {
      return '未命名文档';
    }
  }, [documentData]);

  const handleUpload = async () => {
    setUploadStatus('uploading');
    setUploadProgress(0);

    // 模拟进度条
    const interval = setInterval(() => {
      setUploadProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          return 99;
        }
        return Math.min(prev + 10, 100); // 每次增加10%
      });
    }, 100); // 每100毫秒更新一次进度

    try {
      let name = '';

      if (documentData.dataSourceType === DocumentSourceType.FILE) {
        name = documentData?.fileName?.replace(/\.[^.]+$/, '.md') ||  '未知文件名.md';
      } else if (documentData.dataSourceType === DocumentSourceType.INPUT) {
        name = documentData.name;
      }

      const res = await postV1DatasetByDatasetIdDocuments({
        headers: {
          'Workspace-id': documentData.workspaceId,
        },
        body: {
          name,
          workspaceId: documentData.workspaceId,
          dataSourceType: documentData.dataSourceType,
          chunkSize: documentData.chunkSize || 500,
          separator: documentData.separator || '',
          metadata: documentData.metadata || '',
          content: documentData.dataSourceType === DocumentSourceType.INPUT ?
            documentData.content : '',
          htmlUrl: documentData.dataSourceType === DocumentSourceType.HTML
            ? documentData.htmlUrl?.split('\n')?.filter((v: string) => !!v): [],
          fileId: documentData.dataSourceType === DocumentSourceType.FILE ?
            documentData.fileId : '',
        },
        path: {
          datasetId: documentData.datasetId,
        },
      });

      if (res?.data?.code === ResponseCode.S_OK) {
        clearInterval(interval); // 上传完成后清除进度条
        setUploadStatus('success');
        setUploadProgress(100);
        // 上传成功后清空 fileId
        if (onComplete) {
          onComplete();
        }
      } else {
        setUploadStatus('error');
      }
    } catch (error) {
      console.error('Validation failed:', error);
      setUploadStatus('error');
    }
  };

  return (
    <div className="flex flex-col w-full py-6">
      <div className="space-y-4">
        <div className="border border-solid border-gray-200 rounded-lg p-6 flex justify-between items-center">
          <span>{name}</span>
          {uploadStatus === 'idle' && <span className="text-blue-500">上传就绪</span>}
          {uploadStatus === 'uploading' && <span className="text-blue-500">正在上传 {uploadProgress}%</span>}
          {uploadStatus === 'success' && <span className="text-green-500">上传成功</span>}
          {uploadStatus === 'error' && <span className="text-red-500">上传失败</span>}
        </div>
      </div>

      <div className="mt-8 flex justify-center">
        <Space>
          {uploadStatus === 'idle' && (
            <>
              <Button size="large" onClick={onPrev}>
                上一步
              </Button>
              <Button type="primary" size="large" onClick={handleUpload}>
                开始上传
              </Button>
            </>
          )}
          {uploadStatus === 'success' && (
            <Button type="primary" size="large" onClick={() => window.history.back()}>
              完成
            </Button>
          )}
          {uploadStatus === 'error' && (
            <>
              <Button size="large" onClick={onPrev}>
                上一步
              </Button>
              <Button type="primary" size="large" danger onClick={handleUpload}>
                重新上传
              </Button>
            </>
          )}
        </Space>
      </div>
    </div>
  );
};

export default UploadData;
