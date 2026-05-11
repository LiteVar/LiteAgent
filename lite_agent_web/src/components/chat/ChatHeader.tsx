import React from 'react';
import { Avatar, Button, Dropdown } from 'antd';
import { ClearOutlined, EllipsisOutlined } from '@ant-design/icons';
import ClearImg from '@/assets/agent/clear.png';
import { ChatHeaderProps } from '@/types/chat';

const ChatHeader: React.FC<ChatHeaderProps> = ({ mode, agentId, agentName, onResetSession }) => {
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
      className={
        mode === 'dev'
          ? 'bg-transparent flex-none flex items-center h-[60px] px-6'
          : 'bg-transparent h-[60px] flex-none flex items-center px-6 pt-2'
      }
    >
      {mode === 'prod' && <div className="flex-1 text-lg font-medium text-[#1D4A6B]">{agentName}</div>}
      {mode === 'prod' && (
        <Dropdown menu={{ items: agentDropDownItems }} trigger={['click']} placement="bottomRight">
          <div className="w-8 h-8 flex items-center justify-center cursor-pointer hover:bg-black/5 rounded-full transition-colors">
            <EllipsisOutlined className="text-2xl text-[#ACB6BE]" />
          </div>
        </Dropdown>
      )}
      {mode === 'dev' && <div className="flex-1 text-lg text-[#1D4A6B] font-medium">调试</div>}
      {mode === 'dev' && (
        <Button 
          type="text"
          onClick={onResetSession} 
          icon={
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M6.75 9.75L14.25 17.25" stroke="#40A5EE" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M4.125 17.625L6.75 15" stroke="#40A5EE" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M6.375 19.875L9 17.25" stroke="#40A5EE" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M14.625 11.625L16.1897 13.1897C16.4708 13.471 16.6287 13.8523 16.6287 14.25C16.6287 14.6477 16.4708 15.029 16.1897 15.3103L9 22.5L1.5 15L8.68969 7.81031C8.97096 7.52922 9.35235 7.37132 9.75 7.37132C10.1477 7.37132 10.529 7.52922 10.8103 7.81031L12.375 9.375L17.9062 2.90625C18.3289 2.48356 18.9022 2.24609 19.5 2.24609C20.0978 2.24609 20.6711 2.48356 21.0938 2.90625C21.5164 3.32894 21.7539 3.90222 21.7539 4.5C21.7539 5.09777 21.5164 5.67106 21.0938 6.09375L14.625 11.625Z" stroke="#40A5EE" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          }
          className="text-[#40A5EE] text-base border h-10 border-[#40A5EE] rounded-lg [&_.ant-btn-icon]:w-6 [&_.ant-btn-icon]:h-6"
        >
          清空
        </Button>
      )}
    </div>
  );

};

export default ChatHeader; 