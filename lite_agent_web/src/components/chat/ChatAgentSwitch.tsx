import React, { FC, useMemo, useState } from 'react';

import { OutMessage } from '@/client';
import { MessageRole } from '@/types/Message';
import { AgentMessage } from './Chat';
import { TaskMessageType } from '../../types/Message';
import { DownOutlined, UpOutlined } from '@ant-design/icons';

export type AgentSwitchMessage = OutMessage & {
	messages?: AgentMessage[];
}

interface AgentSwitchMessageProp {
	agentSwitchMessage: AgentSwitchMessage;
}

const ChatAgentSwitch: FC<AgentSwitchMessageProp> = ({ agentSwitchMessage }) => {
	const [isShowContent, setIsShowContent] = useState(true);
	// console.log('agentSwitchMessage.messages', agentSwitchMessage.messages);

	return (
		<div className='mb-3'>
			<div className='mb-2 inline-block flex items-center'>
				<span onClick={() => setIsShowContent(!isShowContent)} className='text-sm cursor-pointer text-[#666666]'>{`调用agent: ${agentSwitchMessage.content.agentName || ''}`}</span>
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
			{isShowContent && <div>
				{agentSwitchMessage.messages?.map((message, index) => {
					if (message.role === MessageRole.AGENT && message.type === TaskMessageType.DISPATCH) {
						return (
							<div className='mb-2 text-[#999] text-xs'>{`输入指令: ${message.content?.cmd}`}</div>
						);
					} else if (message.role === MessageRole.ASSISTANT && message.type === TaskMessageType.TEXT) {
						return (
							<div className='mb-2 text-[#999] text-xs break-all'>{`输出内容: ${message.content}`}</div>
						);
					}
				})}
			</div>}
		</div>
	);
};

export default ChatAgentSwitch;