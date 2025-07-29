import React from 'react';
import { Menu } from 'antd';
import documentIcon from '@/assets/dataset/doc_svg';
import settingIcon from '@/assets/dataset/setting_svg';
import apiIcon from '@/assets/agent/api_svg';
import logsIcon from '@/assets/agent/logs_svg';

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
      icon: (
        <span className={`${selectedTab === 'edit' ? 'rgba(102, 172, 252, 1)' : 'text-gray-400'}`}>
          {documentIcon}
        </span>
      ),
    },
    ...(canEdit
      ? [
          {
            key: 'api',
            className: 'my-6 mx-0',
            icon: (
              <span className={`${selectedTab === 'api' ? 'rgba(102, 172, 252, 1)' : 'text-gray-400'}`}>
                {apiIcon}
              </span>
            ),
          },
          {
            key: 'logs',
            className: 'my-6 m-0',
            icon: (
              <span className={`${selectedTab === 'logs' ? 'rgba(102, 172, 252, 1)' : 'text-gray-400'}`}>
                {logsIcon}
              </span>
            ),
          },
          {
            key: 'setting',
            className: 'm-0',
            icon: (
              <span className={`${selectedTab === 'setting' ? 'rgba(102, 172, 252, 1)' : 'text-gray-400'}`}>
                {settingIcon}
              </span>
            ),
          },
        ]
      : []),
    
  ];

  return (
    <div
      className="w-[80px] pt-8 flex flex-col"
      style={{
        borderRight: '1px solid #f0f0f0',
      }}
    >
      <Menu
        mode="inline"
        defaultSelectedKeys={[selectedTab]}
        selectedKeys={[selectedTab]}
        style={{ borderRight: 'none' }}
        className="customeSvg [&_.ant-menu-item]:bg-transparent [&_.ant-menu-item-selected]:bg-transparent"
        items={menuItems}
        onClick={({ key }) => onTabChange(key)}
      />
    </div>
  );
};

export default SideMenu;