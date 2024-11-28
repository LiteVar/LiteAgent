import React from 'react';
import {Form, Input, Button, Checkbox, Image, message} from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import Logo from '@/assets/login/logo.png';
import {postV1AuthLogin} from "@/client";
import {useLoginRedirect} from "@/hooks/useLoginRedirect";
import {setAccessToken} from "@/utils/cache";
import ResponseCode from "@/config/ResponseCode";

const LoginPage = () => {
  const redirect = useLoginRedirect();

  const onFinish = async (values: any) => {
    const res = await postV1AuthLogin({
      query: {
        email: values.username,
        password: values.password,
      }
    })
    if (res?.data?.code === ResponseCode.S_OK) {
      setAccessToken(res.data.data as string);
      window.location.href = redirect
    } else {
      message.error(res?.data?.message)
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md flex items-center justify-center">
        <Image
          preview={false}
          className="mr-8 h-12 w-auto"
          src={Logo}
          alt="LiteAgent"
        />
        <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
          LiteAgent
        </h2>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
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
              <Input prefix={<UserOutlined className="site-form-item-icon text-blue-500" />} placeholder="邮箱" />
            </Form.Item>
            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入您的密码!' }]}
            >
              <Input
                prefix={<LockOutlined className="site-form-item-icon text-blue-500" />}
                type="password"
                placeholder="密码"
              />
            </Form.Item>
            <Form.Item>
              <Form.Item name="remember" valuePropName="checked" noStyle>
                <Checkbox>自动登录</Checkbox>
              </Form.Item>

              <a className="login-form-forgot float-right text-blue-500" href="">
                忘记密码
              </a>
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" className="w-full">
                登录
              </Button>
            </Form.Item>
          </Form>
        </div>
      </div>

      <div className="mt-8 text-center text-sm text-gray-500">
        Copyright ©
      </div>
    </div>
  );
};

export default LoginPage;
