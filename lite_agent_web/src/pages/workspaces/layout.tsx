import React, { useState, useEffect, useMemo, useCallback, Suspense } from 'react';
import { Layout, Menu, message, Select, Skeleton } from 'antd';
import {
  UserOutlined,
  SwapOutlined,
} from '@ant-design/icons';
import { useNavigate, useLocation, Link, Outlet } from 'react-router-dom';
import { getV1UserInfoOptions, getV1WorkspaceListOptions } from '@/client/@tanstack/query.gen';
import { useQuery } from '@tanstack/react-query';
import { WorkSpaceVO } from '@/client';
import { WorkspaceProvider } from '@/contexts/workspaceContext';
import { ToolIcon, DatasetsIcon, ModelIcon, UserIcon, AgentIcon, PluginConnectIcon } from '@/assets/workspaces/workspace_tabs_icons_svg';
import Bg from '@/assets/common/bg';

const { Sider, Content } = Layout;

export default function WorkspaceLayout() {
  const location = useLocation();
  const pathname = location.pathname;
  const workspaceId = pathname?.split('/')?.[2];
  const [collapsed, setCollapsed] = useState(false);
  const [currentWorkspace, setCurrentWorkspace] = useState<WorkSpaceVO>();

  const navigate = useNavigate();

  const { data } = useQuery({
    ...getV1WorkspaceListOptions({}),
  });
  const { data: userInfoResult } = useQuery({
    ...getV1UserInfoOptions({}),
  });

  const workspaces = useMemo(() => data?.data || [], [data]);
  const userInfo = userInfoResult?.data;

  useEffect(() => {
    const workspaceId = pathname.split('/')[2];
    const workspace = workspaces.find((w) => w.id === workspaceId) || workspaces[0];
    setCurrentWorkspace(workspace);
  }, [pathname, workspaces]);

  const handleWorkspaceChange = useCallback(
    (value: string) => {
      const newWorkspace = workspaces.find((w) => w.id === value);
      setCurrentWorkspace(newWorkspace);
      const newPath = `/workspaces/${value}/${pathname.split('/')[3] || 'agents'}`;
      navigate(newPath);
    },
    [workspaces, navigate, pathname]
  );

  const menuItems = [
    {
      key: 'agents',
      icon: <AgentIcon active={pathname.includes('agents')} />,
      label: <Link to={`/workspaces/${workspaceId}/agents`}>Agents管理</Link>,
    },
    {
      key: 'tools',
      icon: <ToolIcon active={pathname.includes('tools')} />,
      label: <Link to={`/workspaces/${workspaceId}/tools`}>工具管理</Link>,
    },
    {
      key: 'datasets',
      icon: <DatasetsIcon active={pathname.includes('datasets')} />,
      label: <Link to={`/workspaces/${workspaceId}/datasets`}>知识库管理</Link>,
    },
    {
      key: 'models',
      icon: <ModelIcon active={pathname.includes('models')} />,
      label: <Link to={`/workspaces/${workspaceId}/models`}>模型管理</Link>,
    },
    {
      key: 'users',
      icon: <UserIcon active={pathname.includes('users')} />,
      label: <Link to={`/workspaces/${workspaceId}/users`}>用户管理</Link>,
    },
    {
      key: 'plugin-connects',
      icon: <PluginConnectIcon active={pathname.includes('plugin-connects')} />,
      label: <Link to={`/workspaces/${workspaceId}/plugin-connects`}>插件智连</Link>,
    }
  ];

  return (
    <Layout className="h-screen overflow-hidden bg-white">
      <div className="fixed inset-0 pointer-events-none">
        <Bg/>
      </div>
      <Sider
        theme="light"
        onCollapse={(value) => setCollapsed(value)}
        className="p-0 m-0 backdrop-blur-md border-r border-white/80 h-screen overflow-hidden z-20 [&_.ant-layout-sider-children]:rounded-2xl [&_.ant-layout-sider-children]:bg-white/60 [&_.ant-layout-sider-children]:ml-4 [&_.ant-layout-sider-children]:h-[calc(100vh-32px)]"
        width={272}
      >
        <div className="m-2 mb-7">
          <Select
            className="w-full h-16 rounded-xl bg-white/80 border-white/80 shadow-sm [&_.ant-select-selection-item]:text-[#1D4A6B] [&_.ant-select-selection-item]:text-[18px] [&_.ant-select-selection-item]:font-medium"
            prefix={<UserOutlined className="text-[#1D4A6B] cursor-pointer pointer-events-none"/>}
            variant="borderless"
            value={currentWorkspace?.id}
            onChange={handleWorkspaceChange}
            suffixIcon={<SwapOutlined className="text-[#1D4A6B] cursor-pointer pointer-events-none"/>}
            dropdownStyle={{ borderRadius: '12px' }}
          >
            {workspaces.map((workspace) => (
              <Select.Option key={workspace.id} value={workspace.id}>
                {workspace.name}
              </Select.Option>
            ))}
          </Select>
        </div>

        <Menu
          theme="light"
          mode="inline"
          items={menuItems}
          selectedKeys={[pathname.split('/')[3] || 'agents']}
          rootClassName='px-3 gap-y-1 flex flex-col'
          className="bg-transparent border-r-0 [&_.ant-menu-item]:p-3 [&_.ant-menu-item]:h-[48px] [&_.ant-menu-item]:rounded-xl [&_.ant-menu-item-selected]:bg-white [&_.ant-menu-item-selected]:text-[#1D4A6B] [&_.ant-menu-item-selected]:shadow-sm [&_.ant-menu-item]:text-[#7C8B98] [&_.ant-menu-item]:font-medium [&_.ant-menu-item:hover]:bg-white/80"
        />

      </Sider>
      <Layout className="h-screen overflow-hidden bg-transparent z-10">
        <Suspense fallback={<Skeleton active className="p-8" />}>
          <Content className="h-full overflow-y-auto">
            <WorkspaceProvider value={{ workspace: currentWorkspace, userInfo: userInfo }}>
              <Outlet />
            </WorkspaceProvider>
          </Content>
        </Suspense>
      </Layout>
    </Layout>
  );
}
