import {Table, Button, Dropdown, message, Modal} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, {useCallback, useMemo, useState} from "react";
import AddMemberModal from "./components/AddMemberModal";
import {UserType} from "@/types/User";
import {postV1WorkspaceMember, putV1WorkspaceMemberByMemberId, deleteV1WorkspaceMemberByMemberId , WorkspaceMemberVO} from "@/client";
import {getV1WorkspaceMemberListOptions} from "@/client/@tanstack/query.gen";
import {useQuery} from "@tanstack/react-query";
import {useUserInfo, useWorkspace} from "@/contexts/workspaceContext";
import ResponseCode from "@/constants/ResponseCode";
import Header from '@/components/workspace/Header';
import { DownOutlined } from '@ant-design/icons';
export default function Users () {
  const [searchValue, setSearchValue] = useState('');
  const [usernameValue, setUsernameValue] = useState('');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const workspace = useWorkspace()
  const userInfo = useUserInfo()

  const canManageUsers = useMemo(() => {
    if (workspace) {
      return Number(workspace?.role) === UserType.Admin
    }
  }, [workspace])

  const isSelf = useCallback((userId: string) => {
    return userId === userInfo?.id
  }, [userInfo])

  const {data, refetch} = useQuery({
    ...getV1WorkspaceMemberListOptions({
      query: {
        pageNo: pageNo,
        pageSize: pageSize,
        username: usernameValue
      },
      headers: {
        'Workspace-id': workspace?.id || '',
      },
    }),
  })

  const showModal = () => {
    setIsModalVisible(true);
  };

  const handleCancel = () => {
    setIsModalVisible(false);
  };

  const handleSearch = useCallback(() => {
    setUsernameValue(searchValue);
  }, [searchValue]);

  const handleAddMembers = useCallback(async (emails: string[], role: UserType) => {
    const res = await postV1WorkspaceMember({
      query: {
        emails: emails.join(','),
        role: role
      },
      headers: {
        'Workspace-id': workspace?.id || '',
      }
    })
    if (res?.data?.code === ResponseCode.S_OK) {
      message.success(`邀请成功`);
    } else {
      message.error(res?.data?.message)
    }
    setIsModalVisible(false);
  }, [workspace]);

  const handleMenuClick = useCallback(async (e: any, memberId: string) => {
    if (e.key === 'delete') {
      await deleteV1WorkspaceMemberByMemberId({
        path: {memberId: memberId},
        headers: {'Workspace-id': workspace?.id || '',}
      })
      message.success('删除成功');
    } else if (e.key === 'quit') {
      Modal.confirm({
        title: '退出工作空间',
        content: '在工作空间中的创建的agent聊天对话、agent、工具之类都会被删除',
        onOk: async () => {
          const res = await deleteV1WorkspaceMemberByMemberId({
            path: {memberId: memberId},
            headers: {'Workspace-id': workspace?.id || '',}
          })
          if (res?.data?.code === ResponseCode.S_OK) {
            message.success('删除成功');
            await refetch()
          } else {
            message.error(res?.data?.message)
          }
        }
      })
    } else {
      await putV1WorkspaceMemberByMemberId({
        path: {memberId: memberId},
        query: {role: e.key},
        headers: {'Workspace-id': workspace?.id || '',}
      })
      message.success('修改成功');
    }

    await refetch()
  }, [workspace, refetch])

  const items: any = useCallback((userId: string) => {
    if (isSelf(userId) && !canManageUsers) {
      return [{
        key: "quit",
        label: '退出',
        danger: true,
      }]
    } else {
      return [
        {
          key: UserType.Normal,
          label: '设置为普通成员',
        },
        {
          key: UserType.Developer,
          label: '设置为开发者',
        },
        {
          key: 'delete',
          label: '删除',
          danger: true,
        }
      ]
    }
  }, [workspace, isSelf, canManageUsers]);

  const columns: ColumnsType<WorkspaceMemberVO> = [
    {
      title: '成员名称',
      dataIndex: 'name',
      key: 'name',
      render: (name) => <span className="text-[#383F44] font-medium">{name}</span>
    },
    {
      title: '成员信息',
      dataIndex: 'email',
      key: 'email',
      render: (email) => <span className="text-[#58636C]">{email}</span>
    },
    {
      title: '成员权限',
      key: 'role',
      render: (_, record) => (
        <>
          {(canManageUsers || isSelf(record.userId!)) ? (
            <Dropdown
              menu={{
                items: items(record.userId),
                onClick: (e) => handleMenuClick(e, record.id!)
              }}
              disabled={record.role === UserType.Admin}
              trigger={['click']}
            >
              <Button className="rounded-xl border-[#E0E3E6] text-[#383F44] hover:text-[#40A5EE] hover:border-[#40A5EE] flex items-center gap-1">
                {getRoleLabel(record.role!)}
                {record.role !== UserType.Admin && <DownOutlined className="text-[10px] text-[#7C8B98]" />}
              </Button>
            </Dropdown>
          ) : (
            <span className="text-[#58636C] px-4 py-1 bg-[#F2F3F5] rounded-lg inline-block text-xs">
              {getRoleLabel(record.role!)}
            </span>
          )}
        </>

      ),
    },
  ];

  return (
    <div className="flex flex-col h-full overflow-hidden">
      <Header
        title="成员管理"
        placeholder="搜索成员"
        searchValue={searchValue}
        onSearchChange={setSearchValue}
        onSearch={handleSearch}
        showCreateButton={canManageUsers}
        createButtonText="添加成员"
        onCreateClick={showModal}
      />
      
      <div className="flex-1 overflow-y-auto px-4 pb-8">
        <div className="h-full bg-white/60 backdrop-blur-md rounded-2xl border border-white/80 shadow-sm overflow-hidden">
          <Table 
            columns={columns} 
            dataSource={data?.data?.list?.map(member => ({ ...member, key: member.id }))}
            rowKey={(record) => record?.id || ''}
            className="[&_.ant-table]:bg-transparent [&_.ant-table-thead_th]:bg-transparent [&_.ant-table-thead_th]:text-[#1D4A6B] [&_.ant-table-thead_th]:font-semibold [&_.ant-table-row:hover_td]:bg-white/40"
            pagination={
              {
                current: pageNo,
                pageSize: pageSize,
                total: Number(data?.data?.total || 10),
                onChange: (page, pageSize) => {
                  setPageNo(page);
                  setPageSize(pageSize);
                },
                className: "px-6 py-4 mb-0",
              }
            }
          />
        </div>
      </div>

      <AddMemberModal
        visible={isModalVisible}
        onCancel={handleCancel}
        onOk={handleAddMembers}
      />
    </div>
  );
}

const getRoleLabel = (role: UserType) => {
  switch (role) {
    case UserType.Normal:
      return '普通成员';
    case UserType.Developer:
      return '开发者';
    case UserType.Admin:
      return '管理员';
    default:
      return '未知角色';
  }
};
