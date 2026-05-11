import React, { ChangeEvent } from 'react';
import { Button } from 'antd';
import { CloudSyncOutlined, SearchOutlined } from '@ant-design/icons';
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
    <div className="flex justify-between items-center pr-4">
      {/* 左侧：云端/本地 切换标题 */}
      <div className="flex items-center gap-4 py-6 pr-4">
        <h2
          onClick={onSelectCloudType}
          className={`text-lg font-medium cursor-pointer transition-colors ${
            agentListType === EAgentListType.CLOUD
              ? 'text-[#1D4A6B]'
              : 'text-[#7C8B98] hover:text-[#383F44]'
          }`}
        >
          云端Agents管理
        </h2>
        <h2
          onClick={onSelectLocalType}
          className={`text-lg font-medium cursor-pointer transition-colors ${
            agentListType === EAgentListType.LOCAL
              ? 'text-[#1D4A6B]'
              : 'text-[#7C8B98] hover:text-[#383F44]'
          }`}
        >
          本地Agents管理
        </h2>
      </div>

      {/* 右侧：搜索框 + 同步按钮 */}
      <div className="flex items-center gap-4 py-4">
        {/* 玻璃态搜索框 */}
        <div className="flex items-center gap-2 px-4 h-10 rounded-xl bg-white/60 backdrop-blur-[4px] border border-white/80">
          <SearchOutlined className="text-[#58636C] text-base flex-none" />
          <input
            type="text"
            placeholder="搜索你的Agent"
            className="bg-transparent border-none outline-none text-sm text-[#383F44] placeholder-[#58636C] w-48"
            value={searchValue}
            onChange={handleSearch}
          />
        </div>

        {/* 同步按钮 */}
        <Button
          type="primary"
          icon={<CloudSyncOutlined />}
          onClick={onSync}
          className="h-10 rounded-xl px-4 text-base font-normal flex items-center justify-center border-none bg-[#40A5EE]"
        >
          同步
        </Button>
      </div>
    </div>
  );
};

export default Header;
