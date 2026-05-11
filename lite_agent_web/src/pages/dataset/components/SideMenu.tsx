import React from 'react';
import { Menu } from 'antd';
import { EditIcon, ApiIcon, SettingsIcon, SearchIcon } from '@/assets/agent/agent_tabs_icons_svg';
import { useLocation } from 'react-router-dom';

interface SideMenuProps {
  canEdit: boolean;
  canDelete: boolean;
  selectedTab: string;
  onTabChange: (key: string) => void;
}

const SideMenu = ({ selectedTab, onTabChange, canEdit, canDelete }: SideMenuProps) => {
  const location = useLocation();
  const pathname = location.pathname;
  const menuItems = [
    {
      key: 'documents',
      className: 'm-0',
      icon: <EditIcon active={selectedTab === 'documents' || pathname.includes('fragments')} />,
    },
    {
      key: 'test',
      className: 'my-4 mx-0',
      icon: <SearchIcon active={selectedTab === 'test'} />,
    },
    {
      key: 'apis',
      className: 'my-4 mx-0',
      icon: <ApiIcon active={selectedTab === 'apis'} />,
    },
    ...((canEdit && canDelete) ? [{
      key: 'settings',
      className: 'm-0',
      icon: <SettingsIcon active={selectedTab === 'settings'} />,
    }] : [])
  ];

  return (
    <div className="w-8 pt-6 px-4 flex flex-col bg-white/60 backdrop-blur-[4px] border border-white rounded-2xl">
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
