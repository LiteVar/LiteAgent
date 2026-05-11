import React from 'react';
import { Menu } from 'antd';
import { EditIcon, ApiIcon, LogsIcon, SettingsIcon } from '@/assets/agent/agent_tabs_icons_svg';

interface SideMenuProps {
  canEdit: boolean;
  selectedTab: string;
  onTabChange: (key: string) => void;
}

const SideMenu: React.FC<SideMenuProps> = ({ canEdit, selectedTab, onTabChange }) => {
  const menuItems = [
    {
      key: 'edit',
      className: 'm-0',
      icon: <EditIcon active={selectedTab === 'edit'} />,
    },
    ...(canEdit
      ? [
          {
            key: 'api',
            className: 'my-4 mx-0',
            icon: <ApiIcon active={selectedTab === 'api'} />
          },
          {
            key: 'logs',
            className: 'my-4 m-0',
            icon: <LogsIcon active={selectedTab === 'logs'} />,
          },
          {
            key: 'setting',
            className: 'm-0',
            icon: <SettingsIcon active={selectedTab === 'setting'} />,
          },
        ]
      : []),
    
  ];

  return (
    <div
      className="w-8 pt-6 px-4 flex flex-col bg-white/30 backdrop-blur-[4px] border border-white rounded-2xl"
    >
      <Menu
        mode="inline"
        defaultSelectedKeys={[selectedTab]}
        selectedKeys={[selectedTab]}
        style={{ borderRight: 'none', background: 'transparent' }}
        className="customSvg [&_.ant-menu-item]:w-full [&_.ant-menu-item]:bg-transparent [&_.ant-menu-item-selected]:bg-transparent [&_.ant-menu-item]:flex [&_.ant-menu-item]:justify-center [&_.ant-menu-item]:px-0"
        items={menuItems}
        onClick={({ key }) => onTabChange(key)}
      />
    </div>
  );
};



export default SideMenu;
