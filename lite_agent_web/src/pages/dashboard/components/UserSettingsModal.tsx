import React, { MouseEvent, useState } from 'react';
import { Modal, Upload, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { UploadChangeParam } from 'antd/es/upload/interface';
import { Account, putV1UserUpdate } from '@/client';
import { buildImageUrl } from '@/utils/buildImageUrl';
import ResponseCode from '@/constants/ResponseCode';
import ResetPasswordModal from './ResetPasswordModal';
import { beforeUpload, onUploadAction } from '@/utils/uploadFile';

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
        await putV1UserUpdate({
            query: {
                avatar: info.file.xhr.responseURL.split('=')[1],
                name: userInfo?.name!,
            },
        });
        await refreshAgent();
        await message.success(`${info.file.name} 上传成功`);
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
        title={'设置'}
        closable
        onCancel={onCancel}
        className="!w-[538px]"
        footer={null}
        maskClosable={false}
        open={modalOpen}
        centered
      >
        <div className="px-8 pt-7 pb-12 flex flex-col items-center">
          <div className="w-full">
            <div className="flex items-end mb-5">
              <div className="w-[88px] flex justify-end font-xs mr-3">头像：</div>
              <div className="avatarWrapper">
                <Upload
                  name="icon"
                  maxCount={1}
                  defaultFileList={
                    userInfo?.avatar
                      ? [
                          {
                            uid: '',
                            name: '',
                            thumbUrl: buildImageUrl(userInfo?.avatar),
                          },
                        ]
                      : undefined
                  }
                  accept=".png,.jpg,.jpeg,.svg,.gif,.webp"
                  listType="picture-card"
                  className="avatar-uploader"
                  showUploadList={{
                    showDownloadIcon: false,
                    showRemoveIcon: false,
                    showPreviewIcon: false,
                  }}
                  action={onUploadAction}
                  beforeUpload={beforeUpload}
                  onChange={handleImageUpload}
                >
                  <div>
                    <PlusOutlined />
                    <div style={{ marginTop: 8 }}>{userInfo?.avatar ? '修改头像' : '上传头像'}</div>
                  </div>
                </Upload>
              </div>
            </div>
            <div className="flex items-center h-[46px]">
              <div className="w-[88px] flex justify-end font-xs mr-3 flex-none">昵称：</div>
              {!editing && (
                <div className="flex items-center">
                  <div className="mr-2 font-xs line-clamp-1 break-all">{userInfo?.name}</div>
                  <div onClick={onEditName} className="font-xs text-[#1296DB] cursor-pointer flex-none">
                    修改
                  </div>
                </div>
              )}
              {editing && (
                <div className="flex items-center font-xs">
                  <input
                    autoFocus
                    onChange={onUserNameChange}
                    value={name}
                    maxLength={10}
                    className="h-8 font-xs text-black border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none"
                  />
                  <div
                    onClick={onUpdateUserName}
                    className="mx-2 flex-none font-xs cursor-pointer border border-[#D9D9D9] border-solid rounded-md py-2.5 px-4"
                  >
                    确认
                  </div>
                  <div
                    className="flex-none font-xs cursor-pointer border border-[#D9D9D9] border-solid rounded-md py-2.5 px-4"
                    onClick={() => setEditing(false)}
                  >
                    取消
                  </div>
                </div>
              )}
            </div>
            <div className="w-full h-px bg-[#F2F2F2] my-6"></div>
            <div className="flex items-center mb-6">
              <div className="w-[88px] flex justify-end font-xs mr-3">账号：</div>
              <input
                disabled
                className="w-[266px] h-8 font-xs text-black/50 border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none"
                value={userInfo?.email}
              />
            </div>
            <div className="flex items-center">
              <div className="w-[88px] flex justify-end font-xs mr-3">密码：</div>
              <div onClick={showPasswordModal} className="font-xs text-[#2A82E4] cursor-pointer opacity-70">
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
