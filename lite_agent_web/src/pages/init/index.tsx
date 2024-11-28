import React from 'react';
import {Form, Input, Button, Image} from 'antd';
import Logo from '@/assets/login/logo.png';
import {useQuery} from "@tanstack/react-query";
import {postV1AuthLoginOfInit} from "@/client"
import {getV1AuthInitStatusOptions} from "@/client/@tanstack/query.gen";
import {setAccessToken} from "@/utils/cache";
import {useNavigate} from "react-router-dom";
import {ROUTES} from "@/config/constants";
import ResponseCode from "@/config/ResponseCode";

const InitPage = () => {
  const [form] = Form.useForm()
  const navigate = useNavigate()

  const {data} = useQuery({
    ...getV1AuthInitStatusOptions({})
  })
  const hadInit = data?.data === 1

  const validateConfirmPassword = (_: any, value: any) => {
    if (!value || form.getFieldValue('password') === value) {
      return Promise.resolve();
    }
    return Promise.reject(new Error('两次输入的密码不一致'));
  };

  const onFinish = async (values: any) => {
    const res = await postV1AuthLoginOfInit({
      query: {
        email: values.email,
        username: values.username,
        password: values.password
      }
    })
    if (res?.data?.code === ResponseCode.S_OK) {
      setAccessToken(res.data.data as string)
      window.location.href = ROUTES.DASHBOARD
    }
  };

  return (
    <div className="min-h-screen bg-[#F0F3F7] flex flex-col justify-center sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md flex items-center justify-center mb-16">
        <Image
          className="mr-8 h-12 w-auto"
          src={Logo}
          alt="LiteAgent"
        />
        <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
          LiteAgent
        </h2>
      </div>

      {hadInit && <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="text-base text-center mb-12">系统已经初始化完成，请点击下方按钮登录</div>
        <Button
          type="primary"
          size="large"
          className="w-full"
          onClick={() => navigate(ROUTES.LOGIN)}
        >
          登录
        </Button>
      </div>
      }

      {!hadInit && (
        <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
          <div className="text-base text-center mb-12">请设置系统的第一个用户</div>
          <div className="px-4 sm:rounded-lg sm:px-10">
            <Form
              form={form}
              name="normal_login"
              className="login-form"
              initialValues={{remember: true}}
              onFinish={onFinish}
            >
              <div className="flex items-center mb-4">
                <label className="w-24 text-right text-base mr-6">账号:</label>
                <Form.Item
                  style={{marginBottom: 0, width: '266px'}}
                  name="email"
                  rules={[{required: true, message: '请输入您的账号'}, {
                    type: 'email',
                    message: '请输入正确的邮箱地址'
                  }]}
                >
                  <Input placeholder="请输入您的账号"/>
                </Form.Item>
              </div>

              <div className="flex items-center mb-4">
                <label className="w-24 text-right text-base mr-6">昵称:</label>
                <Form.Item
                  style={{marginBottom: 0, width: '266px'}}
                  name="username"
                  rules={[{required: true, message: '请输入您的昵称'}]}
                >
                  <Input placeholder="请输入您的昵称"/>
                </Form.Item>
              </div>

              <div className="flex items-center mb-4">
                <label className="w-24 text-right text-base mr-6">新密码:</label>
                <Form.Item
                  style={{marginBottom: 0, width: '266px'}}
                  name="password"
                  rules={[{required: true, message: '请输入您的新密码'}]}
                >
                  <Input type="password" placeholder="请再次输入新密码"/>
                </Form.Item>
              </div>

              <div className="flex items-center mb-4">
                <label className="w-24 text-right text-base mr-6">新密码确认:</label>
                <Form.Item
                  style={{marginBottom: 0, width: '266px'}}
                  name="confirmPassword"
                  rules={[
                    {required: true, message: '请再次输入新密码'},
                    {validator: validateConfirmPassword},
                  ]}
                >
                  <Input type="password" placeholder="请再次输入新密码"/>
                </Form.Item>
              </div>

              <Form.Item>
                <Button type="primary" htmlType="submit" size={"large"} className="w-full mt-12">
                  登录
                </Button>
              </Form.Item>
            </Form>
          </div>
        </div>
      )}
    </div>
  );
};

export default InitPage;
