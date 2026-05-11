import React, { useMemo } from 'react';
import { Button, Checkbox } from 'antd';
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
    <div
      className={`
        relative mb-2 p-3 rounded-2xl transition-all duration-200 group border border-solid
        ${selected 
          ? 'bg-blue-50/50 border-blue-200 shadow-sm' 
          : 'bg-white/40 border-gray-200 hover:bg-white/60 hover:shadow-md'}
      `}
    >
      <div className="flex items-start gap-4">
        <Checkbox 
          checked={selected} 
          onChange={(e) => onSelect(e.target.checked)} 
          className="custom-checkbox" 
        />
        
        <div className="flex-1 min-w-0 flex flex-col gap-3">
          <div
            className="flex items-center gap-4 mt-1 cursor-pointer"
            onClick={() => onEdit(item.id!, true, fragmentIndex)}
          >
            <span className="text-xs text-gray-600">片段{fragmentIndex}</span>
            <div className="flex items-center gap-1.5 text-gray-500 text-xs">
              <span className="font-medium">字数:</span>
              <span>{item.content?.length || 0}</span>
            </div>
            <div className="flex items-center gap-1.5 text-xs">
              <span className="font-medium text-gray-500">状态:</span>
              {item.enableFlag ? (
                <span className="text-green-600 font-medium">已激活</span>
              ) : (
                <span className="text-yellow-600 font-medium">已冻结</span>
              )}
            </div>
          </div>

          <div className="flex items-start justify-between gap-4 min-w-0">
            <div
              className="flex-1 min-w-0 text-[#383F44] text-sm leading-relaxed break-all line-clamp-2 bg-white/30 rounded-xl border border-white/40 group-hover:bg-white/50 transition-colors cursor-pointer"
              onClick={() => onEdit(item.id!, true, fragmentIndex)}
            >
              {highlightSearchText(item.content!)}
            </div>
            <div className="flex items-center gap-2 shrink-0">
              {canEdit && (
                <>
                  <Button
                    type="link"
                    size="small"
                    className="text-[#40a5ee] font-medium !px-0"
                    onClick={(e) => {
                      e.stopPropagation();
                      onEdit(item.id!, false, fragmentIndex);
                    }}
                  >
                    编辑
                  </Button>
                  <Button
                    type="link"
                    size="small"
                    className={`font-medium !px-0 ${item.enableFlag ? 'text-[#40a5ee]' : '!text-green-500'}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleFreeze(item.id!);
                    }}
                  >
                    {item.enableFlag ? '冻结' : '激活'}
                  </Button>
                </>
              )}
              {canDelete && (
                <Button
                  type="link"
                  size="small"
                  danger
                  className="font-medium !px-0"
                  onClick={(e) => {
                    e.stopPropagation();
                    onDelete(item.id!);
                  }}
                >
                  删除
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FragmentItem;
