import React, { FC } from 'react';

import { AgentMessage } from './Chat';

interface ChatReflectProps {
	message: AgentMessage;
}

const ChatThink: FC<ChatReflectProps> = ({ message }) => {

	return (
		<div className='text-xs text-[#aaa] mb-3'>
			<div className='mb-2 text-sm text-[#666]'>思考过程</div>
			<div className='mb-2 break-all text-xs leading-[1.25rem] text-[#999]'>{message.content}</div>
		</div>
	);
};

export default ChatThink;