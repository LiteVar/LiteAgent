import { Table, Button, message, Popconfirm, Modal, Dropdown } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useMemo, useState } from 'react';
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
  getV1ModelExportById
} from '@/client';
import { useWorkspace } from '@/contexts/workspaceContext';
import ResponseCode from '@/constants/ResponseCode';
import { UserType } from '@/types/User';
import Header from '@/components/workspace/Header';
import type { MenuProps } from 'antd';
import FileExportModal from '@/components/workspace/FileExportModal';
import { PlusOutlined } from '@ant-design/icons';
import { FilterAllIcon, FilterMyIcon, FilterSystemIcon } from '@/assets/agent/shop_filter_icons_svg';

enum CreateModelType {
  CREATE = 'create',
  IMPORT = 'import',
}

export default function Modals() {
  const [isInfoModalVisible, setIsInfoModalVisible] = useState(false);
  const [isFormModalVisible, setIsFormModalVisible] = useState(false);
  const [isExportModalVisible, setIsExportModalVisible] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelVOAddAction | undefined>(undefined);
  const [exportModel, setExportModel] = useState<ModelDTO | undefined>(undefined);
  const [pageNo, setPageNo] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchValue, setSearchValue] = useState('');
  const [tab, setTab] = useState<number>(0);
  const workspace = useWorkspace();

  const { data, refetch } = useQuery({
    ...getV1ModelListOptions({
      query: {
        status: 1,
        query: searchValue.trim(),
        pageNo: pageNo + 1,
        pageSize: pageSize,
        tab: tab,
      },
      headers: {
        'Workspace-id': workspace?.id || '',
      },
    }),
    enabled: !!workspace,
  });

  // 根据 tab 和搜索值过滤当前页数据
  const filteredModels = useMemo(() => {
    let models = data?.data?.list || [];
    
    // 在"全部"条件下，admin 创建的排在前面
    if (tab === 0) {
      models = [...models].sort((a, b) => {
        const aIsAdmin = a.createUser === 'admin';
        const bIsAdmin = b.createUser === 'admin';
        if (aIsAdmin && !bIsAdmin) return -1;
        if (!aIsAdmin && bIsAdmin) return 1;
        return 0;
      });
    }
    return models.map(item => ({ ...item, key: item.id }));
  }, [data?.data?.list, tab]);

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
          setPageNo(0);
          closeFormModal();
          setTimeout(async () => {
            refetch();
          }, 100);
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

  const columns: ColumnsType<ModelDTO> = useMemo(() => {
    const baseColumns: ColumnsType<ModelDTO> = [
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
        key: 'apiKey'
      },
      {
        title: '创建者',
        dataIndex: 'createUser',
        key: 'createUser',
      },
    ];

    // 如果当前 tab 是"全部"，添加"来源"列
    if (tab === 0) {
      baseColumns.push({
        title: '来源',
        dataIndex: 'workspaceId',
        key: 'source',
        render: (workspaceId: string) => {
          return workspaceId === '0' ? '系统' : '我的';
        },
      });
    }

    // 添加操作列
    baseColumns.push({
      title: '操作',
      key: 'action',
      align: 'center',
      render: (_, record) => (
        <div className="flex justify-center gap-2">
          {record?.canEdit ? (
            <Button type="link" className="p-0 h-auto text-[#40A5EE]" onClick={(e) => showFormModal(e, record as any)} key={`edit-${record.id}`}>
              编辑
            </Button>
          ) : (
            <span className="w-[30px] inline-block text-center text-[#7C8B98]">-</span>
          )}
          <Button type="link" className="p-0 h-auto text-[#40A5EE]" onClick={event => showExportModal(event, record as any)} key={`export-${record.id}`}>
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
              <Button onClick={(e) => e.stopPropagation()} type="link" danger className="p-0 h-auto border-none bg-transparent shadow-none" key={`delete-${record.id}`}>
                删除
              </Button>
            </Popconfirm>
          )}
          {!record?.canEdit && !record?.canDelete && <span className="text-[#7C8B98]">-</span>}
        </div>
      ),
    });

    return baseColumns;
  }, [tab, handleDelete]);

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
          setPageNo(0);
          setTimeout(async () => {
            refetch();
          }, 100);
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

  const handleSearch = useCallback(() => {
    // 搜索时重置到第一页，React Query 会自动重新请求
    setPageNo(0);
  }, []);

  const handleTabChange = (key: number) => {
    setTab(key);
    setPageNo(0); // 切换 tab 时重置到第一页，React Query 会自动重新请求
  };

  const filterOptions = [
    {
      key: 0, label: '全部',
      Icon: FilterAllIcon,
    },
    {
      key: 1, label: '系统',
      Icon: FilterSystemIcon,
    },
    {
      key: 2, label: '我的',
      Icon: FilterMyIcon,
    },
  ];
  

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
      <Dropdown menu={{ items }} placement="bottomRight" arrow={{ pointAtCenter: true }}>
        <Button
          icon={<PlusOutlined />}
          iconPosition='start'
          type="primary"
          size='large'
          className="rounded-xl bg-[#40A5EE] hover:!bg-[#40A5EE]/90 border-none shadow-md shadow-blue-200/50 flex items-center gap-2 h-10"
        >
          新建模型
        </Button>
      </Dropdown>
    )
  }, [workspace, onImportFiles, showFormModal]);

  return (
    <div className="flex flex-col h-full overflow-hidden">
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

      <div className="flex-1 overflow-y-auto px-4 pb-8 space-y-4">
        <div className="flex justify-start items-center">
          <div className="flex gap-2.5">
            {filterOptions.map((option) => (
              <button
                key={option.key}
                onClick={() => handleTabChange(option.key)}
                className={`flex items-center gap-1 px-2 py-1.5 rounded-xl cursor-pointer border-none transition-all ${
                  tab === option.key
                    ? 'text-[#383F44] shadow-sm bg-white'
                    : 'bg-transparent text-[#58636C] hover:bg-white/50'
                }`}
                style={{
                  fontSize: 14,
                  fontWeight: 500,
                }}
              >
                <span className="w-5 h-5 flex-none flex items-center justify-center">
                  <option.Icon 
                    active={tab === option.key} 
                    color={tab === option.key ? '#383F44' : '#58636C'} 
                  />
                </span>
                <span>{option.label}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="h-[calc(100%-48px)] bg-white/60 backdrop-blur-md rounded-2xl border border-white/80 shadow-sm overflow-hidden">
          <Table
            columns={columns}
            dataSource={filteredModels}
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
            className="[&_.ant-table]:bg-transparent [&_.ant-table-thead_th]:bg-transparent [&_.ant-table-thead_th]:text-[#1D4A6B] [&_.ant-table-thead_th]:font-semibold [&_.ant-table-row:hover_td]:bg-white/40"
            pagination={{
              current: pageNo + 1,
              pageSize: pageSize,
              total: Number(data?.data?.total || 0),
              onChange: (page, pageSize) => {
                setPageNo(page - 1);
                setPageSize(pageSize);
              },
              className: "px-6 py-4 mb-0",
            }}
          />
        </div>
      </div>

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
