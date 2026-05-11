import React from 'react';
import emptyImage from '@/assets/dataset/no_knowledge_base.png';

interface EmptyStateProps {
  text?: string;
  className?: string;
}

const EmptyState: React.FC<EmptyStateProps> = ({ text = '暂无数据', className }) => {
  return (
    <div className={`flex flex-col items-center justify-center py-20 ${className}`}>
      <div className="relative w-[265px] h-[265px] flex items-center justify-center">
        {/* Decorative elements based on Figma description if possible, or just the image */}
        <div className="absolute inset-0 bg-[#C3DBE9]/30 blur-[11px] rounded-[27px] border-2 border-white" />
        <img 
          src={emptyImage} 
          alt="No Data" 
          className="relative z-10 w-[120px] h-[120px] object-contain opacity-80" 
        />
        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 whitespace-nowrap">
          <span className="text-[#1D4A6B] font-medium text-sm">{text}</span>
        </div>
      </div>
    </div>
  );
};

export default EmptyState;
