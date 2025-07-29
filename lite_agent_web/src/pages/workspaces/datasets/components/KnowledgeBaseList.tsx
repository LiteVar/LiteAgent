import React from 'react';
import { List, Avatar, Typography, Tooltip, Dropdown, Empty, Tag } from 'antd';
import { FolderOutlined, EllipsisOutlined } from '@ant-design/icons';
import { DatasetsVO, Dataset } from '@/client';
import emptyImage from '@/assets/dataset/no_knowledge_base.png';
import { PaginationConfig } from 'antd/es/pagination';

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
    <List
      grid={{
        gutter: 16,
        sm: 1,
        md: 1,
        lg: 2,
        xl: 3,
        xxl: 4,
      }}
      dataSource={knowledgeBases}
      className="p-8 pt-4"
      locale={{
        emptyText: (
          <div className="flex justify-center items-center h-full mt-40">
            <Empty image={emptyImage} imageStyle={{ height: '330px' }} description={searchTerm ? '没有找到相关知识库' : '还没有知识库'} />
          </div>
        ),
      }}
      pagination={{
        align: 'end',
        current: pagination.current,
        pageSize: pagination.pageSize,
        total: total,
        onChange: (page, pageSize) => setPagination({ ...pagination, current: page, pageSize }),
      }}
      renderItem={(item) => (
        <List.Item
          onClick={() => onOpen(item.id!)}
          className={`cursor-pointer border border-gray-200 border-solid rounded-md hover:shadow-lg p-4 mb-4 transition-all`}
        >
          <List.Item.Meta
            avatar={
              <Avatar
                shape="square"
                // src={item.icon}
                icon={<FolderOutlined />}
                size={32}
                className="bg-gray-100 text-black"
              />
            }
            title={
              <div className="flex items-center">
                <div className="flex-1">
                  <Title level={5} className="mb-0 mt-0 text-gray-800 flex items-center max-w-[160px]">
                    <div className="truncate" title={item.name}>
                      {item.name}
                    </div>
                  </Title>
                </div>
                <Dropdown menu={{ items: menuItems(item) }} trigger={['hover']}>
                  <EllipsisOutlined className="text-gray-500 cursor-pointer px-5 pt-3 mb-3" />
                </Dropdown>
              </div>
            }
            description={
              <>
                <div className="flex items-center text-xs text-gray-600 mt-2">
                  <Tag>文件: {item.docCount}</Tag>
                  <Tag>字数: {formatWordCount(item.wordCount || 0)}</Tag>
                  <Tag>Agent: {item.agentCount}</Tag>
                </div>
                <Tooltip title={item.description}>
                  <div className="text-sm text-gray-500 mt-3 line-clamp-3 min-h-16">{item.description}</div>
                </Tooltip>
              </>
            }
          />
        </List.Item>
      )}
    />
  );
};

export default KnowledgeBaseList;
