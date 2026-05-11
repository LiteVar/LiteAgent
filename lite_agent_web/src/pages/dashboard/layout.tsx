import React, { useState, useEffect, useMemo, Suspense } from 'react';
import { Dropdown, Layout, Menu, Modal, Skeleton, Image, Tooltip } from 'antd';
import {
  CheckOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useLocation, useNavigate } from 'react-router-dom';

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
import shopSelectedIcon from '@/assets/dashboard/agent-shop-active.svg';
import logoIcon from '@/assets/login/logo_agent.png';
import Bg from '@/assets/common/bg';
import { MessageRole } from '../../types/Message';
import { AgentMessageMap } from '@/types/chat/messages';

const { Content } = Layout;

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
            <div className="w-full cursor-pointer break-all line-clamp-1 flex-1 text-ellipsis overflow-hidden mr-6">
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
      ...((userInfo as any)?.systemRole ? [{
        key: 'system',
        label: (
          <div onClick={onNavigateAdmin} className="cursor-pointer w-full flex items-center py-1.5 px-3">
            <svg width="13" height="12" viewBox="0 0 13 12" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M5.0625 7.5C6.6158 7.5 7.875 6.2408 7.875 4.6875C7.875 3.1342 6.6158 1.875 5.0625 1.875C3.5092 1.875 2.25 3.1342 2.25 4.6875C2.25 6.2408 3.5092 7.5 5.0625 7.5Z" stroke="#454545" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M1.125 9.375C2.08828 8.22891 3.44813 7.5 5.0625 7.5C6.67687 7.5 8.03672 8.22891 9 9.375" stroke="#454545" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M10.5 7.125C10.9142 7.125 11.25 6.78921 11.25 6.375C11.25 5.96079 10.9142 5.625 10.5 5.625C10.0858 5.625 9.75 5.96079 9.75 6.375C9.75 6.78921 10.0858 7.125 10.5 7.125Z" stroke="#454545" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M10.5 5.625V5.0625" stroke="#454545" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M9.85031 6L9.36328 5.71875" stroke="#343330" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M9.85031 6.75L9.36328 7.03125" stroke="#343330" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M10.5 7.125V7.6875" stroke="#343330" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M11.1497 6.75L11.6367 7.03125" stroke="#343330" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M11.1497 6L11.6367 5.71875" stroke="#343330" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            <div className="ml-2">系统管理</div>
          </div>
        ),
      }] : []),
      {
        key: 'admin',
        label: (
          <div onClick={onNavigateWorkspace} className="cursor-pointer w-full flex items-center py-1.5 px-3">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M3.75 5.25C4.57843 5.25 5.25 4.57843 5.25 3.75C5.25 2.92157 4.57843 2.25 3.75 2.25C2.92157 2.25 2.25 2.92157 2.25 3.75C2.25 4.57843 2.92157 5.25 3.75 5.25Z" stroke="#454545" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M8.25 5.25C9.07843 5.25 9.75 4.57843 9.75 3.75C9.75 2.92157 9.07843 2.25 8.25 2.25C7.42157 2.25 6.75 2.92157 6.75 3.75C6.75 4.57843 7.42157 5.25 8.25 5.25Z" stroke="#454545" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M3.75 9.75C4.57843 9.75 5.25 9.07843 5.25 8.25C5.25 7.42157 4.57843 6.75 3.75 6.75C2.92157 6.75 2.25 7.42157 2.25 8.25C2.25 9.07843 2.92157 9.75 3.75 9.75Z" stroke="#454545" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M8.25 9.75C9.07843 9.75 9.75 9.07843 9.75 8.25C9.75 7.42157 9.07843 6.75 8.25 6.75C7.42157 6.75 6.75 7.42157 6.75 8.25C6.75 9.07843 7.42157 9.75 8.25 9.75Z" stroke="#454545" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            <div className="ml-2">管理我的workspace</div>
          </div>
        ),
      },
      {
        key: 'setting',
        label: (
          <div onClick={() => setModalOpen(true)} className="cursor-pointer w-full flex items-center py-1.5 px-3">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M6 7.875C7.03553 7.875 7.875 7.03553 7.875 6C7.875 4.96447 7.03553 4.125 6 4.125C4.96447 4.125 4.125 4.96447 4.125 6C4.125 7.03553 4.96447 7.875 6 7.875Z" stroke="#454545" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M1.94203 8.34818C1.73483 7.99129 1.57609 7.6084 1.47 7.20959L2.25656 6.22521C2.24766 6.07472 2.24766 5.92383 2.25656 5.77334L1.47047 4.78896C1.57638 4.3901 1.7348 4.00706 1.94156 3.6499L3.19359 3.50928C3.29358 3.39663 3.40017 3.29004 3.51281 3.19006L3.65344 1.9385C4.01008 1.73271 4.39249 1.57524 4.79063 1.47021L5.775 2.25678C5.92549 2.24787 6.07638 2.24787 6.22688 2.25678L7.21125 1.47068C7.61012 1.57659 7.99316 1.73501 8.35031 1.94178L8.49094 3.19381C8.60358 3.29379 8.71018 3.40038 8.81016 3.51303L10.0617 3.65365C10.2689 4.01054 10.4277 4.39344 10.5338 4.79225L9.74719 5.77662C9.75609 5.92711 9.75609 6.078 9.74719 6.2285L10.5333 7.21287C10.4281 7.61162 10.2705 7.99466 10.0645 8.35193L8.8125 8.49256C8.71252 8.6052 8.60593 8.7118 8.49328 8.81178L8.35266 10.0633C7.99577 10.2705 7.61287 10.4293 7.21406 10.5354L6.22969 9.74881C6.0792 9.75771 5.92831 9.75771 5.77781 9.74881L4.79344 10.5349C4.39469 10.4297 4.01165 10.2721 3.65438 10.0662L3.51375 8.81412C3.40111 8.71414 3.29451 8.60755 3.19453 8.4949L1.94203 8.34818Z" stroke="#454545" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            <div className="ml-2">设置</div>
          </div>
        ),
      },
      {
        key: 'signout',
        label: (
          <div onClick={onSignOut} className="cursor-pointer w-full flex items-center py-1.5 px-3">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M5.25 1.875H2.25V10.125H5.25" stroke="#383F44" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M5.25 6H10.5" stroke="#383F44" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M8.625 4.125L10.5 6L8.625 7.875" stroke="#383F44" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            <div className="ml-2">退出登录</div>
          </div>
        ),
      },
    ];

    setDropDownItems(items as any);
  }, [workspaces, currentWorkspace, userInfo]);

  const refreshAgent = async () => {
    await refetch();
  };

  const onNavigateShop = () => {
    navigate(`/dashboard/${workspaceId}/shop`);
  };

  const isShopSelected = (pathname.split('/')[3] || 'shop') === 'shop';

  const onNavigateAdmin = () => {
    window.open('/admin', '_blank');
  }

  return (
    <div className="relative h-[100vh] overflow-hidden flex flex-row text-base">
      <Bg/>

      {/* Sidebar 外层：padding 容器，宽度随折叠状态动态变化 */}
      <div className={`relative flex-none flex flex-row items-stretch py-4 px-4 transition-all duration-300 ${collapsed ? 'w-[76px]' : 'w-[256px]'}`}>
        {/* Sidebar Content：玻璃态卡片，收起时更透明 (0.3)，展开时 (0.6) */}
        <div
          className={`flex-1 flex flex-col rounded-2xl overflow-hidden transition-all duration-300 backdrop-blur-[4px] border border-white/80 gap-0 ${
            collapsed ? 'bg-white/30' : 'bg-white/60'
          }`}
        >
          {/* Header：展开时 Logo + Title + 折叠图标，收起时只有折叠图标 */}
          <div className={`flex-none flex items-center px-6 py-8 ${collapsed ? 'justify-center' : 'justify-between'}`}>
            {!collapsed && (
              <div className="flex items-center gap-3 flex-1 overflow-hidden">
                <img className="h-8 flex-none" src={logoIcon} alt="logo" />
              </div>
            )}
            <div
              className="flex-none w-8 h-8 flex justify-center items-center cursor-pointer rounded-xl hover:bg-black/5 text-[#58636C] transition-colors"
              onClick={() => setCollapsed(!collapsed)}
            >
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </div>
          </div>

          {/* Agent商店入口 */}
          <div className="flex-none px-3">
            <Tooltip title="Agent 商店" placement="right" open={collapsed ? undefined : false}>
              <div
                onClick={onNavigateShop}
                className={`flex items-center gap-3 rounded-xl transition-all cursor-pointer hover:bg-white ${
                  collapsed ? 'justify-center p-3' : 'px-4 py-2 my-4 mb-8'
                } ${isShopSelected ? 'bg-white' : ''}`}
              >
                <img className="w-5 h-5 flex-none" src={isShopSelected ? shopSelectedIcon : shopIcon} alt="shop" />
                {!collapsed && <span className="text-[#383F44] text-sm font-medium">Agent 商店</span>}
              </div>
            </Tooltip>
            {!collapsed && (
              <div className="text-[#94A0AB] text-xs font-semibold px-4 my-2 uppercase tracking-wider">最近对话</div>
            )}
          </div>

          {/* 最近对话菜单：透明度控制渐显/渐隐，始终保留在 DOM 中 */}
          <Menu
            theme="light"
            mode="inline"
            inlineCollapsed={false}
            onClick={({ key }) => navigate(`/dashboard/${workspaceId}/${key}`)}
            selectedKeys={[
              (pathname.split('/')[4]
                ? `${pathname.split('/')[3]}/${pathname.split('/')[4]}`
                : pathname.split('/')[3]) || 'shop',
            ]}
            items={menuItems}
            className="border-r-0 bg-transparent px-2
              [&_.ant-menu-item]:rounded-xl
              [&_.ant-menu-item]:!h-11
              [&_.ant-menu-item]:my-1
              [&_.ant-menu-item-selected]:!bg-white
              [&_.ant-menu-item-selected]:!shadow-sm
              [&_.ant-menu-item:hover]:!bg-white
              [&_.ant-menu-item-selected]:!text-[#1D4A6B]
              [&_.ant-menu-item-selected]:font-medium
            "
            style={{
              flex: collapsed ? '0 0 0' : '1 1 0',
              overflowX: 'hidden',
              overflowY: 'auto',
              opacity: collapsed ? 0 : 1,
              pointerEvents: collapsed ? 'none' : 'auto',
              transition: 'opacity 0.2s ease, flex 0.3s ease',
            }}
          />

          {/* 用户信息：收起时只显示头像，展开时显示头像+名字 */}
          <div className="flex-none p-3 mt-auto">
            <Dropdown overlayClassName="dashboardDropdown" menu={{ items: dropDownItems }} placement="topRight">
              <div className={`flex items-center gap-3 rounded-2xl cursor-pointer hover:bg-white/60 hover:shadow-sm transition-all border border-transparent hover:border-white/40 ${collapsed ? 'justify-center p-2' : 'p-3'}`}>
                <div className="flex-none relative">
                  <Image
                    preview={false}
                    className="w-9 h-9 rounded-full ring-2 ring-white/80 shadow-sm"
                    src={buildImageUrl(userInfo?.avatar!) || avatar}
                    alt="avatar"
                  />
                </div>
                {!collapsed && (
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-semibold text-[#1D4A6B] truncate">
                      {userInfo?.name}
                    </div>
                  </div>
                )}
              </div>
            </Dropdown>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="relative flex-1 flex flex-col overflow-hidden">
        <Suspense fallback={<Skeleton />}>
          <Content className="bg-transparent h-full">{children}</Content>
        </Suspense>
      </div>

      <UserSettingsModal
        modalOpen={modalOpen}
        refreshAgent={refreshAgent}
        userInfo={userInfo}
        name={name}
        setName={setName}
        onModalCancel={() => setModalOpen(false)}
      />
    </div>
  );
}
