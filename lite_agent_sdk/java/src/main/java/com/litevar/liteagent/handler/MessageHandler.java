package com.litevar.liteagent.handler;


import com.litevar.liteagent.model.ApiRecords;

/**
 * @author reid
 * @since 2025/6/23
 */

public interface MessageHandler {
    void handleMessage(ApiRecords.AgentMessage message);
    void handleChunk(ApiRecords.AgentMessage chunkMessage);
    void handleFunctionCall(ApiRecords.AgentMessage functionCallMessage);
    void handlePlanningMessage(ApiRecords.AgentMessage planningMessage);
}
