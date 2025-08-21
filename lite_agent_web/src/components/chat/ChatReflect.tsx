import { FC } from 'react';
import { ChatReflectProps } from '@/types/chat';

const ChatReflect: FC<ChatReflectProps> = ({ reflect }) => {

	return (
		<div className='text-sm text-[#aaa] mb-3'>
			<div className='mb-2 text-sm text-[#666666]'>反思内容</div>
			<div className='mb-2 text-sm leading-[1.25rem] text-[#999]'>{JSON.stringify({ rawInput: reflect.content.rawInput, rawOutput: reflect.content.rawOutput })}</div>
			<div className='mb-2 text-sm text-[#999]'>反思结果</div>
			<div className='text-sm text-[#999]'>
				{reflect.content.output.map((msg, index) => (
					<div key={`reflect-output-${index}`} className='text-sm leading-[1.25rem] text-[#999] mb-2'>{JSON.stringify(msg)}</div>
				))}
			</div>
		</div>
	);
};

export default ChatReflect;