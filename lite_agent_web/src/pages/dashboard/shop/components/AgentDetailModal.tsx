import React, { MouseEvent } from 'react';
import { Modal, Button } from 'antd';
import { AgentDTO } from '@/client';
import { buildImageUrl } from '@/utils/buildImageUrl';
import AgentIconSvg from '@/assets/common/agent_icon_svg';

interface AgentDetailModalProps {
  open: boolean;
  agent?: AgentDTO;
  onCancel: () => void;
  onNavigateChatPage: (event: MouseEvent<HTMLElement>) => void;
}

const AgentDetailModal: React.FC<AgentDetailModalProps> = ({ open, agent, onCancel, onNavigateChatPage }) => {
  return (
    <Modal
      closable
      onCancel={onCancel}
      footer={null}
      maskClosable
      open={open}
      centered
      width={420}
      className="rounded-2xl overflow-hidden"
      title={
        <span className="text-[#1D4A6B] text-[18px] font-medium">
          Agent详情
        </span>
      }
    >
      {/* 内容区 */}
      <div className="flex flex-col p-0">

        {/* Agent Banner 卡片 */}
        <div
          className="relative overflow-hidden h-[230px] bg-[#ECF6FD] rounded-xl"
        >
          {/* 装饰性模糊背景（类似 BgSvg 色块效果） */}
          <div className="absolute inset-0 overflow-hidden">
            <div
              className="absolute w-[233px] h-[200px] -top-[96px] -right-[40px] bg-[#B7DCFD] rounded-full blur-[73px] opacity-70"
            />
            <div
              className="absolute w-[399px] h-[354px] -top-[150px] -left-[39px] bg-[#C1D2EC] rounded-full blur-[73px] opacity-70"
            />
            <div
              className="absolute w-[266px] h-[256px] top-[33px] left-[174px] bg-[#C4E7F5] rounded-full blur-[73px] opacity-70"
            />
            <div
              className="absolute w-[263px] h-[329px] top-[58px] -right-[80px] bg-[#B7DCFD] rounded-full blur-[73px] opacity-60"
            />
            <div
              className="absolute w-[264px] h-[334px] top-[107px] -left-[30px] bg-[#C1D2EC] rounded-full blur-[73px] opacity-60"
            />
          </div>

          {/* Agent 图标：80x80，绝对定位于 Banner 内居中偏上 */}
          <div
            className="absolute left-1/2 -translate-x-1/2 top-[63px] w-20 h-20 rounded-2xl flex items-center justify-center overflow-hidden"
          >
            {agent?.icon ? (
              <img
                src={buildImageUrl(agent.icon)}
                alt={agent?.name}
                className="w-20 h-20 object-cover rounded-2xl"
              />
            ) : (
              <AgentIconSvg seed={agent?.id} width={80} height={80} />
            )}
          </div>

          {/* Agent 名称：置于图标下方 */}
          <div
            className="absolute left-1/2 -translate-x-1/2 top-[159px] w-[220px] text-center text-[18px] font-medium text-black whitespace-nowrap overflow-hidden text-ellipsis"
          >
            {agent?.name}
          </div>
        </div>

        {/* 描述信息 */}
        {(agent?.description || agent?.autoAgentFlag) && (
          <div className="pt-4 flex flex-col">
            {!!agent?.autoAgentFlag && (
              <div
                className="text-sm px-3 py-1 rounded-full self-start bg-[#ECF6FD] text-[#1D4A6B]"
              >
                类型： Auto Muti Agent
              </div>
            )}
            {agent?.description && (
              <p className="text-sm leading-relaxed text-[#58636C]">
                {agent.description}
              </p>
            )}
          </div>
        )}

        {/* Footer：开始聊天按钮 */}
        <div 
          className={`${agent?.description ? 'pt-2' : 'pt-6'}`}
        >
          <button
            onClick={(e) => onNavigateChatPage(e)}
            className="w-full h-12 rounded-xl bg-[#40A5EE] border-none text-white text-base font-normal cursor-pointer hover:bg-[#40A5EE]/90 transition-colors"
          >
            开始聊天
          </button>
        </div>
      </div>
    </Modal>
  );
};

export default AgentDetailModal;
