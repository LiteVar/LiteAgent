import React, { useCallback, useMemo, useState } from 'react';
import { Skeleton, Modal, message } from 'antd';
import KnowledgeBaseList from './components/KnowledgeBaseList';
import CreateKnowledgeBaseModal from './components/CreateKnowledgeBaseModal';
import ExternalKnowledgeBaseModal from './components/ExternalKnowledgeBaseModal';
import { useWorkspace } from '@/contexts/workspaceContext';
import { UserType } from '@/types/User';
import { useNavigate } from 'react-router-dom';
import { getV1DatasetListOptions, getV1ModelListOptions } from '@/client/@tanstack/query.gen';
import { useQuery } from '@tanstack/react-query';
import { deleteV1DatasetById } from '@/client';
import ResponseCode from '@/constants/ResponseCode';
import { PaginationConfig } from 'antd/es/pagination';
import Header from '@/components/workspace/Header';

const DatasetIndex: React.FC = () => {
  const [searchValue, setSearchValue] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState<string>(''); // Add this line
  const [showCreateKnowledgeBaseModal, setShowCreateKnowledgeBaseModal] = useState<boolean>(false);
  const [showAddExternalKnowledgeBaseModal, setShowAddExternalKnowledgeBaseModal] = useState<boolean>(false);
  const workspace = useWorkspace();
  const navigate = useNavigate();
  const [pagination, setPagination] = useState<PaginationConfig>({
    current: 1,
    pageSize: 12,
  });

  const { data, isLoading, refetch } = useQuery({
    ...getV1DatasetListOptions({
      query: {
        pageNo: pagination.current!,
        pageSize: pagination.pageSize!,
        query: searchTerm,
      },
      headers: {
        'Workspace-id': workspace?.id!,
      },
    }),
    enabled: !!workspace?.id,
  });

  const { data: models } = useQuery({
    ...getV1ModelListOptions({
      headers: {
        'Workspace-id': workspace?.id!,
      },
      query: {
        pageNo: 0,
        pageSize: 100000000,
      },
    }),
    enabled: !!workspace?.id,
  });

  const knowledgeBases = useMemo(() => {
    return data?.data?.list || [];
  }, [data]);

  const handleSearch = useCallback(() => {
    setSearchTerm(searchValue);
    setPagination({
      current: 1,
      pageSize: 10,
    });
  }, [searchValue]);

  const handleOpenKnowledgeBase = useCallback(
    (id: string) => {
      navigate(`/dataset/${workspace?.id}/${id}`);
    },
    [navigate, workspace?.id]
  );

  const handleDeleteKnowledgeBase = (id: string) => {
    Modal.confirm({
      centered: true,
      title: '删除知识库',
      content: '删除后，知识库中的内容将无法再被引用，确认删除？',
      onOk: async () => {
        console.log('deleteKnowledgeBase', id);
        const res = await deleteV1DatasetById({
          path: {
            id,
          },
          headers: {
            'Workspace-id': workspace?.id!,
          },
        });
        if (res.data?.code === ResponseCode.S_OK) {
          message.success('删除成功');
          refetch();
        } else {
          message.error(res.data?.message || '删除失败');
        }
      },
    });
  };

  return (
    <div className="space-y-4">
      <Header
        title="知识库管理"
        placeholder="搜索你的知识库"
        searchValue={searchValue}
        onSearchChange={setSearchValue}
        onSearch={handleSearch}
        showCreateButton={Number(workspace?.role) !== UserType.Normal}
        createButtonText="新建知识库"
        onCreateClick={() => setShowCreateKnowledgeBaseModal(true)}
      />

      {!isLoading && (
        <KnowledgeBaseList
          searchTerm={searchTerm}
          pagination={pagination}
          setPagination={setPagination}
          total={data?.data?.total || 0}
          knowledgeBases={knowledgeBases}
          onOpen={handleOpenKnowledgeBase}
          onDelete={handleDeleteKnowledgeBase}
        />
      )}

      {isLoading && <Skeleton />}

      {showCreateKnowledgeBaseModal && (
        <CreateKnowledgeBaseModal
          refresh={refetch}
          models={models?.data?.list || []}
          visible={showCreateKnowledgeBaseModal}
          onCancel={() => setShowCreateKnowledgeBaseModal(false)}
        />
      )}
      {showAddExternalKnowledgeBaseModal && (
        <ExternalKnowledgeBaseModal
          visible={showAddExternalKnowledgeBaseModal}
          onCancel={() => setShowAddExternalKnowledgeBaseModal(false)}
        />
      )}
    </div>
  );
};

export default DatasetIndex;
