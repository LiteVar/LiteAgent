import React, { useState } from 'react';
import { AgentMessage } from './Chat';
import { Button } from 'antd';

interface ChatPlanningProps {
	message: AgentMessage;
	onSendMessage: (type: 'text' | 'execute' | 'imageUrl', text?: string) => Promise<void>;
}

const ChatPlanningChildren: React.FC<{ childrens: any[], letterIndex?: boolean }> = ({ childrens, letterIndex = false }) => {
	const letter = ['a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'];
	return (
		<div className='pl-4'>
			{childrens.map((task, index) => (
				<div>
					<div className='mt-2' key={`planning-task-${index}`}>{`(${(!!letterIndex && !!letter[index]) ? letter[index] : index + 1}) ${task.name}, ${task.description?.duty}, ${task.description?.constraint}`}</div>
					{task.children && <ChatPlanningChildren childrens={task.children} letterIndex />}
				</div>
			))}
		</div>
	)
};

const ChatPlanning: React.FC<ChatPlanningProps> = (props) => {
	const { message, onSendMessage } = props;

	const [disabled, setDisabled] = useState(false);

	if (!message?.content?.taskList) {
		return null;
	}

	const onContinuePlanning = async (event: React.MouseEvent) => {
		event.stopPropagation();
		setDisabled(true);
		await onSendMessage('execute', message.content.planId)
	};

	return (
		<div>
			<div className='my-2 text-base text-[#333]'>总结回复内容</div>
			<div className='px-4 py-3 rounded-lg text-sm bg-[#F5F5F5]'>
				{message?.content?.taskList?.map((task, index) => (
					<div>
						<div className={index != 0 ? 'mt-2' : ''}
						     key={`planning-task-${index}`}>{`${index + 1}、${task.name}, ${task.description?.duty}, ${task.description?.constraint}`}</div>
						{!!task.children && <ChatPlanningChildren childrens={task.children} />}
					</div>
				))}
				{!!message?.visible && <div className='w-full flex justify-end'>
					<Button
						type="primary"
						disabled={disabled}
						onClick={onContinuePlanning}
						className='px-4 py-3 mt-2 rounded-lg text-base text-white text-base h-auto'>
						执行方案
					</Button>
				</div>}
			</div>
		</div>
	);
};

export default ChatPlanning;