import React, { FC, useMemo, useState } from 'react';

import { OutMessage } from '@/client';
import { MessageRole } from '@/types/Message';
import { DownOutlined, UpOutlined } from '@ant-design/icons';

export interface ToolMessage {
  req?: OutMessage & {
    tool?: {
      id: string;
      name: string;
      arguments: any;
    };
  };
  res?: OutMessage;
  role?: MessageRole;
  createTime?: string;
  responding: boolean;
}

interface ChatToolProps {
  tool: ToolMessage;
}

const ChatTool: FC<ChatToolProps> = ({ tool }) => {
  const [isShowContent, setIsShowContent] = useState(true);

  const toolName = useMemo(() => {
    if (!tool.req?.tool?.name) return '无工具调用';

    return `调用${tool.req.tool.name}工具`;
  }, [tool]);

  const reqParameter = useMemo(() => {
    if (!tool.req?.tool?.arguments) return '无请求参数';

    if (typeof tool.req.tool.arguments === 'object') {
      return JSON.stringify(tool.req.tool.arguments);
    } else {
      return tool.req.tool.arguments;
    }

  }, [tool]);

  const resContent = useMemo(() => {
    if (!tool?.res?.content && tool?.responding) return '正在响应中...';
    if (!tool?.res?.content && !tool?.responding) return '无响应内容';
    if (typeof tool.res.content === 'string') return tool.res.content;
    return JSON.stringify(tool.res.content);
  }, [tool]);

  return (
    <div className='mb-3'>
      <div className='mb-2 inline-block flex items-center'>
        <span onClick={() => setIsShowContent(!isShowContent)} className='cursor-pointer text-sm text-[#666666]'>{toolName}</span>
        <span
          className='ml-1 cursor-pointer text-xs'
          onClick={() => setIsShowContent(!isShowContent)}
        >
          {isShowContent
            ? <UpOutlined style={{ color: '#000' }} />
            : <DownOutlined style={{ color: '#000' }} />
          }
        </span>
      </div>
      {isShowContent &&<div>
        <div>
          <div className='mb-2 text-[#999] text-xs'>{`接收信息: ${reqParameter}`}</div>
          <div className='mb-2 text-[#999] text-xs break-all'>
            {`工具结果: ${resContent}`}
          </div>
        </div>
      </div>}
    </div>
  );
};

export default ChatTool;