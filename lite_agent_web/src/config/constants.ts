export const ACCESS_TOKEN = 'access_token';

export const ROUTES = {
  LOGIN: '/login',
  INIT: '/init',
  DASHBOARD: '/dashboard',
  DASHBOARD_WORKSPACE: '/dashboard/:workspaceId',
  SHOP: '/dashboard/:workspaceId/shop',
  CHAT: '/dashboard/:workspaceId/chat/:agentId',
  ShareAgent: '/dashboard/:workspaceId/shareAgent/:agentId',
  ACTIVATE: '/activate',
  AGENT: '/agent/:agentId',
  WORKSPACES: '/workspaces',
  WORKSPACE_AGENTS: '/workspaces/:workspaceId/agents',
  WORKSPACE_TOOLS: '/workspaces/:workspaceId/tools',
  WORKSPACE_MODELS: '/workspaces/:workspaceId/models',
  WORKSPACE_USERS: '/workspaces/:workspaceId/users',
  ERROR: '*',
};
