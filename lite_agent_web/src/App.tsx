import React, { Suspense } from 'react';
import { Navigate, Routes, Route } from 'react-router-dom';
import RootLayout from '@/components/RootLayout';
import { ROUTES } from '@/constants/routes';
const InitPage = React.lazy(() => import('@/pages/init'));
const ErrorPage = React.lazy(() => import('@/components/error'));
const LoginPage = React.lazy(() => import('@/pages/login'));
const DashboardPage = React.lazy(() => import('@/pages/dashboard'));
const ShopPage = React.lazy(() => import('@/pages/dashboard/shop'));
const ChatPage = React.lazy(() => import('@/pages/dashboard/chat'));
const ShareAgentPage = React.lazy(() => import('@/pages/dashboard/shareAgent'));
const ActivatePage = React.lazy(() => import('@/pages/activate'));
const AgentPage = React.lazy(() => import('@/pages/agent'));
const WorkspacesPage = React.lazy(() => import('@/pages/workspaces'));
const WorkspaceLayout = React.lazy(() => import('@/pages/workspaces/layout'));
const WorkspaceAgentsPage = React.lazy(() => import('@/pages/workspaces/agents'));
const WorkspaceToolsPage = React.lazy(() => import('@/pages/workspaces/tools'));
const WorkspaceModelsPage = React.lazy(() => import('@/pages/workspaces/models'));
const WorkspaceUsersPage = React.lazy(() => import('@/pages/workspaces/users'));
const WorkspaceDatasetsPage = React.lazy(() => import('@/pages/workspaces/datasets'));
const DatasetDetailPage = React.lazy(() => import('@/pages/dataset'));
const ResetPasswordPage = React.lazy(() => import('@/pages/reset'));

function App() {
  return (
    <div>
      <Routes>
        <Route element={<RootLayout />}>
          <Route
            path={ROUTES.LOGIN}
            element={
              <Suspense fallback={<div>Loading Login...</div>}>
                <LoginPage />
              </Suspense>
            }
          />
          <Route
            path={ROUTES.INIT}
            element={
              <Suspense fallback={<div>Loading Init...</div>}>
                <InitPage />
              </Suspense>
            }
          />
          <Route
            path={ROUTES.DASHBOARD}
            element={
              <Suspense fallback={<div>Loading Dashboard...</div>}>
                <DashboardPage />
              </Suspense>
            }
          />
          <Route
            path={ROUTES.DASHBOARD_WORKSPACE}
            element={
              <Suspense fallback={<div>Loading Dashboard...</div>}>
                <DashboardPage />
              </Suspense>
            }
          />
          <Route path="/" element={<Navigate to={ROUTES.DASHBOARD} replace />} />
          <Route
            path={ROUTES.SHOP}
            element={
              <Suspense fallback={<div>Loading Shop...</div>}>
                <ShopPage />
              </Suspense>
            }
          />
          <Route
            path={ROUTES.CHAT}
            element={
              <Suspense fallback={<div>Loading Chat...</div>}>
                <ChatPage />
              </Suspense>
            }
          />
          <Route
            path={ROUTES.ShareAgent}
            element={
              <Suspense fallback={<div>Loading Share Agent...</div>}>
                <ShareAgentPage />
              </Suspense>
            }
          />
          <Route
            path={ROUTES.ACTIVATE}
            element={
              <Suspense fallback={<div>Loading Activate...</div>}>
                <ActivatePage />
              </Suspense>
            }
          />
          <Route
            path={ROUTES.RESET_PASSWORD}
            element={
              <Suspense fallback={<div>Loading Reset Password...</div>}>
                <ResetPasswordPage />
              </Suspense>
            }
          />
          <Route
            path={ROUTES.AGENT}
            element={
              <Suspense fallback={<div>Loading Agent...</div>}>
                <AgentPage />
              </Suspense>
            }
          />
          <Route
            path={ROUTES.DATASET}
            element={
              <Suspense fallback={<div>Loading Dataset...</div>}>
                <DatasetDetailPage />
              </Suspense>
            }
          />
          <Route
            path={ROUTES.WORKSPACES}
            element={
              <Suspense fallback={<div>Loading Workspaces...</div>}>
                <WorkspacesPage />
              </Suspense>
            }
          />
          <Route element={<WorkspaceLayout />}>
            <Route
              path={ROUTES.WORKSPACE_AGENTS}
              element={
                <Suspense fallback={<div>Loading Workspace Agents...</div>}>
                  <WorkspaceAgentsPage />
                </Suspense>
              }
            />
            <Route
              path={ROUTES.WORKSPACE_TOOLS}
              element={
                <Suspense fallback={<div>Loading Workspace Tools...</div>}>
                  <WorkspaceToolsPage />
                </Suspense>
              }
            />
            <Route
              path={ROUTES.WORKSPACE_MODELS}
              element={
                <Suspense fallback={<div>Loading Workspace Models...</div>}>
                  <WorkspaceModelsPage />
                </Suspense>
              }
            />
            <Route
              path={ROUTES.WORKSPACE_USERS}
              element={
                <Suspense fallback={<div>Loading Workspace Users...</div>}>
                  <WorkspaceUsersPage />
                </Suspense>
              }
            />
            <Route
              path={ROUTES.WORKSPACE_DATASETS}
              element={
                <Suspense fallback={<div>Loading Workspace Datasets...</div>}>
                  <WorkspaceDatasetsPage />
                </Suspense>
              }
            />
          </Route>
          <Route
            path={ROUTES.ERROR}
            element={
              <Suspense fallback={<div>Loading Error Page...</div>}>
                <ErrorPage code={404} />
              </Suspense>
            }
          />
        </Route>
      </Routes>
    </div>
  );
}

export default App;
