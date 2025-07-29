import React, { useState, useCallback, useMemo, useEffect } from 'react';
import { Modal, Button, Tag, Space, Empty } from 'antd';
import { CloseOutlined } from '@ant-design/icons';

import { FunctionVO, ToolDTO, AgentDetailVO } from '@/client';

interface SequenceModalProps {
  agentInfo: AgentDetailVO;
  toggleSequence: (tool: ToolDTO, removeIndex ?: number) => void;
  restoreSequence: (sequence: string[]) => void; 
  visible: boolean;
  onCancel: () => void;
}

const SequenceModal: React.FC<SequenceModalProps> = ({
  agentInfo,
  toggleSequence,
  restoreSequence,
  visible,
  onCancel,
}) => {

  const [initialSequence, setInitialSequence] = useState<string[]>([]);

  const fnList = useMemo(() => {
    return agentInfo?.functionList || [];
  }, [agentInfo]);

  const selectedFnList = useMemo(() => {
    const agentSequence = agentInfo?.agent?.sequence || [];
    const newFnList: FunctionVO[] = [];
    
    agentSequence.forEach(fnId => {
      fnList.forEach(fn => {
        if (fn.functionId === fnId) {
          newFnList.push(fn);
        }
      });
    });

    return newFnList;
  }, [agentInfo, fnList]);
 
  const onSureClick = useCallback(() => {
    onCancel();
  }, []);

  const handleCancel = useCallback(() => {
    restoreSequence(initialSequence);
    onCancel();
  }, [initialSequence, onCancel]);

  useEffect(() => {
    if (visible) {
      setInitialSequence([...(agentInfo?.agent?.sequence || [])]);
    }
  }, [visible]);

  return (
    <Modal
      title="方法序列设置"
      open={visible}
      onCancel={onCancel}
      width={700}
      footer={[
        <div key="footer" className="flex justify-end">
          <Space>
            <Button onClick={handleCancel}>
              取消
            </Button>
            <Button
              type="primary"
              onClick={onSureClick}
            >
              确定
            </Button>
          </Space>
        </div>
      ]}
    >
      <div className="flex flex-wrap p-3 min-h-20 bg-[#f5f5f5] rounded-md max-h-[20vh] overflow-y-auto">
        {selectedFnList.length > 0 ? (
          selectedFnList.map((fn, index) => (
            <div
              key={`${index}-${fn.functionId}`}
              className="flex border bg-white rounded-md p-2 mr-3 mb-3 h-10"
            >
              <div className='text-center'>
                <div className='text-sm whitespace-nowrap'>
                  {fn.functionName}
                </div>
                <div className='text-xs whitespace-nowrap'>
                  {fn.toolName}
                </div>
              </div>       
              <div>
                <CloseOutlined 
                  className='pl-3 cursor-pointer' 
                  onClick={() => toggleSequence(fn, index)}
                /> 
              </div>      
            </div>
          ))
        ) : (
          <div className="w-full flex justify-center items-center h-20">
            <Empty 
              image={Empty.PRESENTED_IMAGE_SIMPLE} 
              description="暂无已选方法" 
            />
          </div>
        )}
      </div>
      <div className='mt-6 mb-2'>可选工具方法</div>
      <div className='max-h-[30vh] overflow-y-auto'>
        {fnList.length > 0 ? (
          <div className="grid grid-cols-4 gap-2">
            {fnList.map(fn => (
              <Tag
                key={fn.functionId}
                className="px-3 py-2 cursor-pointer text-center"
                onClick={() => toggleSequence(fn)}
              >
                <div>{fn.functionName}</div>
                <div>{fn.toolName}</div> 
              </Tag>
            ))}
          </div>
        ) : (
          <div className="flex justify-center items-center h-20">
            <Empty 
              image={Empty.PRESENTED_IMAGE_SIMPLE} 
              description="暂无可选方法" 
            />
          </div>
        )}
      </div> 
    </Modal>
  );
};

export default SequenceModal;
