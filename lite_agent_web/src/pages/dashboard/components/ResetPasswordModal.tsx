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
        const res = await putV1UserUpdatePwd({
            query: {
                originPwd: values.originPassword,
                newPwd: values.newPassword,
            },
        });
        if (res?.data?.code === ResponseCode.S_OK) {
            message.success('密码修改成功，请重新登录');
            removeAccessToken();
            navigate(ROUTES.LOGIN);
        } else {
            message.error(res?.data?.message || '密码修改失败');
        }
    };

    return (
        <Modal
            title={<span className="text-[18px] font-medium text-[#1D4A6B]">修改密码</span>}
            closable
            onCancel={onModalCancel}
            className="user-settings-modal"
            width={460}
            maskClosable={false}
            open={modalOpen}
            centered
            footer={[
                <Button 
                    key="cancel" 
                    onClick={onModalCancel}
                    className="h-9 px-6 rounded-lg border-[#E0E3E6] text-[#383F44] hover:text-[#40A5EE] hover:border-[#40A5EE] transition-colors"
                >
                    取消
                </Button>,
                <Button 
                    key="submit" 
                    type="primary" 
                    onClick={() => form.submit()}
                    className="h-9 px-6 rounded-lg bg-[#40A5EE] border-none hover:bg-[#40A5EE]/90 transition-colors"
                >
                    确认
                </Button>
            ]}
        >
            <div className="px-6 pt-6 pb-2">
                <Form 
                    form={form} 
                    name="update_password" 
                    onFinish={onChangePassword} 
                    layout="horizontal" 
                    colon={false}
                    requiredMark={false}
                >
                    <Form.Item
                        name="originPassword"
                        label={<span className="w-[70px] inline-block text-right text-sm text-[#383F44] mr-4">旧密码</span>}
                        rules={[{ required: true, message: '请输入旧密码!' }]}
                        className="mb-5"
                    >
                        <Input.Password
                            className="h-8 text-sm border-[#E0E3E6] rounded-lg hover:border-[#40A5EE] focus:border-[#40A5EE] transition-all py-1 px-2"
                            placeholder="请输入旧密码"
                        />
                    </Form.Item>
                    <Form.Item
                        name="newPassword"
                        label={<span className="w-[70px] inline-block text-right text-sm text-[#383F44] mr-4">新密码</span>}
                        rules={passwordRules}
                        className="mb-5"
                    >
                        <Input.Password
                            className="h-8 text-sm border-[#E0E3E6] rounded-lg hover:border-[#40A5EE] focus:border-[#40A5EE] transition-all py-1 px-2"
                            placeholder="请输入新密码"
                        />
                    </Form.Item>

                    <Form.Item
                        name="confirmPassword"
                        label={<span className="w-[70px] inline-block text-right text-sm text-[#383F44] mr-4">确认密码</span>}
                        rules={[
                            { required: true, message: '请再次输入新密码!' },
                            { validator: validateConfirmPassword },
                        ]}
                        className="mb-2"
                    >
                        <Input.Password
                            className="h-8 text-sm border-[#E0E3E6] rounded-lg hover:border-[#40A5EE] focus:border-[#40A5EE] transition-all py-1 px-2"
                            placeholder="请再次输入新密码"
                        />
                    </Form.Item>
                </Form>
            </div>
        </Modal>
    );


};

export default ResetPasswordModal;
