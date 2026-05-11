import React, { useCallback, useState } from 'react';
import { Button, message, Typography } from 'antd';
import { getV2FileMarkdownPreview, SegmentVO  } from '@/client';
import DocIcon from '@/assets/dataset/doc_svg';
import ResponseCode from '@/constants/ResponseCode';
import PreviewSourceModal from './PreviewSourceModal';
import noSearchImg from '@/assets/dataset/no-search.png';
interface SearchResultsProps {
  results: SegmentVO[];
}

const SearchResults: React.FC<SearchResultsProps> = ({ results }) => {

  const [previewModalVisible, setPreviewModalVisible] = useState(false);
  const [previewMarkdown, setPreviewMarkdown] = useState('');
  const [previewTitle, setPreviewTitle] = useState('');

  const handleReadSourceFile = async (result: SegmentVO) => {
    const res = await getV2FileMarkdownPreview({
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
    fetch(`/v2/file/download?fileId=${fileId}`, {
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
    return (
      <div className="flex flex-col items-center h-full py-20">
        <div className="mt-[16%]">
          <img src={noSearchImg} className="w-[180px]" />
        </div>
        <div className="text-[#1D4A6B]">没有找到相关搜索结果</div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <div className="flex-1 max-h-[80vh] overflow-auto px-6 py-4 custom-scrollbar">
        <div className="space-y-4">
          {results.map((result, index) => {
            if (result.id === '000') {
              return (
                <div key={result.id} className="p-4 rounded-xl bg-gray-50/50 border border-black/5 text-gray-500 italic">
                  {result.content}
                </div>
              );
            } else {
              return (
                <div key={result.id} className="p-5 rounded-2xl bg-white/40 border border-white/60 shadow-sm hover:bg-white/60 transition-all group">
                  <div className="flex justify-between items-start mb-4">
                    <div className="flex items-center gap-2 text-xs font-bold text-[#40A5EE]">
                      <span>#{index + 1}</span>
                      {result.score !== undefined && (
                        <>
                          <span>|</span>
                          <span>关联度: {result.score.toFixed(2)}</span>
                        </>
                      )}
                    </div>
                  </div>
                  
                  <div className="mb-4">
                    <Typography.Paragraph
                      className="!text-[#383F44] !text-sm !leading-relaxed !mb-0"
                      ellipsis={{
                        rows: 4,
                        expandable: 'collapsible',
                        symbol: (expanded: boolean) => (
                          <span className="text-blue-500 ml-1 hover:underline cursor-pointer">
                            {expanded ? '收起' : '展开'}
                          </span>
                        ),
                      }}
                    >
                      {result.content}
                    </Typography.Paragraph>
                  </div>

                  {result.tokenCount && (
                    <div className="text-xs text-gray-400 mb-4">
                      token: {result.tokenCount}
                    </div>
                  )}

                  <div className="pt-4 border-t border-black/5 flex items-center justify-between">
                    <div className="flex items-center gap-2 max-w-[60%]">
                      <div className="w-5 h-5 flex items-center justify-center bg-blue-50 rounded text-[#40A5EE] [&_svg]:w-3 [&_svg]:h-3">
                        <DocIcon />
                      </div>
                      <span className="text-xs text-gray-500 truncate font-medium">
                        {result.documentName || '链接文档'}
                      </span>
                    </div>
                    
                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      {!!result.fileId && (
                        <>
                          <Button 
                            type="text" 
                            size="small" 
                            className="!text-blue-500 !text-[11px] hover:!bg-blue-50 font-medium px-2"
                            onClick={() => handleReadSourceFile(result)}
                          >
                            查看原文
                          </Button>
                          <Button 
                            type="text" 
                            size="small" 
                            className="!text-blue-500 !text-[11px] hover:!bg-blue-50 font-medium px-2"
                            onClick={() => handleDownloadSourceFile(result.fileId!)}
                          >
                            下载
                          </Button>
                        </>
                      )}
                    </div>
                  </div>
                </div>
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
