import React, { ChangeEvent } from 'react';
import { Input, Button } from 'antd';
import { CloudSyncOutlined } from '@ant-design/icons';
import { EAgentListType } from '../index';

interface HeaderProps {
  agentListType: EAgentListType;
  onSelectCloudType: () => void;
  onSelectLocalType: () => void;
  onSync: () => void;
  searchValue: string;
  onSearch: (value: string) => void;
}

const Header: React.FC<HeaderProps> = ({
  agentListType,
  onSelectCloudType,
  onSelectLocalType,
  onSync,
  searchValue,
  onSearch,
}) => {
  const handleSearch = (e: ChangeEvent<HTMLInputElement>) => {
    onSearch(e.target.value);
  };

  return (
    <div
      className="flex justify-between items-center px-6 py-4"
      style={{ borderBottom: '1px solid #E5E5E5' }}
    >
      <h2
        onClick={onSelectCloudType}
        className={`text-lg cursor-pointer font-bold mr-4 ${
          agentListType === EAgentListType.CLOUD ? 'opacity-100' : 'opacity-50'
        }`}
      >
        云端Agents管理
      </h2>
      <h2
        onClick={onSelectLocalType}
        className={`text-lg cursor-pointer font-bold ${
          agentListType === EAgentListType.LOCAL ? 'opacity-100' : 'opacity-50'
        }`}
      >
        本地Agents管理
      </h2>
      <Input
        placeholder="搜索你的Agent"
        className="flex-1 ml-6 mr-6 bg-gray-100 p-3 rounded-md"
        value={searchValue}
        onChange={handleSearch}
        allowClear
      />
      <Button type="primary" size="large" icon={<CloudSyncOutlined />} onClick={onSync}>
        同步
      </Button>
    </div>
  );
};

export default Header;
