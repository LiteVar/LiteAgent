import React, { useState } from 'react';
import { Checkbox, CheckboxChangeEvent, Modal } from 'antd';

interface FileExportModalProps {
  title: string;
  visible: boolean;
  disabled?: boolean;
  id: string | undefined;
  onClose: () => void;
  onOk: (id: string, checked: boolean) => void;
}

const FileExportModal: React.FC<FileExportModalProps> = ({ title, visible, id, onClose, onOk, disabled = false }) => {
  const [checked, setChecked] = useState(false);

  const onCloseModal = () => {
    setChecked(false);
    onClose();
  };

  const onChange = (e: CheckboxChangeEvent) => {
    setChecked(e.target.checked);
  };

  return (
    <Modal
      zIndex={1000}
      centered
      title={`导出${title}`}
      open={visible}
      onCancel={onCloseModal}
      okText="确定"
      cancelText="取消"
      onOk={() => onOk(id as string, checked)}
    >
      <div className='leading-6 text-sm'>
        {`为保障您的账户安全，${title}中包含的 API Key 将默认以“暗文” 形式导出。“暗文”导出的 Key 无法在导出的文件中直接被读取，在重新导入平台时需重新设置才可正常使用。`}
      </div>
      <Checkbox disabled={disabled} className='mt-9' checked={checked} onChange={onChange}>以明文形式导出 API Key</Checkbox>
      <div className='leading-6 text-sm text-gray-500 mt-2'>请注意：开启后，API Key 的完整内容将直接显示在导出的文件中。这可能带来安全风险，请确保您在安全的环境下存储和传输该文件。</div>
    </Modal>
  );
};

export default FileExportModal;
