import React, { Suspense } from 'react';
import { Layout, Menu, Skeleton } from 'antd';
import { useLocation, Link, Outlet } from 'react-router-dom';
import { getV1UserInfoOptions } from '@/client/@tanstack/query.gen';
import { useQuery } from '@tanstack/react-query';
import { AdminProvider } from '@/contexts/adminContext';
import Bg from '@/assets/common/bg';
import { ModelIcon, PluginConnectIcon } from '@/assets/workspaces/workspace_tabs_icons_svg';

const { Sider, Content } = Layout;

export default function AdminLayout() {
  const location = useLocation();
  const pathname = location.pathname;

  const { data: userInfoResult } = useQuery({
    ...getV1UserInfoOptions({}),
  });
  const userInfo = userInfoResult?.data;

  const menuItems = [
    {
      key: 'models',
      icon: <ModelIcon active={pathname.includes('models')} />,
      label: <Link to={`/admin/models`}>模型管理</Link>,
    },
    {
      key: 'plugins',
      icon: <PluginConnectIcon active={pathname.includes('plugins')} />,
      label: <Link to={`/admin/plugins`}>插件管理</Link>,
    }
  ];

  // 获取当前选中的菜单项
  const selectedKey = pathname.split('/')[2] || 'models';

  return (
    <Layout className="h-screen overflow-hidden bg-white relative">
      <div className="fixed inset-0 pointer-events-none z-0">
        <Bg/>
      </div>
      <Sider
        theme="light"
        className="py-4 pl-4 backdrop-blur-md border-r border-white/80 h-screen overflow-hidden z-20 [&_.ant-layout-sider-children]:py-2 [&_.ant-layout-sider-children]:px-4 [&_.ant-layout-sider-children]:bg-white/60 [&_.ant-layout-sider-children]:rounded-2xl"
        width={288}
      >
        <div className="mb-8 px-4 bg-white rounded-xl flex items-center">
          <div>
          <svg width="25" height="24" viewBox="0 0 25 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M10.125 15C13.2316 15 15.75 12.4816 15.75 9.375C15.75 6.2684 13.2316 3.75 10.125 3.75C7.0184 3.75 4.5 6.2684 4.5 9.375C4.5 12.4816 7.0184 15 10.125 15Z" stroke="#454545" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M2.25 18.75C4.17656 16.4578 6.89625 15 10.125 15C13.3537 15 16.0734 16.4578 18 18.75" stroke="#454545" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M21 14.25C21.8284 14.25 22.5 13.5784 22.5 12.75C22.5 11.9216 21.8284 11.25 21 11.25C20.1716 11.25 19.5 11.9216 19.5 12.75C19.5 13.5784 20.1716 14.25 21 14.25Z" stroke="#454545" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M21 11.25V10.125" stroke="#454545" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M19.7006 12L18.7266 11.4375" stroke="#343330" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M19.7006 13.5L18.7266 14.0625" stroke="#343330" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M21 14.25V15.375" stroke="#343330" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M22.2993 13.5L23.2734 14.0625" stroke="#343330" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M22.2993 12L23.2734 11.4375" stroke="#343330" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          </div>
          <h2 className="text-[#1D4A6B] text-[18px] ml-2 font-medium">系统管理</h2>
        </div>

        <Menu
          theme="light"
          mode="inline"
          items={menuItems}
          selectedKeys={[selectedKey]}
          className="bg-transparent border-r-0 [&_.ant-menu-item]:rounded-xl [&_.ant-menu-item-selected]:bg-white [&_.ant-menu-item-selected]:text-[#1D4A6B] [&_.ant-menu-item-selected]:shadow-sm [&_.ant-menu-item]:text-[#7C8B98] [&_.ant-menu-item]:font-medium [&_.ant-menu-item:hover]:text-[#1D4A6B]"
        />
      </Sider>
      <Layout className="h-screen overflow-hidden bg-transparent z-10">
        <Suspense fallback={<Skeleton active className="p-8" />}>
          <Content className="h-full overflow-y-auto">
            <AdminProvider value={{ userInfo: userInfo }}>
              <Outlet />
            </AdminProvider>
          </Content>
        </Suspense>
      </Layout>
    </Layout>
  );
}
