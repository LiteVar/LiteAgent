import React, { FC } from 'react';

import { MessageRole, TaskMessageType } from '@/types/Message';

export interface ReflectMessage {
	taskId: string;
	role: MessageRole;
	type: TaskMessageType;
	content: {
		input: any;
		rawInput: string;
		rawOutput: string;
		output: {
			score: number;
			information: string;
		}[];
	};
}

interface ChatReflectProps {
	reflect: ReflectMessage;
}

const ChatReflect: FC<ChatReflectProps> = ({ reflect }) => {

	return (
		<div className='text-xs text-[#aaa] mb-3'>
			<div className='mb-2 text-sm text-[#666]'>反思内容</div>
			<div className='mb-2 text-xs leading-[1.25rem] text-[#999]'>{JSON.stringify({ rawInput: reflect.content.rawInput, rawOutput: reflect.content.rawOutput })}</div>
			<div className='mb-2 text-xs text-[#666]'>反思结果</div>
			<div className='text-xs text-[#999]'>
				{reflect.content.output.map((msg, index) => (
					<div key={`reflect-output-${index}`} className='text-xs leading-[1.25rem] text-[#999] mb-2'>{JSON.stringify(msg)}</div>
				))}
			</div>
		</div>
	);
};

export default ChatReflect;