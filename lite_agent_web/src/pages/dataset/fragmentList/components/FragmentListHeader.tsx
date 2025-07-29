import React from 'react';
import { Button, Input, Checkbox } from 'antd';
import { PlusOutlined, SearchOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

interface FragmentListHeaderProps {
  total: number;
  canEdit: boolean;
  canDelete: boolean;
  onSearch: (value: string) => void;
  onCreateNew: () => void;
  selectedCount: number;
  onBatchDelete?: () => void;
  onSelectAll: (checked: boolean) => void;
  selectAll: boolean;
}

const FragmentListHeader: React.FC<FragmentListHeaderProps> = (props) => {
  const navigate = useNavigate();
  const {
    total,
    canEdit,
    canDelete,
    onSearch,
    onCreateNew,
    selectedCount,
    onBatchDelete,
    onSelectAll,
    selectAll,
  } = props;
  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <div className="flex items-center space-x-4 mb-6">
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} className="hover:bg-gray-100">
            返回
          </Button>
          <div className="h-4 w-[1px] bg-gray-200" />
          <h2 className="text-xl m-0">片段列表</h2>
        </div>
        {canEdit && (
          <Button type="primary" icon={<PlusOutlined />} onClick={onCreateNew}>
            新建片段
          </Button>
        )}
      </div>

      <div className="flex justify-between items-center">
        <div className="flex items-center space-x-4">
          <Checkbox checked={selectAll} onChange={(e) => onSelectAll(e.target.checked)}>
            全选
          </Checkbox>
          <div className="h-4 w-[1px] bg-gray-200" />
          <span className="text-gray-500">共 {total} 个片段</span>
          {selectedCount > 0 && (
            <>
              <div className="h-4 w-[1px] bg-gray-200" />
              <span className="text-gray-500">
                已选择 {selectedCount} 项
                {canDelete && (
                  <Button danger onClick={onBatchDelete} className="ml-4">
                    批量删除
                  </Button>
                )}
              </span>
            </>
          )}
        </div>
        <Input.Search
          placeholder="搜索片段内容"
          onSearch={onSearch}
          enterButton={<SearchOutlined />}
          allowClear
          className="max-w-sm"
        />
      </div>
    </div>
  );
};

export default FragmentListHeader;
