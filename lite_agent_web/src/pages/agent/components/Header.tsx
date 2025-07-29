import React, { useCallback, useMemo } from 'react';
import { Button, Space, Typography, Image, Dropdown, Tooltip, Modal } from 'antd';
import { LeftOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { AgentDetailVO } from '@/client';
import ExclamationCircleOutlined from '@ant-design/icons/ExclamationCircleOutlined';
import { buildImageUrl } from '@/utils/buildImageUrl';
import placeholderIcon from '@/assets/login/logo_black.png';
import { useNavigate } from 'react-router-dom';

const { Title } = Typography;

interface HeaderProps {
  className?: string;
  agentInfo: AgentDetailVO | undefined;
  onSave: () => void;
  handlePublish: () => void;
  handleDelete: () => void;
  hasUnsavedChanges: boolean;
  showMaxTokenWarning: boolean;
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
    showMaxTokenWarning
  } = props;
  const agentName = useMemo(() => agentInfo?.agent?.name, [agentInfo]);
  const items: MenuProps['items'] = [
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
  }, [hasUnsavedChanges, agentInfo]);

  const releaseTip = useMemo(() => {  
    if (showMaxTokenWarning) {
      return '模型名称的 Max Token 值已发生修改，为了确保 Agent 能够正常使用，请重新保存并发布';
    }
    return '内容有更新，如需同步到客户端，请选择发布';
  }, [showMaxTokenWarning]);

  return (
    <header className={`bg-white h-11 p-6 flex justify-between items-center ${className}`}>
      <div className="flex items-center">
        <LeftOutlined className="text-lg mr-4 cursor-pointer" onClick={onBack} />
        <Image
          preview={false}
          alt="agent icon"
          src={buildImageUrl(agentInfo?.agent?.icon!) || placeholderIcon}
          width={48}
          height={48}
          className="rounded-md bg-[#F5F5F5] p-3"
        />
        <div className="flex items-center ml-4">
          <Title level={4} className="m-0">
            {agentName}
          </Title>
        </div>
      </div>
      <Space size="middle">
        {agentInfo?.canEdit && (
          <Button onClick={onSave} type="primary" className="mr-2">
            保存
          </Button>
        )}
        {agentInfo?.canEdit && (
          <Button
            type="primary"
            onClick={handlePublish}
            disabled={!agentInfo?.canRelease}
            className="mr-4"
            // className="text-gray-600 bg-transparent hover:text-black focus:ring-0 focus:outline-none"
          >
            发布
          </Button>
        )}
        {((agentInfo?.canRelease || showMaxTokenWarning) && !agentInfo?.agent?.autoAgentFlag) && (
          <Tooltip title={releaseTip} defaultOpen>
            <ExclamationCircleOutlined className="text-yellow-500 -ml-7" />
          </Tooltip>
        )}
        {agentInfo?.canDelete && !agentInfo?.agent?.autoAgentFlag && (
          <Dropdown
            menu={{
              items,
              onClick: (e) => {
                if (e.key === 'delete') handleDelete();
              },
            }}
          >
            <Button
              className="border border-black bg-transparent text-black rounded-md hover:bg-gray-100 transition"
            >
              更多
            </Button>
          </Dropdown>
        )}
      </Space>
    </header>
  );
};

export default Header;
