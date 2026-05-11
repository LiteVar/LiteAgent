import React, { useMemo } from 'react';
import { Card, Dropdown, Tooltip, message } from 'antd';
import type { MenuProps } from 'antd';
import { EllipsisOutlined, CheckCircleFilled } from '@ant-design/icons';
import { PluginConnect } from '@/types/plugin-connect';
import { buildImageUrl } from '@/utils/buildImageUrl';
import { ConnectorDTO } from '@/client';
import pluginConnectIcon from '@/assets/plugin/plugin_connect.png';
import statisticsIcon from '@/assets/plugin/statistics.png';
import { UserType } from "@/types/User";
import { useWorkspace } from '@/contexts/workspaceContext';

// 智连状态枚举
enum ConnectStatus {
  INIT = 0,        // 初始化
  CONFIGURED = 1,  // 已配置
  ONLINE = 2,      // 已上线
  OFFLINE = 3,     // 已下线
}

// 插件状态枚举
enum PluginStatus {
  ONLINE = 2,      // 上线
  OFFLINE = 3,     // 下线
}

interface PluginConnectCardProps {
  pluginConnect: ConnectorDTO;
  onConfig: (id: string) => void;
  onEdit: (connect: PluginConnect) => void;
  onOffline: (id: string) => void;
  onOnline: (id: string) => void;
  onDelete: (id: string) => void;
  onAnalytics: (id: string) => void;
}

const PluginConnectCard: React.FC<PluginConnectCardProps> = ({
  pluginConnect,
  onConfig,
  onOffline,
  onOnline,
  onDelete,
  onAnalytics,
}) => {

  const { id, name, pluginName, icon, description, status, pluginStatus, createUser, canEdit } = pluginConnect;

  // 根据状态获取操作菜单项
  const getMenuItems = (): MenuProps['items'] => {
    const items: MenuProps['items'] = [];

    if ((status === ConnectStatus.OFFLINE || status === ConnectStatus.INIT) && pluginStatus === PluginStatus.ONLINE) { 
      items.push(
        {
          key: 'config',
          label: '编辑',
          onClick: (e) => {
            e?.domEvent?.stopPropagation();
            onConfig(id!);
          },
        }
      );
    }

    if (status === ConnectStatus.INIT || status === ConnectStatus.CONFIGURED) {
      // 初始化或已配置状态
      items.push(
        {
          key: 'delete',
          label: '删除',
          danger: true,
          onClick: (e) => {
            e?.domEvent?.stopPropagation();
            onDelete(id!);
          },
        }
      );
    } else if (status === ConnectStatus.ONLINE) {
      // 已上线状态
      items.push(
        {
          key: 'offline',
          label: '下线',
          onClick: (e) => {
            e?.domEvent?.stopPropagation();
            onOffline(id!);
          },
        },
      );
    } else if (status === ConnectStatus.OFFLINE) {
      // 已下线状态
      items.push(
        {
          key: 'online',
          label: '上线',
          onClick: (e) => {
            e?.domEvent?.stopPropagation();
            onOnline(id!);
          },
        }
      );
    }

    return items;
  };

  // 卡片点击事件
  const handleCardClick = () => {
    if (pluginStatus === PluginStatus.OFFLINE) {
      return;
    }

    if (!canEdit) {
      message.warning('你没有权限编辑当前插件智连');
      return;
    }

    onConfig(id!);
  };

  // 数据分析图标点击事件
  const handleAnalyticsClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onAnalytics(id!);
  };

  const cardContent = (
    <Card
      hoverable
      onClick={handleCardClick}
      className="relative h-full bg-white/60 backdrop-blur-[4px] border-white/80 rounded-xl shadow-none overflow-hidden group transition-all duration-300 hover:bg-white/80 hover:shadow-lg cursor-pointer"
      styles={{ body: { padding: '22px 16px' } }}
    >
      {/* Logo 和标题区域 */}
      <div className="flex items-start mb-4">
        <div
          className="w-10 h-10 flex items-center justify-center rounded-xl mr-4 flex-shrink-0 shadow-inner overflow-hidden"
          style={{
            backgroundColor: icon ? 'transparent' : '#40A5EE',
          }}
        >
          {icon ? (
            <img src={buildImageUrl(icon)} alt={name} className="w-full h-full object-cover" />
          ) : (
            <img src={pluginConnectIcon} alt="default icon" className="w-full h-full" />
          )}
        </div>

        <div className="flex-1 min-w-0 pr-6">
          <div className="flex items-center gap-2">
            <h3 className="text-base font-semibold text-[#1D4A6B] truncate m-0" title={name}>{name}</h3>
          </div>
          <span className="text-[#7C8B98] text-xs truncate block">{pluginName}</span>
        </div>

        {/* 状态图标 */}
        {status === ConnectStatus.ONLINE && (
          <CheckCircleFilled className="text-[#40A5EE] text-2xl flex-shrink-0" />
        )}
      </div>

      {/* 说明 */}
      <p className="text-[#58636C] text-sm mb-4 break-all line-clamp-2 h-[50px] leading-relaxed">
        {description || ''}
      </p>

      {/* 底部操作区域 */}
      <div className="flex items-center justify-between gap-2 h-4">
        {/* 创建者：左侧，灰色圆点 + 创建者名 创建 */}
        <div className="flex items-center min-w-0 flex-shrink-0">
          <span className="text-sm text-[#7C8B98] truncate">
            {createUser ? `@${createUser}` : ''}
          </span>
        </div>

        {/* 右侧：统计图标 + 操作菜单 */}
        <div className="flex items-center gap-2 flex-shrink-0">
          {pluginStatus !== PluginStatus.OFFLINE && (
            <img
              src={statisticsIcon}
              alt="statistics"
              className="w-4 h-4 cursor-pointer mr-3"
              onClick={handleAnalyticsClick}
              title="数据统计"
            />
          )}
          {canEdit && (
            <Dropdown menu={{ items: getMenuItems() }} trigger={['click']} disabled={!canEdit}>
              <EllipsisOutlined
                className="text-base text-[#7C8B98] cursor-pointer hover:text-[#40A5EE] transition-colors"
                onClick={(e) => e.stopPropagation()}
                title={canEdit ? '' : '不能编辑当前插件智连'}
              />
            </Dropdown>
          )}
        </div>
      </div>
    </Card>
  );

  // 如果插件状态为下线，显示提示
  if (pluginStatus === PluginStatus.OFFLINE) {
    return (
      <Tooltip title={`${pluginName} 已关闭，不可使用。有需要，请联系管理员`}>
        {cardContent}
      </Tooltip>
    );
  }

  return cardContent;
};

export default PluginConnectCard;

