import React, { useState, useCallback, useRef } from 'react';
import { Form, Input, Image, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { getV1RegisterPicCaptcha } from '@/client';

interface ImageCaptchaProps {
  value?: {
    captchaId: string;
    captchaValue: string;
  };
  onChange?: (value: { captchaId: string; captchaValue: string }) => void;
}

const ImageCaptcha: React.FC<ImageCaptchaProps> = ({ 
  value = { captchaId: '', captchaValue: '' }, 
  onChange, 
}) => {
  const [form] = Form.useForm();
  const [captchaImage, setCaptchaImage] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const initCaptchaRef = useRef(false);

  const refreshCaptcha = useCallback(async () => {
    if (loading) return;
    setLoading(true);
    try {
      const res = await getV1RegisterPicCaptcha();
      if (res.data?.data) {
        setCaptchaImage(res.data.data.captchaImageBase64!);
      
        // 更新captchaId，清空用户输入
        onChange?.({
          captchaId: res.data.data.id!,
          captchaValue: ''
        });
        form.setFieldsValue({
          captchaValue: ''
        });
      }
    } catch (error) {
      message.error('获取验证码失败，请重试');
    } finally {
      setLoading(false);
    }
  }, [onChange, form, loading]);

  // 初始化时获取验证码
  React.useEffect(() => {
    if (initCaptchaRef.current) return;
    initCaptchaRef.current = true;
    refreshCaptcha();
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const captchaValue = e.target.value;
    onChange?.({
      captchaId: value.captchaId,
      captchaValue
    });
  };

  return (
    <div className='flex items-center gap-2'>
      <Form className="flex-1 mb-0 h-10" form={form}>
        <Form.Item
          name="captchaValue"
          rules={[
            { required: true, message: '请输入验证码' },
            { len: 4, message: '验证码为4位' }
          ]}
        >
          <Input
            key={value.captchaId}
            placeholder="请输入验证码"
            value={123}
            onChange={handleInputChange}
            maxLength={4}
          />
        </Form.Item>
      </Form>
      
      <div className="flex items-center gap-1">
        {captchaImage && (
          <Image
            src={captchaImage}
            alt="验证码"
            width={100}
            height={40}
            preview={false}
            className="border border-gray-300 rounded cursor-pointer"
            onClick={refreshCaptcha}
          />
        )}
      </div>
    </div>
  );
};

export default ImageCaptcha;
