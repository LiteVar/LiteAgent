import React, {useCallback, useMemo} from 'react';
import {Button, Space, Input, Typography, Image, Dropdown, Tooltip, Modal, message} from 'antd';
import { LeftOutlined, CheckOutlined, CloseOutlined, EditOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';
import {useHandleBackNavigation} from "@/hooks/useHandleBackNavigation";
import {AgentDetailVO} from "@/client";
import ExclamationCircleOutlined from "@ant-design/icons/ExclamationCircleOutlined"
import {buildImageUrl} from "@/utils/buildImageUrl";
import placeholderIcon from '@/assets/dashboard/avatar.png'

const { Title } = Typography;

interface HeaderProps {
  className?: string;
  agentInfo: AgentDetailVO | undefined
  editingName: boolean;
  onEdit: () => void;
  onSave: () => void;
  onCancel: () => void;
  handlePublish: () => void;
  handleShare: () => void;
  handleDelete: () => void;
  hasUnsavedChanges: boolean
  setHasUnsavedChanges: (t: boolean) => void;
  setAgentName: (name: string) => void;
}

const Header: React.FC<HeaderProps> = (props) => {
  const handleBackNavigation = useHandleBackNavigation()
  const { agentInfo, editingName, onEdit, onSave, onCancel, setAgentName, handleShare, handlePublish, handleDelete,
    className, hasUnsavedChanges, setHasUnsavedChanges } = props;
  const agentName = useMemo(() => agentInfo?.agent?.name,[agentInfo])
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
          handleBackNavigation()
        }
      })
    } else {
      handleBackNavigation()
    }
  }, [hasUnsavedChanges])

  const handleSave = useCallback(() => {
    if (!agentName?.trim()) {
      message.warning("名称不能为空")
    } else {
      onSave()
    }
  }, [agentName])

  return (
    <header className={`bg-gray-900 h-11 p-6 flex justify-between items-center ${className}`}>
      <div className="flex items-center">
        <LeftOutlined className="text-lg text-white mr-4 cursor-pointer" onClick={onBack} />
        <Image
          preview={false}
          alt="agent icon"
          src={buildImageUrl(agentInfo?.agent?.icon!) || placeholderIcon}
          width={40}
          height={40}
          className="rounded"
        />
        {editingName ? (
          <div className="flex items-center ml-4">
            <Input
              maxLength={20}
              defaultValue={agentName}
              onChange={(e) => {
                setAgentName(e.target.value)
                setHasUnsavedChanges(true)
              }}
              className="mr-2"
            />
            <CheckOutlined className="text-large mr-2 text-white cursor-pointer" onClick={handleSave} />
            <CloseOutlined className="text-large text-white cursor-pointer" onClick={onCancel} />
          </div>
        ) : (
          <div className="flex items-center ml-4">
            <Title level={4} className="m-0 text-white">{agentName}</Title>
            {agentInfo?.canEdit &&
              <EditOutlined onClick={onEdit} className="ml-2 text-white cursor-pointer" />
            }
          </div>
        )}
      </div>
      <Space size="large">
        {agentInfo?.canEdit &&
          <Button onClick={onSave} type="primary" className="mr-2">保存</Button>
        }
        {agentInfo?.canEdit &&
          <Button
            type="link"
            onClick={handlePublish}
            disabled={!agentInfo?.canRelease}
            className="text-gray-200 bg-transparent hover:text-white focus:ring-0 focus:outline-none"
          >
            发布
          </Button>
        }
        {agentInfo?.canRelease && (
          <Tooltip title="内容有更新，如需同步到客户端，请选择发布">
            <ExclamationCircleOutlined className="text-yellow-500 -ml-7" />
          </Tooltip>
        )}
        {agentInfo?.canEdit &&
          <Button
            type="link"
            disabled={agentInfo?.agent?.status === 0}
            onClick={handleShare}
            className="text-gray-200 bg-transparent hover:text-white focus:ring-0 focus:outline-none"
          >
            分享
          </Button>
        }
        {agentInfo?.canDelete &&
          <Dropdown menu={{ items, onClick: (e)=> {if(e.key === "delete") handleDelete()} }}>
            <Button
              type="link"
              className="text-gray-200 bg-transparent hover:text-white focus:ring-0 focus:outline-none"
            >
              更多
            </Button>
          </Dropdown>
        }
      </Space>
    </header>
  );
};

export default Header;
