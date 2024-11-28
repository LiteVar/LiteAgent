import React, {useState, useEffect, useMemo, Suspense, MouseEvent} from 'react';
import { Button, Dropdown, Form, Input, Layout, Menu, message, Modal, Skeleton, Image, Upload } from 'antd';
import {
  ToolOutlined,
  AppstoreOutlined,
  CheckOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined, PlusOutlined,
} from '@ant-design/icons';
import {useLocation, useNavigate, Link} from "react-router-dom";

import {getV1WorkspaceListOptions, getV1UserInfoOptions, getV1ChatRecentAgentOptions} from "@/client/@tanstack/query.gen";
import {putV1UserUpdate, putV1UserUpdatePwd, postV1FileUpload, Account, WorkSpaceVO} from "@/client";
import {useQuery} from '@tanstack/react-query'
import {removeAccessToken} from "@/utils/cache";
import avatar from "@/assets/dashboard/avatar.png";
import {ROUTES} from "@/config/constants";
import ResponseCode from "@/config/ResponseCode";
import { RcFile, UploadChangeParam } from 'antd/es/upload/interface';
import {buildImageUrl} from "@/utils/buildImageUrl";
import {ItemType} from "antd/es/menu/interface";

const {Sider, Content} = Layout;

const passwordRules = [
  { required: true, message: '请输入新密码' },
  { min: 8, message: '密码至少需要8个字符' },
  { max: 20, message: '密码最多不能超过20个字符' },
  {
    validator: (_: any, value: string) => {
      if (!/[a-zA-Z]/.test(value)) {
        return Promise.reject('密码必须包含至少一个字母');
      }
      if (!/\d/.test(value)) {
        return Promise.reject('密码必须包含至少一个数字');
      }
      return Promise.resolve();
    },
  },
];


export default function PageLayout({children}: {
  children: React.ReactNode;
}) {
  const [collapsed, setCollapsed] = useState(false);
  const [currentWorkspace, setCurrentWorkspace] = useState<WorkSpaceVO>();

  const [form] = Form.useForm()
  const [modalOpen, setModalOpen] = useState(false);
  const [modalType, setModalType] = useState<"SETTING" | "PASSWORD">("SETTING");
  const [userInfo, setUserInfo] = useState<Account>();
  const [name, setName] = useState<string>('');
  const [dropDownItems, setDropDownItems] = useState<ItemType[]>([]);
  const [editing, setEditing] = useState(false);
  const [menuItems, setMenuItems] = useState([
    {
      key: 'shop',
      icon: <AppstoreOutlined/>,
      label: <Link to={`/dashboard/${currentWorkspace?.id}/shop`}>agent商店</Link>,
    },
    {
      key: 'agents',
      label: 'agents',
    }]);

  const navigate = useNavigate();
  const pathname = useLocation().pathname;
  const workspaceId = pathname?.split('/')?.[2];

  const {data: userInfoResult, refetch} = useQuery({
    ...getV1UserInfoOptions({})
  })

  const {data: recentAgentListResult} = useQuery({
    ...getV1ChatRecentAgentOptions({
      headers: {
        'Workspace-id': workspaceId
      },
    }),
    enabled: !!workspaceId,
  })

  const {data} = useQuery({
    ...getV1WorkspaceListOptions({})
  });
  const workspaces = useMemo(() => data?.data || [], [data]);

  useEffect(() => {
    const workspaceId = pathname.split('/')[2];
    const workspace = workspaces.find(w => w.id === workspaceId) || workspaces[0];
    setCurrentWorkspace(workspace);
  }, [pathname, workspaces]);

  useEffect(() => {
    if (userInfoResult?.data) {
      setUserInfo(userInfoResult.data)
      setName(userInfoResult?.data?.name || "")
    }
  }, [userInfoResult]);

  useEffect(() => {
    if (recentAgentListResult?.data) {
      const chatItems = [
        {
          key: 'shop',
          icon: <AppstoreOutlined/>,
          label: <Link to={`/dashboard/${workspaceId}/shop`}>agent商店</Link>,
        },
        {
          key: 'agents',
          label: '最近对话',
        }];
      recentAgentListResult.data.map(item => {
        console.log('item', item);
        chatItems.push({
          key: `chat/${item.agentId}`,
          icon: <ToolOutlined/>,
          label: <Link to={`/dashboard/${workspaceId}/chat/${item.agentId}`}>{item.name}</Link>,
        })
      })
      setMenuItems(chatItems)
    }
  }, [recentAgentListResult, workspaceId])

  const onSignOut = () => {
    Modal.confirm({
      title: '退出登录',
      content: "确定退出？",
      okText: "确定",
      cancelText: "取消",
      onOk: () => {
        removeAccessToken();
        navigate(ROUTES.LOGIN)
        console.log('sign out');
        // 退出登录
        // 跳转到登录页面
      },
      onCancel() {
        console.log('sign out cancel');
      },
    });
  }

  const onSetting = () => {
    setModalType("SETTING");
    setModalOpen(true);
  }

  const onNavigateWorkspace = () => {
    window.open(ROUTES.WORKSPACES, '_blank');
  }

  function modifySecondPathSegment(url: string, secondSegment: string) {
    const urlObj = new URL(url);
    const pathArray = urlObj.pathname.split('/').filter(segment => segment !== '');

    if (pathArray.length >= 2) {
      pathArray[1] = secondSegment;
    }

    return `/${pathArray.join('/')}`;
  }

  const onChangeWorkerSpace = (workspaceId: string) => {
    const newUrl = modifySecondPathSegment(window.location.href, workspaceId);
    // navigate(newUrl)
    console.log('workspaceId', workspaceId);
    navigate(`/dashboard/${workspaceId}`)
  }

  const onModalCancel = () => {
    if (modalType === "PASSWORD") {
      setModalType("SETTING");
    } else {
      setModalOpen(false);
    }
    setEditing(false);
  }

  const onUpdatePassword = () => {
    setModalType("PASSWORD");
  }

  const validateConfirmPassword = (_: any, value: string) => {
    if (!value || form.getFieldValue('newPassword') === value) {
      return Promise.resolve();
    }
    return Promise.reject(new Error('两次输入的密码不一致'));
  };

  const onUpdateUserName = async (event: MouseEvent<HTMLElement>) => {
    event.stopPropagation();
    if (!name) {
      message.error("昵称不能为空");
    } else if (!!name && name != userInfo?.name) {
      console.log('update name', name);
      const res = await putV1UserUpdate({
        query: {
          avatar: userInfo?.avatar,
          name: name,
        }
      })
      if (res?.data?.code === ResponseCode.S_OK) {
        message.success("昵称修改成功");
        await refetch();
        setEditing(false);
      } else {
        message.error("昵称修改失败");
      }
    } else {
      setEditing(false);
    }
  }

  const onEditName = () => {
    !!userInfo && setName(userInfo?.name || "");
    setEditing(true);
  }

  const onUserNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    event.stopPropagation();
    setName(event.target.value.trim());
  }

  const onChangePassword = async (values: any) => {
    Modal.confirm({
      title: '修改密码',
      content: '修改密码后，您将需要重新登录！',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        console.log("values", values);
        const res = await putV1UserUpdatePwd({
          query: {
            originPwd: values.originPassword,
            newPwd: values.newPassword,
          }
        })
        if (res?.data?.code === ResponseCode.S_OK) {
          message.success("密码修改成功");
          removeAccessToken();
          navigate(ROUTES.LOGIN)
        } else {
          message.error(res?.data?.message || "密码修改失败");
        }
      }
    })
  }

  useEffect(() => {
    const items = [
      {
        type: 'divider',
      },
      {
        key: '1',
        label: (
          <div onClick={onNavigateWorkspace} className="cursor-pointer py-1.5 px-3">
            管理我的workspace
          </div>
        ),
      },
      {
        key: '2',
        label: (
          <div onClick={onSetting} className="cursor-pointer py-1.5 px-3">
            设置
          </div>
        ),
      },
      {
        key: '3',
        label: (
          <div onClick={onSignOut} className="cursor-pointer py-1.5 px-3">
            退出登录
          </div>
        ),
      },
    ];
    if (workspaces.length > 0) {
      workspaces.reverse().map((item) => {
        items.unshift({
          key: item?.id!,
          label: (
            <div>
              <div onClick={() => onChangeWorkerSpace(item?.id!)}
                   className="flex items-center max-w-[230px] overflow-hidden py-1.5 px-3">
                <div className="w-full cursor-pointer flex-1 text-ellipsis overflow-hidden mr-6">
                  {item.name}
                </div>
                {currentWorkspace?.id === item.id &&
                  <CheckOutlined size={14} className='flex-none'></CheckOutlined>}
              </div>
            </div>
          ),
        })
      })
    }

    setDropDownItems(items as any);
  }, [workspaces, currentWorkspace])


  const onUploadAction = async (file: RcFile) => {
    const response = await postV1FileUpload({
      body: {
        file: file
      }
    });
    if (response.data?.code === ResponseCode.S_OK) {
      return "/v1/file/download?filename=" + response.data.data
    } else {
      message.error('上传失败');
      return '';
    }
  }

  const handleImageUpload = async (info: UploadChangeParam) => {
    console.log(info)
    if (info.file.status === 'done') {
      await putV1UserUpdate({
        query: {
          avatar: info.file.xhr.responseURL.split('=')[1],
          name: userInfo?.name!,
        }
      })
      await refetch();
      await message.success(`${info.file.name} 上传成功`)
    } else if (info.file.status === 'error') {
      message.error(`${info.file.name} 上传失败`);
    }
  };

  return (
    <Layout className="h-[100vh] overflow-hidden">
      <Sider trigger={null} collapsible collapsed={collapsed} onCollapse={(value) => setCollapsed(value)}
             className="pageLayoutSider bg-gray-900 py-3 flex flex-col" width={250}>
        <div className="mt-3 px-5 flex-none flex justify-center items-center mb-9">
          {!collapsed && <div className='flex-1 text-lg text-white'>LiteAgent</div>}
          <div className="flex-none">
            {!collapsed &&
              <MenuFoldOutlined onClick={() => setCollapsed(!collapsed)} style={{fontSize: '16px', color: '#fff'}}/>}
            {collapsed &&
              <MenuUnfoldOutlined onClick={() => setCollapsed(!collapsed)} style={{fontSize: '16px', color: '#fff'}}/>}
          </div>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[(pathname.split('/')[4] ? `${pathname.split('/')[3]}/${pathname.split('/')[4]}` : pathname.split('/')[3]) || "shop"]}
          items={menuItems}
          className="bg-gray-900 border-r-0 !px-2 flex-1 overflow-y-auto"
        />
        <Dropdown  overlayClassName="dashboardDropdown" menu={{items: dropDownItems}} placement="top">
          <div className="flex-none px-5 justify-center h-[40px] flex justify-center items-center">
            <Image preview={false} className='flex-none w-6 rounded-full' src={buildImageUrl(userInfo?.avatar!) || avatar} alt='avatar'/>
            {!collapsed && <div className='flex-1 ml-3 line-clamp-1 break-all text-sm text-white cursor-pointer'>{userInfo?.name}</div>}
          </div>
        </Dropdown>

        <Modal
          title={modalType === "SETTING" ? "设置" : "修改密码"}
          closable
          onCancel={onModalCancel}
          className="!w-[538px]"
          footer={null}
          maskClosable={false}
          open={modalOpen}
          centered
        >
          <div className="px-8 pt-7 pb-12 flex flex-col items-center">
            {modalType === "SETTING" && <div className="w-full">
              <div className="flex items-end mb-5">
                <div className="w-[88px] flex justify-end font-xs mr-3">头像：</div>
                <div className='avatarWrapper'>
                  <Upload
                      name="icon"
                      maxCount={1}
                      defaultFileList={userInfo?.avatar ? [{uid: "", name: "", thumbUrl: buildImageUrl(userInfo?.avatar)}] : undefined}
                      accept=".png,.jpg,.jpeg,.svg,.gif,.webp"
                      listType="picture-card"
                      className="avatar-uploader"
                      showUploadList={{
                        showDownloadIcon: false,
                        showRemoveIcon: false,
                        showPreviewIcon: false
                      }}
                      action={onUploadAction}
                      onChange={handleImageUpload}
                  >
                    <div>
                      <PlusOutlined />
                      <div style={{ marginTop: 8 }}>{userInfo?.avatar ? "修改头像" : "上传头像"}</div>
                    </div>
                  </Upload>
                </div>
              </div>
              <div className="flex items-center h-[46px]">
                <div className="w-[88px] flex justify-end font-xs mr-3 flex-none">昵称：</div>
                {!editing && <div className='flex items-center'>
                  <div className='mr-2 font-xs line-clamp-1 break-all'>{userInfo?.name}</div>
                  <div onClick={onEditName} className='font-xs text-[#1296DB] cursor-pointer flex-none'>修改</div>
                </div>}
                {editing && <div className="flex items-center font-xs">
                  <input autoFocus onChange={onUserNameChange} value={name}
                         className='h-8 font-xs text-black border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none' />
                  <div onClick={onUpdateUserName} className="mx-2 flex-none font-xs cursor-pointer border border-[#D9D9D9] border-solid rounded-md py-2.5 px-4">确认</div>
                  <div className="flex-none font-xs cursor-pointer border border-[#D9D9D9] border-solid rounded-md py-2.5 px-4" onClick={() => setEditing(false)}>取消</div>
                </div>}

              </div>
              <div className="w-full h-px bg-[#F2F2F2] my-6"></div>
              <div className="flex items-center mb-6">
                <div className="w-[88px] flex justify-end font-xs mr-3">账号：</div>
                <input disabled
                       className="w-[266px] h-8 font-xs text-black/50 border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none"
                       value={userInfo?.email}/>
              </div>
              <div className="flex items-center">
                <div className="w-[88px] flex justify-end font-xs mr-3">密码：</div>
                <div onClick={onUpdatePassword}
                     className="font-xs text-[#2A82E4] cursor-pointer opacity-70">修改密码
                </div>
              </div>
            </div>}
            {modalType === "PASSWORD" && <div className="w-full updatePassword">
              <Form
                form={form}
                name="update_password"
                onFinish={onChangePassword}
              >
                <Form.Item
                  name="originPassword"
                  rules={[{required: true, message: '请输入旧密码!'}]}
                >
                  <div className="flex items-center">
                    <div className="w-[88px] flex justify-end font-xs mr-3">旧密码：</div>
                    <Input
                      className="w-[266px] h-8 font-xs text-black/50 border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none"
                      type="password" placeholder="请输入旧密码"/>
                  </div>
                </Form.Item>
                <Form.Item
                  name="newPassword"
                  rules={passwordRules}
                >
                  <div className="flex items-center">
                    <div className="w-[88px] flex justify-end font-xs mr-3">新密码：</div>
                    <Input
                      className="w-[266px] h-8 font-xs text-black/50 border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none"
                      type="password" placeholder="请输入新密码"/>
                  </div>
                </Form.Item>

                <Form.Item
                  name="confirmPassword"
                  rules={[
                    {required: true, message: '请再次输入新密码!'},
                    {validator: validateConfirmPassword}
                  ]}
                >
                  <div className="flex items-center">
                    <div className="w-[88px] flex justify-end font-xs mr-3">新密码确认：</div>
                    <Input
                      className="w-[266px] h-8 font-xs text-black/50 border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none"
                      type="password" placeholder="请再次输入新密码"/>
                  </div>
                </Form.Item>

                <Form.Item>
                  <div className="flex items-center justify-end mt-10">
                    <div onClick={onModalCancel}
                         className="w-[88px] h-8 cursor-point flex items-center justify-center border border-solid border-[#D9D9D9] rounded-sm text-black/65 mr-4">取消
                    </div>
                    <Button type="primary" htmlType="submit"
                            className="w-[88px] h-8 flex items-center justify-center border border-solid border-[#1890FF] rounded-sm bg-[#1890FF] text-white">
                      确定
                    </Button>
                  </div>
                </Form.Item>
              </Form>
            </div>}
          </div>
        </Modal>
      </Sider>
      <Layout>
        <Suspense fallback={<Skeleton/>}>
          <Content className="bg-white rounded-lg p-5">
            {children}
          </Content>
        </Suspense>
      </Layout>
    </Layout>
  );
}
