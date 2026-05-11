// 插件智连相关工具函数
import {
  getV1AgentList,
  getV1PluginConnectorByIdData,
  postV1PluginConnectorByIdStatus,
} from '@/client';

/**
 * 生成随机 Token (32位)
 */
export const generateToken = (): string => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  for (let i = 0; i < 32; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
};

/**
 * 生成加密密钥 (43位)
 */
export const generateEncryptionKey = (): string => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  for (let i = 0; i < 43; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
};

/**
 * 生成访问密钥 (32位)
 */
export const generateAccessKey = (): string => {
  return generateToken();
};

/**
 * 复制到剪贴板
 */
export const copyToClipboard = (text: string): void => {
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text);
  } else {
    // 降级方案
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
      document.execCommand('copy');
    } catch (error) {
      console.error('复制失败', error);
    }
    textArea.remove();
  }
};

/**
 * 获取可用的 Agent 列表
 */
export const getAvailableAgents = async (
  workspaceId: string,
  params?: { query?: string; pageSize?: number }
) => {
  const response = await getV1AgentList({
    headers: {
      'Workspace-id': workspaceId,
    },
    query: {
      name: params?.query || '',
      tab: 0, // 0-全部
    },
  });

  const agents = response.data?.data || [];
  
  // 如果有 pageSize 限制，进行裁剪
  const list = params?.pageSize ? agents.slice(0, params.pageSize) : agents;
  
  return {
    list,
    total: agents.length,
  };
};

/**
 * 获取插件智连配置数据
 */
export const getPluginConnectConfigData = async (connectorId: string) => {
  const response = await getV1PluginConnectorByIdData({
    path: { id: connectorId },
  });
  
  return response.data?.data;
};

/**
 * 提交插件智连配置（设置为上线状态）
 * 注意：此函数用于配置完成后上线智连
 * 具体的配置数据需要根据后端 API 确定如何保存
 */
export const submitPluginConnectOnline = async (
  connectorId: string,
  workspaceId: string
) => {
  const response = await postV1PluginConnectorByIdStatus({
    path: { id: connectorId },
    query: { status: 2 }, // 2-上线
    headers: {
      'Workspace-id': workspaceId,
    },
  });
  
  return response.data;
};

/**
 * 更新插件智连配置
 * TODO: 根据后端实际 API 实现配置数据的保存
 * 目前后端可能需要提供专门的配置保存接口
 */
export const updatePluginConnectConfig = async (params: {
  id: string;
  config: any;
}) => {
  // TODO: 这里需要根据后端提供的配置保存接口来实现
  // 可能需要调用 getV1PluginByPluginIdSchema 获取配置 schema
  // 然后根据 schema 验证和保存配置数据
  console.warn('updatePluginConnectConfig 需要根据后端 API 实现');
  
  // 临时方案：将配置保存到 localStorage
  const configKey = `plugin_connect_config_${params.id}`;
  localStorage.setItem(configKey, JSON.stringify(params.config));
  
  return { success: true };
};

/**
 * 获取插件智连配置（临时方案）
 */
export const getPluginConnectConfig = (connectorId: string) => {
  const configKey = `plugin_connect_config_${connectorId}`;
  const config = localStorage.getItem(configKey);
  return config ? JSON.parse(config) : null;
};

