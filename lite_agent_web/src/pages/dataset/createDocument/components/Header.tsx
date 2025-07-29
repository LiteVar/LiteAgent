import React from 'react';
import { Button, Steps, Typography } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

interface HeaderProps {
  steps: { title: string; content: React.ReactNode }[];
  currentStep: number;
}

const Header: React.FC<HeaderProps> = ({ currentStep, steps }) => {
  const navigate = useNavigate();

  return (
    <div className="space-y-4 mb-4">
      <div className="flex items-center justify-center">
        <div className="flex items-center space-x-4">
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} className="hover:bg-gray-100">
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
