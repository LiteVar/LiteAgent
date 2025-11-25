import React from 'react';
import { Modal, Button } from 'antd';
import { marked } from 'marked';
import { linkRenderer } from '@/utils/markdownRenderer';
import { copyToClipboard } from '@/utils/clipboard';

interface PreviewSourceModalProps {
  open: boolean;
  title: string;
  markdown: string;
  onCancel: () => void;
}

const renderer = new marked.Renderer();
renderer.link = linkRenderer;

marked.setOptions({
  gfm: true,
  renderer: renderer,
});

const PreviewSourceModal: React.FC<PreviewSourceModalProps> = ({
  open,
  markdown,
  onCancel,
  title
}) => {

  return (
    <Modal
      centered
      className='preview-source-modal'
      title={title}
      open={open}
      onCancel={onCancel}
      footer={null}
      maskClosable={false}
      destroyOnClose
      width={800}
    >
      <div className="max-h-[60vh] flex flex-col pt-5">
        <div
          className={`flex-1 prose markdown overflow-hidden overflow-y-auto px-6`}
          dangerouslySetInnerHTML={{
            __html: marked.parse(markdown || ''),
          }}
        ></div>
        <div className="flex-none flex justify-between items-center border-0 border-t border-t-gray-200 border-solid py-4 mx-6">
          <div className='text-blue-500 cursor-pointer text-xs' onClick={() => {
            copyToClipboard(markdown);
          }}>复制内容</div>
          <Button className='py-2 px-7 text-xs' onClick={onCancel}>关闭</Button>
        </div>
      </div>
    </Modal>
  );
};

export default PreviewSourceModal;
