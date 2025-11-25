import React from 'react';
import { Input, Button } from 'antd';

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
    <div className="flex items-center py-4 px-8" style={{ borderBottom: '1px solid #e0e0e0' }}>
      <h2 className="text-xl font-bold">{title}</h2>
      <Input
        placeholder={placeholder}
        className={`ml-6 mr-6 flex-1 bg-gray-100 p-3 rounded-md ${showSearch ? 'opacity-100' : 'opacity-0'}`}
        value={searchValue}
        onChange={(e) => onSearchChange && onSearchChange(e.target.value)}
        onKeyDown={(e) => e.key === 'Enter' && onSearch && onSearch()}
      />
      {showCreateButton && (
        <Button
          type="primary"
          size='large'
          onClick={(e) => onCreateClick && onCreateClick(e)}
        >
          {createButtonText}
        </Button>
      )}

      {!!createButton && createButton}

    </div>
  );
};

export default Header; 