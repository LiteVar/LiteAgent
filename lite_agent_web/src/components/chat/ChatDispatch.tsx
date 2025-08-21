import { FC } from 'react';
import { ChatDispatchProps } from '@/types/chat';

const ChatDispatch: FC<ChatDispatchProps> = ({ message }) => {
	return (
		<div className='mb-3'>
			<div>
				<div>
					<div className='mb-2 text-[#666666] text-sm'>{`输入指令: ${message.content?.cmd}`}</div>
				</div>
			</div>
		</div>
	);
};

export default ChatDispatch;