import React, { useCallback } from 'react';
import { MessageRole, TaskMessageType } from '../types/Message';
import { OutMessage } from '../client';
import { FN_CALL_LIST, TOOL_RETURN } from '../constants/message';

interface UseChatAdjustAssistantMsgExport {
	adjustAssistantMsg:  (msgsArr: OutMessage[]) => void;
}

export default function useChatAdjustAssistantMsg(): UseChatAdjustAssistantMsgExport {

	const mergeToolReqAndResById = (reqArr, resArr) => {
		// 1. 创建哈希映射提高查询效率
		const map = new Map();

		// 2. 处理第一个数组（保留原始顺序）
		reqArr.forEach(item => {
			item.toolCalls?.forEach(tool => {
				map.set(tool.id, { role: MessageRole.TOOL, req: { ...item, tool: tool }, createTime: item.createTime });
			});
		});

		// 3. 合并第二个数组（后者覆盖前者）
		resArr.forEach(item => {
			if (map.has(item.toolCallId)) {
				const existing = map.get(item.toolCallId);
				map.set(item.toolCallId, { ...existing, res: item });
			} else {
				map.set(item.toolCallId, { res: item });
			}
		});

		// 4. 返回合并结果（保留第一个数组的初始顺序）
		return Array.from(map.values());
	}

	const sortByCreateTime = ( arr, order: 'asc' | 'desc' = 'asc') => {
		return [...arr].sort((a, b) => {
			return order === 'asc'
				? new Date(a.createTime).getTime() - new Date(b.createTime).getTime()
				: new Date(b.createTime).getTime() - new Date(a.createTime).getTime();
		});
	}

	const sliceByAgentSwitch = (data) => {
		const result = [];
		let i = 0;

		while (i < data.length) {
			const item = data[i];

			// 识别 agentSwitch 起点
			if (item.role === MessageRole.AGENT && item.type === TaskMessageType.AGENT_SWITCH) {
				const agentSwitch = { ...item, messages: [] };
				i++;

				// 收集下一个 agentSwitch 之前的所有数据
				while (
					i < data.length &&
					!(data[i].role === MessageRole.AGENT && data[i].type === TaskMessageType.AGENT_SWITCH)
					) {
					agentSwitch.messages.push(data[i]);
					i++;
				}

				result.push(agentSwitch);
			} else {
				i++;
			}
		}

		return result;
	}

	const adjustAssistantMsg = useCallback((msgsArr: OutMessage[]) => {
		const newMsgArr  = [];
		const assistantIndex = msgsArr.findIndex((m) => m.role === MessageRole.ASSISTANT && m.type === TaskMessageType.TEXT);
		const errorIndex = msgsArr.findIndex((m) => m.role === MessageRole.AGENT && m.type === TaskMessageType.ERROR);

		//思考
		const reqArr = msgsArr.filter((m) => m.type === FN_CALL_LIST);
		const resArr = msgsArr.filter((m) => m.type === TOOL_RETURN);
		const firstSwitchIndex = msgsArr.findIndex((m) => m.role === MessageRole.AGENT && m.type === TaskMessageType.AGENT_SWITCH);
		const dispatches = msgsArr.slice(0, firstSwitchIndex >= 0 ? firstSwitchIndex : msgsArr.length).filter((m) => m.role === MessageRole.AGENT && m.type === TaskMessageType.DISPATCH);
		const agentSwitches = sliceByAgentSwitch(msgsArr);
		const tools = mergeToolReqAndResById(reqArr, resArr);
		const reflects = msgsArr.filter((m) => m.role === MessageRole.REFLECTION && m.type === TaskMessageType.REFLECT);
		const knowledges = msgsArr.filter((m) => m.role === MessageRole.AGENT && m.type === TaskMessageType.KNOWLEDGE);
		const think = msgsArr.filter((m) => m.role === MessageRole.ASSISTANT && m.type === TaskMessageType.THINK);
		const thoughtProcessMessages = sortByCreateTime(think.concat(dispatches).concat(agentSwitches).concat(tools).concat(reflects).concat(knowledges));

		//结果
		const resultArr = msgsArr.filter((m) => ((m.role === MessageRole.ASSISTANT && m.type === TaskMessageType.TEXT) || (m.role === MessageRole.AGENT && m.type === TaskMessageType.ERROR)));
		const planningArr = msgsArr.filter((m) => (m.role === MessageRole.AGENT && m.type === TaskMessageType.PLANNING));
		const subAgentArr = adjustSubAgentMsg(msgsArr);
		const resultMessages = sortByCreateTime(resultArr.concat(planningArr).concat(subAgentArr));

		newMsgArr.push(msgsArr.filter((m) => m.role === MessageRole.USER)[0]);
		if (assistantIndex !== -1) {
			const newMsg = { ...msgsArr[assistantIndex], thoughtProcessMessages: thoughtProcessMessages, resultProcessMessages: resultMessages };
			newMsgArr.push(newMsg);
		} else if (errorIndex !== -1) {
			const newMsg = { ...msgsArr[errorIndex], thoughtProcessMessages: thoughtProcessMessages, resultProcessMessages: resultMessages };
			newMsgArr.push(newMsg);
		}
		return newMsgArr;
	}, []);

	const adjustSubAgentMsg = useCallback((msgsArr: OutMessage[]) => {
		let taskId;
		let subAgents = [];
		msgsArr.map((msg, index) => {
			if ((msg.role === MessageRole.AGENT && msg.type === TaskMessageType.BROADCAST) || (msg.role === MessageRole.SUBAGENT && msg.type === TaskMessageType.DISPATCH)) {
				taskId = msg.taskId;
				const newSubAgents = [];
				msg.content.map((subAgent) => {
					newSubAgents.push({
						taskId: taskId,
						role: MessageRole.SUBAGENT,
						...subAgent,
					})
				});
				subAgents = subAgents.concat(newSubAgents || []);
			}
		});
		return subAgents;
	}, []);

	return {
		adjustAssistantMsg,
	}
}