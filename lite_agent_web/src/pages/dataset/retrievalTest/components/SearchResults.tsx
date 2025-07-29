import React from 'react';
import { Card, Typography } from 'antd';
import { SegmentVO } from '@/client';
import documentIcon from '@/assets/dataset/doc_svg';
interface SearchResultsProps {
  results: SegmentVO[];
}

const SearchResults: React.FC<SearchResultsProps> = ({ results }) => {
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
                        expandable: true,
                      }}
                    >
                      {result.content}
                    </Typography.Paragraph>
                  </div>
                  <div className="text-gray-400 text-sm flex items-center">
                      {result.tokenCount && (
                        <span>token: {result.tokenCount}</span>
                      )}
                      <span className="ml-4 flex items-center customeSvg">
                        <span className="mr-1 text-sm w-4 h-4">{documentIcon}</span>
                        {result.documentName || '链接文档'}
                      </span>
                    </div>
                </Card>
              );
            }
          })}
        </div>
      </div>
    </div>
  );
};

export default SearchResults;
