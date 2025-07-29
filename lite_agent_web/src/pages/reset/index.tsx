import React, { useState, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, message } from 'antd';
import { ArrowLeftOutlined, UserOutlined } from '@ant-design/icons';

import {
  postV1UserResetPwdCaptcha,
  postV1UserResetPwdCaptchaVerify,
  postV1UserResetPwdConfirm,
} from '@/client';
import ResponseCode from '@/constants/ResponseCode';
import AuthPageLayout from '@/components/auth-page-layout';

enum ResetStep {
  INPUT_ACCOUNT = 0,
  VERIFY_CODE = 1,
  RESET_PASSWORD = 2,
  SUCCESS = 3,
}

const ResetPassword: React.FC = () => {
  const [currentStep, setCurrentStep] = useState<ResetStep>(ResetStep.INPUT_ACCOUNT);
  const [form] = Form.useForm();
  const [email, setEmail] = useState<string>('');
  const [countdown, setCountdown] = useState<number>(0);
  const navigate = useNavigate();

  // 新增：记录每个邮箱的倒计时
  const [countdownMap, setCountdownMap] = useState<Record<string, number>>({});
  const countdownTimerRef = React.useRef<NodeJS.Timeout | null>(null);

  // 新增：倒计时启动函数
  const startCountdown = useCallback((_email: string, seconds: number) => {
    setCountdown(seconds);
    setCountdownMap((prev) => ({ ...prev, [_email]: seconds }));

    if (countdownTimerRef.current) clearInterval(countdownTimerRef.current);

    countdownTimerRef.current = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(countdownTimerRef.current!);
          setCountdownMap((map) => ({ ...map, [_email]: 0 }));
          return 0;
        }
        setCountdownMap((map) => ({ ...map, [_email]: prev - 1 }));
        return prev - 1;
      });
    }, 1000);
  }, []);

  const sendVerificationCode = useCallback(
    async (_email: string) => {
      // 如果当前邮箱倒计时未结束，不允许发送
      if (countdownMap[_email] > 0) {
        setCountdown(countdownMap[_email]);
        message.warning(`请${countdownMap[_email]}秒后再试`);
        return;
      }

      try {
        const res = await postV1UserResetPwdCaptcha({
          query: { email: _email },
        });

        if (res.data?.code === ResponseCode.S_OK) {
          setCurrentStep(ResetStep.VERIFY_CODE);
          message.success('验证码已发送');

          // 开始倒计时
          startCountdown(_email, 30);
        } else {
          message.error(res.data?.message || '邮箱验证失败');
        }
      } catch (error) {
        message.error('操作失败，请重试');
      }
    },
    [countdownMap, startCountdown]
  );

  const handleSubmitAccount = useCallback(
    async (values: { email: string }) => {
      const _email = values.email.trim();
      setEmail(_email);

      // 如果有倒计时，直接进入验证码页面并恢复倒计时
      if (countdownMap[_email] > 0) {
        setCurrentStep(ResetStep.VERIFY_CODE);
        setCountdown(countdownMap[_email]);
        return;
      }
      sendVerificationCode(_email);
    },
    [countdownMap, sendVerificationCode]
  );

  const handleVerifyCode = useCallback(
    async (values: { verificationCode: string }) => {
      const code = values.verificationCode.trim();

      try {
        const res = await postV1UserResetPwdCaptchaVerify({
          query: { email, captcha: code },
        });

        if (res.data?.code === ResponseCode.S_OK) {
          message.success('验证成功');
          setCurrentStep(ResetStep.RESET_PASSWORD);
        } else {
          message.error(res.data?.message || '验证码验证失败');
        }
      } catch (error) {
        message.error('验证码验证失败');
      }
    },
    [email]
  );

  const handleResetPassword = useCallback(
    async (values: { password: string; confirmPassword: string }) => {
      try {
        const res = await postV1UserResetPwdConfirm({
          query: { email, password: values.password },
        });

        res.data?.code === ResponseCode.S_OK
          ? setCurrentStep(ResetStep.SUCCESS)
          : message.error(res.data?.message || '重置密码失败');
      } catch (error) {
        message.error('重置密码失败');
      }
    },
    [email]
  );

  const goToLogin = useCallback(() => {
    navigate('/login');
  }, []);

  const goBack = useCallback(() => {
    if (currentStep === ResetStep.VERIFY_CODE) {
      setCurrentStep(ResetStep.INPUT_ACCOUNT);
    } else if (currentStep === ResetStep.RESET_PASSWORD) {
      setCurrentStep(ResetStep.VERIFY_CODE);
      // 恢复倒计时
      setCountdown(countdownMap[email] || 0);
    }
  }, [currentStep, email, countdownMap]);

  const formatEmail = useCallback((email: string): string => {
    const [localPart, domain] = email.split('@');
    const visiblePart = localPart.length > 4 ? localPart.slice(0, 4) : '';
    return `${visiblePart}****@${domain}`;
  }, []);

  useEffect(() => {
    return () => {
      if (countdownTimerRef.current) clearInterval(countdownTimerRef.current);
    };
  }, []);

  // 渲染不同步骤的表单
  const renderForm = () => {
    switch (currentStep) {
      case ResetStep.INPUT_ACCOUNT:
        return (
          <>
            <h2 className="text-center text-2xl font-bold mb-6">找回密码</h2>
            <Form form={form} onFinish={handleSubmitAccount} layout="vertical">
              <Form.Item
                name="email"
                rules={[
                  { required: true, message: '请输入您的登录邮箱账号' },
                  { type: 'email', message: '邮箱格式不正确' },
                ]}
              >
                <Input prefix={<UserOutlined />} placeholder="请输入您的登录邮箱账号" size="large" />
              </Form.Item>
              <Form.Item>
                <Button type="primary" htmlType="submit" block size="large" className="mt-4">
                  下一步
                </Button>
              </Form.Item>
            </Form>
            <div className="text-center mt-4">
              <Button type="link" onClick={goToLogin}>
                <ArrowLeftOutlined /> 返回登录
              </Button>
            </div>
          </>
        );

      case ResetStep.VERIFY_CODE:
        return (
          <>
            <h2 className="text-center text-2xl font-bold mb-6">找回密码</h2>
            <p className="text-center mb-6 text-[#666] text-[14px]">
              验证码已发送到 {formatEmail(email)} 邮箱，请输入验证码
            </p>
            <Form form={form} onFinish={handleVerifyCode} layout="vertical">
              <div className="flex">
                <Form.Item
                  name="verificationCode"
                  className="flex-1 mb-0"
                  style={{ height: '50px' }}
                  rules={[{ required: true, message: '请输入验证码' }]}
                >
                  <Input placeholder="请输入验证码" size="large" />
                </Form.Item>
                <Button
                  type="primary"
                  disabled={countdown > 0}
                  onClick={() => {
                    form.resetFields(['verificationCode']);
                    sendVerificationCode(email);
                  }}
                  className="ml-2 h-10"
                >
                  {countdown > 0 ? `重新发送(${countdown})` : '重新发送'}
                </Button>
              </div>
              <Form.Item className="mt-4">
                <Button type="primary" htmlType="submit" block size="large">
                  下一步
                </Button>
              </Form.Item>
            </Form>
            <div className="text-center mt-4">
              <Button type="link" onClick={goBack}>
                <ArrowLeftOutlined /> 返回上一步
              </Button>
            </div>
          </>
        );

      case ResetStep.RESET_PASSWORD:
        return (
          <>
            <h2 className="text-center text-2xl font-bold mb-6">重置密码</h2>
            <p className="text-center mb-6 text-[#666] text-[14px]">账号验证成功，请输入新密码</p>
            <Form form={form} onFinish={handleResetPassword} layout="vertical">
              <Form.Item
                name="password"
                rules={[
                  { required: true, message: '请输入新密码' },
                  { min: 6, message: '密码不能低于6位' },
                  {
                    pattern: /^(?!.*(\d)\1{5})(?!.*([a-zA-Z])\2{5}).*$/,
                    message: '不能使用连续性、重复性的组合',
                  },
                ]}
              >
                <Input.Password placeholder="请输入新密码" size="large" />
              </Form.Item>
              <Form.Item
                name="confirmPassword"
                dependencies={['password']}
                rules={[
                  { required: true, message: '请再次确认新密码' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('password') === value) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error('两次输入的密码不一致'));
                    },
                  }),
                ]}
              >
                <Input.Password placeholder="请再次确认新密码" size="large" />
              </Form.Item>
              <Form.Item>
                <Button type="primary" htmlType="submit" block size="large" className="mt-4">
                  下一步
                </Button>
              </Form.Item>
            </Form>
            <div className="text-center mt-4">
              <Button type="link" onClick={goBack}>
                <ArrowLeftOutlined /> 返回上一步
              </Button>
            </div>
          </>
        );

      case ResetStep.SUCCESS:
        return (
          <>
            <h2 className="text-center text-2xl font-bold mb-6">重置密码成功</h2>
            <p className="text-center mb-6 text-[#666] text-[14px]">请点击登录进入首页</p>
            <Button type="primary" onClick={goToLogin} block size="large">
              登录
            </Button>
          </>
        );

      default:
        return null;
    }
  };

  return <AuthPageLayout>{renderForm()}</AuthPageLayout>;
};

export default ResetPassword;
