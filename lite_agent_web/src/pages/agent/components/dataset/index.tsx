import React, { useState, useMemo } from 'react';
import { List, Button, Divider, Collapse } from 'antd';
import { 
  PlusCircleTwoTone, 
  EditOutlined, 
  UpOutlined, 
  DownOutlined 
} from '@ant-design/icons';

import { DatasetVO } from '@/client';
import ImgIcon from '../img-icon';
import datasetImg from '@/assets/agent/dataset.png';
import removeImg from '@/assets/agent/remove.png';

const { Panel } = Collapse;

interface DatasetProps {
  datasetList: DatasetVO[];
  onAddBase: () => void;
  onEditDataset: (datasetId: string) => void;
  onRemoveDataset: (dataset: DatasetVO) => void;
}

const Dataset: React.FC<DatasetProps> = ({ 
  datasetList, 
  onAddBase, 
  onEditDataset, 
  onRemoveDataset 
}) => {

  const [showAll, setShowAll] = useState(false);

  const displayDatasets = useMemo(() => {
    return showAll ? datasetList : datasetList.slice(0, 2);
  }, [showAll, datasetList]);

  return (
    <div>
      <Divider />
      <Collapse ghost>
        <Panel
          header={<span className="text-base font-medium">知识库</span>}
          collapsible="header"
          key="1"
          extra={
            <Button 
              color="primary" 
              variant="filled" 
              icon={<PlusCircleTwoTone />} 
              onClick={onAddBase}
            >
              添加
            </Button>
          }
        >
          <div className="flex flex-col">
            {datasetList.length === 0 && <div className=""></div>}

            {datasetList.length > 0 && (
              <List
                className={`flex-grow ${showAll ? 'overflow-y-scroll' : 'overflow-hidden'}`}
                style={{ height: showAll ? '18vh' : 'auto' }}
                dataSource={displayDatasets}
                renderItem={(dataset) => (
                  <List.Item
                    className="border rounded p-2 mb-2 hover:bg-gray-100"
                    actions={[
                      <div key="actions" className="flex items-center space-x-8">
                        <EditOutlined
                          className="text-[22px] text-[#172b4d] cursor-pointer"
                          onClick={() => onEditDataset(dataset?.id!)}
                        />
                        <ImgIcon
                          src={removeImg}
                          width={24}
                          className="cursor-pointer"
                          onClick={() => onRemoveDataset(dataset)}
                        />
                      </div>,
                    ]}
                  >
                    <List.Item.Meta
                      avatar={<ImgIcon src={datasetImg} />}
                      title={<div className="pt-2">{dataset.name}</div>}
                    />
                  </List.Item>
                )}
              />
            )}

            {datasetList.length > 2 && (
              <div className="flex justify-center mt-2">
                <Button
                  type="link"
                  onClick={() => setShowAll(!showAll)}
                  icon={showAll ? <UpOutlined /> : <DownOutlined />}
                  iconPosition="end"
                >
                  {showAll ? '收起' : '更多'}
                </Button>
              </div>
            )}
          </div>
        </Panel>
      </Collapse>
    </div>
  );
};

export default Dataset;
