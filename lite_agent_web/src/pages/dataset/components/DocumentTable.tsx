import React, { useState } from 'react';
import { Table, Button, Space, Tag, Dropdown } from 'antd';
import { DatasetDocument } from '@/client/types.gen';
import { DocumentSourceType } from '@/types/dataset';
import { Pagination } from '@/utils/paginationUtils';

interface DocumentTableProps {
  documents: DatasetDocument[];
  total: number;
  loading: boolean;
  canEdit: boolean;
  canDelete: boolean;
  pagination: Pagination;
  selectedRowKeys: number[];
  setSelectedRowKeys: (keys: number[]) => void;
  setPagination: (value: Pagination) => void;
  onStatusToggle: (docId: string, enableFlag: boolean) => void;
  onRename: (docId: string, currentName: string) => void;
  onDelete: (documentId: string, pagination: Pagination, setPagination: (value: Pagination) => void) => void;
  onBatchDelete: (
    documentIds: string[],
    pagination: Pagination,
    setPagination: (value: Pagination) => void
  ) => void;
  onViewDetails: (docId: string) => void;
  handleCreateDocument: () => void;
}

const DocumentTable: React.FC<DocumentTableProps> = ({
  documents,
  total,
  loading,
  canEdit,
  canDelete,
  pagination,
  selectedRowKeys,
  setPagination,
  onStatusToggle,
  onRename,
  onDelete,
  onBatchDelete,
  onViewDetails,
  setSelectedRowKeys,
  handleCreateDocument,
}) => {
  const formatNumber = (num: number): string | number => {
    if (num < 1000) return num;
    if (num < 1000000) return `${(num / 1000).toFixed(1)}k`;
    return `${(num / 1000000).toFixed(1)}m`;
  };

  const columns = [
    {
      title: '名称',
      key: 'name',
      render: (doc: DatasetDocument) => {
        if (doc.name) {
          return doc.name;
        }
        if (doc.dataSourceType === DocumentSourceType.FILE) {
          return doc.filePath?.split('/').pop();
        } else if (doc.dataSourceType === DocumentSourceType.HTML) {
          return '链接文档';
        }
        return '未命名文档';
      },
    },
    {
      title: '字数',
      dataIndex: 'wordCount',
      key: 'wordCount',
      render: (text: number) => formatNumber(text),
    },
    {
      title: '状态',
      dataIndex: 'enableFlag',
      key: 'enableFlag',
      render: (enableFlag: boolean) => (
        <Tag color={enableFlag ? 'green' : 'default'}>{enableFlag ? '已激活' : '已冻结'}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: DatasetDocument) => (
        <Space>
          {canEdit && (
            <Button type="link" onClick={() => onStatusToggle(record.id!, record.enableFlag!)}>
              {record.enableFlag ? '冻结' : '激活'}
            </Button>
          )}
          <Button type="link" onClick={() => onViewDetails(record.id!)}>
            详情
          </Button>
          {(canEdit || canDelete) && (
            <Dropdown
              menu={{
                items: [
                  ...(canEdit
                    ? [
                        {
                          key: 'rename',
                          label: '重命名',
                          onClick: () => onRename(record.id!, record.name!),
                        },
                      ]
                    : []),
                  ...(canDelete
                    ? [
                        {
                          key: 'delete',
                          label: '删除',
                          danger: true,
                          onClick: () => onDelete(record.id!, pagination, setPagination),
                        },
                      ]
                    : []),
                ],
              }}
            >
              <Button type="link">更多</Button>
            </Dropdown>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl">文档列表 (共 {total} 个)</h2>

        <Space>
          {selectedRowKeys.length > 0 && canDelete && (
            <Button
              danger
              onClick={() => onBatchDelete(selectedRowKeys.map(String), pagination, setPagination)}
            >
              批量删除 ({selectedRowKeys.length})
            </Button>
          )}
          {canEdit && (
            <Button type="primary" size="large" onClick={handleCreateDocument}>
              新建文档
            </Button>
          )}
        </Space>
      </div>

      <Table
        className={documents.length === 0 ? 'hidden' : ''}
        columns={columns}
        dataSource={documents}
        loading={loading}
        rowKey="id"
        rowSelection={{
          selectedRowKeys,
          onChange: (selectedKeys) => setSelectedRowKeys(selectedKeys as number[]),
        }}
        pagination={{
          ...pagination,
          total,
          onChange: (current, pageSize) => setPagination({ current, pageSize }),
        }}
      />
    </>
  );
};

export default DocumentTable;
