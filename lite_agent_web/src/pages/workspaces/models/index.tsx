import { Table, Button, message, Popconfirm, Modal } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useCallback, useState } from 'react';
import ModelInfoModal from './components/ModelInfoModal';
import ModelFormModal from './components/ModelFormModal';
import { useQuery } from '@tanstack/react-query';
import { getV1ModelListOptions } from '@/client/@tanstack/query.gen';
import {
  ModelVOAddAction,
  postV1ModelAdd,
  putV1ModelUpdate,
  deleteV1ModelById,
  ModelVOUpdateAction,
  ModelDTO,
} from '@/client';
import { useWorkspace } from '@/contexts/workspaceContext';
import ResponseCode from '@/constants/ResponseCode';
import { UserType } from '@/types/User';
import Header from '@/components/workspace/Header';

export default function Modals() {
  const [isInfoModalVisible, setIsInfoModalVisible] = useState(false);
  const [isFormModalVisible, setIsFormModalVisible] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelVOAddAction | undefined>(undefined);
  const [pageNo, setPageNo] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const workspace = useWorkspace();

  const { data, refetch } = useQuery({
    ...getV1ModelListOptions({
      query: {
        pageNo: pageNo + 1,
        pageSize: pageSize,
      },
      headers: {
        'Workspace-id': workspace?.id || '',
      },
    }),
    enabled: !!workspace,
  });

  const showInfoModal = (model: ModelVOAddAction) => {
    setIsInfoModalVisible(true);
    setEditingModel(model);
  };
  const closeInfoModal = () => {
    setIsInfoModalVisible(false);
    setEditingModel(undefined);
  };

  const showFormModal = (e: any, model: ModelVOAddAction | undefined) => {
    e.stopPropagation();
    setEditingModel(model);
    setIsFormModalVisible(true);
  };

  const closeFormModal = () => {
    setIsFormModalVisible(false);
    setEditingModel(undefined);
  };

  const handleFormSubmit = useCallback(
    async (values: ModelVOUpdateAction) => {
      // values.shareFlag = !!values.shareFlag;
      values.name = values.name.trim();
      values.apiKey = values.apiKey.trim();
      values.baseUrl = values.baseUrl.trim();
      if (values.id) {
        const res = await putV1ModelUpdate({
          body: values,
          headers: { 'Workspace-id': workspace?.id || '' },
        });
        if (res?.data?.code === ResponseCode.S_OK) {
          message.success('更新模型成功');
          refetch();
          closeFormModal();
        } else {
          message.error(res?.data?.message);
        }
      } else {
        const res = await postV1ModelAdd({
          body: values,
          headers: { 'Workspace-id': workspace?.id || '' },
        });
        if (res?.data?.code === ResponseCode.S_OK) {
          message.success('创建模型成功');
          refetch();
          closeFormModal();
        } else {
          message.error(res?.data?.message);
        }
      }
    },
    [refetch, workspace]
  );

  const handleDelete = useCallback(
    async (id: string) => {
      const res = await deleteV1ModelById({
        path: { id: id },
        headers: { 'Workspace-id': workspace?.id || '' },
      });
      if (res?.data?.code === ResponseCode.S_OK) {
        message.success('删除模型成功');
        refetch();
        closeFormModal();
      } else {
        if (res?.data?.code === 40001) {
          const data = res?.data?.data as any;
          Modal.confirm({
            centered: true,
            width: 500,
            title: '删除模型失败',
            cancelButtonProps: { style: { display: 'none' } },
            content: (
              <div>
                <p>以下知识库正在使用该模型作为嵌套模型，无法删除：</p>
                <div className="mb-4 max-h-[300px] overflow-auto">
                  {data?.map((item: any) => (
                    <div
                      key={item.id}
                      className="flex items-center w-[350px] truncate p-2 bg-gray-100 rounded-md mb-2"
                    >
                      <span>{item.name}</span>
                    </div>
                  ))}
                </div>
              </div>
            ),
            onOk: () => {
              closeFormModal();
            },
          });
        } else {
          message.error('删除模型失败');
        }
      }
    },
    [refetch, workspace]
  );

  const columns: ColumnsType<ModelDTO> = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '连接别名',
      dataIndex: 'alias',
      key: 'alias',
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
    },
    {
      title: 'key值',
      dataIndex: 'apiKey',
      key: 'apiKey',
      render: (text, record) => {
        return record?.canEdit ? text : '******';
      }
    },
    {
      title: '创建者',
      dataIndex: 'createUser',
      key: 'createUser',
    },
    {
      title: '操作',
      key: 'action',
      align: 'center',
      render: (_, record) => (
        <>
          {record?.canEdit ? (
            <Button type="link" onClick={(e) => showFormModal(e, record as any)} key={`edit-${record.id}`}>
              编辑
            </Button>
          ) : (
            <span className="w-[60px] inline-block text-center">-</span>
          )}
          {record?.canDelete && (
            <Popconfirm
              title="确认删除模型？"
              onConfirm={(e) => {
                e?.stopPropagation();
                handleDelete(record.id!);
              }}
              onCancel={(e) => e?.stopPropagation()}
              okText="确认"
              cancelText="取消"
            >
              <Button onClick={(e) => e.stopPropagation()} type="link" danger key={`delete-${record.id}`}>
                删除
              </Button>
            </Popconfirm>
          )}
          {!record?.canEdit && !record?.canDelete && '-'}
        </>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <Header
        title="模型管理"
        placeholder="搜索你的模型"
        showSearch={false}
        showCreateButton={Number(workspace?.role) !== UserType.Normal}
        createButtonText="新建模型"
        onCreateClick={(e) => showFormModal(e, undefined)}
      />

      <Table
        columns={columns}
        dataSource={data?.data?.list || []}
        className="px-8"
        rowKey={(record) => record?.id || ''}
        onRow={(record) => {
          return {
            onClick: () => {
              if (record?.canRead) {
                showInfoModal(record as ModelVOAddAction);
              }
            },
          };
        }}
        pagination={{
          current: pageNo + 1,
          pageSize: pageSize,
          total: Number(data?.data?.total || 10),
          onChange: (page, pageSize) => {
            setPageNo(page - 1);
            setPageSize(pageSize);
          },
        }}
      />

      <ModelInfoModal visible={isInfoModalVisible} onClose={closeInfoModal} modelInfo={editingModel} />

      <ModelFormModal
        visible={isFormModalVisible}
        onCancel={closeFormModal}
        onOk={(values) => handleFormSubmit(values as ModelVOUpdateAction)}
        onDelete={handleDelete}
        initialData={editingModel}
      />
    </div>
  );
}
