import React, {useState, useEffect, useMemo, useCallback, Suspense} from 'react';
import { Layout, Menu, Select, Skeleton } from 'antd';
import {TeamOutlined, ToolOutlined, AppstoreOutlined, UserOutlined, SwapOutlined,} from '@ant-design/icons';
import {useNavigate, useLocation, Link, Outlet} from "react-router-dom";
import {getV1UserInfoOptions, getV1WorkspaceListOptions} from "@/client/@tanstack/query.gen";
import {useQuery} from '@tanstack/react-query'
import {WorkSpaceVO} from "@/client";
import {WorkspaceProvider} from "@/contexts/workspaceContext";

const { Sider, Content } = Layout;

export default function WorkspaceLayout() {
  const location = useLocation();
  const pathname = location.pathname
  const workspaceId = pathname?.split('/')?.[2];
  const [collapsed, setCollapsed] = useState(false);
  const [currentWorkspace, setCurrentWorkspace] = useState<WorkSpaceVO>();

  const navigate = useNavigate();

  const { data } = useQuery({
    ...getV1WorkspaceListOptions({})
  });
  const {data: userInfoResult} = useQuery({
    ...getV1UserInfoOptions({})
  })
  const workspaces = useMemo(() => data?.data || [], [data]);
  const userInfo = userInfoResult?.data;

  useEffect(() => {
    const workspaceId = pathname.split('/')[2];
    const workspace = workspaces.find(w => w.id === workspaceId) || workspaces[0];
    setCurrentWorkspace(workspace);
  }, [pathname, workspaces]);

  const handleWorkspaceChange = useCallback((value: string) => {
    const newWorkspace = workspaces.find(w => w.id === value);
    setCurrentWorkspace(newWorkspace);
    const newPath = `/workspaces/${value}/${pathname.split('/')[3] || 'agents'}`;
    navigate(newPath);
  }, [workspaces, navigate, pathname]);

  const menuItems = [
    {
      key: 'agents',
      icon: <TeamOutlined />,
      label: <Link to={`/workspaces/${workspaceId}/agents`}>Agents管理</Link>,
    },
    {
      key: 'tools',
      icon: <ToolOutlined />,
      label: <Link to={`/workspaces/${workspaceId}/tools`}>工具管理</Link>,
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
    <Layout className="min-h-screen">
      <Sider collapsible collapsed={collapsed} onCollapse={(value) => setCollapsed(value)}
             className="bg-gray-900 text-white py-3" width={250}>
        <div className="h-8 m-4 bg-gray-800 rounded flex items-center justify-between cursor-pointer">
          <Select
            className='w-full rp-[.ant-select-selection-item]:text-white'
            variant="borderless"
            size="large"
            dropdownStyle={{width: 218}}
            value={currentWorkspace?.id}
            onChange={handleWorkspaceChange}
            suffixIcon={<SwapOutlined className="text-white text-base" />}
          >
            {workspaces.map(workspace => (
              <Select.Option key={workspace.id} value={workspace.id}>
                {workspace.name}
              </Select.Option>
            ))}
          </Select>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[pathname.split('/')[3] || 'agents']}
          items={menuItems}
          className="bg-gray-900 border-r-0 !px-2"
        />
      </Sider>
      <Layout>
        <Suspense fallback={<Skeleton/>}>
          <Content className="m-5 p-5 bg-white rounded-lg">
            <WorkspaceProvider value={{workspace: currentWorkspace, userInfo: userInfo}}>
              <Outlet />
            </WorkspaceProvider>
          </Content>
        </Suspense>
      </Layout>
    </Layout>
  );
}
