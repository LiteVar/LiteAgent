import React from 'react';
import { Avatar, Button, Dropdown } from 'antd';
import { ClearOutlined, EllipsisOutlined } from '@ant-design/icons';
import ClearImg from '@/assets/agent/clear.png';

interface IChatHeaderProps {
  mode: 'dev' | 'prod';
  agentId: string;
  agentName?: string;
  onResetSession: () => void;
}

const ChatHeader: React.FC<IChatHeaderProps> = ({ mode, agentId, agentName, onResetSession }) => {
  const agentDropDownItems = [
    {
      key: `clear-session-${agentId}`,
      label: (
        <div onClick={onResetSession} className="flex items-center">
          <ClearOutlined />
          <div className="ml-2">清空上下文</div>
        </div>
      ),
    },
  ];

  return (
    <div
      style={{ borderBottom: mode === 'prod' ? '2px solid rgba(0,0,0,0.05)' : 'none' }}
      className={
        mode === 'dev'
          ? 'bg-white flex-none flex items-center h-[60px] ml-1.5 px-6'
          : 'bg-white h-[60px] flex-none flex items-center px-6 pb-4 pt-5'
      }
    >
      {mode === 'prod' && <div className="flex-1 text-[18px] text-[#333]">{agentName}</div>}
      {mode === 'prod' && (
        <Dropdown menu={{ items: agentDropDownItems }} trigger={['click']} placement="bottom">
          <a onClick={(e) => e.preventDefault()}>
            <EllipsisOutlined style={{ fontSize: '24px' }} className="flex-none" />
          </a>
        </Dropdown>
      )}
      {mode === 'dev' && <div className="flex-1 text-[18px] text-black/85 font-bold">调试</div>}
      {mode === 'dev' && (
        <Button color="primary" variant="filled" onClick={onResetSession} icon={<Avatar size={16} src={ClearImg} />}>
          清空
        </Button>
      )}
    </div>
  );
};

export default ChatHeader; 