import React, { useCallback, useEffect, useRef } from 'react';
import { MessageRole, TaskMessageType } from '../types/Message';
import { FN_CALL_LIST, TOOL_RETURN } from '../constants/message';
import { AgentMessage, AgentMessageMap } from '../components/chat/Chat';

interface UseChatMessageEventProps {
	scrollToBottom: () => void;
	agentSwitchRef: any;
	setMessagesMap: (messagesMap: AgentMessageMap) => void;
}

interface UseChatMessageEventExport {
	handleMessageEvent: (jsonData: any, id: string, agentId: string) => void;
}

export default function useChatMessageEvent(props: UseChatMessageEventProps): UseChatMessageEventExport {
	const { scrollToBottom, setMessagesMap, agentSwitchRef } = props;

	const handleMessageEvent = useCallback(
		(jsonData: any, id: string, agentId: string) => {
			if (jsonData.role === 'user' && jsonData.content) {
				//@ts-ignore
				setMessagesMap((prev) => {
					let msgsMap = JSON.parse(JSON.stringify(prev));
					const newMessages = JSON.parse(JSON.stringify(prev?.[jsonData.agentId]?.messages || []));
					newMessages.push(jsonData);
					newMessages.push({
						role: MessageRole.SYSTEM,
						type: 'loading',
						content: '正在处理中...',
						agentId: jsonData.agentId,
						taskId: jsonData.taskId,
						id: id,
					});
					msgsMap[jsonData.agentId] = {
						messages: newMessages,
					};
					return msgsMap;
				});
				scrollToBottom();
				return;
			}

			if (jsonData.type === FN_CALL_LIST) {
				//@ts-ignore
				setMessagesMap((prev) => {
					let msgsMap = JSON.parse(JSON.stringify(prev));
					const reqArr = [];
					jsonData.toolCalls.map((tool) => {
						reqArr.push({
							role: MessageRole.TOOL,
							req: {
								...jsonData,
								tool: tool,
							},
							createTime: jsonData.createTime,
							responding: true,
						});
					});
					// 替换现有消息
					const newMsgs = JSON.parse(JSON.stringify(prev?.[agentId]?.messages || []));
					const index = newMsgs.findIndex(
						(msg) => (msg.taskId === jsonData.taskId && msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.TEXT)
					);
					if (index !== -1) {
						newMsgs[index].responding = true;
						newMsgs[index].thoughtProcessMessages = newMsgs[index].thoughtProcessMessages.concat(reqArr);
					} else {
						// 添加新消息
						let loadingIndex = newMsgs.findIndex(
							(msg) => msg.taskId === jsonData.taskId && msg.role === MessageRole.SYSTEM && msg.type === 'loading');
						if (loadingIndex === -1) {
							loadingIndex = newMsgs.length;
						}
						newMsgs[loadingIndex] = {
							role: MessageRole.ASSISTANT,
							type: 'text',
							content: '',
							responding: true,
							agentId: jsonData.agentId,
							taskId: jsonData.taskId,
							thoughtProcessMessages: reqArr,
							resultProcessMessages: [],
							id: id,
						}!;
					}
					msgsMap[agentId] = {
						messages: newMsgs,
					};
					return msgsMap;
				})
				scrollToBottom();
				return;
			}

			if (jsonData.type === TOOL_RETURN) {
				//@ts-ignore
				setMessagesMap((prev) => {
					let msgsMap = JSON.parse(JSON.stringify(prev));
					// 替换现有消息
					const newMsgs = JSON.parse(JSON.stringify(prev?.[agentId]?.messages || []));
					const index = newMsgs.findIndex(
						(msg) => (msg.taskId === jsonData.taskId && msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.TEXT)
					);
					if (index != -1) {
						newMsgs[index].responding = true;
						newMsgs[index].thoughtProcessMessages?.map((message) => {
							if (message.role === MessageRole.TOOL && message.req?.tool?.id === jsonData.toolCallId) {
								message.res = jsonData;
							}
						});
					}
					msgsMap[agentId] = {
						messages: newMsgs,
					};
					return msgsMap;
				})
				scrollToBottom();
				return;
			}

			let message = { ...jsonData };
			let subAgents = [];

			if (jsonData.role === MessageRole.SUBAGENT && jsonData.type === TaskMessageType.DISPATCH) {
				message = {
					taskId: jsonData.taskId,
					role: MessageRole.SUBAGENT,
					...jsonData
				};
				jsonData.content.map(subAgent => {
					subAgents.push({
						agentId: jsonData.agentId,
						taskId: jsonData.taskId,
						role: MessageRole.SUBAGENT,
						...subAgent
					})
				})
			}

			if (jsonData.role === MessageRole.AGENT && jsonData.type === TaskMessageType.PLANNING) {
				message = {
					...jsonData,
					visible: true
				};
			}

			//@ts-ignore
			setMessagesMap((prev) => {
				let msgsMap = JSON.parse(JSON.stringify(prev));
				const newMsgs = JSON.parse(JSON.stringify(prev?.[agentId]?.messages || []));
				const index = newMsgs.findIndex(
					(msg) => (msg.taskId === jsonData.taskId && msg.role === MessageRole.ASSISTANT && msg.type === TaskMessageType.TEXT)
				);
				// 替换现有消息
				if (index !== -1) {
					newMsgs[index].responding = true;
					if (message.role === MessageRole.TOOL || message.role === MessageRole.REFLECTION || (message.role === MessageRole.AGENT && message.type === TaskMessageType.KNOWLEDGE)) {
						newMsgs[index].thoughtProcessMessages.push(message);
					} else if (message.role === MessageRole.SUBAGENT || message.type === TaskMessageType.PLANNING || (message.role === MessageRole.AGENT && message.type === TaskMessageType.ERROR)) {
						if (message.role === MessageRole.SUBAGENT) {
							newMsgs[index].resultProcessMessages = newMsgs[index].resultProcessMessages.concat(subAgents);
						} else {
							newMsgs[index].resultProcessMessages.push(message);
						}
					} else if (message.role === MessageRole.AGENT && message.type === TaskMessageType.DISPATCH) {
						if (!!agentSwitchRef.current) {
							const agentSwitchIndex = newMsgs[index].thoughtProcessMessages.findIndex(
								(message) => message.type === TaskMessageType.AGENT_SWITCH && message.agentId === agentSwitchRef.current.agentId && message.createTime === agentSwitchRef.current.createTime
							);
							if (agentSwitchIndex != -1) {
								if (newMsgs[index]?.thoughtProcessMessages?.[agentSwitchIndex]?.messages) {
									newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages.push(message);
								} else {
									newMsgs[index].thoughtProcessMessages[agentSwitchIndex].messages = [message];
								}
							} else {
								newMsgs[index].thoughtProcessMessages.push(message);
							}
						} else {
							newMsgs[index].thoughtProcessMessages.push(message);
						}
					} else if (message.role === MessageRole.AGENT && message.type === TaskMessageType.AGENT_SWITCH) {
						message.messages = [];
						agentSwitchRef.current = message;
						newMsgs[index].thoughtProcessMessages.push(message);
					}
				} else {
					// 添加新消息
					let loadingIndex = newMsgs.findIndex(
						(msg) => msg.taskId === jsonData.taskId && msg.role === MessageRole.SYSTEM && msg.type === 'loading');
					if (loadingIndex === -1) {
						loadingIndex = newMsgs.length;
					}
					if (message.role === MessageRole.TOOL || message.role === MessageRole.REFLECTION || (message.role === MessageRole.AGENT && message.type === TaskMessageType.KNOWLEDGE)) {
						newMsgs[loadingIndex] = {
							role: MessageRole.ASSISTANT,
							agentId: message.agentId,
							type: 'text',
							content: '',
							responding: true,
							taskId: jsonData.taskId,
							thoughtProcessMessages: [message],
							resultProcessMessages: [],
							id: id,
						}!;
					} else if (message.role === MessageRole.SUBAGENT || message.type === TaskMessageType.PLANNING || (message.role === MessageRole.AGENT && message.type === TaskMessageType.ERROR)) {
						newMsgs[loadingIndex] = {
							role: MessageRole.ASSISTANT,
							agentId: message.agentId,
							responding: true,
							type: 'text',
							content: '',
							taskId: jsonData.taskId,
							thoughtProcessMessages: [],
							resultProcessMessages: message.role === MessageRole.SUBAGENT ? subAgents : [message],
							id: id,
						}!;
					} else if (message.role === MessageRole.AGENT && message.type === TaskMessageType.DISPATCH) {
						newMsgs[loadingIndex] = {
							role: MessageRole.ASSISTANT,
							agentId: message.agentId,
							responding: true,
							type: 'text',
							content: '',
							taskId: jsonData.taskId,
							thoughtProcessMessages: [message],
							resultProcessMessages: [],
							id: id,
						}!;
					} else if (message.role === MessageRole.AGENT && message.type === TaskMessageType.AGENT_SWITCH) {
						message.messages = [];
						agentSwitchRef.current = message;
						newMsgs[loadingIndex] = {
							role: MessageRole.ASSISTANT,
							agentId: message.agentId,
							responding: true,
							type: 'text',
							content: '',
							taskId: jsonData.taskId,
							thoughtProcessMessages: [message],
							resultProcessMessages: [],
							id: id,
						}!;
					}
				}
				msgsMap[agentId] = {
					messages: newMsgs,
				};
				return msgsMap;
			});
			scrollToBottom();

		},
		[]
	);

	return {
		handleMessageEvent,
	}
}