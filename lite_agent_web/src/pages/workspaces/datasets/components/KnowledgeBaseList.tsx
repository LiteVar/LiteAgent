import React from 'react';
import { List, Card, Typography, Tooltip, Dropdown, Empty, Tag } from 'antd';
import { EllipsisOutlined } from '@ant-design/icons';
import DatasetIcon from '@/assets/dataset/dataset-logo.svg';
import { DatasetsVO, Dataset } from '@/client';
import emptyImage from '@/assets/dataset/no_knowledge_base.png';
import { PaginationConfig } from 'antd/es/pagination';
import { buildImageUrl } from '@/utils/buildImageUrl';

const { Text, Title } = Typography;

interface KnowledgeBaseListProps {
  searchTerm?: string;
  knowledgeBases: DatasetsVO[];
  pagination: PaginationConfig;
  setPagination: (pagination: PaginationConfig) => void;
  total: number;
  onOpen: (id: string) => void;
  onDelete: (id: string) => void;
}

const formatWordCount = (count: number): string => {
  if (count < 1000) return count.toString();
  if (count < 1000000) return `${(count / 1000).toFixed(1)}k`;
  return `${(count / 1000000).toFixed(1)}m`;
};

const KnowledgeBaseList: React.FC<KnowledgeBaseListProps> = (props) => {
  const { knowledgeBases, pagination, setPagination, total, onOpen, onDelete, searchTerm } = props;
  const menuItems = (item: Dataset) => [
    {
      key: 'open',
      label: '打开',
      onClick: (e: any) => {
        e.domEvent.stopPropagation();
        onOpen(item.id!);
      },
    },
    ...(item.canDelete
      ? [
          {
            key: 'delete',
            label: '删除',
            danger: true,
            onClick: (e: any) => {
              e.domEvent.stopPropagation();
              onDelete(item.id!);
            },
          },
        ]
      : []),
  ];

  return (
    <div className="px-4 pb-8">
      <List
        className="[&_.ant-col]:h-full"
        grid={{
          gutter: 8,
          xs: 1,
          sm: 1,
          md: 2,
          lg: 3,
          xl: 4,
          xxl: 4,
        }}
        dataSource={knowledgeBases}
        locale={{
          emptyText: (
            <div className="flex justify-center items-center py-20">
              <Empty image={emptyImage} imageStyle={{ height: '200px' }} description={searchTerm ? '没有找到相关知识库' : '还没有知识库'} />
            </div>
          ),
        }}
        pagination={{
          align: 'end',
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: total,
          hideOnSinglePage: true,
          onChange: (page, pageSize) => setPagination({ ...pagination, current: page, pageSize }),
          className: "!mt-4"
        }}
        renderItem={(item) => (
          <List.Item className="!mb-2 !flex h-[calc(100%-8px)]">
            <Card
              className="w-full h-full bg-white/60 backdrop-blur-sm border-white/80 rounded-xl hover:shadow-lg transition-all cursor-pointer overflow-hidden border"
              bodyStyle={{ padding: '22px 16px' }}
              onClick={() => onOpen(item.id!)}
            >
              <div className="flex h-full flex-col gap-4">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2.5 flex-1 min-w-0">
                    <div className="w-10 h-10 flex-shrink-0">
                      <img
                        src={item.icon ? buildImageUrl(item.icon!) : DatasetIcon}
                        alt={item.name}
                        className="w-full h-full object-cover rounded-lg"
                      />
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className="text-[14px] font-medium text-[#383F44] truncate m-0" title={item.name}>
                        {item.name}
                      </h3>
                    </div>
                  </div>
                  <Dropdown menu={{ items: menuItems(item) }} trigger={['click']}>
                    <div 
                      className="w-8 h-8 flex items-center justify-center text-[#94A0AB] hover:text-[#383F44] transition-colors rounded-lg hover:bg-white/40"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <EllipsisOutlined style={{ fontSize: '20px' }} />
                    </div>
                  </Dropdown>
                </div>

                <div className="space-y-3">
                  <div className="flex flex-wrap gap-2">
                    <Tag className="m-0 border-none bg-white/60 rounded-lg px-2 text-[11px] text-[#58636C]">文件: {item.docCount}</Tag>
                    <Tag className="m-0 border-none bg-white/60 rounded-lg px-2 text-[11px] text-[#58636C]">字数: {formatWordCount(item.wordCount || 0)}</Tag>
                    <Tag className="m-0 border-none bg-white/60 rounded-lg px-2 text-[11px] text-[#58636C]">Agent: {item.agentCount}</Tag>
                  </div>
                  
                  {item.description && <Tooltip title={item.description}>
                    <p className="text-[12px] text-[#58636C] h-[40px] break-all line-clamp-2 leading-[20px] m-0">
                      {item.description || '暂无描述'}
                    </p>
                  </Tooltip>}
                </div>
              </div>
            </Card>
          </List.Item>
        )}
      />
    </div>
  );
};

export default KnowledgeBaseList;
