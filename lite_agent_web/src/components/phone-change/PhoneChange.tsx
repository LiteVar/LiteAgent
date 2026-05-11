import React, { useState, useCallback } from 'react';
import { Form, Input, Button, Select, message, Modal } from 'antd';
import { CloseOutlined } from '@ant-design/icons';
import { maskPhone } from '@/utils/validate';
import { postV1RegisterSendSms, postV1RegisterVerifyCode } from '@/client';
import ResponseCode from '@/constants/ResponseCode';

const { Option } = Select;

interface PhoneChangeProps {
  visible: boolean;
  onCancel: () => void;
  currentPhone?: string; // 当前手机号
  onChangePhone: (newPhone: string, newCode: string, oldPhone: string, oldCode: string) => Promise<{success: boolean}>;
}

enum ChangeStep {
  VERIFY_CURRENT = 0, // 验证当前手机号
  SET_NEW_PHONE = 1,  // 设置新手机号
}

const PhoneChange: React.FC<PhoneChangeProps> = ({
  visible,
  onCancel,
  currentPhone,
  onChangePhone
}) => {
  const [form] = Form.useForm();
  const [step, setStep] = useState<ChangeStep>(ChangeStep.VERIFY_CURRENT);
  const [countdown, setCountdown] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [oldCode, setOldCode] = useState<string>('');
  const countdownTimerRef = React.useRef<NodeJS.Timeout | null>(null);

  // 启动倒计时
  const startCountdown = useCallback((seconds: number) => {
    setCountdown(seconds);

    if (countdownTimerRef.current) {
      clearInterval(countdownTimerRef.current);
    }

    countdownTimerRef.current = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(countdownTimerRef.current!);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, []);

  // 清理定时器
  React.useEffect(() => {
    return () => {
      if (countdownTimerRef.current) {
        clearInterval(countdownTimerRef.current);
      }
    };
  }, []);

  // 重置状态
  const resetState = useCallback(() => {
    setStep(ChangeStep.VERIFY_CURRENT);
    setCountdown(0);
    setIsLoading(false);
    form.resetFields();
    if (countdownTimerRef.current) {
      clearInterval(countdownTimerRef.current);
    }
  }, [form]);

  // 关闭弹窗
  const handleCancel = useCallback(() => {
    resetState();
    onCancel();
  }, [resetState, onCancel]);

  // 发送当前手机号验证码
  const handleSendCurrentPhoneCode = useCallback(async () => {
    try {
      setIsLoading(true);
      const res = await postV1RegisterSendSms({
        query: {
          mobile: currentPhone!,
        },
      })

      if (res.data?.code === ResponseCode.S_OK) {
        message.success('验证码已发送');
        startCountdown(60);
      } else {
        message.error(res.data?.message || '发送验证码失败');
      }
    } catch (error) {
      message.error('发送验证码失败，请重试');
    } finally {
      setIsLoading(false);
    }
  }, [currentPhone, startCountdown]);

  // 发送新手机号验证码
  const handleSendNewPhoneCode = useCallback(async () => {
    try {
      const phoneValue = form.getFieldValue('newPhone');
      if (!phoneValue) {
        message.error('请先输入新手机号');
        return;
      }

      // 简单的手机号格式验证
      const phoneRegex = /^1[3-9]\d{9}$/;
      if (!phoneRegex.test(phoneValue)) {
        message.error('请输入正确的手机号码');
        return;
      }

      setIsLoading(true);

      const res = await postV1RegisterSendSms({
        query: {
          mobile: phoneValue!,
        },
      })

      if (res.data?.code === ResponseCode.S_OK) {
        message.success('验证码已发送');
        startCountdown(60);
      } else {
        message.error(res.data?.message || '发送验证码失败');
      }
    } catch (error) {
      message.error('发送验证码失败，请重试');
    } finally {
      setIsLoading(false);
    }
  }, [form, startCountdown]);

  // 验证当前手机号
  const handleVerifyCurrentPhone = useCallback(async (values: { verificationCode: string }) => {
    try {

      const res = await postV1RegisterVerifyCode({
        query: {
          mobile: currentPhone!,
          code: values.verificationCode,
        },
      })

      if (res?.data?.data) {
        message.success('验证成功');
        setOldCode(values.verificationCode);
        setStep(ChangeStep.SET_NEW_PHONE);
        form.resetFields(['verificationCode']);
        setCountdown(0);
      } else {
        message.error('验证码已失效');
      }
    } catch (error) {
      message.error('验证失败，请重试');
    } finally {
      setIsLoading(false);
    }
  }, [currentPhone, form]);

  // 确认换绑新手机号
  const handleChangePhone = useCallback(async (values: { countryCode: string; newPhone: string; verificationCode: string }) => {
    try {
      const newPhone = values.newPhone;
      const res = await onChangePhone(newPhone, values.verificationCode, currentPhone!, oldCode);
      if (res.success) {
        resetState();
      }
    } catch (error) {
      message.error('修改失败，请重试');
    } finally {
      setIsLoading(false);
    }
  }, [onChangePhone, handleCancel, oldCode]);

  // 渲染验证当前手机号步骤
  const renderVerifyCurrentStep = () => (
    <>
      <div className="mb-6">
        <p className="text-sm text-gray-600 leading-relaxed">
          <div className="text-gray-900">修改手机号码需要进行身份验证，将向手机号{maskPhone(currentPhone!)}</div>
          <div className="text-gray-900">发送验证码，请输入验证码进行验证</div>
        </p>
      </div>

      <Form
        style={{ height: '130px' }}
        form={form}
        onFinish={handleVerifyCurrentPhone}
        layout="vertical"
      >
        {/* 验证码输入 */}
        <div className="flex gap-3 mb-6">
          <Form.Item
            name="verificationCode"
            className="flex-1 mb-0"
            rules={[
              { required: true, message: '请输入验证码' },
              { len: 4, message: '验证码为4位数字' },
              { pattern: /^\d+$/, message: '验证码只能包含数字' }
            ]}
          >
            <Input
              style={{ width: '350px' }}
              placeholder="输入验证码"
              size="large"
              maxLength={4}
            />
          </Form.Item>
          
          <Button
            type="default"
            size="large"
            onClick={handleSendCurrentPhoneCode}
            disabled={countdown > 0 || isLoading}
            loading={isLoading}
            className="px-6 min-w-20"
          >
            {countdown > 0 ? `${countdown}s` : '获取验证码'}
          </Button>
        </div>

        {/* 按钮组 */}
        <div className="flex gap-3 justify-end absolute bottom-5 right-5">
          <Button size="large" onClick={handleCancel}>
            取消
          </Button>
          <Button 
            type="primary" 
            htmlType="submit" 
            size="large"
            loading={isLoading}
          >
            确定
          </Button>
        </div>
      </Form>
    </>
  );

  // 渲染设置新手机号步骤
  const renderSetNewPhoneStep = () => (
    <>
      <div className="mb-6">
        <p className="text-sm text-gray-600 leading-relaxed">
          手机号码{maskPhone(currentPhone!)}验证成功，请输入新手机号码进行绑定
        </p>
      </div>

      <Form
        style={{ height: '180px' }}
        form={form}
        onFinish={handleChangePhone}
        layout="vertical"
      >
        {/* 新手机号输入 */}
        <Form.Item
          name="newPhone"
        >
          <Input.Group compact>
            <Form.Item
              name="countryCode"
              noStyle
            >
              <Select
                style={{ width: '20%' }}
                size="large"
                defaultValue="+86"
              >
                <Option value="+86">+86</Option>
              </Select>
            </Form.Item>
            <Form.Item
              name="newPhone"
              noStyle
              rules={[
                { required: true, message: '请输入手机号' },
                { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码' }
              ]}
            >
              <Input 
                style={{ width: '80%' }}
                placeholder="11位手机号"
                size="large"
                maxLength={11}
              />
            </Form.Item>
          </Input.Group>
        </Form.Item>

        {/* 验证码输入 */}
        <div className="flex gap-3 mb-6">
          <Form.Item
            name="verificationCode"
            className="flex-1 mb-0"
            rules={[
              { required: true, message: '请输入验证码' },
              { len: 4, message: '验证码为4位数字' },
              { pattern: /^\d+$/, message: '验证码只能包含数字' }
            ]}
          >
            <Input
              style={{ width: '350px' }}
              placeholder="输入验证码"
              size="large"
              maxLength={4}
            />
          </Form.Item>
          
          <Button
            type="default"
            size="large"
            onClick={handleSendNewPhoneCode}
            disabled={countdown > 0 || isLoading}
            loading={isLoading}
            className="px-6 min-w-20"
          >
            {countdown > 0 ? `${countdown}s` : '获取验证码'}
          </Button>
        </div>

        {/* 按钮组 */}
        <div className="flex gap-3 justify-end absolute bottom-5 right-5">
          <Button size="large" onClick={handleCancel}>
            取消
          </Button>
          <Button 
            type="primary" 
            htmlType="submit" 
            size="large"
            loading={isLoading}
          >
            确定
          </Button>
        </div>
      </Form>
    </>
  );

  return (
    <Modal
      className="!w-[538px]"
      title={
        <div className="flex items-center justify-between">
          <span>修改手机号码</span>
          <Button 
            type="text" 
            icon={<CloseOutlined />} 
            onClick={handleCancel}
            className="border-0 shadow-none"
          />
        </div>
      }
      open={visible}
      onCancel={handleCancel}
      footer={null}
      width={500}
      closable={false}
      maskClosable={false}
      centered
      destroyOnClose
    >
      {step === ChangeStep.VERIFY_CURRENT ? renderVerifyCurrentStep() : renderSetNewPhoneStep()}
    </Modal>
  );
};

export default PhoneChange;