import React, { useState, useEffect, useMemo, useCallback, Suspense } from 'react';
import { Layout, Menu, Select, Skeleton } from 'antd';
import {
  TeamOutlined,
  ToolOutlined,
  AppstoreOutlined,
  UserOutlined,
  SwapOutlined,
  FolderOutlined,
} from '@ant-design/icons';
import { useNavigate, useLocation, Link, Outlet } from 'react-router-dom';
import { getV1UserInfoOptions, getV1WorkspaceListOptions } from '@/client/@tanstack/query.gen';
import { useQuery } from '@tanstack/react-query';
import { WorkSpaceVO } from '@/client';
import { WorkspaceProvider } from '@/contexts/workspaceContext';
import agentLogo from '@/assets/login/logo_white.png';

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
      icon: <img src={agentLogo} alt="Agents Logo" className="w-4 h-4" />,
      label: <Link to={`/workspaces/${workspaceId}/agents`}>Agents管理</Link>,
    },
    {
      key: 'tools',
      icon: <ToolOutlined />,
      label: <Link to={`/workspaces/${workspaceId}/tools`}>工具管理</Link>,
    },
    {
      key: 'datasets',
      icon: <FolderOutlined />,
      label: <Link to={`/workspaces/${workspaceId}/datasets`}>知识库管理</Link>,
    },
    {
      key: 'models',
      icon: <AppstoreOutlined />,
      label: <Link to={`/workspaces/${workspaceId}/models`}>模型管理</Link>,
    },
    {
      key: 'users',
      icon: <UserOutlined />,
      label: <Link to={`/workspaces/${workspaceId}/users`}>用户管理</Link>,
    },
  ];

  return (
    <Layout className="h-screen overflow-hidden">
      <Sider
        theme="dark"
        onCollapse={(value) => setCollapsed(value)}
        className="px-4 bg-[#001529] text-[#ABB9C9] h-screen overflow-hidden"
        width={208}
      >
        <div className="mb-4 mt-6">
          <Select
            className="w-full rounded-lg bg-[#2D4257] [&_.ant-select-selection-item]:text-white"
            size="large"
            variant="borderless"
            value={currentWorkspace?.id}
            onChange={handleWorkspaceChange}
            suffixIcon={<SwapOutlined className="text-white" />}
            dropdownStyle={{ width: 208 }}
          >
            {workspaces.map((workspace) => (
              <Select.Option key={workspace.id} value={workspace.id}>
                {workspace.name}
              </Select.Option>
            ))}
          </Select>
        </div>

        <Menu
          theme="dark"
          mode="inline"
          items={menuItems}
          selectedKeys={[pathname.split('/')[3] || 'agents']}
          className={`bg-[#001529] border-r-0 [&_.ant-menu-item-selected]:bg-[#2D4257]`}
        />
      </Sider>
      <Layout className="h-screen overflow-hidden">
        <Suspense fallback={<Skeleton />}>
          <Content className="bg-white h-full overflow-y-auto">
            <WorkspaceProvider value={{ workspace: currentWorkspace, userInfo: userInfo }}>
              <Outlet />
            </WorkspaceProvider>
          </Content>
        </Suspense>
      </Layout>
    </Layout>
  );
}
