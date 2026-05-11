import React, { useState, useMemo, useCallback } from 'react';
import { Button, Checkbox, List, Modal, Pagination, message } from 'antd';
import EmptyState from '@/components/common/EmptyState';
import FragmentListHeader from './components/FragmentListHeader';
import FragmentModal from './components/FragmentModal';
import FragmentItem from './components/FragmentItem';
import SummaryModal from './components/SummaryModal';
import { useQuery } from '@tanstack/react-query';
import { getV1DatasetDocumentsByDocumentIdSegmentsOptions } from '@/client/@tanstack/query.gen';
import { useDatasetContext } from '@/contexts/datasetContext';
import {
  deleteV1DatasetSegmentsBySegmentId,
  DocumentSegment,
  postV1DatasetDocumentsByDocumentIdSegments,
  putV1DatasetSegmentsBySegmentId,
  putV1DatasetSegmentsBySegmentIdEnable,
  deleteV1DatasetSegmentsBatchDelete,
  getV1DatasetDocumentsByDocumentIdSummary,
  putV1DatasetDocumentSummary,
  getV1DatasetDocumentsInfoByDocumentId
} from '@/client';
import ResponseCode from '@/constants/ResponseCode';
import { handlePaginationAfterDelete } from '@/utils/paginationUtils';
import { ExclamationCircleFilled, SearchOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const FragmentList = () => {
  // 从路由获取初始值
  const [documentId, initialShowSummary, fileId] = useMemo(() => {
    const searchParams = new URLSearchParams(window.location.search);
    return [
      searchParams.get('documentId'),
      searchParams.get('showSummary') === '1',
      searchParams.get('fileId') || undefined
    ];
  }, []);
  
  const { workspaceId, datasetInfo } = useDatasetContext();
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedFragments, setSelectedFragments] = useState<string[]>([]);
  const [selectAll, setSelectAll] = useState(false);
  const [fragmentIndex, setFragmentIndex] = useState<string | null>(null);
  const [modalMode, setModalMode] = useState<'create' | 'edit' | 'read'>('create');
  const [editingFragment, setEditingFragment] = useState<DocumentSegment | null>(null);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10 });
  const [createLoading, setCreateLoading] = useState(false);
  const canEdit = datasetInfo?.canEdit;
  const canDelete = datasetInfo?.canDelete;

  const navigate = useNavigate();

  // 新增：摘要弹窗相关状态
  const [summaryVisible, setSummaryVisible] = useState(false);
  const [summary, setSummary] = useState('');
  const [loadingSummary, setLoadingSummary] = useState(false);
  const [updating, setUpdating] = useState(false);

  const datasetId = useMemo(() => {
    const path = window.location.pathname;
    const id = path.split('/')[3];
    return id;
  }, []);

  // showSummary 改为动态状态，路由值作为默认值
  const [showSummary, setShowSummary] = useState(initialShowSummary);

  const { data, isLoading, refetch } = useQuery({
    ...getV1DatasetDocumentsByDocumentIdSegmentsOptions({
      query: {
        pageNo: pagination.current,
        pageSize: pagination.pageSize,
        query: searchTerm,
      },
      path: {
        documentId: documentId!,
      },
    }),
    enabled: !!documentId && !!workspaceId,
  });

  const fragments = useMemo(() => {
    return data?.data?.list || [];
  }, [data]);

  // 封装的更新 showSummary 函数
  const updateShowSummaryStatus = useCallback(async () => {
    if (!documentId || !workspaceId) return;
    
    try {
      const res = await getV1DatasetDocumentsInfoByDocumentId({
        path: { documentId },
        headers: { 'Workspace-id': workspaceId } as Record<string, string>
      });
      
      if (res.data?.code === ResponseCode.S_OK && res.data?.data?.needSummary !== undefined) {
        setShowSummary(res.data.data.needSummary);
      }
    } catch (error) {
      console.error('更新 showSummary 状态失败:', error);
    }
  }, [documentId, workspaceId]);

  const handleSearch = useCallback((value: string) => {
    setPagination({
      current: 1,
      pageSize: 10,
    });
    setSearchTerm(value);
    setSelectAll(false);
    setSelectedFragments([]);
  }, []);

  const handleCreateFragment = async ({ content, metadata }: { content: string; metadata: string }) => {
    try {
      setCreateLoading(true);

      const apiCall =
        modalMode === 'edit'
          ? putV1DatasetSegmentsBySegmentId({
              path: { segmentId: editingFragment?.id! },
              body: { content, metadata },
              headers: { 'Workspace-id': workspaceId! },
            })
          : postV1DatasetDocumentsByDocumentIdSegments({
              path: { documentId: documentId! },
              body: { content, metadata },
              headers: { 'Workspace-id': workspaceId! },
            });

      const res = await apiCall;

      if (res.data?.code === ResponseCode.S_OK) {
        refetch();
        message.success('操作成功');
        // 操作成功后更新 showSummary 状态
        updateShowSummaryStatus();
      } else {
        message.error(res.data?.message || '操作失败');
      }
      setCreateLoading(false);
      setModalVisible(false);
      setEditingFragment(null);
    } catch (error) {
      console.error(error);
    }
  };

  const handleEdit = (id: string, readonly?: boolean, fragmentIndex?: string) => {
    const fragment = fragments.find((f) => f?.id === id);
    if (fragment) {
      setEditingFragment(fragment);
      setModalMode(readonly ? 'read' : 'edit');
      setFragmentIndex(fragmentIndex!);
      setModalVisible(true);
    }
  };

  const handleDelete = (id: string) => {
    Modal.confirm({
      centered: true,
      title: '确认删除',
      content: '删除后，片段内容将无法再被引用，确认删除？',
      onOk: async () => {
        console.log('删除片段:', id);
        const res = await deleteV1DatasetSegmentsBySegmentId({
          path: {
            segmentId: id,
          },
          headers: {
            'Workspace-id': workspaceId!,
          },
        });
        if (res.data?.code === ResponseCode.S_OK) {
          message.success('删除片段成功');
          handlePaginationAfterDelete(data?.data?.total || 0, 1, pagination, setPagination);
          refetch();
          // 操作成功后更新 showSummary 状态
          updateShowSummaryStatus();
        } else {
          message.error(res.data?.message || '删除片段失败');
        }
      },
    });
  };

  const handleBatchDelete = () => {
    Modal.confirm({
      centered: true,
      title: '确认删除',
      content: `确定要删除选中的 ${selectedFragments.length} 个片段吗？片段内容将无法再被引用`,
      onOk: async () => {
        const res = await deleteV1DatasetSegmentsBatchDelete({
          body: selectedFragments,
          headers: {
            'Workspace-id': workspaceId!,
          },
        });
        if (res.data?.code === ResponseCode.S_OK) {
          message.success('批量删除片段成功');
          handlePaginationAfterDelete(
            data?.data?.total || 0,
            selectedFragments.length,
            pagination,
            setPagination
          );
          setSelectAll(false);
          setSelectedFragments([]);
          refetch();
          // 操作成功后更新 showSummary 状态
          updateShowSummaryStatus();
        } else {
          message.error(res.data?.message || '批量删除片段失败');
        }
      },
    });
  };

  const handleToggleFreeze = async (segmentId: string) => {
    await putV1DatasetSegmentsBySegmentIdEnable({
      path: {
        segmentId: segmentId!,
      },
      headers: {
        'Workspace-id': workspaceId!,
      },
    });
    refetch();
    // 操作成功后更新 showSummary 状态
    updateShowSummaryStatus();
  };

  const handleSelectAll = (checked: boolean) => {
    setSelectAll(checked);
    if (checked) {
      setSelectedFragments(fragments.map((f) => f?.id || ''));
    } else {
      setSelectedFragments([]);
    }
  };

  const handleItemSelect = useCallback((checked: boolean, id: string) => {
    if (checked) {
      setSelectedFragments([...selectedFragments, id]);
      setSelectAll(selectedFragments.length + 1 === fragments.length);
    } else {
      setSelectedFragments(selectedFragments.filter((v) => v !== id));
      setSelectAll(false);
    }
  }, [selectedFragments, fragments]);

  const handleViewSummary = useCallback(async () => {
    setSummaryVisible(true);
    if (!documentId) {
      setSummary('未提供 documentId，无法自动加载摘要。');
      return;
    }
    setLoadingSummary(true);
    try {
      const res = await getV1DatasetDocumentsByDocumentIdSummary({ 
        path: { documentId },
       });

       if (res.data?.code === 200) {
        setSummary(res.data?.data?.trimStart() || '');
       }
    } catch (err) {
      console.error(err);
      message.error('获取摘要失败');
      setSummary('');
    } finally {
      setLoadingSummary(false);
    }
  }, [documentId]);

  const handleUpdateSummary = useCallback(async () => {
    if (!documentId) {
      message.warning('无法更新：未提供 documentId');
      return;
    }

    setUpdating(true);

    try {
      const res = await putV1DatasetDocumentSummary({
        query: { docIds: [documentId] },
        headers: {
          'Workspace-id': workspaceId!
        }
      });

      if (res.data?.code === 200) {
        message.success('更新成功');
        // 操作成功后更新 showSummary 状态
        updateShowSummaryStatus();
      } else if (res.data?.code === ResponseCode.MODEL_CLOSED) {
        Modal.confirm({
          icon: <ExclamationCircleFilled style={{ color: '#40A5EE' }} />,
          content: '当前摘要模型已停用，请切换为其他模型后重试。',
          centered: true,
          okText: '去设置',
          cancelText: '取消',
          okButtonProps: {
            style: { backgroundColor: '#40A5EE', borderColor: '#40A5EE', color: '#fff', marginTop: '32px' },
          },
          onOk: () => {
            navigate(`/dataset/${workspaceId}/${datasetId}/settings`);
          },
        });
      } else {
        message.error(res.data?.message || '更新文档摘要失败');
      }
    } catch (err) {
      console.error(err);
      message.error('更新失败');
    } finally {
      setUpdating(false);
    }
  }, [documentId, workspaceId, updateShowSummaryStatus, navigate, datasetId]);

  return (
    <div className="h-[calc(100%-32px)] overflow-hidden bg-white/60 rounded-2xl p-4">
      <FragmentListHeader
        canEdit={canEdit!}
        canDelete={canDelete!}
        onCreateNew={() => {
          setModalMode('create');
          setEditingFragment(null);
          setModalVisible(true);
        }}
        onViewSummary={handleViewSummary}
        onUpdateSummary={handleUpdateSummary}
        isUpdateingSummary={updating}
        showSummary={showSummary}
        fileId={fileId}
      />

      <div className="bg-white p-4 mt-4 rounded-2xl">
        <div className="flex justify-between items-center py-3">
          <div className="flex items-center gap-4 pl-2">
            <Checkbox 
              checked={selectAll} 
              onChange={(e) => handleSelectAll(e.target.checked)}
              className="custom-checkbox"
            >
              <span className="text-sm">全选</span>
            </Checkbox>
            {/* 灰色 竖的 分割线  */}
            <span className="w-0.5 h-4 bg-gray-300"></span>
            <span className="text-sm">共 {data?.data?.total || 0} 个片段</span>
            
            {selectedFragments.length > 0 && (
              <div className="flex items-center gap-3 animate-in fade-in slide-in-from-left-2">
                <div className="w-1 h-1 bg-gray-300 rounded-full" />
                <span className="text-sm text-[#40a5ee] font-medium">
                  已选择 {selectedFragments.length} 项
                </span>
                {canDelete && (
                  <Button 
                    danger 
                    type="text"
                    size="small"
                    onClick={handleBatchDelete}
                    className="hover:!bg-red-50"
                  >
                    批量删除
                  </Button>
                )}
              </div>
            )}
          </div>

          <div className="flex items-center gap-3">
            <div className="relative group flex items-center w-64 h-10 px-3 rounded-xl bg-[#f2f3f5] border border-transparent transition-all">
              <input
                placeholder="搜索片段内容"
                onChange={(e) => handleSearch(e.target.value)}
                className="flex-1 bg-transparent outline-none text-sm text-gray-700  border-none"
              />
              <SearchOutlined className="text-gray-400 transition-colors text-base" />
            </div>
          </div>
        </div>

        {fragments.length > 0 ? (
          <>
            <List
              loading={isLoading}
              dataSource={fragments}
              className="max-h-[calc(100vh-366px)] overflow-y-auto"
              renderItem={(item, index) => (
                <FragmentItem
                  item={item}
                  index={index}
                  canEdit={canEdit!}
                  canDelete={canDelete!}
                  searchText={searchTerm}
                  pagination={pagination}
                  selected={selectedFragments.includes(item?.id || '')}
                  onSelect={(checked) => handleItemSelect(checked, item?.id || '')}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onToggleFreeze={handleToggleFreeze}
                />
              )}
            />
            <div className="flex justify-end mt-4">
              <Pagination
                total={data?.data?.total}
                pageSize={pagination.pageSize}
                current={pagination.current}
                onChange={(page, pageSize) => {
                  setPagination({
                    current: page,
                    pageSize: pageSize,
                  });
                }}
              />
            </div>
          </>
        ) : (
          <EmptyState text="暂无片段" className="mt-20" />
        )}
      </div>

      <FragmentModal
        open={modalVisible}
        mode={modalMode}
        fragmentIndex={fragmentIndex}
        initialValues={editingFragment || undefined}
        onCancel={() => {
          setModalVisible(false);
          setEditingFragment(null);
          setModalMode('create');
        }}
        loading={createLoading}
        onSubmit={handleCreateFragment}
      />

      <SummaryModal
        open={summaryVisible}
        summary={summary}
        loading={loadingSummary}
        onClose={() => setSummaryVisible(false)}
      />
    </div>
  );
};

export default FragmentList;
