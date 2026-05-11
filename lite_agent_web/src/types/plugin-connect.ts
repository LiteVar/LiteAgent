// 插件智连相关类型定义
// 基于后端接口 ConnectorDTO 定义

/**
 * 智连状态
 * 0 - 初始化
 * 1 - 已配置
 * 2 - 已上线
 * 3 - 已下线
 */
export type PluginConnectStatus = 0 | 1 | 2 | 3;

/**
 * 插件智连数据结构
 */
export interface PluginConnect {
  /** connector id */
  id: string;
  /** 插件ID */
  pluginId: string;
  /** 插件名称 */
  pluginName: string;
  /** 插件描述 */
  pluginDescription?: string;
  /** connector 名称 */
  name: string;
  /** 描述 */
  description?: string;
  /** 图标 */
  icon?: string;
  /** 状态: 0-初始化, 1-已配置, 2-已上线, 3-已下线 */
  status: PluginConnectStatus;
  /** 是否能编辑 */
  canEdit?: boolean;
}

/**
 * 创建插件智连的数据
 */
export interface CreatePluginConnectData {
  /** 插件ID */
  pluginId: string;
  /** 智连名称 */
  name: string;
  /** 描述 */
  description?: string;
  /** 图标 */
  icon?: string;
}

/**
 * 更新插件智连的数据
 */
export interface UpdatePluginConnectData {
  /** 智连ID */
  id: string;
  /** 插件ID */
  pluginId?: string;
  /** 智连名称 */
  name: string;
  /** 描述 */
  description?: string;
  /** 图标 */
  icon?: string;
}

/**
 * 插件信息
 */
export interface PluginInfo {
  id: string;
  name: string;
  type: string;
  enabled: boolean;
}

/**
 * 获取状态显示文本
 */
export const getStatusText = (status: PluginConnectStatus): string => {
  const statusMap: Record<PluginConnectStatus, string> = {
    0: '待配置',
    1: '已配置',
    2: '已上线',
    3: '已下线'
  };
  return statusMap[status] || '未知';
};

/**
 * 检查是否是初始状态
 */
export const isInitStatus = (status: PluginConnectStatus): boolean => {
  return status === 0;
};

/**
 * 检查是否已上线
 */
export const isOnlineStatus = (status: PluginConnectStatus): boolean => {
  return status === 2;
};

/**
 * 检查是否已下线
 */
export const isOfflineStatus = (status: PluginConnectStatus): boolean => {
  return status === 3;
};

/**
 * 检查是否已配置（已上线或已下线）
 */
export const isConfiguredStatus = (status: PluginConnectStatus): boolean => {
  return status === 2 || status === 3;
};

