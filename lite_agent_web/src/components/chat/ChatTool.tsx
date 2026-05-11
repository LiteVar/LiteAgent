import { FC, useMemo, useState } from 'react';
import { DownOutlined, UpOutlined } from '@ant-design/icons';
import { ChatToolProps } from '@/types/chat';

const ChatTool: FC<ChatToolProps> = ({ tool }) => {
  const [isShowContent, setIsShowContent] = useState(true);
  const [isExpandAllResults, setIsExpandAllResults] = useState(false);

  const toolName = useMemo(() => {
    if (!tool.req?.tool?.toolName && !tool.req?.tool?.functionName) return '无工具调用';

    return `调用${tool.req.tool.toolName ? `${tool.req.tool.toolName}_` : ''}${tool.req.tool.functionName}工具`;
  }, [tool]);

  const reqParameter = useMemo(() => {
    if (!tool.req?.tool?.arguments) return '无请求参数';

    if (typeof tool.req.tool.arguments === 'object') {
      return JSON.stringify(tool.req.tool.arguments);
    } else {
      return String(tool.req.tool.arguments);
    }
  }, [tool]);

  const resToolArrayResult = useMemo(() => {
    const resArr: any[] = [];
    // res 现在是数组，一个 req 可以对应多个 res
    if (Array.isArray(tool?.res)) {
      tool.res.forEach((resItem: any) => {
        const processedItem = { ...resItem };
        if (!resItem && tool?.responding) {
          processedItem.resContent = '正在响应中...';
        } else if (!resItem?.content && !tool?.responding) {
          processedItem.resContent = '无响应内容';
        } else if (resItem && typeof resItem.content === 'string') {
          processedItem.resContent = resItem.content;
        } else {
          processedItem.resContent = resItem ? JSON.stringify(resItem.content) : '无响应内容';
        }
        resArr.push(processedItem);
      });
    } else if (tool?.responding) {
      // 如果正在响应但还没有 res，显示响应中状态
      resArr.push({ resContent: '正在响应中...' });
    }
    return resArr;
  }, [tool?.res, tool?.responding]);

  return (
    <div className="mb-6">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center">
          <div className="w-2 h-2 rounded-full bg-[#1D4A6B] mr-2"></div>
          <div className="text-sm font-medium text-[#1D4A6B]">{toolName}</div>
        </div>
        <div 
          onClick={() => setIsShowContent(!isShowContent)}
          className="w-6 h-6 flex items-center justify-center cursor-pointer hover:bg-black/5 rounded-full text-[#ACB6BE]"
        >
          {isShowContent ? <UpOutlined className="text-[12px]" /> : <DownOutlined className="text-[12px]" />}
        </div>
      </div>
      
      {isShowContent && (
        <div className="pl-4 border-l-2 border-[#E0E3E6] flex flex-col gap-3">
          <div className="p-3 bg-[#F5F7F9] rounded-lg">
            <div className="text-xs font-medium text-[#ACB6BE] mb-1 uppercase">输入参数</div>
            <div className="text-sm text-[#383F44] break-all font-mono leading-relaxed">{reqParameter}</div>
          </div>
          
          <div className="flex flex-col gap-2">
            <div className="text-xs font-medium text-[#ACB6BE] mb-1 uppercase">输出结果</div>
            {resToolArrayResult?.slice(0, isExpandAllResults ? resToolArrayResult.length : 3).map((resItem: any, index: number) => (
              <div key={resItem.id || index} className="p-3 bg-white border border-[#E0E3E6] rounded-lg shadow-sm">
                <div className="text-sm text-[#383F44] break-all leading-relaxed font-mono">
                  {resItem.resContent}
                </div>
              </div>
            ))}
            
            {resToolArrayResult && resToolArrayResult.length > 3 && (
              <div 
                className="mt-1 cursor-pointer text-[#40A5EE] text-sm hover:text-[#1D4A6B] flex items-center justify-center py-2 border border-dashed border-[#E0E3E6] rounded-lg bg-gray-50/50"
                onClick={() => setIsExpandAllResults(!isExpandAllResults)}
              >
                {isExpandAllResults ? (
                  <><UpOutlined className="mr-2" /> 收起</>
                ) : (
                  <><DownOutlined className="mr-2" /> 展开剩余 {resToolArrayResult.length - 3} 条结果</>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );

};

export default ChatTool;
