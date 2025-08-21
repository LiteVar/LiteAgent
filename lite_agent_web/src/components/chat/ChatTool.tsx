import { FC, useMemo, useState } from 'react';
import { DownOutlined, UpOutlined } from '@ant-design/icons';
import { ChatToolProps } from '@/types/chat';

const ChatTool: FC<ChatToolProps> = ({ tool }) => {
  const [isShowContent, setIsShowContent] = useState(true);

  const toolName = useMemo(() => {
    if (!tool.req.tool.toolName && !tool.req.tool.functionName) return '无工具调用';

    return `调用${tool.req.tool.toolName ? `${tool.req.tool.toolName}_` : ''}${tool.req.tool.functionName}工具`;
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
    if (tool.res && typeof tool.res.content === 'string') return tool.res.content;
    return tool.res ? JSON.stringify(tool.res.content) : '无响应内容';
  }, [tool]);

  return (
    <div className="mb-3">
      <div className="mb-2 flex items-center">
        <span
          onClick={() => setIsShowContent(!isShowContent)}
          className="cursor-pointer text-sm text-[#999] break-all"
        >
          {toolName}
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
          <div>
            <div className="mb-2 text-[#666666] text-sm">{`接收信息: ${reqParameter}`}</div>
            <div className="mb-2 text-[#666666] text-sm break-all">{`工具结果: ${resContent}`}</div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ChatTool;
