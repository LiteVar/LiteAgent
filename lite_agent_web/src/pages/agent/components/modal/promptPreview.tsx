// 
import React, { useState } from 'react';
import { Modal } from 'antd';
import { marked } from 'marked';
import { linkRenderer } from '@/utils/markdownRenderer';

interface PromptPreviewProps {
  prompt: string;
  visible: boolean;
  onClose: () => void;
}

const renderer = new marked.Renderer();
renderer.link = linkRenderer;

marked.setOptions({
  gfm: true,
  renderer: renderer,
});
const PromptPreview: React.FC<PromptPreviewProps> = ({ prompt, visible, onClose }) => {
  const handleOk = () => {
    onClose();
  };

  return (
    <Modal
      centered
      title="提示词预览"
      open={visible}
      onOk={handleOk}
      onCancel={handleOk}
      width={800}
      footer={null}
      maskClosable={false}
      destroyOnClose
    >
      <div className="max-h-[70vh] overflow-y-auto px-4">
        <div
          className={`prose markdown w-full overflow-hidden`}
          dangerouslySetInnerHTML={{
            __html: marked.parse(prompt || ''),
          }}
        ></div>
      </div>
    </Modal>
  );
};
export default PromptPreview;