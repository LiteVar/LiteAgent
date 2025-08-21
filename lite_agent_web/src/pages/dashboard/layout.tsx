import React, { useState, useEffect, useMemo, Suspense } from 'react';
import { Dropdown, Layout, Menu, Modal, Skeleton, Image, Tooltip } from 'antd';
import {
  AppstoreOutlined,
  CheckOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import { useLocation, useNavigate, Link } from 'react-router-dom';

import {
  getV1WorkspaceListOptions,
  getV1UserInfoOptions,
  getV1ChatRecentAgentOptions,
} from '@/client/@tanstack/query.gen';
import { Account, WorkSpaceVO, postV1AuthLogout } from '@/client';
import { useQuery } from '@tanstack/react-query';
import { removeAccessToken } from '@/utils/cache';
import avatar from '@/assets/dashboard/avatar.png';
import { ROUTES } from '@/constants/routes';
import { buildImageUrl } from '@/utils/buildImageUrl';
import { ItemType } from 'antd/es/menu/interface';
import AgentNameInput from '@/components/AgentNameInput';
import UserSettingsModal from '@/pages/dashboard/components/UserSettingsModal';
import shopIcon from '@/assets/dashboard/agent-shop.svg';
import logoIcon from '@/assets/login/logo_svg';
import { AgentMessageMap } from '../../components/chat/Chat';
import { MessageRole } from '../../types/Message';

const { Sider, Content } = Layout;

export default function PageLayout({ children, agentMap }: { children: React.ReactNode, agentMap?: AgentMessageMap }) {
  const [collapsed, setCollapsed] = useState(false);
  const [currentWorkspace, setCurrentWorkspace] = useState<WorkSpaceVO>();
  const [modalOpen, setModalOpen] = useState(false);
  const [userInfo, setUserInfo] = useState<Account>();
  const [name, setName] = useState<string>('');
  const [dropDownItems, setDropDownItems] = useState<ItemType[]>([]);
  const [menuItems, setMenuItems] = useState([]);

  const navigate = useNavigate();
  const pathname = useLocation().pathname;
  const workspaceId = pathname?.split('/')?.[2];

  const { data: userInfoResult, refetch } = useQuery({
    ...getV1UserInfoOptions({}),
  });

  const { data: recentAgentListResult } = useQuery({
    ...getV1ChatRecentAgentOptions({
      headers: {
        'Workspace-id': workspaceId,
      },
    }),
    enabled: !!workspaceId,
  });

  const { data } = useQuery({
    ...getV1WorkspaceListOptions({}),
  });
  const workspaces = useMemo(() => data?.data || [], [data]);

  useEffect(() => {
    const workspaceId = pathname.split('/')[2];
    const workspace = workspaces.find((w) => w.id === workspaceId) || workspaces[0];
    setCurrentWorkspace(workspace);
  }, [pathname, workspaces]);

  useEffect(() => {
    if (userInfoResult?.data) {
      setUserInfo(userInfoResult.data);
      setName(userInfoResult?.data?.name || '');
    }
  }, [userInfoResult]);

  useEffect(() => {
    if (recentAgentListResult?.data) {
      const chatItems = [];
      recentAgentListResult.data.map((item) => {
        let responding = false;
        const length = agentMap?.[item.agentId]?.messages?.filter((message) => message.role != MessageRole.SEPARATOR)?.length;
        let num = 0;
        agentMap?.[item.agentId]?.messages?.filter((message) => message.role != MessageRole.SEPARATOR)?.map((message, index) => {
          if (message.responding) {
            responding = true;
          } else {
            num = num + 1;
          }
        });
        if (num == length) {
          responding = false;
        }
        chatItems.push({
          key: `chat/${item.agentId}`,
          icon: <span className='w-4 h-4'>{logoIcon}</span>,
          label: <AgentNameInput responding={responding} collapsed={collapsed} agent={item} workspaceId={workspaceId} />,
        });
      });
      setMenuItems(chatItems);
    }
  }, [recentAgentListResult, workspaceId, collapsed, agentMap]);

  const onSignOut = async () => {
    Modal.confirm({
      title: '退出登录',
      content: '确定退出？',
      okText: '确定',
      cancelText: '取消',
      centered: true,
      onOk: async () => {
        await postV1AuthLogout({});
        removeAccessToken();
        navigate(ROUTES.LOGIN);
        console.log('sign out');
      },
      onCancel() {
        console.log('sign out cancel');
      },
    });
  };

  const onNavigateWorkspace = () => {
    window.open(ROUTES.WORKSPACES, '_blank');
  };

  const onChangeWorkerSpace = (workspaceId: string) => {
    navigate(`/dashboard/${workspaceId}`);
  };

  useEffect(() => {
    const workspaceItems = workspaces.map((item) => ({
      key: item?.id!,
      label: (
        <div>
          <div
            onClick={() => onChangeWorkerSpace(item?.id!)}
            className="flex items-center max-w-[230px] overflow-hidden py-1.5 px-3"
          >
            <div className="w-full cursor-pointer flex-1 text-ellipsis overflow-hidden mr-6">
              {item.name}
            </div>
            {currentWorkspace?.id === item.id && (
              <CheckOutlined size={14} className="flex-none"></CheckOutlined>
            )}
          </div>
        </div>
      ),
    }));

    const items = [
      ...workspaceItems,
      {
        type: 'divider',
      },
      {
        key: 'admin',
        label: (
          <div onClick={onNavigateWorkspace} className="cursor-pointer w-full py-1.5 px-3">
            管理我的workspace
          </div>
        ),
      },
      {
        key: 'setting',
        label: (
          <div onClick={() => setModalOpen(true)} className="cursor-pointer w-full py-1.5 px-3">
            设置
          </div>
        ),
      },
      {
        key: 'signout',
        label: (
          <div onClick={onSignOut} className="cursor-pointer w-full py-1.5 px-3">
            退出登录
          </div>
        ),
      },
    ];

    setDropDownItems(items as any);
  }, [workspaces, currentWorkspace]);

  const refreshAgent = async () => {
    await refetch();
  };

  const onNavigateShop = () => {
    navigate(`/dashboard/${workspaceId}/shop`);
  }

  return (
    <Layout className="h-[100vh] overflow-hidden text-base">
      <Sider
        theme="dark"
        trigger={null}
        collapsible
        collapsed={collapsed}
        onCollapse={(value) => setCollapsed(value)}
        className="pageLayoutSider flex flex-col px-3 pb-3 bg-[#001529] text-white"
        width={240}
      >
        <div className={`mt-6 mb-8 flex-none flex items-center justify-center pr-2 ${collapsed ? 'pl-2' : 'pl-6'}`}>
          {!collapsed && <div className="flex-1 text-white text-lg font-medium h-7 overflow-hidden">Lite Agent</div>}
          <div className="flex-none w-8 h-8 flex justify-center items-center cursor-pointer rounded-lg hover:bg-black/10">
            {!collapsed ? <MenuFoldOutlined onClick={() => setCollapsed(!collapsed)} /> : 
             <MenuUnfoldOutlined onClick={() => setCollapsed(!collapsed)} />}
          </div>
        </div>
        <div className="w-full flex-none">
          <Tooltip title={'agent商店'} open={(collapsed) ? undefined : false}>
            <div onClick={onNavigateShop} className={`mx-1 my-1 pl-6 pr-4 h-10 cursor-pointer flex items-center ${collapsed ? 'w-12 justify-center': ''}`}>
              <img className="w-4 mr-3" src={shopIcon} />
              {!collapsed && <div className='text-[rgba(255,255,255,.7)] text-sm overflow-hidden h-5'>agent商店</div>}
            </div>
          </Tooltip>

          <div className="w-full h-px bg-[rgba(255,255,255,.2)] my-2"></div>
          <div className={`text-[rgba(255,255,255,.5)] text-sm mx-1 mb-3 mt-4 pr-4 h-4 overflow-hidden ${collapsed ? 'pl-4' : 'pl-6'}`}>最近对话</div>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[
            (pathname.split('/')[4]
              ? `${pathname.split('/')[3]}/${pathname.split('/')[4]}`
              : pathname.split('/')[3]) || 'shop',
          ]}
          items={menuItems}
          className="bg-[#001529] border-r-0 flex-1 overflow-y-auto
            [&_.ant-menu-item-selected]:bg-[#2D4257]
         "
        />
        <Dropdown overlayClassName="dashboardDropdown" menu={{ items: dropDownItems }} placement="top">
          <div className="flex-none px-4 py-3 mt-2 flex items-center cursor-pointer hover:bg-black/10 rounded-lg">
            <Image
              preview={false}
              className="flex-none w-8 h-8 rounded-full"
              src={buildImageUrl(userInfo?.avatar!) || avatar}
              alt="avatar"
            />
            {!collapsed && (
              <div className="flex-1 text-sm ml-3 line-clamp-1 break-all">
                {userInfo?.name}
              </div>
            )}
          </div>
        </Dropdown>

        <UserSettingsModal
          modalOpen={modalOpen}
          refreshAgent={refreshAgent}
          userInfo={userInfo}
          name={name}
          setName={setName}
          onModalCancel={() => setModalOpen(false)}
        />
      </Sider>
      <Layout>
        <Suspense fallback={<Skeleton />}>
          <Content className="bg-white">{children}</Content>
        </Suspense>
      </Layout>
    </Layout>
  );
}
