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

  const onFinish = async (values: any) => {
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
  }, []);

  return (
    <AuthPageLayout>
      <div className="sm:mx-auto sm:w-full sm:max-w-md flex items-center justify-center">
        <div className="my-10">
          <Image
            className="h-16 mx-auto mb-2"
            src={logoAgent}
            preview={false}
            alt="logo"
          />
        </div>
      </div>
      <Form 
        name="normal_login" 
        className="login-form" 
        initialValues={{ remember: true }} 
        onFinish={onFinish}
      >
        <Form.Item 
          name="username" 
          rules={[{ required: true, message: '请输入您的邮箱!' }]}
        >
          <Input 
            prefix={<UserOutlined className="site-form-item-icon" />} 
            placeholder="邮箱" 
          />
        </Form.Item>
        <Form.Item 
          name="password" 
          rules={[{ required: true, message: '请输入您的密码!' }]}
        >
          <Input
            prefix={<LockOutlined className="site-form-item-icon" />}
            type="password"
            placeholder="密码"
          />
        </Form.Item>
        <Form.Item>
          <Form.Item name="remember" valuePropName="checked" noStyle>
            <Checkbox>自动登录</Checkbox>
          </Form.Item>

          <a 
            className="login-form-forgot float-right text-blue-500" 
            href="" 
            onClick={goResetPassword}
          >
            忘记密码
          </a>
        </Form.Item>

        <Form.Item>
          <Button type="primary" htmlType="submit" className="w-full h-12">
            登录
          </Button>
        </Form.Item>
      </Form>
    </AuthPageLayout>
  );
};

export default LoginPage;
