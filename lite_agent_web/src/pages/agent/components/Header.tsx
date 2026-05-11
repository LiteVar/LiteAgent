import React, { useCallback, useMemo } from 'react';
import { Button, Space, Typography, Image, Dropdown, Tooltip, Modal } from 'antd';
import { LeftOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { Agent, AgentDetailVO } from '@/client';
import ExclamationCircleOutlined from '@ant-design/icons/ExclamationCircleOutlined';
import { buildImageUrl } from '@/utils/buildImageUrl';
import placeholderIcon from '@/assets/agent/agent.png';
import { useNavigate } from 'react-router-dom';

const { Title } = Typography;

interface HeaderProps {
  className?: string;
  agentInfo: AgentDetailVO | undefined;
  onSave: () => void;
  handlePublish: () => void;
  handleDelete: () => void;
  showExportModal: (agent: Agent) => void;
  hasUnsavedChanges: boolean;
  showMaxTokenWarning: boolean;
  visibleButtons: boolean;
}

const Header: React.FC<HeaderProps> = (props) => {
  const navigate = useNavigate();
  
  const {
    className, 
    agentInfo, 
    onSave, 
    handlePublish, 
    handleDelete, 
    hasUnsavedChanges,
    showExportModal,
    showMaxTokenWarning,
    visibleButtons = false
  } = props;
  const agentName = useMemo(() => agentInfo?.agent?.name, [agentInfo]);
  const items: MenuProps['items'] = [
    {
      key: 'export',
      label: '导出',
      danger: false,
    },
    {
      key: 'delete',
      label: '删除',
      danger: true,
    },
  ];
  const onBack = useCallback(() => {
    if (hasUnsavedChanges) {
      Modal.confirm({
        title: '系统消息',
        content: '有内容未保存，确认离开？',
        onOk: async () => {
          navigate(`/workspaces/${agentInfo?.agent?.workspaceId}/agents`);
        },
      });
    } else {
      navigate(`/workspaces/${agentInfo?.agent?.workspaceId}/agents`);
    }
  }, [hasUnsavedChanges, agentInfo, navigate]);

  const releaseTip = useMemo(() => {  
    if (showMaxTokenWarning) {
      return '模型名称的 Max Token 值已发生修改，为了确保 Agent 能够正常使用，请重新保存并发布';
    }
    return '内容有更新，如需同步到客户端，请选择发布';
  }, [showMaxTokenWarning]);

  return (
    <header className={`bg-white/60 mb-4 px-6 py-4 flex justify-between items-center ${className}`}>
      <div className="flex items-center">
        <div 
          className="flex items-center justify-center w-8 h-8 rounded-full hover:bg-black/5 cursor-pointer transition-colors ml-2 mr-8"
          onClick={onBack}
        >
          <LeftOutlined className="text-gray-600" />
        </div>
        <div className="flex items-center justify-center border border-white">
          <Image
            preview={false}
            alt="agent icon"
            src={buildImageUrl(agentInfo?.agent?.icon!) || placeholderIcon}
            width={40}
            height={40}
            className="rounded-lg object-cover"
          />
        </div>
        <div className="flex flex-col ml-2">
          <Title level={4} style={{ margin: 0, color: '#1D4A6B', fontSize: '18px', fontWeight: 500 }}>
            {agentName}
          </Title>
        </div>
      </div>
      {visibleButtons && <Space size="middle">
        {agentInfo?.canEdit && (
          <Button 
            icon={
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M20.25 7.81031V19.5C20.25 19.6989 20.171 19.8897 20.0303 20.0303C19.8897 20.171 19.6989 20.25 19.5 20.25H4.5C4.30109 20.25 4.11032 20.171 3.96967 20.0303C3.82902 19.8897 3.75 19.6989 3.75 19.5V4.5C3.75 4.30109 3.82902 4.11032 3.96967 3.96967C4.11032 3.82902 4.30109 3.75 4.5 3.75H16.1897C16.3883 3.75009 16.5788 3.82899 16.7194 3.96938L20.0306 7.28063C20.171 7.42117 20.2499 7.61166 20.25 7.81031Z" stroke="#40A5EE" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M7.5 20.25V14.25C7.5 14.0511 7.57902 13.8603 7.71967 13.7197C7.86032 13.579 8.05109 13.5 8.25 13.5H15.75C15.9489 13.5 16.1397 13.579 16.2803 13.7197C16.421 13.8603 16.5 14.0511 16.5 14.25V20.25" stroke="#40A5EE" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M14.25 6.75H9" stroke="#40A5EE" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            }
            onClick={onSave} 
            type="primary" 
            className="bg-transparent border-[#40A5EE] text-[#40A5EE] rounded-xl h-10 px-4 font-medium [&_.ant-btn-icon]:w-6 [&_.ant-btn-icon]:h-6"
          >
            保存
          </Button>
        )}
        {agentInfo?.canEdit && (
          <Button
            type="primary"
            onClick={handlePublish}
            icon={
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M10.125 13.875L15 9" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M20.9711 3.9544C21.0073 3.82611 21.0086 3.6905 20.9749 3.56153C20.9412 3.43256 20.8738 3.3149 20.7796 3.22065C20.6853 3.1264 20.5676 3.05897 20.4387 3.0253C20.3097 2.99163 20.1741 2.99294 20.0458 3.02909L2.0458 8.48722C1.89885 8.52869 1.76805 8.61403 1.6709 8.73183C1.57375 8.84964 1.51487 8.99429 1.50213 9.14645C1.48939 9.29861 1.5234 9.45104 1.59961 9.58335C1.67582 9.71567 1.7906 9.82157 1.92861 9.8869L10.1252 13.875L14.1133 22.0707C14.1786 22.2087 14.2845 22.3235 14.4169 22.3997C14.5492 22.4759 14.7016 22.5099 14.8538 22.4971C15.0059 22.4844 15.1506 22.4255 15.2684 22.3284C15.3862 22.2312 15.4715 22.1004 15.513 21.9535L20.9711 3.9544Z" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            }
            disabled={!agentInfo?.canRelease}
            className="bg-[#40A5EE] border-[#40A5EE] rounded-xl h-10 px-4 font-medium disabled:opacity-50 text-white [&_.ant-btn-icon]:w-6 [&_.ant-btn-icon]:h-6"
          >
            发布
          </Button>
        )}
        {(agentInfo?.canRelease || showMaxTokenWarning) && (
          <Tooltip title={releaseTip} defaultOpen>
            <ExclamationCircleOutlined className="text-yellow-500 -ml-3" />
          </Tooltip>
        )}
        {agentInfo?.canDelete && !agentInfo?.agent?.autoAgentFlag && (
          <Dropdown
            menu={{
              items,
              onClick: (e) => {
                if (e.key === 'delete') handleDelete();
                if (e.key === 'export') showExportModal(agentInfo?.agent!);
              },
            }}
          >
            <Button
              className="border-[#E0E3E6] bg-white/60 text-gray-700 rounded-xl h-10 px-4 hover:bg-white/80 transition"
            >
              更多
            </Button>
          </Dropdown>
        )}
      </Space>}
    </header>
  );
};



export default Header;
