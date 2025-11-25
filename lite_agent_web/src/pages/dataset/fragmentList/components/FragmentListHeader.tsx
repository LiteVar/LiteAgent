import React, { useCallback } from 'react';
import { Button, Input, Checkbox, Space, message } from 'antd';
import { PlusOutlined, SearchOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { getAccessToken } from '@/utils/cache';

interface FragmentListHeaderProps {
  total: number;
  canEdit: boolean;
  canDelete: boolean;
  onSearch: (value: string) => void;
  onCreateNew: () => void;
  selectedCount: number;
  onBatchDelete?: () => void;
  onSelectAll: (checked: boolean) => void;
  selectAll: boolean;
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
    total,
    canEdit,
    canDelete,
    onSearch,
    onCreateNew,
    selectedCount,
    onBatchDelete,
    onSelectAll,
    selectAll,
    onViewSummary,
    onUpdateSummary,
    isUpdateingSummary,
    showSummary,
    fileId
  } = props;

  const onDownloadMarkdown = useCallback((fileId: string) => {
    fetch(`/v1/file/dataset/markdown/download?fileId=${fileId}`, {
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
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <div className="flex items-center space-x-4 mb-6">
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} className="hover:bg-gray-100">
            返回
          </Button>
          <div className="h-4 w-[1px] bg-gray-200" />
          <h2 className="text-xl m-0">片段列表</h2>
        </div>
        {canEdit && (
          <Space>          
            <Button onClick={onViewSummary}>
              查看文档摘要
            </Button>        
            <Button 
              onClick={onUpdateSummary} 
              loading={isUpdateingSummary}
              disabled={!showSummary}
            >
              更新文档摘要
            </Button>
            {fileId && (
              <Button onClick={() => onDownloadMarkdown(fileId)}>
                下载Markdown文档
              </Button>
            )}
            <Button type="primary" icon={<PlusOutlined />} onClick={onCreateNew}>
              新建片段
            </Button>
          </Space>
        )}
      </div>

      <div className="flex justify-between items-center">
        <div className="flex items-center space-x-4">
          <Checkbox checked={selectAll} onChange={(e) => onSelectAll(e.target.checked)}>
            全选
          </Checkbox>
          <div className="h-4 w-[1px] bg-gray-200" />
          <span className="text-gray-500">共 {total} 个片段</span>
          {selectedCount > 0 && (
            <>
              <div className="h-4 w-[1px] bg-gray-200" />
              <span className="text-gray-500">
                已选择 {selectedCount} 项
                {canDelete && (
                  <Button danger onClick={onBatchDelete} className="ml-4">
                    批量删除
                  </Button>
                )}
              </span>
            </>
          )}
        </div>
        <Input.Search
          placeholder="搜索片段内容"
          onSearch={onSearch}
          enterButton={<SearchOutlined />}
          allowClear
          className="max-w-sm"
        />
      </div>
    </div>
  );
};

export default FragmentListHeader;
