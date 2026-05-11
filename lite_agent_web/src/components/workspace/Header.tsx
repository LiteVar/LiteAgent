import React from 'react';
import { Input, Button } from 'antd';
import { PlusOutlined, SearchOutlined } from '@ant-design/icons';

interface HeaderProps {
  title: string;
  placeholder?: string;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  onSearch?: () => void;
  showCreateButton?: boolean;
  showSearch?: boolean;
  createButtonText?: string;
  createButton?: React.ReactNode;
  onCreateClick?: (e: MouseEvent) => void;
}

const Header: React.FC<HeaderProps> = ({
  title,
  placeholder = '搜索',
  searchValue,
  onSearchChange,
  onSearch,
  showCreateButton = true,
  showSearch = true,
  createButtonText = '新建',
  createButton,
  onCreateClick
}) => {

  return (
    <div className="flex items-center justify-between p-4">
      <div className="flex items-center flex-1">
        <h2 className="text-[18px] font-medium text-[#1D4A6B] whitespace-nowrap m-0">{title}</h2>
      </div>
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2 px-4 h-10 rounded-xl bg-white/60 backdrop-blur-[4px] border border-white/80">
          <input
            type="text"
            placeholder={placeholder}
            className="bg-transparent border-none outline-none text-sm text-[#383F44] placeholder-[#58636C] w-48"
            value={searchValue}
            onChange={(e) => onSearchChange && onSearchChange(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && onSearch && onSearch()}
          />
          <SearchOutlined 
            className="text-[#58636C] text-base flex-none" 
            onClick={() => onSearch && onSearch?.()} 
          />
        </div>
        {showCreateButton && (
          <Button 
          type="primary" 
          size="large"
          className="rounded-xl bg-[#40A5EE] hover:!bg-[#40A5EE]/90 border-none shadow-md shadow-blue-200/50 flex items-center gap-2 h-10"
          icon={<PlusOutlined />} 
          onClick={(e) => onCreateClick && onCreateClick(e as any)}
        >
          {createButtonText}
        </Button>
        )}
        {!!createButton && createButton}
      </div>
    </div>
  );
};

export default Header; 