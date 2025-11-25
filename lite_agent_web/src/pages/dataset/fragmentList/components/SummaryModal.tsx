import React from 'react';
import { Modal, Button, Input, Spin } from 'antd';

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
      title="文档摘要"
      open={open}
      onCancel={onClose}
      width={800}
      style={{
        maxHeight: '70vh'
      }}
      footer={[
        <Button key="close" type="primary" onClick={onClose}>
          关闭
        </Button>,
      ]}
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: 24 }}>
          <Spin />
        </div>
      ) : (
        <Input.TextArea
          readOnly
          value={summary}
          rows={20}
        />
      )}
    </Modal>
  );
};

export default SummaryModal;