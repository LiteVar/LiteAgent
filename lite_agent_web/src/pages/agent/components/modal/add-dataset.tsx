import React, { useCallback } from 'react';
import { Modal, List, Button } from 'antd';
import { FolderOutlined} from '@ant-design/icons';

import { AgentDetailVO, DatasetsVO } from "@/client";
import ImgIcon from '../img-icon';
import datasetImg from '@/assets/agent/dataset.png';

interface AddDatasetModalProps {
  visible: boolean;
  agentInfo: AgentDetailVO;
  datasetList: DatasetsVO[];
  onCancel: () => void;
  toggleDataset: (tool: DatasetsVO) => void;
}

const AddDatasetModal: React.FC<AddDatasetModalProps> = (props) => {
  const { visible, agentInfo, datasetList, onCancel, toggleDataset } = props;
 
  const isDatasetSelected = useCallback((datasetId: string) => {
    return agentInfo.datasetList?.find((dataset) => dataset.id === datasetId) ? true : false;
  }, [agentInfo]);

  return (
    <Modal
      centered
      title="添加知识库"
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={600}
      styles={{body : {display: 'flex', flexDirection: 'column', maxHeight: '70vh' }}}
    >
      <div className="flex-grow overflow-y-auto p-2">
        <List
          dataSource={datasetList}
          renderItem={item => (
            <List.Item
              key={item.id}
              actions={[
                <Button 
                  key={item.id} 
                  variant="filled" 
                  color={isDatasetSelected(item.id!) ? 'danger' : 'primary'}
                  onClick={() => toggleDataset(item)} 
                >
                  {isDatasetSelected(item.id!) ? '移除' : '添加'}
                </Button>
              ]} 
            > 
              <List.Item.Meta
                avatar={<ImgIcon src={datasetImg} />}
                title={<div className='pt-2'>{item.name}</div>}
              />
            </List.Item>
          )}
        />
      </div>
    </Modal>
  );
};

export default AddDatasetModal;
