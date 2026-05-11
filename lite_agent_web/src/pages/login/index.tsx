import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Checkbox, message, Image } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';

import { postV1AuthLogin } from '@/client';
import { useLoginRedirect } from '@/hooks/useLoginRedirect';
import { setAccessToken } from '@/utils/cache';
import ResponseCode from '@/constants/ResponseCode';
import AuthPageLayout from '@/components/auth-page-layout';
import logoAgent from '@/assets/login/logo_agent.png';

const LoginPage = () => {
  const redirect = useLoginRedirect();
  const navigate = useNavigate();

  const onFinish = async (values: { username: string; password: string; remember?: boolean }) => {
    const res = await postV1AuthLogin({
      query: {
        email: values.username,
        password: values.password,
      },
    });
    if (res?.data?.code === ResponseCode.S_OK) {
      setAccessToken(res.data.data as string);
      window.location.href = redirect;
    } else {
      message.error(res?.data?.message);
    }
  };

  const goResetPassword = useCallback(() => {
    navigate('/reset');
  }, [navigate]);

  return (
    <AuthPageLayout>
      <div className="flex flex-col items-center gap-8 mb-8">
        <div className="h-[60px] flex items-center gap-2">
          <Image
            className="w-auto h-12"
            src={logoAgent}
            preview={false}
            alt="logo"
          />
        </div>
      </div>
      <Form 
        name="normal_login" 
        className="login-form flex flex-col gap-4" 
        initialValues={{ remember: true }} 
        onFinish={onFinish}
      >
        <Form.Item 
          name="username" 
          className="mb-0"
          rules={[{ required: true, message: '请输入您的邮箱!' }]}
        >
          <Input 
            prefix={
              <div className="flex items-center gap-2 pr-2">
                <UserOutlined className="text-[#58636C] text-xl" />
                <div className="w-px h-3 bg-[#E0E3E6] rounded-full" />
              </div>
            }
            placeholder="请输入您的邮箱" 
            className="h-12 box-border rounded-xl border-white bg-white/60 backdrop-blur-[2px] hover:border-[#40a5ee] focus:border-[#40a5ee]"
          />
        </Form.Item>
        <Form.Item 
          name="password" 
          className="mb-0"
          rules={[{ required: true, message: '请输入您的密码!' }]}
        >
          <Input.Password
            prefix={
              <div className="flex items-center gap-2 pr-2">
                <LockOutlined className="text-[#58636C] text-xl" />
                <div className="w-px h-3 bg-[#E0E3E6] rounded-full" />
              </div>
            }
            placeholder="请输入您的密码"
            className="h-12 box-border rounded-xl border-white bg-white/60 backdrop-blur-[2px] hover:border-[#40a5ee] focus:border-[#40a5ee]"
          />
        </Form.Item>
        
        <div className="flex items-center justify-between px-1">
          <Form.Item name="remember" valuePropName="checked" noStyle>
            <Checkbox className="text-[#58636C]">自动登录</Checkbox>
          </Form.Item>

          <Button 
            type="link" 
            className="text-[#40a5ee] hover:text-[#40a5ee]/80 p-0 h-auto" 
            onClick={goResetPassword}
          >
            忘记密码
          </Button>
        </div>

        <Form.Item className="mb-0 mt-4">
          <Button 
            type="primary" 
            htmlType="submit" 
            className="w-full h-12 rounded-xl bg-[#40a5ee] border-[#40a5ee] text-base font-medium"
          >
            登录
          </Button>
        </Form.Item>
      </Form>
    </AuthPageLayout>
  );
};

export default LoginPage;
