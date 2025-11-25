import React, { useCallback } from 'react';
import { Table, Button, Space, Tag, Dropdown, message } from 'antd';

import { useDatasetContext } from '@/contexts/datasetContext';
import { Pagination } from '@/utils/paginationUtils';
import { getAccessToken } from '@/utils/cache';
import { putV1DatasetDocumentSummary } from '@/client';
import { DatasetDocument } from '@/client/types.gen';
import { DocumentSourceType } from '@/types/dataset';

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
  onViewDetails: (docId: string, isShowSummary: boolean, fileId?: string) => void;
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

  const token = getAccessToken();
  const { workspaceId } = useDatasetContext();
  
  const formatNumber = (num: number): string | number => {
    if (num < 1000) return num;
    if (num < 1000000) return `${(num / 1000).toFixed(1)}k`;
    return `${(num / 1000000).toFixed(1)}m`;
  };

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
  }, []);

  const updateDocumentSummary = useCallback(async () => {
    try {
      const docIds = selectedRowKeys.map(String);

      if (docIds.length === 0) {
        message.warning('请先选择要更新摘要的文档');
        return;
      }

      const res = await putV1DatasetDocumentSummary({
        query: { docIds },
        headers: {
          'Workspace-id': workspaceId!
        }
      });

      if (res.data?.code === 200) {
        message.success('文档摘要更新成功');
      } else {
        message.error(res.data?.message || '更新文档摘要失败');
      }
    } catch (error) {
      console.error('更新文档摘要时出错:', error);
      message.error('更新文档摘要失败');
    }
  }, [selectedRowKeys, workspaceId]);

  const columns = [
    {
      title: '名称',
      key: 'name',
      render: (doc: DatasetDocument) => {
        if (doc.name) {
          return doc.name;
        }

        if (doc.dataSourceType === DocumentSourceType.HTML) {
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
          <Button type="link" onClick={() => onViewDetails(record.id!, !!record?.needSummary, record?.fileId)}>
            详情
          </Button>
          {record?.fileId && (
            <Button type="link" onClick={() => onDownloadMarkdown(record.fileId!)}>
              下载
            </Button>
          )}
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
            <Button size="large" onClick={updateDocumentSummary}>
              更新文档摘要
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
