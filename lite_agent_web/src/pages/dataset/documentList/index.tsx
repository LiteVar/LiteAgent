import React, { useMemo } from 'react';
import { useState } from 'react';
import { Button, Modal, message, Input } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  deleteV1DatasetDocumentsBatchDelete,
  deleteV1DatasetDocumentsByDocumentId,
  putV1DatasetDocumentsByDocumentIdEnable,
  putV1DatasetDocumentsByDocumentIdRename,
} from '@/client';
import { getV1DatasetByDatasetIdDocumentsOptions } from '@/client/@tanstack/query.gen';
import { useDatasetContext } from '@/contexts/datasetContext';
import ResponseCode from '@/constants/ResponseCode';
import emptyImage from '@/assets/dataset/no_knowledge_base.png';
import { handlePaginationAfterDelete, Pagination } from '@/utils/paginationUtils';
import DocumentTable from '../components/DocumentTable';

const DocumentList = () => {
  const [renameModal, setRenameModal] = useState<{
    visible: boolean;
    docId: string | null;
    currentName: string;
  }>({
    visible: false,
    docId: null,
    currentName: '',
  });
  const [newDocName, setNewDocName] = useState('');
  const navigate = useNavigate();
  const pathname = useLocation().pathname;
  const { workspaceId, datasetInfo } = useDatasetContext();
  const datasetId = datasetInfo?.id;
  const canEdit = datasetInfo?.canEdit;
  const canDelete = datasetInfo?.canDelete;
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10 });

  const { data, isLoading, refetch } = useQuery({
    ...getV1DatasetByDatasetIdDocumentsOptions({
      query: {
        pageNo: pagination.current,
        pageSize: pagination.pageSize,
      },
      path: {
        datasetId: datasetId!,
      },
    }),
    enabled: !!datasetId && !!workspaceId,
  });

  const documents = useMemo(() => {
    return data?.data?.list || [];
  }, [data]);

  // 处理文档状态切换（冻结/激活）
  const handleStatusToggle = async (docId: string, enableFlag: boolean) => {
    Modal.confirm({
      centered: true,
      title: '确认操作',
      content: `确定要${enableFlag ? '冻结' : '激活'}该文档吗？`,
      onOk: async () => {
        const res = await putV1DatasetDocumentsByDocumentIdEnable({
          path: { documentId: docId },
          headers: {
            'Workspace-id': workspaceId!,
          },
        });
        if (res.data?.code === ResponseCode.S_OK) {
          message.success('操作成功');
          refetch();
        } else {
          message.error('操作失败，请重试');
        }
      },
    });
  };

  // 处理重命名
  const handleRename = (docId: string, currentName: string) => {
    setRenameModal({ visible: true, docId, currentName });
    setNewDocName(currentName);
  };

  // 确认重命名
  const handleRenameConfirm = async () => {
    try {
      const { docId } = renameModal;
      if (newDocName.trim() === '') {
        message.error('请输入新的文档名称');
        return;
      }
      const res = await putV1DatasetDocumentsByDocumentIdRename({
        query: {
          name: newDocName,
        },
        path: { documentId: docId! },
        headers: {
          'Workspace-id': workspaceId!,
        },
      });
      if (res.data?.code === ResponseCode.S_OK) {
        message.success('重命名成功');
        setRenameModal({ visible: false, docId: null, currentName: '' });
        refetch();
      } else {
        message.error('重命名失败，请重试');
      }
    } catch (error) {
      message.error('重命名失败，请重试');
    }
  };

  // 处理删除
  const handleDelete = async (
    documentId: string,
    pagination: Pagination,
    setPagination: (value: Pagination) => void
  ) => {
    Modal.confirm({
      centered: true,
      title: '确认删除',
      content: '确定要删除该文档吗？删除后将无法恢复。',
      onOk: async () => {
        const res = await deleteV1DatasetDocumentsByDocumentId({
          path: { documentId: documentId },
          headers: {
            'Workspace-id': workspaceId!,
          },
        });
        if (res.data?.code === ResponseCode.S_OK) {
          message.success('删除成功');
          handlePaginationAfterDelete(data?.data?.total || 0, 1, pagination, setPagination);
          refetch();
        } else {
          message.error('删除失败，请重试');
        }
      },
    });
  };

  const handleBatchDelete = async (
    documentIds: string[],
    pagination: Pagination,
    setPagination: (value: Pagination) => void
  ) => {
    Modal.confirm({
      centered: true,
      title: '确认删除',
      content: '确定要删除所选文档吗？删除后将无法恢复。',
      onOk: async () => {
        const res = await deleteV1DatasetDocumentsBatchDelete({
          body: documentIds,
          headers: {
            'Workspace-id': workspaceId!,
          },
        });
        if (res.data?.code === ResponseCode.S_OK) {
          message.success('删除成功');
          setSelectedRowKeys([]);
          handlePaginationAfterDelete(data?.data?.total || 0, documentIds.length, pagination, setPagination);
          refetch();
        } else {
          message.error('删除失败，请重试');
        }
      },
    });
  };

  // 处理详情页跳转
  const handleViewDetails = (docId: string, isShowSummary: boolean, fileId?: string) => {
    const path = pathname.endsWith('/') ? pathname.slice(0, -1) : pathname;
    const showSummaryParam = isShowSummary ? '&showSummary=1' : '';
    const fileIdParam = fileId ? `&fileId=${fileId}` : '';
    navigate(`${path}/fragments?documentId=${docId}${showSummaryParam}${fileIdParam}`);
  };

  const handleCreateDocument = () => {
    const path = pathname.endsWith('/') ? pathname.slice(0, -1) : pathname;
    navigate(`${path}/createDocument`);
  };

  return (
    <div>
      <DocumentTable
        documents={documents}
        total={data?.data?.total || 0}
        loading={isLoading}
        canEdit={canEdit!}
        canDelete={canDelete!}
        pagination={pagination}
        setPagination={setPagination}
        onStatusToggle={handleStatusToggle}
        onRename={handleRename}
        onDelete={handleDelete}
        selectedRowKeys={selectedRowKeys}
        setSelectedRowKeys={setSelectedRowKeys}
        onBatchDelete={handleBatchDelete}
        onViewDetails={handleViewDetails}
        handleCreateDocument={handleCreateDocument}
      />

      {!isLoading && documents.length === 0 && (
        <div className="flex flex-col items-center justify-center h-full mt-40">
          <img src={emptyImage} alt="empty" className="h-[330px]" />
          <p className="text-gray-500">暂无文档</p>
        </div>
      )}

      {/* 重命名弹窗 */}
      <Modal
        centered
        title="重命名文档"
        open={renameModal.visible}
        onOk={handleRenameConfirm}
        onCancel={() => setRenameModal({ visible: false, docId: null, currentName: '' })}
      >
        <label className="text-sm font-medium">文档名称</label>
        <Input
          value={newDocName}
          maxLength={60}
          className="mt-2"
          onChange={(e) => setNewDocName(e.target.value)}
          placeholder="请输入新的文档名称"
        />
      </Modal>
    </div>
  );
};

export default DocumentList;
