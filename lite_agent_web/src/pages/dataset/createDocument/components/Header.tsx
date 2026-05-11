import React from 'react';
import { Steps, Typography, Modal } from 'antd';
import { LeftOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { DatasetDocument } from '@/client';

interface HeaderProps {
  steps: { title: string; content: React.ReactNode }[];
  currentStep: number;
  documentData?: DatasetDocument | null;
  onBackWithCleanup?: () => Promise<void>;
}

const Header: React.FC<HeaderProps> = ({ 
  currentStep, 
  steps,
  documentData,
  onBackWithCleanup,
}) => {
  const navigate = useNavigate();

  const handleBack = async () => {
    // 如果存在已上传的文件，提示用户确认
    if (documentData?.fileId) {
      Modal.confirm({
        title: '确认退出',
        icon: <ExclamationCircleOutlined />,
        content: '当前有未保存的数据，退出后将会丢失，确认要退出吗？',
        okText: '确认',
        cancelText: '取消',
        okButtonProps: { className: 'rounded-lg' },
        cancelButtonProps: { className: 'rounded-lg' },
        onOk: async () => {
          // 用户确认后，删除已上传的文件
          if (onBackWithCleanup) {
            await onBackWithCleanup();
          }
          navigate(-1);
        },
      });
    } else {
      // 没有未保存的数据，直接返回
      navigate(-1);
    }
  };

  return (
    <div className="mb-6">
      <div>
        <div className="flex items-center gap-4">
          <div 
            onClick={handleBack}
            className="w-9 h-9 flex items-center justify-center rounded-xl bg-white/80 border border-white/60 shadow-sm hover:bg-white cursor-pointer transition-all"
          >
            <LeftOutlined className="text-gray-600" />
          </div>
          <Typography.Title level={4} className="!m-0 !text-[#1D4A6B] !font-semibold !text-lg">
            新建文档
          </Typography.Title>
        </div>
        
        <div className="flex-1 flex justify-center">
          <Steps 
            current={currentStep} 
            items={steps.map((item) => ({ title: item.title }))} 
            className="max-w-2xl customSteps [&_.ant-steps-item-finish_.ant-steps-item-icon]:border [&_.ant-steps-item-finish_.ant-steps-item-icon]:border-blue-600 [&_.ant-steps-item-finish_.ant-steps-item-icon]:border-solid"
          />
        </div>
      </div>
    </div>
  );
};

export default Header;
