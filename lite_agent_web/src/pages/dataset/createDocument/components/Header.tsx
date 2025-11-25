import React from 'react';
import { Button, Steps, Typography, Modal } from 'antd';
import { ArrowLeftOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
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
    <div className="space-y-4 mb-4">
      <div className="flex items-center justify-center">
        <div className="flex items-center space-x-4">
          <Button icon={<ArrowLeftOutlined />} onClick={handleBack} className="hover:bg-gray-100">
            返回
          </Button>
          <div className="h-4 w-[1px] bg-gray-200" />
          <Typography.Title level={4} style={{ margin: 0 }}>
            新建文档
          </Typography.Title>
        </div>
        <div className="flex-1 flex justify-center items-center">
          <Steps current={currentStep} items={steps.map((item) => ({ title: item.title }))} 
          className="max-w-2xl -ml-[100px]" />
        </div>
      </div>
    </div>
  );
};

export default Header;
