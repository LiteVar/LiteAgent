import React from 'react';
import { Modal } from 'antd';
import SearchResults from '@/pages/dataset/retrievalTest/components/SearchResults';
import { SegmentVO } from '@/client';

interface KnowledgeModalProps {
  visible: boolean;
  queryText: string;
  results: SegmentVO[];
  onClose: () => void;
}

const KnowledgeModal: React.FC<KnowledgeModalProps> = ({
  visible,
  queryText,
  results,
  onClose,
}) => {
  return (
    <Modal
      title=""
      centered
      open={visible}
      footer={null}
      width={800}
      onCancel={onClose}
    >
      <div className="pt-3">
        <div className="mb-6">
          检索内容: <span className="text-blue-400">{queryText}</span>
        </div>
        <SearchResults results={results} />
      </div>
    </Modal>
  );
};

export default KnowledgeModal;