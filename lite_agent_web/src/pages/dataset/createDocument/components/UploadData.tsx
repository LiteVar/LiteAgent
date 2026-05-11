import React, { useMemo, useState } from 'react';
import { Button, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import ResponseCode from '@/constants/ResponseCode';
import { DocumentSourceType } from '@/types/dataset';
import { postV1DatasetByDatasetIdDocuments } from '@/client';
import { normalizeSeparator } from '@/utils/normalizeSeparator';

type UploadDocumentData = {
  workspaceId: string;
  datasetId: string;
  dataSourceType: DocumentSourceType;
  name?: string;
  fileName?: string;
  fileId?: string;
  chunkSize?: number;
  separator?: string;
  metadata?: string;
  content?: string;
  htmlUrl?: string;
};

interface UploadDataProps {
  documentData: UploadDocumentData;
  onPrev: () => void;
  onComplete?: () => void;
}

const UploadData: React.FC<UploadDataProps> = ({ documentData, onPrev, onComplete }) => {
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatus, setUploadStatus] = useState<'idle' | 'uploading' | 'success' | 'error'>('idle');
  const navigate = useNavigate();
  const handleBackToList = () => {
    navigate(-1);
  };

  const name = useMemo(() => {
    if (documentData.dataSourceType === DocumentSourceType.INPUT) {
      return documentData.name || '未命名文档';
    } else if (documentData.dataSourceType === DocumentSourceType.FILE) {
      return documentData?.fileName?.replace(/\.[^.]+$/, '.md') ||  '未知文件名.md';
    } else if (documentData.dataSourceType === DocumentSourceType.HTML) {
      return documentData.name || '未命名文档';
    } else {
      return '未命名文档';
    }
  }, [documentData]);

  const handleUpload = async () => {
    setUploadStatus('uploading');
    setUploadProgress(0);

    // 模拟进度条（失败/成功都要清理 interval，避免继续跑）
    const interval = window.setInterval(() => {
      setUploadProgress((prev) => {
        if (prev >= 100) {
          window.clearInterval(interval);
          return 100;
        }
        return Math.min(prev + 10, 100); // 每次增加10%
      });
    }, 100); // 每100毫秒更新一次进度

    try {
      let name = '';

      if (documentData.dataSourceType === DocumentSourceType.FILE) {
        name = documentData?.fileName?.replace(/\.[^.]+$/, '.md') ||  '未知文件名.md';
      } else {
        name = documentData.name || '未命名文档';
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
          separator: normalizeSeparator(documentData.separator || '') || '',
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

      const code = res?.data?.code;
      const SUMMARY_FAILED_CODE = 20009;

      if (code === ResponseCode.S_OK || code === SUMMARY_FAILED_CODE) {
        window.clearInterval(interval); // 上传完成后清除进度条
        setUploadStatus('success');
        setUploadProgress(100);
        // 上传成功后清空 fileId
        if (onComplete) {
          onComplete();
        }

        if (code === SUMMARY_FAILED_CODE) {
          message.info('文件上传成功，但摘要更新失败，请稍后在文档列表重试', 5);
        }
      } else {
        message.error(res?.data?.message || '上传失败', 3);
        window.clearInterval(interval);
        setUploadStatus('error');
      }
    } catch (error) {
      console.error('Validation failed:', error);
      window.clearInterval(interval);
      setUploadStatus('error');
    }
  };

  const statusText = (() => {
    if (uploadStatus === 'idle') return '上传就绪';
    if (uploadStatus === 'uploading') return `正在上传 ${uploadProgress}%`;
    if (uploadStatus === 'success') return '上传成功';
    return '上传失败';
  })();

  const statusColorClass = (() => {
    if (uploadStatus === 'idle') return 'text-[#40A5EE]';
    if (uploadStatus === 'uploading') return 'text-[#40A5EE]';
    if (uploadStatus === 'success') return 'text-[#52c41a]';
    return 'text-[#ff4d4f]';
  })();

  return (
    <div className="min-h-0 h-[93%] bg-white rounded-2xl p-6 flex flex-col">
      <div className="flex-1 min-h-0">
        <div className=" bg-white border border-solid border-[#E0E3E6] rounded-xl overflow-hidden">
          <div className="flex items-center justify-between px-6 py-4">
            <div className="text-sm text-[#383F44] font-normal">{name}</div>
            <div className={`text-sm ${statusColorClass}`}>{statusText}</div>
          </div>
        </div>
      </div>

      <div className="mt-auto flex justify-end gap-4">
        {(uploadStatus === 'idle' || uploadStatus === 'uploading') && (
          <>
            <Button
              size="large"
              onClick={onPrev}
              disabled={uploadStatus === 'uploading'}
              className="rounded-xl border-[#E0E3E6] text-gray-600 hover:!text-blue-500 hover:!border-blue-500 px-8"
            >
              上一步
            </Button>
            <Button
              type="primary"
              size="large"
              onClick={handleUpload}
              disabled={uploadStatus !== 'idle'}
              className="rounded-xl bg-[#40A5EE] hover:!bg-[#40A5EE]/90 border-none shadow-md shadow-blue-200/50 px-12 font-bold"
            >
              开始上传
            </Button>
          </>
        )}
        {uploadStatus === 'success' && (
          <Button
            type="primary"
            size="large"
            onClick={handleBackToList}
            className="rounded-xl bg-[#40A5EE] border-none shadow-md px-12 font-bold"
          >
            返回文档列表
          </Button>
        )}
        {uploadStatus === 'error' && (
          <>
            <Button
              size="large"
              onClick={onPrev}
              className="rounded-xl border-[#E0E3E6] text-gray-600 hover:!text-blue-500 hover:!border-blue-500 px-8"
            >
              上一步
            </Button>
            <Button
              type="primary"
              size="large"
              danger
              onClick={handleUpload}
              className="rounded-xl bg-[#40A5EE] hover:!bg-[#40A5EE]/90 border-none shadow-md shadow-red-200 px-12 font-bold"
            >
              重新上传
            </Button>
          </>
        )}
      </div>
    </div>
  );
};

export default UploadData;
