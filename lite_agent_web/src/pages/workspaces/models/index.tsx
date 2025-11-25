import { Table, Button, message, Popconfirm, Modal, Dropdown } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useCallback, useMemo, useState } from 'react';
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
  getV1ModelExportById,
  postV1ModelImport,
} from '@/client';
import { useWorkspace } from '@/contexts/workspaceContext';
import ResponseCode from '@/constants/ResponseCode';
import { UserType } from '@/types/User';
import Header from '@/components/workspace/Header';
import type { MenuProps } from 'antd';
import FileExportModal from '@/components/workspace/FileExportModal';
import { DownOutlined } from '@ant-design/icons';

enum CreateModelType {
  CREATE = 'create',
  IMPORT = 'import',
}

export default function Modals() {
  const [isInfoModalVisible, setIsInfoModalVisible] = useState(false);
  const [isFormModalVisible, setIsFormModalVisible] = useState(false);
  const [isExportModalVisible, setIsExportModalVisible] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelVOAddAction | undefined>(undefined);
  const [exportModel, setExportModel] = useState<ModelVOAddAction | undefined>(undefined);
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

  const showExportModal = (event: any, model: ModelVOAddAction) => {
    event.stopPropagation();
    setIsExportModalVisible(true);
    setExportModel(model);
  };

  const closeExportModal = () => {
    setIsExportModalVisible(false);
    setExportModel(undefined);
  };

  const closeInfoModal = () => {
    setIsInfoModalVisible(false);
    setEditingModel(undefined);
  };

  const showFormModal = (e: any, model: ModelVOAddAction | undefined) => {
    e && e.stopPropagation();
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

  const onExportFile = useCallback(async (id: string, checked: boolean) => {
    try {
      const res = await getV1ModelExportById({
        path: { id: id },
        query: {
          plainText: checked,
        },
      });
      
      if (!res.data) throw new Error('导出模型失败');
      message.success('导出模型成功');
      closeExportModal();
  
      const text = JSON.stringify(res.data, null, 2);
      const blob = new Blob([text], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${res.data.name}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      console.log('error', error)
      message.error('导出模型失败');
    }
  },[]);

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
          <Button type="link" onClick={event => showExportModal(event, record as any)} key={`edit-${record.id}`}>
              导出
          </Button>
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

  const onImportFiles = useCallback(async (e: any) => {
    e.stopPropagation();
    try {
      const file = document.createElement('input');
      file.multiple = true;
      file.type = 'file';
      file.accept = '.json';
      file.onchange = async (e: any) => {
        const files = Array.from(e.target?.files) as File[];
        if (!files.length) return;

        // 校验文件
        for (const file of files) {
          const isJson = file.type === "application/json" || file.name.endsWith(".json");
          if (!isJson) {
            message.error(`${file.name} 不是 JSON 文件`);
            return;
          }
          const isLt200KB = file.size / 1024 <= 200;
          if (!isLt200KB) {
            message.error(`${file.name} 超过 200KB 限制`);
            return;
          }
        } 

        const formData = new FormData();
        for (const f of files) {
          formData.append("files", f); // ⚡ 多文件上传的关键
        }

        const response = await fetch('/v1/model/import', {
          method: 'POST',
          body: formData,
          headers: {
            'Workspace-id': workspace?.id || '',
            Authorization: `Bearer ${localStorage.getItem('access_token')}`,
          },
        });

        if (!response.ok) {
          throw new Error('导入模型失败');
        }
  
        const res = await response.json();
        console.log('res', res)
        if (res?.code === ResponseCode.S_OK) {
          message.success('导入模型成功');
          refetch();
        } else {
          message.error(res?.message);
        }
      }
      file.click();
    } catch (error) {
      console.log('error', error)
      message.error('导入模型失败');
    }
  }, [workspace, refetch]);
  

  const createButton = useMemo(() => {
    if (!workspace?.role ||Number(workspace?.role) === UserType.Normal) return null;

    const items: MenuProps['items'] = [
    {
      label: (
        <div onClick={(e) => showFormModal(e, undefined)} className="text-[14px]">
          新建模型
        </div>
      ),
      key: CreateModelType.CREATE,
    },
    {
      label: (
        <div onClick={(e) => onImportFiles(e)} className="text-[14px]">
          导入模型
        </div>
      ),
      key: CreateModelType.IMPORT,
    },
  ];

    return (
      <Dropdown menu={{ items }}>
        <a onClick={(e) => e.preventDefault()}>
        <Button
          icon={<DownOutlined />}
          iconPosition='end'
          type="primary"
          size='large'
        >
          新建模型
        </Button>
        </a>
    </Dropdown>
    )
  }, [workspace, onImportFiles, showFormModal]);

  return (
    <div className="space-y-4">
      <Header
        title="模型管理"
        placeholder="搜索你的模型"
        showCreateButton={false}
        showSearch={false}
        createButton={createButton}
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
        showExportModal={showExportModal}
        onOk={(values) => handleFormSubmit(values as ModelVOUpdateAction)}
        onDelete={handleDelete}
        initialData={editingModel}
      />

      <FileExportModal title="大模型" disabled={!exportModel?.canDelete} visible={isExportModalVisible && !!exportModel?.id} id={exportModel?.id} onClose={closeExportModal} onOk={onExportFile} />

    </div>
  );
}
