import React, { FC, useState } from 'react';
import { DownOutlined, UpOutlined } from '@ant-design/icons';
import { ChatKnowledgeProps } from '@/types/chat';

const ChatKnowledge: FC<ChatKnowledgeProps> = ({ knowledge, onSearchKnowledgeResult }) => {
  const [isShowContent, setIsShowContent] = useState(true);
  return (
    <div className="text-sm text-[#aaa] mb-3">
      <div className="mb-2 flex items-center">
        <span
          onClick={() => setIsShowContent(!isShowContent)}
          className="text-sm cursor-pointer text-[#6E6E6E]"
        >
          检索知识库
        </span>
        <span className="ml-1 cursor-pointer text-xs" onClick={() => setIsShowContent(!isShowContent)}>
          {isShowContent ? (
            <UpOutlined style={{ color: '#999' }} />
          ) : (
            <DownOutlined style={{ color: '#999' }} />
          )}
        </span>
      </div>
      {isShowContent && (
        <div>
          <div className="mb-2 text-sm text-[#6E6E6E] pl-[1em]">
            <span>检索内容: </span>
            <span>{knowledge.content.retrieveContent}</span>
          </div>
          <div className="mb-2 text-sm text-[#6E6E6E] pl-[1em]">
            <span>检索结果: </span>
            {knowledge.content.info.map((item, index) => (
              <span
                onClick={(event) =>
                  onSearchKnowledgeResult(event, item.id, knowledge.content.retrieveContent)
                }
                className="cursor-pointer text-blue-400"
                key={item.id}
              >{`${item.datasetName}${index < knowledge.content.info.length - 1 ? ', ' : ''}`}</span>
            ))}
            {knowledge.content.info?.length === 0 && <span>空</span>}
          </div>
        </div>
      )}
    </div>
  );
};

export default ChatKnowledge;
