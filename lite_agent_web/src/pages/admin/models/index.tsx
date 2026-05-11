import { Table, Button, message, Popconfirm, Modal, Dropdown, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getV1ModelListForSystemOptions } from '@/client/@tanstack/query.gen';
import {
  ModelVOAddAction,
  ModelVOUpdateAction,
  ModelDTO,
  getV1ModelExportById,
  postV1ModelAddForSystem,
  putV1ModelUpdateForSystem,
  deleteV1ModelDeleteForSystemById,
  postV1ModelByIdStatusToggle,
} from '@/client';
import ResponseCode from '@/constants/ResponseCode';
import Header from '@/components/workspace/Header';
import type { MenuProps } from 'antd';
import FileExportModal from '@/components/workspace/FileExportModal';
import { PlusOutlined } from '@ant-design/icons';
import ModelInfoModal from '@/pages/workspaces/models/components/ModelInfoModal';
import ModelFormModal from '@/pages/workspaces/models/components/ModelFormModal';

// Model status constants
const MODEL_STATUS = {
  PENDING: 0,    // 待启用
  ENABLED: 1,    // 已启用
  DISABLED: 2,   // 已停用
} as const;

type ModelStatus = typeof MODEL_STATUS[keyof typeof MODEL_STATUS];

// Extended ModelDTO with status and priceConfig fields
interface AdminModelDTO extends ModelDTO {
  status?: ModelStatus;
}

const STATUS_CONFIG: Record<number, { text: string; color: string; bgColor: string; borderColor: string }> = {
  [MODEL_STATUS.ENABLED]: { text: '已启用', color: '#52C41A', bgColor: '#F6FFED', borderColor: '#B7EB8F' },
  [MODEL_STATUS.DISABLED]: { text: '已停用', color: '#CC2D3A', bgColor: '#FAEAEB', borderColor: '#E9A5AA' },
  [MODEL_STATUS.PENDING]: { text: '待启用', color: '#7C8B98', bgColor: '#F2F3F5', borderColor: '#E0E3E6' },
};

enum CreateModelType {
  CREATE = 'create',
  IMPORT = 'import',
}

export default function AdminModels() {
  const [isInfoModalVisible, setIsInfoModalVisible] = useState(false);
  const [isFormModalVisible, setIsFormModalVisible] = useState(false);
  const [isExportModalVisible, setIsExportModalVisible] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelVOAddAction | undefined>(undefined);
  const [exportModel, setExportModel] = useState<ModelDTO | undefined>(undefined);
  const [pageNo, setPageNo] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchValue, setSearchValue] = useState('');

  const { data, refetch } = useQuery({
    ...getV1ModelListForSystemOptions({
      query: {
        query: searchValue.trim(),
        pageNo: pageNo + 1,
        pageSize: pageSize,
      },
    }),
  });

  const showInfoModal = (model: ModelVOAddAction) => {
    setIsInfoModalVisible(true);
    setEditingModel(model);
  };

  const showExportModal = (event: any, model: ModelVOAddAction | ModelDTO) => {
    event.stopPropagation();
    setIsExportModalVisible(true);
    setExportModel(model as ModelDTO);
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

  // Enable model with pricing check
  const handleEnable = useCallback(async (e: any, record: AdminModelDTO) => {
    e?.stopPropagation();

    try {
      const res = await postV1ModelByIdStatusToggle({
        path: { id: record.id! },
        query: { status: MODEL_STATUS.ENABLED },
      });
      if ((res?.data as any)?.code === ResponseCode.S_OK) {
        message.success('模型启用成功');
        refetch();
      } else {
        message.error((res?.data as any)?.message || '启用失败');
      }
    } catch {
      message.error('启用失败');
    }
  }, [refetch]);

  // Disable model
  const handleDisable = useCallback(async (e: any, record: AdminModelDTO) => {
    e?.stopPropagation();
    try {
      const res = await postV1ModelByIdStatusToggle({
        path: { id: record.id! },
        query: { status: MODEL_STATUS.DISABLED },
      });
      if ((res?.data as any)?.code === ResponseCode.S_OK) {
        message.success('模型已停用');
        refetch();
      } else {
        message.error((res?.data as any)?.message || '停用失败');
      }
    } catch {
      message.error('停用失败');
    }
  }, [refetch]);

  const handleFormSubmit = useCallback(
    async (values: ModelVOUpdateAction) => {
      values.name = values.name.trim();
      values.apiKey = values.apiKey.trim();
      values.baseUrl = values.baseUrl.trim();
      if ('llmModelId' in values) {
        (values as any).llmModelId = 'system';
      }
      if (values.id) {
        const res = await putV1ModelUpdateForSystem({
          body: values,
        });
        if (res?.data?.code === ResponseCode.S_OK) {
          message.success('更新模型成功');
          refetch();
          closeFormModal();
        } else {
          message.error(res?.data?.message);
        }
      } else {
        const res = await postV1ModelAddForSystem({
          body: values,
        });
        if (res?.data?.code === ResponseCode.S_OK) {
          message.success('创建模型成功');
          setPageNo(0);
          closeFormModal();
          setTimeout(async () => {
            await refetch();
          }, 100);
        } else {
          message.error(res?.data?.message);
        }
      }
    },
    [refetch]
  );

  const handleDelete = useCallback(
    async (id: string) => {
      const res = await deleteV1ModelDeleteForSystemById({
        path: { id: id },
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
    [refetch]
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

  const renderStatusTag = (status?: number) => {
    const config = STATUS_CONFIG[status ?? MODEL_STATUS.PENDING];
    if (!config) return '-';
    return (
      <Tag
        style={{
          color: config.color,
          backgroundColor: config.bgColor,
          borderColor: config.borderColor,
          borderRadius: 4,
        }}
      >
        {config.text}
      </Tag>
    );
  };

  const columns: ColumnsType<AdminModelDTO> = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      ellipsis: true,
      width: 200,
    },
    {
      title: '连接别名',
      dataIndex: 'alias',
      key: 'alias',
      ellipsis: true,
      width: 200,
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 80,
    },
    {
      title: '创建者',
      dataIndex: 'createUser',
      key: 'createUser',
      width: 120,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => renderStatusTag(status),
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      render: (_, record) => {
        const status = record.status ?? MODEL_STATUS.PENDING;
        const isEnabled = status === MODEL_STATUS.ENABLED;
        const isDisabled = status === MODEL_STATUS.DISABLED;
        const isPending = status === MODEL_STATUS.PENDING;

        // 已启用 → 不支持编辑, 不支持删除
        // 已停用 → 不支持删除
        // 待启用 → 支持编辑, 支持删除
        const canEdit = !isEnabled;
        const canDelete = isPending;

        return (
          <div className="flex items-center gap-0 flex-nowrap">
            {canEdit ? (
              <Button type="link" size="small" className="text-[#383f44] font-normal px-2" onClick={(e) => showFormModal(e, record as any)}>
                编辑
              </Button>
            ) : null}

            <Button type="link" size="small" className="text-[#383f44] font-normal px-2" onClick={(e) => showExportModal(e, record as any)}>
              导出
            </Button>

            {/* 启用 button: for 待启用 and 已停用 */}
            {(isPending || isDisabled) && (
              <Button
                type="link"
                size="small"
                className="px-2"
                style={{ color: '#40A5EE' }}
                onClick={(e) => handleEnable(e, record)}
                icon={
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M7 2.625V7" stroke="#40A5EE" strokeLinecap="round" strokeLinejoin="round"/>
                    <path d="M9.625 3.0625C10.9419 3.92109 11.8125 5.3107 11.8125 7C11.8125 8.27635 11.3055 9.50043 10.403 10.403C9.50043 11.3055 8.27635 11.8125 7 11.8125C5.72365 11.8125 4.49957 11.3055 3.59705 10.403C2.69453 9.50043 2.1875 8.27635 2.1875 7C2.1875 5.3107 3.05813 3.92109 4.375 3.0625" stroke="#40A5EE" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                }
              >
                启用
              </Button>
            )}

            {/* 停用 button: for 已启用 */}
            {isEnabled && (
              <Popconfirm
                title="确认停用该模型？"
                description="停用后用户将无法再使用此模型"
                onConfirm={(e) => {
                  e?.stopPropagation();
                  handleDisable(e!, record);
                }}
                onCancel={(e) => e?.stopPropagation()}
                okText="确认"
                cancelText="取消"
                okButtonProps={{ danger: true }}
              >
                <Button
                  type="link"
                  size="small"
                  danger
                  className="px-2"
                  onClick={(e) => e.stopPropagation()}
                  icon={
                    <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M10.7122 10.7125L3.28784 3.28809" stroke="#CC2D3A" strokeLinecap="round" strokeLinejoin="round"/>
                      <path d="M7 12.25C9.8995 12.25 12.25 9.8995 12.25 7C12.25 4.10051 9.8995 1.75 7 1.75C4.10051 1.75 1.75 4.10051 1.75 7C1.75 9.8995 4.10051 12.25 7 12.25Z" stroke="#CC2D3A" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                  }
                >
                  停用
                </Button>
              </Popconfirm>
            )}

            {/* 删除 button: only for 待启用 */}
            {canDelete && (
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
                <Button type="link" size="small" danger className="px-2" onClick={(e) => e.stopPropagation()}>
                  删除
                </Button>
              </Popconfirm>
            )}
          </div>
        );
      },
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
          formData.append("files", f);
        }

        const response = await fetch('/v1/model/importForSystem', {
          method: 'POST',
          body: formData,
          headers: {
            Authorization: `Bearer ${localStorage.getItem('access_token')}`,
          },
        });

        if (!response.ok) {
          throw new Error('导入模型失败');
        }
  
        const res = await response.json();
        if (res?.code === ResponseCode.S_OK) {
          message.success('导入模型成功');
          refetch();
        } else {
          message.error(res?.message);
        }
      }
      file.click();
    } catch (error) {
      message.error('导入模型失败');
    }
  }, [refetch]);

  const handleSearch = useCallback(() => {
    setPageNo(0);
    refetch();
  }, [refetch]);

  const createButton = useMemo(() => {
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
            icon={<PlusOutlined />}
            iconPosition='start'
            type="primary"
            size='large'
            className="rounded-xl bg-[#40A5EE] hover:!bg-[#40A5EE]/90 border-none shadow-md shadow-blue-200/50 flex items-center gap-2 h-10"
          >
            新建模型
          </Button>
        </a>
      </Dropdown>
    )
  }, [onImportFiles, showFormModal]);

  return (
    <div className="py-4 space-y-6 h-full flex flex-col overflow-hidden">
      <Header
        title="模型管理"
        placeholder="搜索你的模型"
        searchValue={searchValue}
        onSearchChange={setSearchValue}
        onSearch={handleSearch}
        showCreateButton={false}
        showSearch={true}
        createButton={createButton}
      />

      <div className="flex-1 overflow-hidden bg-white/60 backdrop-blur-md border border-white/80 rounded-2xl flex flex-col mx-4 mb-4">
        <Table
          columns={columns}
          dataSource={data?.data?.list || []}
          className="flex-1 overflow-auto [&_.ant-table-thead]:bg-white [&_.ant-table]:bg-transparent [&_.ant-table-container]:bg-transparent [&_.ant-table-thead_tr_th]:bg-transparent [&_.ant-table-thead_tr_th]:text-[#7C8B98] [&_.ant-table-thead_tr_th]:border-b-white/80 [&_.ant-table-tbody_tr_td]:border-b-white/50"
          rowKey={(record) => record?.id || ''}
          onRow={(record) => {
            return {
              onClick: () => {
                if (record?.canRead) {
                  showInfoModal(record as ModelVOAddAction);
                }
              },
              className: 'cursor-pointer hover:bg-white/40 transition-colors',
            };
          }}
          pagination={{
            current: pageNo + 1,
            pageSize: pageSize,
            total: Number(data?.data?.total || 0),
            className: 'px-6 py-4',
            onChange: (page, pageSize) => {
              setPageNo(page - 1);
              setPageSize(pageSize);
            },
          }}
        />
      </div>

      <ModelInfoModal visible={isInfoModalVisible} onClose={closeInfoModal} modelInfo={editingModel} />
      
      <ModelFormModal
        modelVisible={true}
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
