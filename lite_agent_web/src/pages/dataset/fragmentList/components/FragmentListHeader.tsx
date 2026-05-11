import React, { useCallback } from 'react';
import { Button, message } from 'antd';
import { PlusOutlined, LeftOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { getAccessToken } from '@/utils/cache';

interface FragmentListHeaderProps {
  canEdit: boolean;
  canDelete: boolean;
  onCreateNew: () => void;
  onViewSummary: () => void;
  onUpdateSummary: () => void;
  isUpdateingSummary?: boolean;
  showSummary?: boolean;
  fileId?: string;
}

const FragmentListHeader: React.FC<FragmentListHeaderProps> = (props) => {
  const navigate = useNavigate();
  const token = getAccessToken();
  const {
    canEdit,
    onCreateNew,
    onViewSummary,
    onUpdateSummary,
    isUpdateingSummary,
    showSummary,
    fileId
  } = props;

  const onDownloadMarkdown = useCallback((fileId: string) => {
    fetch(`/v2/file/download/markdown?fileId=${fileId}`, {
      method: 'GET',
      headers: {
        Accept: 'application/zip,application/octet-stream',
        Authorization: `Bearer ${token}`,
      },
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`下载失败: ${response.status}`);
        }

        // 获取并解析文件名
        const disposition = response.headers.get('content-disposition');
        let fileName = `${fileId}.zip`; // 默认文件名

        if (disposition) {
          // 处理 filename*=UTF-8''... 格式
          const filenameMatch = disposition.match(/filename\*=UTF-8''(.+)$/i);
          if (filenameMatch && filenameMatch[1]) {
            fileName = decodeURIComponent(filenameMatch[1]);
          } else {
            // 处理普通 filename=... 格式
            const normalMatch = disposition.match(/filename=["']?([^"']+)["']?/i);
            if (normalMatch && normalMatch[1]) {
              fileName = normalMatch[1];
            }
          }
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      })
      .catch((error) => {
        console.error('下载文件时出错:', error);
        message.error('文件下载失败，请稍后重试');
      });
  }, [token]);
  return (
    <div className="flex flex-col gap-6">
      <div className="flex justify-between items-center">
        <div className="flex items-center gap-4">
          <div 
            onClick={() => navigate(-1)}
            className="w-9 h-9 flex items-center justify-center rounded-xl bg-white/80 border border-white/60 shadow-sm hover:bg-white cursor-pointer transition-all"
          >
            <LeftOutlined className="text-gray-600" />
          </div>
          <p className="text-xl m-0">片段列表</p>
        </div>
        
        {canEdit && (
          <div className="flex items-center gap-3">
            <Button 
              className="rounded-xl bg-transparent text-[#40A5EE] border-[#40A5EE] flex items-center h-10"
              onClick={onViewSummary}
            >
              查看文档摘要
            </Button>        
            <Button 
              className="rounded-xl bg-transparent text-[#40A5EE] border-[#40A5EE] flex items-center h-10"
              onClick={onUpdateSummary} 
              loading={isUpdateingSummary}
              disabled={!showSummary}
            >
              更新文档摘要
            </Button>
            {fileId && (
              <Button 
                className="rounded-xl bg-transparent text-[#40A5EE] border-[#40A5EE] flex items-center h-10"
                onClick={() => onDownloadMarkdown(fileId)}
              >
                下载Markdown
              </Button>
            )}
            <Button 
              type="primary" 
              size="large"
              className="rounded-xl bg-[#40A5EE] hover:!bg-[#40A5EE]/90 border-none shadow-md shadow-blue-200/50 flex items-center gap-2 h-10"
              icon={<PlusOutlined />} 
              onClick={onCreateNew}
            >
              新建片段
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};

export default FragmentListHeader;
