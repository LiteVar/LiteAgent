package com.litevar.agent.rest.openai.message;

import com.litevar.agent.base.vo.OutMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 知识库消息
 *
 * @author uncle
 * @since 2025/4/17 18:33
 */
@Data
@AllArgsConstructor
public class KnowledgeMessage implements AgentMessage {
    private String sessionId;
    private String taskId;
    private String agentId;

    /**
     * 检索的内容
     */
    private String retrieveContent;

    /**
     * 检索历史信息
     */
    List<OutMessage.KnowledgeHistoryInfo> historyInfo;
}
