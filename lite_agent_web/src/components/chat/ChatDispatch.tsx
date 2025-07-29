import React, { FC } from 'react';

import { AgentMessage } from './Chat';

interface ChatDispatchProps {
	message: AgentMessage;
}

const ChatDispatch: FC<ChatDispatchProps> = ({ message }) => {
	return (
		<div className='mb-3'>
			<div>
				<div>
					<div className='mb-2 text-[#999] text-xs'>{`输入指令: ${message.content?.cmd}`}</div>
				</div>
			</div>
		</div>
	);
};

export default ChatDispatch;