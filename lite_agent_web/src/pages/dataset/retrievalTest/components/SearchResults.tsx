import React, { useCallback, useState } from 'react';
import { Card, message, Typography } from 'antd';
import { getV1FileDatasetMarkdownPreview, SegmentVO } from '@/client';
import documentIcon from '@/assets/dataset/doc_svg';
import ResponseCode from '@/constants/ResponseCode';
import PreviewSourceModal from './PreviewSourceModal';
interface SearchResultsProps {
  results: SegmentVO[];
}

const SearchResults: React.FC<SearchResultsProps> = ({ results }) => {

  const [previewModalVisible, setPreviewModalVisible] = useState(false);
  const [previewMarkdown, setPreviewMarkdown] = useState('');
  const [previewTitle, setPreviewTitle] = useState('');

  const handleReadSourceFile = async (result: SegmentVO) => {
    const res = await getV1FileDatasetMarkdownPreview({
      query: {
        fileId: result.fileId!,
      },
    });
    if (res.data?.code === ResponseCode.S_OK && res.data.data) {
      setPreviewMarkdown(res.data.data);
      setPreviewTitle(result.documentName || '链接文档');
      setPreviewModalVisible(true);
    } else {
      message.error(res.data?.message || '获取文件预览失败');
    }
  };

  const handleDownloadSourceFile = useCallback((fileId: string) => {
    fetch(`/v1/file/dataset/file/download?fileId=${fileId}`, {
      method: 'GET',
      headers: {
        Accept: 'application/zip,application/octet-stream',
        Authorization: `Bearer ${localStorage.getItem('access_token')}`,
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
  }, []);

  if (!results.length) {
    return <div className="text-center text-gray-500 py-20">检索结果在此处显示</div>;
  }

  return (
    <div className="flex flex-col h-[calc(100vh-20rem)]">
      <div className="flex-1 overflow-auto pr-2">
        <div className="space-y-4">
          {results.map((result, index) => {
            if (result.id === '000') {
              return (
                <Card key={index} className="border-gray-200 bg-gray-50">
                  <div className="mb-2 overflow-auto">{result.content}</div>
                </Card>
              );
            } else {
              return (
                <Card key={index} className="border-gray-200 bg-gray-50">
                  <div className="flex justify-between items-center mb-3">
                    <div className="flex items-center gap-3 text-blue-400 font-medium">
                      <span>#{index + 1}</span> |
                      {result.score && <span>关联度: {result.score?.toFixed(2)}</span>}
                    </div>
                  </div>
                  <div className="mb-2 overflow-auto">
                    <Typography.Paragraph
                      ellipsis={{
                        rows: 3,
                        expandable: 'collapsible',
                      }}
                    >
                      {result.content}
                    </Typography.Paragraph>
                  </div>
                  <div className="text-gray-400 text-sm flex items-center">
                      {result.tokenCount && (
                        <span>token: {result.tokenCount}</span>
                      )}
                      {!!result.fileId && <span className="ml-4 flex items-center customeSvg">
                        <span className="mr-1 text-sm w-4 h-4">{documentIcon}</span>
                        {result.documentName || '链接文档'}
                      </span>}
                      {!!result.fileId && <span className="ml-4 text-blue-400 cursor-pointer" onClick={() => handleReadSourceFile(result)}>查看原文</span>}
                      {!!result.fileId && <span className="ml-4 text-blue-400 cursor-pointer" onClick={() => handleDownloadSourceFile(result.fileId!)}>下载源文件</span>}
                    </div>
                </Card>
              );
            }
          })}
        </div>
      </div>
      <PreviewSourceModal
        open={previewModalVisible}
        markdown={previewMarkdown}
        onCancel={() => setPreviewModalVisible(false)}
        title={previewTitle}
      />
    </div>
  );
};

export default SearchResults;
