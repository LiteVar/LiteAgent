import { Menu } from 'antd';
import documentIcon from '@/assets/dataset/doc_svg';
import retrievalTestIcon from '@/assets/dataset/retrieval_test_svg';
import settingIcon from '@/assets/dataset/setting_svg';
import apisIcon from '@/assets/agent/api_svg';

interface SideMenuProps {
  canEdit: boolean;
  canDelete: boolean;
  selectedTab: string;
  onTabChange: (key: string) => void;
}

const SideMenu = ({ selectedTab, onTabChange, canEdit, canDelete }: SideMenuProps) => {
  return (
    <div
      className="w-[80px] pt-8 flex flex-col"
      style={{
        borderRight: '1px solid #f0f0f0',
      }}
    >
      <Menu
        mode="inline"
        defaultSelectedKeys={['documents']}
        selectedKeys={[selectedTab]}
        style={{ borderRight: 'none' }}
        className="customeSvg [&_.ant-menu-item]:bg-transparent [&_.ant-menu-item-selected]:bg-transparent"
        items={[
          {
            key: 'documents',
            className: 'm-0',
            icon: (
              <span className={`${selectedTab === 'documents' ? 'rgba(102, 172, 252, 1)' : 'text-gray-400'}`}>
                {documentIcon}
              </span>
            ),
          },
          {
            key: 'test',
            className: 'my-6 mx-0',
            icon: (
              <span className={`${selectedTab === 'test' ? 'rgba(102, 172, 252, 1)' : 'text-gray-400'}`}>
                {retrievalTestIcon}
              </span>
            ),
          },
          {
            key: 'apis',
            className: 'my-6 mx-0',
            icon: (
              <span className={`${selectedTab === 'apis' ? 'rgba(102, 172, 252, 1)' : 'text-gray-400'}`}>
                {apisIcon}
              </span>
            ),
          },
          {
            key: 'settings',
            className: (canEdit && canDelete) ? 'mt-auto' : 'hidden',
            icon: (
              <span className={`${selectedTab === 'settings' ? 'rgba(102, 172, 252, 1)' : 'text-gray-400'}`}>
                {settingIcon}
              </span>
            ),
          },
        ]}
        onClick={({ key }) => onTabChange(key)}
      />
    </div>
  );
};

export default SideMenu;
