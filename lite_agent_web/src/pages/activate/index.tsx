import React, {useEffect, useState} from 'react';
import {Form, Input, Button, Image, message} from 'antd';
import Logo from '@/assets/login/logo.png';
import {useQuery} from "@tanstack/react-query";
import {postV1WorkspaceActivateMember} from "@/client"
import {getV1WorkspaceActivateInfoOptions} from "@/client/@tanstack/query.gen";
import {setAccessToken} from "@/utils/cache";
import {useNavigate, useSearchParams} from "react-router-dom";
import {ROUTES} from "@/config/constants";
import ResponseCode from "@/config/ResponseCode";

const passwordRules = [
  { required: true, message: '请输入新密码' },
  { min: 8, message: '密码至少需要8个字符' },
  { max: 20, message: '密码最多不能超过20个字符' },
  {
    validator: (_, value) => {
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

const ActivatePage = () => {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const activateToken = searchParams.get('token')
  const [step, setStep] = useState(1)

  const {data} = useQuery({
    ...getV1WorkspaceActivateInfoOptions({
      query: {
        token: activateToken as string
      }
    })
  })

  const activateInfo:any = data?.data
  const isExpired = typeof data?.code === 'number' && data?.code !== 200

  useEffect(() => {
    if (isExpired) {
      setStep(-1)
    }
  }, [isExpired])

  const validateConfirmPassword = (_: any, value: any) => {
    if (!value || form.getFieldValue('password') === value) {
      return Promise.resolve();
    }
    return Promise.reject(new Error('两次输入的密码不一致'));
  };

  const onFinish = async (values: any) => {
    const res = await postV1WorkspaceActivateMember({
      query: {
        token: activateToken!,
        username: values.username,
        password: values.password
      }
    })
    if (res?.data?.code === ResponseCode.S_OK) {
      setAccessToken(res?.data?.data as string)
      window.location.href = ROUTES.DASHBOARD
    } else {
      message.error(res?.data?.message || "网络错误")
    }
  };

  return (
    <div className="min-h-screen bg-[#F0F3F7] flex flex-col justify-center sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md flex items-center justify-center mb-16">
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

      {
        step === -1 &&
        <div>
          <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
            <div className="py-8 px-4 sm:rounded-lg sm:px-10 flex flex-col items-center">
              <div className="w-fit py-4 px-12 border border-[#F44336] border-solid rounded-md text-base text-center">
                邀请已过期，请联系管理员
              </div>
            </div>
          </div>
        </div>
      }

      {step === 1 &&
        <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
          <div className="py-8 px-4 sm:rounded-lg sm:px-10 flex flex-col items-center">
            <div className="w-fit py-4 px-12 border border-[#CCCCCC] border-solid rounded-md text-base text-center">
              {activateInfo?.workspaceName}
            </div>
            <div className="w-fit text-base mt-5 mb-16">邀请您加入工作空间</div>
            <Button type="primary" size={"large"} className="w-56" onClick={() => setStep(2)}>
              加入
            </Button>
          </div>
        </div>
      }

      {step === 2 &&
        <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
          <div className="text-base text-center mb-12">请设置您的账号信息</div>
          <div className="px-4 sm:rounded-lg sm:px-10">
            <Form
              form={form}
              name="normal_login"
              className="login-form"
              initialValues={{remember: true, email: activateInfo?.email}}
              onFinish={onFinish}
            >
              <div className="flex items-center mb-4">
                <label className="w-24 text-right text-base mr-6">账号:</label>
                <Form.Item
                  style={{marginBottom: 0, width: '266px'}}
                  name="email"
                  rules={[{required: true, message: '请输入您的账号'}]}
                >
                  <Input disabled placeholder="请输入您的账号"/>
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
                  rules={passwordRules}
                >
                  <Input type="password" placeholder="请输入新密码"/>
                </Form.Item>
              </div>

              <div className="flex items-center mb-4">
                <label className="w-24 text-right text-base mr-6">新密码确认:</label>
                <Form.Item
                  style={{marginBottom: 0, width: '266px'}}
                  name="confirmPassword"
                  rules={[
                    {required: true, message: '请再次输入新密码'},
                    { validator: validateConfirmPassword },
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
      }
    </div>
  );
};

export default ActivatePage;
