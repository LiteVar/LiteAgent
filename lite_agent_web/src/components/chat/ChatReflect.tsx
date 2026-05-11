import { FC } from 'react';
import { ChatReflectProps } from '@/types/chat';

const ChatReflect: FC<ChatReflectProps> = ({ reflect }) => {

	return (
		<div className='mb-6'>
      <div className="flex items-center mb-3">
        <div className="w-2 h-2 rounded-full bg-[#FAAD14] mr-2"></div>
        <div className="text-sm font-medium text-[#1D4A6B]">反思过程</div>
      </div>
      
      <div className="pl-4 border-l-2 border-[#E0E3E6] flex flex-col gap-4">
        <div className="p-3 bg-[#F5F7F9] rounded-lg">
          <div className="text-xs font-medium text-[#ACB6BE] mb-2 uppercase">反思内容</div>
          <div className="text-sm text-[#383F44] font-mono break-all whitespace-pre-wrap leading-relaxed">
            {JSON.stringify({ rawInput: reflect.content.rawInput, rawOutput: reflect.content.rawOutput }, null, 2)}
          </div>
        </div>
        
        <div className="flex flex-col gap-2">
          <div className="text-xs font-medium text-[#ACB6BE] mb-2 uppercase">反思结果</div>
          <div className="flex flex-col gap-2">
            {reflect.content.output.map((msg, index) => (
              <div key={`${msg.id}-reflect-output-${index}`} className="p-3 bg-white border border-[#E0E3E6] rounded-lg shadow-sm">
                <div className="text-sm text-[#383F44] font-mono break-all whitespace-pre-wrap leading-relaxed">{JSON.stringify(msg)}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
		</div>
	);

};

export default ChatReflect;