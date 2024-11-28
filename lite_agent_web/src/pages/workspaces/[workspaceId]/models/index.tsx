import {Table, Button, message, Popconfirm} from 'antd';
import type {ColumnsType} from 'antd/es/table';
import React, {useCallback, useState} from "react";
import {PlusOutlined} from "@ant-design/icons";
import ModelInfoModal from "./components/ModelInfoModal";
import ModelFormModal from "./components/ModelFormModal";
import {useQuery} from "@tanstack/react-query";
import {getV1ModelListOptions} from "@/client/@tanstack/query.gen";
import {
  ModelVOAddAction,
  postV1ModelAdd,
  putV1ModelUpdate,
  deleteV1ModelById,
  ModelVOUpdateAction, ModelDTO
} from "@/client";
import {useWorkspace} from "@/contexts/workspaceContext";
import ResponseCode from "@/config/ResponseCode";
import {UserType} from "@/types/User";

export default function Modals() {
  const [isInfoModalVisible, setIsInfoModalVisible] = useState(false);
  const [isFormModalVisible, setIsFormModalVisible] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelVOAddAction | undefined>(undefined);
  const [pageNo, setPageNo] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const workspace = useWorkspace()

  const {data, refetch} = useQuery({
    ...getV1ModelListOptions({
      query: {
        pageNo: pageNo,
        pageSize: pageSize
      },
      headers: {
        'Workspace-id': workspace?.id || '',
      },
    })
  });

  const showInfoModal = (model: ModelVOAddAction) => {
    setIsInfoModalVisible(true)
    setEditingModel(model)
  };
  const closeInfoModal = () => {
    setIsInfoModalVisible(false)
    setEditingModel(undefined)
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

  const handleFormSubmit = useCallback(async (values: ModelVOUpdateAction) => {
    values.shareFlag = !!values.shareFlag;
    if (values.id) {
      const res = await putV1ModelUpdate({
        body: values,
        headers: {"Workspace-id": workspace?.id || ""}
      });
      if (res?.data?.code === ResponseCode.S_OK) {
        message.success('更新模型成功');
      } else {
        message.error(res?.data?.message)
      }
    } else {
      const res = await postV1ModelAdd({
        body: values,
        headers: {"Workspace-id": workspace?.id || ""}
      });
      if (res?.data?.code === ResponseCode.S_OK) {
        message.success('创建模型成功');
      } else {
        message.error(res?.data?.message)
      }
    }
    refetch()
    closeFormModal();
  }, [refetch, workspace]);

  const handleDelete = useCallback(async (id: string) => {
    const res = await deleteV1ModelById({
      path: {id: id},
      headers: {"Workspace-id": workspace?.id || ""}
    })
    if (res?.data?.code !== 200) {
      message.error('删除模型失败');
      return;
    }
    message.success('删除模型成功');
    refetch();
    closeFormModal();
  }, [refetch, workspace])

  const columns: ColumnsType<ModelDTO> = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'key值',
      dataIndex: 'apiKey',
      key: 'apiKey',
    },
    {
      title: '状态',
      dataIndex: 'shareFlag',
      key: 'shareFlag',
      render: (shareFlag) => (
        <span>{shareFlag ? '已分享' : '-'}</span>
      )
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <>
          {record?.canEdit &&
            <Button type="link" onClick={(e) => showFormModal(e, record as any)}>编辑</Button>
          }
          {record?.canDelete &&
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
              <Button onClick={(e) => e.stopPropagation()} type="link" danger>删除</Button>
            </Popconfirm>
          }
          {!record?.canEdit && !record?.canDelete && "-"}
        </>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">模型管理</h1>
        {Number(workspace?.role) !== UserType.Normal &&
          <Button type="primary" icon={<PlusOutlined/>}
                  onClick={(e) => showFormModal(e, undefined)}>新建模型</Button>
        }
      </div>
      <Table columns={columns} dataSource={data?.data?.list || []}
           onRow={(record) => {
             return {
               onClick: () => {
                 if (record?.canRead) {
                   showInfoModal(record as ModelVOAddAction);
                 }
               }
             }
           }}
           pagination={
             {
               current: pageNo + 1,
               pageSize: pageSize,
               total: Number(data?.data?.total || 10),
               onChange: (page, pageSize) => {
                 setPageNo(page-1);
                 setPageSize(pageSize);
               }
             }
           }/>

      <ModelInfoModal
        visible={isInfoModalVisible}
        onClose={closeInfoModal}
        modelInfo={editingModel}
      />

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

