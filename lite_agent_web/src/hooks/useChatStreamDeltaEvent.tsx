import React, { useCallback, useRef } from 'react';
import { MessageRole, TaskMessageType } from '../types/Message';
import { AgentMessage, AgentMessageMap } from '../components/chat/Chat';

interface UseChatStreamDeltaEventProps {
	scrollToBottom: () => void;
	setMessagesMap: (messagesMap: AgentMessageMap) => void;
	agentStatusRef: any;
	agentSwitchRef: any;
}

interface UseChatStreamDeltaEventExport {
	handleDeltaEvent: (jsonData: any, id: string, agentId: string) => void;
}

const createAgentMessage = (jsonData: any, id: string, type: TaskMessageType, content: string) => ({
	role: MessageRole.ASSISTANT,
	type,
	content,
	chunkType: jsonData.chunkType,
	taskId: jsonData.taskId,
	agentId: jsonData.agentId,
	id,
	flag: 0,
});

const updateMessages = (agentSwitchRef: any, prevMessages: AgentMessage[], agentMessage: AgentMessage, jsonData: any, type: TaskMessageType, id, createNewMessage?: boolean) => {
	const index = prevMessages.findIndex(
		(msg) => msg.taskId === jsonData.taskId && msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.TEXT
	);
	const newMsgs = JSON.parse(JSON.stringify(prevMessages));

	if (index !== -1) {
		newMsgs[index].responding = true;
		if (type === TaskMessageType.THINK) {
			if (createNewMessage) {
				newMsgs[index].thoughtProcessMessages.push({ ...agentMessage, type });
			} else if (newMsgs[index].thoughtProcessMessages.length > 0) {
				for (let i = newMsgs[index].thoughtProcessMessages.length - 1; i >= 0; i--) {
					if (newMsgs[index].thoughtProcessMessages[i].role === MessageRole.ASSISTANT && newMsgs[index].thoughtProcessMessages[i].type === TaskMessageType.THINK) {
						newMsgs[index].thoughtProcessMessages[i].content = agentMessage?.content;
						break;
					}
				}
			} else {
				newMsgs[index].thoughtProcessMessages.push({ ...agentMessage, type });
			}
		} else {
			if (!!agentSwitchRef.current) {
				const agentSwitchIndex = newMsgs[index].thoughtProcessMessages.findIndex(
					(message) => message.type === TaskMessageType.AGENT_SWITCH && message.agentId === agentSwitchRef.current.agentId && message.createTime === agentSwitchRef.current.createTime
				);

				if (agentSwitchIndex >= 0) {
					if (createNewMessage) {
						if (!!newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages) {
							newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages.push({ ...agentMessage, type, role: MessageRole.ASSISTANT });
						} else {
							newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages = [{ ...agentMessage, type, role: MessageRole.ASSISTANT }];
						}
					} else if (newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages?.length > 0) {
						for (let i = newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages.length - 1; i >= 0; i--) {
							if (newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages[i].role === MessageRole.ASSISTANT && newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages[i].type === TaskMessageType.TEXT) {
								newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages[i].content = agentMessage?.content;
								break;
							}
						}
					} else {
						if (!!newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages) {
							newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages.push({ ...agentMessage, type, role: MessageRole.ASSISTANT });
						} else {
							newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages = [{ ...agentMessage, type, role: MessageRole.ASSISTANT }];
						}
					}
				}
			}

			if (createNewMessage) {
				newMsgs[index].resultProcessMessages.push({ ...agentMessage, type });
			} else if (newMsgs[index].resultProcessMessages.length > 0) {
				for (let i = newMsgs[index].resultProcessMessages.length - 1; i >= 0; i--) {
					if ((newMsgs[index].resultProcessMessages[i].role === MessageRole.ASSISTANT && newMsgs[index].resultProcessMessages[i].type === TaskMessageType.TEXT) || newMsgs[index].resultProcessMessages[i].role === MessageRole.AGENT) {
						newMsgs[index].resultProcessMessages[i].content = agentMessage?.content;
						break;
					}
				}
			} else {
				newMsgs[index].resultProcessMessages.push({ ...agentMessage, type });
			}
		}
	} else {
		let loadingIndex = newMsgs.findIndex(
			(msg) => msg.taskId === jsonData.taskId && msg.role === MessageRole.SYSTEM && msg.type === 'loading'
		);
		if (loadingIndex === -1) {
			loadingIndex = newMsgs.length;
		}
		newMsgs[loadingIndex] = {
			role: MessageRole.ASSISTANT,
			type: TaskMessageType.TEXT,
			content: '',
			agentId: jsonData.agentId,
			taskId: jsonData.taskId,
			thoughtProcessMessages: type === TaskMessageType.THINK ? [{ ...agentMessage, type }] : [],
			resultProcessMessages: type === TaskMessageType.TEXT ? [{ ...agentMessage, type }] : [],
			id,
		};
	}
	return newMsgs;
};

export default function useChatStreamDeltaEvent(props: UseChatStreamDeltaEventProps): UseChatStreamDeltaEventExport {
	const { scrollToBottom, agentStatusRef, setMessagesMap, agentSwitchRef } = props;

	const handleDeltaEvent = useCallback(
		(jsonData: any, id: string, agentId: string) => {
			const currentAgentStatus = agentStatusRef.current.find((item) => item.id === id);
			if (!currentAgentStatus) return;

			if (currentAgentStatus.agentMessage && currentAgentStatus.agentMessage?.chunkType !== jsonData.chunkType) {
				currentAgentStatus.agentMessage.flag = 1;
			}

			if (jsonData.chunkType === 1) {
				if (currentAgentStatus.agentMessage?.flag === 1) {
					//由于大模型不时返回回车或者空格符影响结果，故过滤最开始的回车或者空格符
					if (!jsonData.part.replace(/[\r\n\s\u3000]/g, '')) return;
					currentAgentStatus.agentMessage = createAgentMessage(jsonData, id, TaskMessageType.THINK, jsonData.part);
					const newAgentMessage = JSON.parse(JSON.stringify(currentAgentStatus.agentMessage));
					//@ts-ignore
					setMessagesMap((prev) => {
						let msgsMap = JSON.parse(JSON.stringify(prev));
						const msgs = JSON.parse(JSON.stringify(prev?.[agentId]?.messages || []));
						const newMsgs = updateMessages(agentSwitchRef, msgs, newAgentMessage, jsonData, TaskMessageType.THINK, id, true);
						msgsMap[agentId] = {
							messages: newMsgs,
						};
						return msgsMap;
					});
					scrollToBottom();
					return;
				} else {
					//由于大模型不时返回回车或者空格符影响结果，故过滤最开始的回车或者空格符
					if (!jsonData.part.replace(/[\r\n\s\u3000]/g, '')) return;
					currentAgentStatus.agentMessage = currentAgentStatus.agentMessage
						? {
							...currentAgentStatus.agentMessage,
							content: (currentAgentStatus.agentMessage.content || '') + jsonData.part,
							taskId: jsonData.taskId,
							chunkType: jsonData.chunkType,
							agentId: jsonData.agentId,
							id,
							flag: 0,
						}
						: createAgentMessage(jsonData, id, TaskMessageType.THINK, jsonData.part);
				}
				const newAgentMessage = JSON.parse(JSON.stringify(currentAgentStatus.agentMessage));
				//@ts-ignore
				setMessagesMap((prev) => {
					let msgsMap = JSON.parse(JSON.stringify(prev));
					const msgs = JSON.parse(JSON.stringify(prev?.[agentId]?.messages || []));
					const newMsgs = updateMessages(agentSwitchRef, msgs, newAgentMessage, jsonData, TaskMessageType.THINK, id);
					msgsMap[agentId] = {
						messages: newMsgs,
					};
					return msgsMap;
				});
			} else if (jsonData.chunkType === 0) {
				if (currentAgentStatus.agentMessage?.flag === 1) {
					currentAgentStatus.agentMessage = createAgentMessage(jsonData, id, TaskMessageType.TEXT, jsonData.part);
					const newAgentMessage = JSON.parse(JSON.stringify(currentAgentStatus.agentMessage));
					//@ts-ignore
					setMessagesMap((prev) => {
						let msgsMap = JSON.parse(JSON.stringify(prev));
						const msgs = JSON.parse(JSON.stringify(prev?.[agentId]?.messages || []));
						const newMsgs = updateMessages(agentSwitchRef, msgs, newAgentMessage, jsonData, TaskMessageType.TEXT, id, true);
						msgsMap[agentId] = {
							messages: newMsgs,
						};
						return msgsMap;
					});
					scrollToBottom();
				} else {
					currentAgentStatus.agentMessage = currentAgentStatus.agentMessage
						? {
							...currentAgentStatus.agentMessage,
							content: (currentAgentStatus.agentMessage.content || '') + jsonData.part,
							taskId: jsonData.taskId,
							chunkType: jsonData.chunkType,
							agentId: jsonData.agentId,
							id,
							flag: 0,
						}
						: createAgentMessage(jsonData, id, TaskMessageType.TEXT, jsonData.part);
					const newAgentMessage = JSON.parse(JSON.stringify(currentAgentStatus.agentMessage));
					//@ts-ignore
					setMessagesMap((prev) => {
						let msgsMap = JSON.parse(JSON.stringify(prev));
						const msgs = JSON.parse(JSON.stringify(prev?.[agentId]?.messages || []));
						const newMsgs = updateMessages(agentSwitchRef, msgs, newAgentMessage, jsonData, TaskMessageType.TEXT, id);
						msgsMap[agentId] = {
							messages: newMsgs,
						};
						return msgsMap;
					});
				}
			}
			scrollToBottom();
		},
		[]
	);

	return {
		handleDeltaEvent,
	};
}