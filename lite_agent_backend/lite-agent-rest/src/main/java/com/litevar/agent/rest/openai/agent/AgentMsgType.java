package com.litevar.agent.rest.openai.agent;

import com.litevar.agent.rest.openai.message.*;

/**
 * agent消息类型
 *
 * @author uncle
 * @since 2025/4/11 12:04
 */
public enum AgentMsgType {
    /**
     * 用户发送消息
     */
    USER_SEND_MSG {
        @Override
        public void handler(AgentMessage msg) {
            UserSendMessage message = (UserSendMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.onSend(message));
        }
    },
    /**
     * 大模型返回的消息
     */
    LLM_MSG {
        @Override
        public void handler(AgentMessage msg) {
            LlmMessage message = (LlmMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.LlmMsg(message));
        }
    },

    /**
     * 大模型思考过程
     */
    THINK_MSG {
        @Override
        public void handler(AgentMessage msg) {
            LlmMessage message = (LlmMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.thinkMsg(message));
        }
    },

    /**
     * 异常信息
     */
    ERROR_MSG {
        @Override
        public void handler(AgentMessage msg) {
            ErrorMessage errorMessage = (ErrorMessage) msg;
            AgentManager.getHandler(errorMessage.getSessionId()).forEach(handler -> handler.onError(errorMessage));
        }
    },
    /**
     * stream流消息片段
     */
    CHUNK_MSG {
        @Override
        public void handler(AgentMessage msg) {
            ChunkMessage message = (ChunkMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.chunk(message));
        }
    },
    /**
     * function-calling请求消息
     */
    FUNCTION_CALL_MSG {
        @Override
        public void handler(AgentMessage msg) {
            LlmMessage message = (LlmMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.functionCalling(message));
        }
    },
    /**
     * open-tool 告诉第三方调用工具消息
     */
    OPEN_TOOL_CALL_MSG {
        @Override
        public void handler(AgentMessage msg) {
            OpenToolMessage message = (OpenToolMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.openToolCall(message));
        }
    },
    /**
     * function-calling返回消息
     */
    FUNCTION_RESULT_MSG {
        @Override
        public void handler(AgentMessage msg) {
            ToolResultMessage message = (ToolResultMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.functionResult(message));
        }
    },
    /**
     * 反思消息
     */
    REFLECT_MSG {
        @Override
        public void handler(AgentMessage msg) {
            ReflectResultMessage message = (ReflectResultMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.reflect(message));
        }
    },
    /**
     * 分发消息
     */
    DISTRIBUTE_MSG {
        @Override
        public void handler(AgentMessage msg) {
            DistributeMessage message = (DistributeMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.distribute(message));
        }
    },
    /**
     * 知识库调用
     */
    KNOWLEDGE_MSG {
        @Override
        public void handler(AgentMessage msg) {
            KnowledgeMessage message = (KnowledgeMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.knowledge(message));
        }
    },

    /**
     * agent切换
     */
    SWITCH_AGENT_MSG {
        @Override
        public void handler(AgentMessage msg) {
            AgentSwitchMessage message = (AgentSwitchMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.agentSwitch(message));
        }
    },

    /**
     * 任务完成
     */
    TASK_DONE {
        @Override
        public void handler(AgentMessage msg) {
            TaskDoneMessage message = (TaskDoneMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.taskDone(message.getAgentId(), message.getTaskId()));
        }
    },
    /**
     * 规划消息
     */
    PLANNING_MSG {
        @Override
        public void handler(AgentMessage msg) {
            PlanningMessage message = (PlanningMessage) msg;
            AgentManager.getHandler(message.getSessionId()).forEach(handler -> handler.planning(message));
        }
    };

    public abstract void handler(AgentMessage msg);
}
