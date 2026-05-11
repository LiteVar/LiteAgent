import React from 'react';
import { Modal, Button, Spin } from 'antd';

export interface SummaryModalProps {
  open: boolean;
  summary: string;
  loading?: boolean;
  onClose: () => void;
}

const SummaryModal: React.FC<SummaryModalProps> = ({
  open,
  summary,
  loading = false,
  onClose,
}) => {
  return (
    <Modal
      title={<span className="text-[#1D4A6B] font-semibold">文档摘要</span>}
      open={open}
      onCancel={onClose}
      width={800}
      centered
      footer={[
        <Button 
          key="close" 
          type="primary" 
          onClick={onClose}
          className="rounded-xl bg-[#40A5EE] hover:!bg-[#40A5EE]/90 border-none shadow-md shadow-blue-200/50 px-8 h-10 font-medium"
        >
          关闭
        </Button>,
      ]}
      className="customModal"
    >
      <div>
        {loading ? (
          <div className="flex flex-col items-center justify-center py-20 gap-4">
            <Spin size="large" />
            <span className="text-gray-400">正在加载摘要...</span>
          </div>
        ) : (
          <div className="bg-gray-50/50 p-6 rounded-2xl border border-black/5 max-h-[60vh] overflow-y-auto custom-scrollbar">
            <div className="text-[#383F44] text-sm leading-relaxed whitespace-pre-wrap">
              {summary || '暂无摘要内容'}
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
};

export default SummaryModal;