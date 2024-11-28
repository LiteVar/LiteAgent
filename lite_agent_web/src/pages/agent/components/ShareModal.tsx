import React, {useCallback, useEffect} from 'react';
import {Modal, Switch, Button, Input, message} from 'antd';
import { useState } from 'react';
import {AgentDetailVO, postV1AgentEnableShareById} from '@/client';
import {copyToClipboard} from "@/utils/clipboard";

interface ShareModalProps {
  visible: boolean;
  onClose: () => void;
  agentInfo: AgentDetailVO;
}

const ShareModal: React.FC<ShareModalProps> = ({ visible, onClose, agentInfo }) => {
  const [isSharing, setIsSharing] = useState(agentInfo?.agent?.shareFlag);
  const agentChatUrl = `${window.location.origin}/dashboard/${agentInfo?.agent?.workspaceId}/shareAgent/${agentInfo?.agent?.id}`;

  useEffect(() => {
    setIsSharing(agentInfo?.agent?.shareFlag);
  }, [agentInfo])

  const handleSwitchChange = useCallback(async (check: boolean) => {
    setIsSharing(check);
    await postV1AgentEnableShareById({
      path: {id: agentInfo?.agent?.id!},
      headers: {
        'Workspace-id': agentInfo?.agent?.workspaceId!
      },});
  }, [agentInfo])

  const handleCopy = useCallback(async () => {
    await copyToClipboard(agentChatUrl);
    message.success('复制成功');
  }, [agentChatUrl])

  return (
    <Modal
      centered
      title="分享"
      open={visible}
      onCancel={onClose}
      footer={null}
      className="w-full max-w-lg"
    >
      <div className="flex items-center mt-4">
        <span className="mr-4">分享链接</span>
        <Switch checked={isSharing} onChange={handleSwitchChange} />
      </div>
      {isSharing ? (
        <div className="mt-4">
          <p className="text-gray-600">已启用，workspace成员可以在用户端使用此agent</p>
          <div className="mt-2 flex">
            <Input value={agentChatUrl} readOnly className="flex-grow mr-2" />
            <Button type="primary" onClick={handleCopy}>复制</Button>
          </div>
          <p className="text-gray-500 mt-2">分享此链接给其他用户，可以使用此agent</p>
        </div>
      ) : (
        <div className="mt-4">
          <p className="text-gray-600">开启后，workspace成员可以在用户端使用此agent</p>
        </div>
      )}
    </Modal>
  );
};

export default ShareModal;
