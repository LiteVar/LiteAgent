import { useCallback } from 'react';
import { MessageRole, TaskMessageType } from '@/types/Message';
import { OutMessage } from '@/client';
import { FN_CALL_LIST, TOOL_RETURN } from '@/constants/message';
import { AgentMessage, ToolMessage, ToolCall } from '@/types/chat';

interface UseChatMessageFormatterReturn {
	adjustAssistantMsg: (msgsArr: OutMessage[]) => AgentMessage[];
}

export const useChatMessageFormatter = (): UseChatMessageFormatterReturn => {

	const mergeToolReqAndResById = (reqArr: OutMessage[], resArr: OutMessage[]): ToolMessage[] => {
		// 1. 创建哈希映射提高查询效率
		const map = new Map<string, ToolMessage>();

		// 2. 处理第一个数组（保留原始顺序）
		reqArr.forEach(item => {
			item.toolCalls?.forEach(tool => {
				const toolWithArgs: ToolCall = {
					id: tool.id || '',
					name: tool.name || 'unknown',
					toolName: tool.toolName,
					functionName: tool.functionName,
					arguments: tool.arguments || ''
				};
				map.set(tool?.id!, { role: MessageRole.TOOL, req: { ...item, tool: toolWithArgs } as any, createTime: item.createTime || '' });
			});
		});

		// 3. 合并第二个数组（后者覆盖前者）
		resArr.forEach(item => {
			if (item.toolCallId && map.has(item.toolCallId)) {
				const existing = map.get(item.toolCallId);
				map.set(item.toolCallId, { ...existing, res: item });
			} else if (item.toolCallId) {
				map.set(item.toolCallId, { res: item });
			}
		});

		// 4. 返回合并结果（保留第一个数组的初始顺序）
		return Array.from(map.values());
	}

	const sortByCreateTime = <T extends { createTime?: string }>(arr: T[], order: 'asc' | 'desc' = 'asc'): T[] => {
		return [...arr].sort((a, b) => {
			const timeA = a.createTime ? new Date(a.createTime).getTime() : 0;
			const timeB = b.createTime ? new Date(b.createTime).getTime() : 0;
			return order === 'asc' ? timeA - timeB : timeB - timeA;
		});
	}

	const adjustSubAgentMsg = useCallback((msgsArr: OutMessage[]): AgentMessage[] => {
		let taskId: string | undefined;
		let subAgents: AgentMessage[] = [];
		msgsArr.forEach((msg) => {
			if ((msg.role === MessageRole.AGENT && msg.type === TaskMessageType.BROADCAST) || (msg.role === MessageRole.SUBAGENT && msg.type === TaskMessageType.DISPATCH)) {
				taskId = msg.taskId;
				const newSubAgents: AgentMessage[] = [];
				if (Array.isArray(msg.content)) {
					msg.content.forEach((subAgent: unknown) => {
						if (typeof subAgent === 'object' && subAgent !== null) {
							newSubAgents.push({
								taskId: taskId,
								role: MessageRole.SUBAGENT,
								...(subAgent as Record<string, unknown>),
							} as AgentMessage);
						}
					});
				}
				subAgents = subAgents.concat(newSubAgents);
			}
		});
		return subAgents;
	}, []);

	const adjustAgentSwitchMsg = useCallback((msgsArr: OutMessage[]): OutMessage[] => {
		//思考
		const reqArr = msgsArr.filter((m) => m.type === FN_CALL_LIST);
		const resArr = msgsArr.filter((m) => m.type === TOOL_RETURN);
		const dispatches = msgsArr.filter((m) => m.role === MessageRole.AGENT && m.type === TaskMessageType.DISPATCH);
		const tools = mergeToolReqAndResById(reqArr, resArr);
		const reflects = msgsArr.filter((m) => m.role === MessageRole.REFLECTION && m.type === TaskMessageType.REFLECT);
		const knowledges = msgsArr.filter((m) => m.role === MessageRole.AGENT && m.type === TaskMessageType.KNOWLEDGE);
		const think = msgsArr.filter((m) => m.role === MessageRole.ASSISTANT && m.type === TaskMessageType.THINK);
		const result = msgsArr.filter((m) => m.role === MessageRole.ASSISTANT && m.type === TaskMessageType.TEXT);

		const agentSwitchIndex = msgsArr.findIndex((m) => m.role === MessageRole.AGENT && m.type === TaskMessageType.AGENT_SWITCH);
		if (agentSwitchIndex !== -1) {
			const agentSwitch = msgsArr[agentSwitchIndex];
			//@ts-ignore
			agentSwitch.messages = sortByCreateTime([...think, ...dispatches, ...tools, ...reflects, ...knowledges, ...result] as AgentMessage[]);
			return msgsArr.filter((m) => (
				m.type != FN_CALL_LIST &&
				m.type != TOOL_RETURN &&
				m.type != TaskMessageType.DISPATCH &&
				m.type != TaskMessageType.KNOWLEDGE &&
				m.type != TaskMessageType.THINK &&
				m.type != TaskMessageType.REFLECT
			));
		}
		return msgsArr;
	}, []);

	const adjustMessageByTaskId = useCallback((msgsArr: OutMessage[]): OutMessage[] => {
		// msgsArr按照taskId分割成不同的数组
		const grouped = Object.values(
			msgsArr.reduce((acc, item) => {
				const key = item.taskId || 'default';
				if (!acc[key]) {
					acc[key] = [];
				}
				acc[key].push(item);
				return acc;
			}, {} as Record<string, OutMessage[]>)
		);
		console.log('grouped---', grouped)

		let flatGrouped: OutMessage[] = [];
		grouped.forEach((group) => {
			const newAgentMessages = adjustAgentSwitchMsg(group as OutMessage[])
			flatGrouped = flatGrouped.concat(newAgentMessages);
		});
		return flatGrouped;
	}, [adjustAgentSwitchMsg]);

	const adjustAssistantMsg = useCallback((msgsArr: OutMessage[]): AgentMessage[] => {
		const newMsgs = adjustMessageByTaskId(msgsArr);
		const newMsgArr: AgentMessage[] = [];
		const assistantIndex = newMsgs.findIndex((m) => m.role === MessageRole.ASSISTANT && m.type === TaskMessageType.TEXT);
		const errorIndex = newMsgs.findIndex((m) => m.role === MessageRole.AGENT && m.type === TaskMessageType.ERROR);

		//思考
		const reqArr = newMsgs.filter((m) => m.type === FN_CALL_LIST);
		const resArr = newMsgs.filter((m) => m.type === TOOL_RETURN);
		const dispatches = newMsgs.filter((m) => m.role === MessageRole.AGENT && m.type === TaskMessageType.DISPATCH);
		const agentSwitches = newMsgs.filter((m) => m.role === MessageRole.AGENT && m.type === TaskMessageType.AGENT_SWITCH);
		const tools = mergeToolReqAndResById(reqArr, resArr);
		const reflects = newMsgs.filter((m) => m.role === MessageRole.REFLECTION && m.type === TaskMessageType.REFLECT);
		const knowledges = newMsgs.filter((m) => m.role === MessageRole.AGENT && m.type === TaskMessageType.KNOWLEDGE);
		const think = newMsgs.filter((m) => m.role === MessageRole.ASSISTANT && m.type === TaskMessageType.THINK);
		const thoughtProcessMessages = sortByCreateTime([...think, ...dispatches, ...agentSwitches, ...tools, ...reflects, ...knowledges] as AgentMessage[]);

		//结果
		const resultArr = newMsgs.filter((m) => ((m.role === MessageRole.ASSISTANT && m.type === TaskMessageType.TEXT) || (m.role === MessageRole.AGENT && m.type === TaskMessageType.ERROR)));
		const planningArr = newMsgs.filter((m) => (m.role === MessageRole.AGENT && m.type === TaskMessageType.PLANNING));
		const subAgentArr = adjustSubAgentMsg(newMsgs);
		const resultMessages = sortByCreateTime(resultArr.concat(planningArr).concat(subAgentArr));

		const userMsg = newMsgs.find((m) => m.role === MessageRole.USER);
		if (userMsg) {
			newMsgArr.push(userMsg as AgentMessage);
		}
		if (assistantIndex !== -1) {
			const newMsg = { ...newMsgs[assistantIndex], thoughtProcessMessages: thoughtProcessMessages, resultProcessMessages: resultMessages } as AgentMessage;
			newMsgArr.push(newMsg);
		} else if (errorIndex !== -1) {
			const newMsg = { ...newMsgs[errorIndex], thoughtProcessMessages: thoughtProcessMessages, resultProcessMessages: resultMessages } as AgentMessage;
			newMsgArr.push(newMsg);
		}
		return newMsgArr;
	}, [adjustSubAgentMsg, adjustMessageByTaskId]);

	return {
		adjustAssistantMsg,
	}
}