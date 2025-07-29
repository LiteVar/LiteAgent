import React, { useMemo, useState } from 'react';
import { Button, Card, Checkbox, Space } from 'antd';
import { DocumentSegment } from '@/client';
interface FragmentItemProps {
  item: DocumentSegment;
  index: number;
  canEdit: boolean;
  canDelete: boolean;
  searchText: string;
  selected: boolean;
  pagination: { current: number; pageSize: number };
  onSelect: (checked: boolean) => void;
  onEdit: (id: string, readonly: boolean, fragmentIndex: string) => void;
  onDelete: (id: string) => void;
  onToggleFreeze: (id: string) => void;
}

const FragmentItem: React.FC<FragmentItemProps> = ({
  item,
  index,
  canEdit,
  canDelete,
  searchText,
  selected,
  pagination,
  onSelect,
  onEdit,
  onDelete,
  onToggleFreeze,
}) => {
  const [hoveredId, setHoveredId] = useState<string | null>(null);
   const fragmentIndex = useMemo(() => {
    return String(index + 1 + (pagination.current - 1) * pagination.pageSize).padStart(2, '0');
   }, [index, pagination]);

  const highlightSearchText = (content: string) => {
    if (!searchText) return content;
    const parts = content.split(new RegExp(`(${searchText})`, 'gi'));
    return (
      <>
        {parts.map((part, i) =>
          part.toLowerCase() === searchText.toLowerCase() ? (
            <span key={i} className="bg-yellow-200">
              {part}
            </span>
          ) : (
            part
          )
        )}
      </>
    );
  };

  return (
    <Card
      className="mb-3 hover:shadow-md transition-shadow"
      styles={{
        body: {
          padding: '16px 24px',
        },
      }}
      onMouseEnter={() => (canEdit || canDelete) && setHoveredId(item.id!)}
      onMouseLeave={() => setHoveredId(null)}
    >
      <div className="flex items-start">
        <Checkbox checked={selected} onChange={(e) => onSelect(e.target.checked)} className="mt-1 mr-4" />
        <div className="flex-1" onClick={() => onEdit(item.id!, true, fragmentIndex)}>
          <Space>
            <div className="text-gray-700 mb-3 text-sm">
              片段
              {fragmentIndex}
            </div>
            <div className="text-gray-700 mb-3 text-sm">字数：{item.content?.length}</div>
            <div className="text-gray-700 mb-3 text-sm">
              状态：
              {item.enableFlag ? (
                <span className="text-green-500 text-sm">已激活</span>
              ) : (
                <span className="text-gray-300 text-sm">已冻结</span>
              )}
            </div>
          </Space>
          <div className="text-gray-700 mb-3 text-sm line-clamp-2">{highlightSearchText(item.content!)}</div>
        </div>
        {hoveredId === item.id && (
          <div className="absolute right-6 top-4 space-x-1 bg-white shadow-sm rounded-md px-2 py-1">
            {canEdit && (
              <>
                <Button type="link" size="small" onClick={() => onEdit(item.id!, false, fragmentIndex)}>
                  编辑
                </Button>
                <Button type="link" size="small" onClick={() => onToggleFreeze(item.id!)}>
                  {item.enableFlag ? '冻结' : '激活'}
                </Button>
              </>
            )}
            {canDelete && (
              <Button type="link" size="small" danger onClick={() => onDelete(item.id!)}>
                删除
              </Button>
            )}
          </div>
        )}
      </div>
    </Card>
  );
};

export default FragmentItem;
