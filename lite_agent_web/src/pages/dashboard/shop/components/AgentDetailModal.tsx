import React, { MouseEvent } from 'react';
import { Modal, Button } from 'antd';
import { AgentDTO } from '@/client';
import { buildImageUrl } from '@/utils/buildImageUrl';
import logoIcon from '@/assets/login/logo_svg';

interface AgentDetailModalProps {
  open: boolean;
  agent?: AgentDTO;
  onCancel: () => void;
  onNavigateChatPage: (event: MouseEvent<HTMLElement>) => void;
}

const AgentDetailModal: React.FC<AgentDetailModalProps> = ({ open, agent, onCancel, onNavigateChatPage }) => {
  return (
    <Modal
      title="Agent详情"
      closable
      onCancel={onCancel}
      className="!w-[538px]"
      footer={null}
      maskClosable={false}
      open={open}
      centered
    >
      <div className="px-2 pt-10 pb-6 flex flex-col items-center">
        {agent?.icon ? (
          <img className="w-[100px] h-[100px]" src={buildImageUrl(agent?.icon || '')} />
        ) : (
          <span className="customeSvg w-[100px] h-[100px] flex items-center justify-center rounded-md bg-[#F5F5F5] text-black">
            <span className="w-[60px] h-[60px]">{logoIcon}</span>
          </span>
        )}
        <div className="text-2xl mt-5">{agent?.name}</div>
        {!!agent?.autoAgentFlag && <div className="text-xl mt-3">类型：Auto Multi Agent</div>}
        <div className="w-full mt-5 mb-16">
          <div className="text-xs text-[#999]">{agent?.description}</div>
        </div>
        <Button onClick={onNavigateChatPage} type="primary" className="w-full h-12">
          开始聊天
        </Button>
      </div>
    </Modal>
  );
};

export default AgentDetailModal; 