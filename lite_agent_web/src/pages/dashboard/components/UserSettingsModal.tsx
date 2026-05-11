import React, { MouseEvent, useState } from 'react';
import { Modal, Upload, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { UploadChangeParam } from 'antd/es/upload/interface';
import { Account, putV1UserUpdate } from '@/client';
import { buildImageUrl } from '@/utils/buildImageUrl';
import ResponseCode from '@/constants/ResponseCode';
import ResetPasswordModal from './ResetPasswordModal';
import { beforeUpload, customUploadRequest } from '@/utils/uploadFile';

interface UserSettingsModalProps {
  modalOpen: boolean;
  onModalCancel: () => void;
  userInfo?: Account;
  name: string;
  setName: (name: string) => void;
  refreshAgent: () => Promise<void>;
}

const UserSettingsModal: React.FC<UserSettingsModalProps> = ({
  modalOpen,
  onModalCancel,
  userInfo,
  name,
  setName,
  refreshAgent
}) => {
  const [editing, setEditing] = useState(false);
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);

  const onUpdateUserName = async (event: MouseEvent<HTMLElement>) => {
    event.stopPropagation();
    if (!name) {
      message.error('昵称不能为空');
    } else if (!!name && name != userInfo?.name) {
      console.log('update name', name);
      const res = await putV1UserUpdate({
        query: {
          avatar: userInfo?.avatar,
          name: name,
        },
      });
      if (res?.data?.code === ResponseCode.S_OK) {
        message.success('昵称修改成功');
        await refreshAgent();
        setEditing(false);
      } else {
        message.error('昵称修改失败');
      }
    } else {
      setEditing(false);
    }
  };

const handleImageUpload = async (info: UploadChangeParam) => {
    console.log(info);
    if (info.file.status === 'done') {
        // info.file.response 就是 customUploadRequest 返回的完整图片 URL
        const imageUrl = info.file.response;
        await putV1UserUpdate({
            query: {
                avatar: imageUrl,
                name: userInfo?.name!,
            },
        });
        await refreshAgent();
        
    } else if (info.file.status === 'error') {
        message.error(`${info.file.name} 上传失败`);
    }
};

  const onEditName = () => {
    setName(userInfo?.name || '');
    setEditing(true);
  };

  const onUserNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    event.stopPropagation();
    setName(event.target.value.trim());
  };

  const showPasswordModal = () => {
    setPasswordModalOpen(true);
  };

  const onCancel = () => {
    setEditing(false);
    onModalCancel();
  };

  return (
    <>
      <Modal
        title={<span className="text-[18px] font-medium text-[#1D4A6B]">设置</span>}
        closable
        onCancel={onCancel}
        className="user-settings-modal"
        width={460}
        footer={null}
        maskClosable={false}
        open={modalOpen}
        centered
      >
        <div className="px-6 pt-5 pb-10 flex flex-col items-center">
          <div className="w-full">
            <div className="flex mb-5">
              <div className="w-[80px] flex justify-end text-sm text-[#383F44] mr-4 mt-2">头像：</div>
              <div className="avatarWrapper">
                <Upload
                  name="icon"
                  maxCount={1}
                  showUploadList={false}
                  accept=".png,.jpg,.jpeg,.svg,.gif,.webp"
                  className="avatar-uploader-custom"
                  customRequest={customUploadRequest}
                  beforeUpload={beforeUpload}
                  onChange={handleImageUpload}
                >
                  <div 
                    className="w-[104px] h-[104px] border border-dashed border-[#D9D9D9] rounded-lg flex flex-col items-center justify-center cursor-pointer hover:border-[#40A5EE] transition-colors overflow-hidden bg-[#FAFAFA]"
                  >
                    {userInfo?.avatar ? (
                      <img src={buildImageUrl(userInfo.avatar)} alt="avatar" className="w-full h-full object-cover" />
                    ) : (
                      <>
                        <PlusOutlined className="text-[#ACB6BE] text-lg" />
                        <div className="mt-2 text-xs text-[#ACB6BE]">上传头像</div>
                      </>
                    )}
                  </div>
                </Upload>
              </div>
            </div>
            
            <div className="flex items-center h-[46px] mb-2">
              <div className="w-[80px] flex justify-end text-sm text-[#383F44] mr-4 flex-none">昵称：</div>
              {!editing && (
                <div className="flex items-center flex-1">
                  <div className="mr-3 text-sm text-[#1D4A6B] line-clamp-1 break-all font-medium">{userInfo?.name}</div>
                  <div onClick={onEditName} className="text-sm text-[#40A5EE] cursor-pointer flex-none">
                    修改
                  </div>
                </div>
              )}
              {editing && (
                <div className="flex items-center flex-1">
                  <input
                    autoFocus
                    onChange={onUserNameChange}
                    value={name}
                    maxLength={10}
                    className="h-8 flex-1 text-sm text-black border border-solid border-[#40A5EE] rounded-lg px-3 outline-none ring-2 ring-[#40A5EE]/10"
                  />
                  <div
                    onClick={onUpdateUserName}
                    className="ml-3 flex-none text-sm text-[#40A5EE] cursor-pointer font-medium"
                  >
                    确认
                  </div>
                  <div
                    className="ml-3 flex-none text-sm text-[#ACB6BE] cursor-pointer"
                    onClick={() => setEditing(false)}
                  >
                    取消
                  </div>
                </div>
              )}
            </div>

            <div className="w-full h-px bg-[#E0E3E6] my-6"></div>
            
            <div className="flex items-center mb-6">
              <div className="w-[80px] flex justify-end text-sm text-[#383F44] mr-4 flex-none">账号：</div>
              <input
                disabled
                className="flex-1 h-8 text-sm text-[#ACB6BE] bg-[#F5F7F9] border border-solid border-[#E0E3E6] rounded-lg px-3 outline-none cursor-not-allowed"
                value={userInfo?.email}
              />
            </div>
            
            <div className="flex items-center">
              <div className="w-[80px] flex justify-end text-sm text-[#383F44] mr-4 flex-none">密码：</div>
              <div onClick={showPasswordModal} className="text-sm text-[#40A5EE] cursor-pointer">
                修改密码
              </div>
            </div>
          </div>
        </div>
      </Modal>

      <ResetPasswordModal modalOpen={passwordModalOpen} onModalCancel={() => setPasswordModalOpen(false)} />
    </>
  );
};

export default UserSettingsModal;
