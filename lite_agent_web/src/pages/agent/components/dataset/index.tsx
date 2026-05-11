import React, { useState, useMemo } from 'react';
import { List, Button, Collapse } from 'antd';
import { 
  PlusCircleTwoTone, 
  EditOutlined, 
  UpOutlined, 
  DownOutlined, 
} from '@ant-design/icons';

import { DatasetVO } from '@/client';
import ImgIcon from '../img-icon';
import datasetImg from '@/assets/agent/dataset.png';
import removeImg from '@/assets/agent/remove.png';
import editImg from '@/assets/agent/edit.png';

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
    <div className="border-t border-white/20 mt-4">
      <Collapse ghost>
        <Panel
          className='[&_.ant-collapse-content-box]:px-0'
          header={<span className="text-base font-medium text-[#383F44]">知识库</span>}
          key="1"
          extra={
            <Button 
              type="link"
              size="small"
              icon={<PlusCircleTwoTone />} 
              onClick={(e) => {
                e.stopPropagation();
                onAddBase();
              }}
              className="text-[#40A5EE] w-[60px] h-7 text-xs border border-[#40A5EE] rounded-lg hover:text-[#40A5EE]/80 font-medium"
            >
              添加
            </Button>
          }
        >
          <div className="flex flex-col gap-2">
            {datasetList.length > 0 && (
              <List
                className={`${showAll ? 'max-h-[300px] overflow-y-auto' : ''}`}
                dataSource={displayDatasets}
                renderItem={(dataset) => (
                  <div
                    key={dataset.id}
                    className="bg-transparent border border-white/60 rounded-xl p-2 mb-2 hover:bg-white transition-colors group"
                  >
                    <div className="flex items-center gap-3">
                      <ImgIcon src={datasetImg} width={40} />
                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-medium text-[#1D4A6B] truncate">
                          {dataset.name}
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <ImgIcon
                          key="edit"
                          src={editImg}
                          width={24}
                          className="cursor-pointer"
                          onClick={() => onEditDataset(dataset?.id!)}
                        />
                        <ImgIcon
                          key="remove"
                          src={removeImg}
                          width={24}
                          className="cursor-pointer"
                          onClick={() => onRemoveDataset(dataset)}
                        />
                      </div>
                    </div>
                  </div>
                )}
              />
            )}

            {datasetList.length > 2 && (
              <div className="flex justify-center mt-2">
                <Button
                  type="link"
                  size="small"
                  onClick={() => setShowAll(!showAll)}
                  icon={showAll ? <UpOutlined /> : <DownOutlined />}
                  className="text-[#40A5EE] hover:text-[#40A5EE]/80"
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
