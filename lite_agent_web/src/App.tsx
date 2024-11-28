import React from "react";
import {Navigate, BrowserRouter, Routes, Route} from "react-router-dom";
import RootLayout from "@/components/RootLayout";
import {ROUTES} from "@/config/constants";
const InitPage = React.lazy(() => import('@/pages/init'));
const ErrorPage = React.lazy(() => import('@/components/error'));
const LoginPage = React.lazy(() => import('@/pages/login'));
const DashboardPage = React.lazy(() => import('@/pages/dashboard'));
const ShopPage = React.lazy(() => import('@/pages/dashboard/[workspaceId]/shop'));
const ChatPage = React.lazy(() => import('@/pages/dashboard/[workspaceId]/chat/[agentId]'));
const ShareAgentPage = React.lazy(() => import('@/pages/dashboard/[workspaceId]/shareAgent/[agentId]'));
const ActivatePage = React.lazy(() => import('@/pages/activate'));
const AgentPage = React.lazy(() => import('@/pages/agent/[agentId]'));
const WorkspacesPage = React.lazy(() => import('@/pages/workspaces'));
const WorkspaceLayout = React.lazy(() => import('@/pages/workspaces/[workspaceId]/layout'));
const WorkspaceAgentsPage = React.lazy(() => import('@/pages/workspaces/[workspaceId]/agents'));
const WorkspaceToolsPage = React.lazy(() => import('@/pages/workspaces/[workspaceId]/tools'));
const WorkspaceModelsPage = React.lazy(() => import('@/pages/workspaces/[workspaceId]/models'));
const WorkspaceUsersPage = React.lazy(() => import('@/pages/workspaces/[workspaceId]/users'));


function App() {
  return (
    <div>
      <Routes>
        <Route element={<RootLayout />}>
          <Route path={ROUTES.LOGIN} element={<LoginPage />} />
          <Route path={ROUTES.INIT} element={<InitPage />} />
          <Route path={ROUTES.DASHBOARD} element={<DashboardPage />} />
          <Route path={ROUTES.DASHBOARD_WORKSPACE} element={<DashboardPage />} />
          <Route path="/" element={<Navigate to={ROUTES.DASHBOARD} replace />} />
          <Route path={ROUTES.SHOP} element={<ShopPage />} />
          <Route path={ROUTES.CHAT} element={<ChatPage />} />
          <Route path={ROUTES.ShareAgent} element={<ShareAgentPage />} />
          <Route path={ROUTES.ACTIVATE} element={<ActivatePage />} />
          <Route path={ROUTES.AGENT} element={<AgentPage />} />
          <Route path={ROUTES.WORKSPACES} element={<WorkspacesPage />} />
          <Route element={<WorkspaceLayout />}>
            <Route path={ROUTES.WORKSPACE_AGENTS} element={<WorkspaceAgentsPage />} />
            <Route path={ROUTES.WORKSPACE_TOOLS} element={<WorkspaceToolsPage />} />
            <Route path={ROUTES.WORKSPACE_MODELS} element={<WorkspaceModelsPage />} />
            <Route path={ROUTES.WORKSPACE_USERS} element={<WorkspaceUsersPage />} />
          </Route>
          <Route path={ROUTES.ERROR} element={<ErrorPage code={404} />} />
        </Route>
      </Routes>
    </div>
  );
}

export default App;
