package com.litevar;

import com.litevar.liteagent.client.LiteAgentClient;
import com.litevar.liteagent.handler.MessageHandler;
import com.litevar.liteagent.model.ApiRecords;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public class Main {
    static final String API_KEY = "your_api_key_here";
    static final String BASE_URL = "http://locahost:8082/liteAgent/v1";

    public static void main(String[] args) {
        // Example usage of the client
        LiteAgentClient client = new LiteAgentClient(BASE_URL, API_KEY);

        // Get the version
        String version = client.getVersion();

        // Initialize a new session
        String sessionId = client.initSession();

        // stop a session (and task)
        ApiRecords.StopResponse stopResponse = client.stopSession(sessionId, null);

        // clear a session
        String id = client.clearSession(sessionId);

        // Get chat history for a session
        List<ApiRecords.AgentMessage> messages = client.chatHistory(sessionId);

        // chat with the agent
        Flux<ServerSentEvent<ApiRecords.AgentMessage>> flux = client.chat(
            sessionId,
            new ApiRecords.ChatRequest(List.of(new ApiRecords.ContentListItem("text", "hello world")), false),
            new MessageHandler() {
                @Override
                public void handleMessage(ApiRecords.AgentMessage message) {
                    System.out.println(message);
                }

                @Override
                public void handleChunk(ApiRecords.AgentMessage chunkMessage) {
                    System.out.println(chunkMessage);
                }

                @Override
                public void handleFunctionCall(ApiRecords.AgentMessage functionCallMessage) {
                    System.out.println(functionCallMessage);
                }

                @Override
                public void handlePlanningMessage(ApiRecords.AgentMessage planningMessage) {
                    System.out.println(planningMessage);
                }
            });
    }
}