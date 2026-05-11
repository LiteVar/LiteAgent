import React from 'react';
import { List } from 'antd';
import { PluginConnect } from '@/types/plugin-connect';
import PluginConnectCard from './PluginConnectCard';

interface PluginConnectListProps {
  pluginConnects: PluginConnect[];
  onConfig: (id: string) => void;
  onEdit: (connect: PluginConnect) => void;
  onOffline: (id: string) => void;
  onOnline: (id: string) => void;
  onDelete: (id: string) => void;
  onAnalytics: (id: string) => void;
}

const PluginConnectList: React.FC<PluginConnectListProps> = ({
  pluginConnects,
  onConfig,
  onEdit,
  onOffline,
  onOnline,
  onDelete,
  onAnalytics,
}) => {
  return (
    <List
      grid={{
        gutter: 16,
        xs: 1,
        sm: 2,
        md: 2,
        lg: 3,
        xl: 3,
        xxl: 4,
      }}
      dataSource={pluginConnects}
      renderItem={(item) => (
        <List.Item>
          <PluginConnectCard
            pluginConnect={item}
            onConfig={onConfig}
            onEdit={onEdit}
            onOffline={onOffline}
            onOnline={onOnline}
            onDelete={onDelete}
            onAnalytics={onAnalytics}
          />
        </List.Item>
      )}
    />
  );
};

export default PluginConnectList;

