import React from 'react';
import { Modal, Form, Input, Button, message } from 'antd';
import { putV1UserUpdatePwd } from '@/client';
import { removeAccessToken } from '@/utils/cache';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import ResponseCode from '@/constants/ResponseCode';

interface ResetPasswordModalProps {
    modalOpen: boolean;
    onModalCancel: () => void;
}

const passwordRules = [
    { required: true, message: '请输入新密码' },
    // { min: 8, message: '密码至少需要8个字符' },
    { max: 50, message: '密码最多不能超过50个字符' },
    {
        validator: (_: any, value: string) => {
            if (!/[a-zA-Z]/.test(value)) {
                return Promise.reject('密码必须包含至少一个字母');
            }
            if (!/\d/.test(value)) {
                return Promise.reject('密码必须包含至少一个数字');
            }
            return Promise.resolve();
        },
    },
];

const ResetPasswordModal: React.FC<ResetPasswordModalProps> = ({ modalOpen, onModalCancel }) => {
    const [form] = Form.useForm();
    const navigate = useNavigate();

    const validateConfirmPassword = (_: any, value: string) => {
        if (!value || form.getFieldValue('newPassword') === value) {
            return Promise.resolve();
        }
        return Promise.reject(new Error('两次输入的密码不一致'));
    };

    const onChangePassword = async (values: any) => {
        Modal.confirm({
            title: '修改密码',
            content: '修改密码后，您将需要重新登录！',
            okText: '确定',
            cancelText: '取消',
            centered: true,
            onOk: async () => {
                console.log('values', values);
                const res = await putV1UserUpdatePwd({
                    query: {
                        originPwd: values.originPassword,
                        newPwd: values.newPassword,
                    },
                });
                if (res?.data?.code === ResponseCode.S_OK) {
                    message.success('密码修改成功');
                    removeAccessToken();
                    navigate(ROUTES.LOGIN);
                } else {
                    message.error(res?.data?.message || '密码修改失败');
                }
            },
        });
    };

    return (
        <Modal
            title="修改密码"
            closable
            onCancel={onModalCancel}
            className="!w-[538px]"
            maskClosable={false}
            open={modalOpen}
            onClose={onModalCancel}
            onOk={() => form.submit()}
            centered
        >
            <div className="px-5 pt-6 flex flex-col items-center">
                <div className="w-full updatePassword">
                    <Form form={form} name="update_password" onFinish={onChangePassword}>
                        <Form.Item
                            name="originPassword"
                            rules={[{ required: true, message: '请输入旧密码!' }]}
                        >
                            <div className="flex items-center">
                                <div className="w-[88px] flex justify-end font-xs mr-3">
                                    旧密码：
                                </div>
                                <Input
                                    className="w-[266px] h-8 font-xs text-black/50 border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none"
                                    type="password"
                                    placeholder="请输入旧密码"
                                />
                            </div>
                        </Form.Item>
                        <Form.Item name="newPassword" rules={passwordRules}>
                            <div className="flex items-center">
                                <div className="w-[88px] flex justify-end font-xs mr-3">
                                    新密码：
                                </div>
                                <Input
                                    className="w-[266px] h-8 font-xs text-black/50 border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none"
                                    type="password"
                                    placeholder="请输入新密码"
                                />
                            </div>
                        </Form.Item>

                        <Form.Item
                            name="confirmPassword"
                            rules={[
                                { required: true, message: '请再次输入新密码!' },
                                { validator: validateConfirmPassword },
                            ]}
                        >
                            <div className="flex items-center">
                                <div className="w-[88px] flex justify-end font-xs mr-3">
                                    新密码确认：
                                </div>
                                <Input
                                    className="w-[266px] h-8 font-xs text-black/50 border border-solid border-[#DBDBDB] rounded-md px-[10px] py-[6px] outline-none"
                                    type="password"
                                    placeholder="请再次输入新密码"
                                />
                            </div>
                        </Form.Item>
                    </Form>
                </div>
            </div>
        </Modal>
    );
};

export default ResetPasswordModal;
