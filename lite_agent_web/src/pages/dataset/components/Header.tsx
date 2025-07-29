import React from 'react';
import { Button, Typography, Image } from 'antd';
import { LeftOutlined } from '@ant-design/icons';
import { FolderOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { Dataset } from '@/client';

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
    <header
      className={`bg-white h-12 p-4 px-8 flex justify-between items-center`}
      style={{
        borderBottom: '1px solid #f0f0f0',
      }}
    >
      <div className="flex items-center">
        <LeftOutlined className="text-lg mr-4 cursor-pointer" onClick={backToDatasets} />
        <FolderOutlined className="text-lg p-3 bg-gray-100 rounded-md cursor-pointer" />
        <div className="flex items-center ml-2">
          <Title level={4} className="m-0 text-lg">
            {datasetInfo?.name || '未知名称'}
          </Title>
        </div>
      </div>
    </header>
  );
};

export default Header;
