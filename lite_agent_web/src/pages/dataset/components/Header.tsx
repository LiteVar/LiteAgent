import React from 'react';
import { Typography } from 'antd';
import { LeftOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { Dataset } from '@/client';
import DatasetIcon from '@/assets/dataset/dataset-logo.svg';
import { buildImageUrl } from '@/utils/buildImageUrl';

const { Title } = Typography;

interface HeaderProps {
  datasetInfo: Dataset | undefined;
}

const Header: React.FC<HeaderProps> = (props) => {
  const { datasetInfo } = props;
  const navigate = useNavigate();

  const backToDatasets = () => {
    navigate(`/workspaces/${datasetInfo?.workspaceId}/datasets`);
  };

  return (
    <header className="h-[88px] px-6 flex items-center bg-white/60 backdrop-blur-md border-b border-white/80 sticky top-0 z-10">
      <div className="flex items-center gap-3">
        <div 
          onClick={backToDatasets}
          className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-black/5 cursor-pointer transition-colors"
        >
          <LeftOutlined className="text-gray-600 text-base" />
        </div>
        
        <img
          src={datasetInfo?.icon ? buildImageUrl(datasetInfo.icon!) : DatasetIcon}
          alt={datasetInfo?.name || '未知名称'}
          className="w-10 h-10 object-cover rounded-lg"
        />
        
        <Title level={4} className="!m-0 !text-[#1D4A6B] !font-semibold !text-lg">
          {datasetInfo?.name || '未知名称'}
        </Title>
      </div>
    </header>
  );
};

export default Header;
