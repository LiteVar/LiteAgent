package com.litevar.liteagent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class ApiRecords {

    // Enums
    public enum SseEventType {
        message, chunk, functionCall
    }

    public enum Role {
        developer, user, agent, assistant, dispatcher, subagent, reflection, tool, client
    }

    public enum MessageType {
        text, imageUrl, contentList, toolCalls, dispatch, reflection, toolReturn, functionCall, taskStatus, reasoningContent, planning
    }

    public enum TaskStatusEnum {
        start, stop, done, toolsStart, toolsDone, exception
    }

    // Base Message Class

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AgentMessage {
        @JsonProperty("sessionId")
        private String sessionId;
        @JsonProperty("taskId")
        private String taskId;
        @JsonProperty("role")
        private Role role;
        @JsonProperty("to")
        private Role to;
        @JsonProperty("type")
        private MessageType type;
        @JsonProperty("createTime")
        private String createTime;
        @JsonProperty("completions")
        private Completions completions;
        @JsonProperty("content")
        private Object content;
        @JsonProperty("part")
        private String part; // For MessageChunk

        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public Role getTo() { return to; }
        public void setTo(Role to) { this.to = to; }
        public MessageType getType() { return type; }
        public void setType(MessageType type) { this.type = type; }
        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
        public Completions getCompletions() { return completions; }
        public void setCompletions(Completions completions) { this.completions = completions; }

        public Object getContent() {
            return content;
        }

        public void setContent(Object content) {
            this.content = content;
        }

        public String getPart() {
            return part;
        }

        public void setPart(String part) {
            this.part = part;
        }

        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return super.toString();
            }
        }
    }

    // Records for nested objects

    public record Usage(
            @JsonProperty("promptTokens") int promptTokens,
            @JsonProperty("completionTokens") int completionTokens,
            @JsonProperty("totalTokens") int totalTokens
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record Completions(
            @JsonProperty("usage") Usage usage,
            @JsonProperty("id") String id,
            @JsonProperty("model") String model
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record ContentListItem(
            @JsonProperty("type") String type,
            @JsonProperty("message") String message
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record ToolCall(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("arguments") Map<String, Object> arguments
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record ToolReturn(
            @JsonProperty("id") String id,
            @JsonProperty("result") Map<String, Object> result
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record DispatchContent(
            @JsonProperty("dispatchId") String dispatchId,
            @JsonProperty("agentId") String agentId,
            @JsonProperty("name") String name,
            @JsonProperty("content") List<ContentListItem> content
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record ReflectScore(
            @JsonProperty("score") int score,
            @JsonProperty("description") String description
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record MessageScore(
            @JsonProperty("content") List<ContentListItem> content,
            @JsonProperty("messageType") String messageType,
            @JsonProperty("message") String message,
            @JsonProperty("reflectScoreList") List<ReflectScore> reflectScoreList
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record ReflectionContent(
            @JsonProperty("isPass") boolean isPass,
            @JsonProperty("agentId") String agentId,
            @JsonProperty("name") String name,
            @JsonProperty("messageScore") MessageScore messageScore,
            @JsonProperty("passScore") String passScore,
            @JsonProperty("count") String count,
            @JsonProperty("maxCount") String maxCount
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record TaskStatusContent(
            @JsonProperty("status") TaskStatusEnum status,
            @JsonProperty("description") Map<String, Object> description
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record FunctionCallContent(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("arguments") Map<String, Object> arguments
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record  Tools(
        @JsonProperty("name") String name,
        @JsonProperty("description") String description
    ){
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record PlanAgent(
            @JsonProperty("agentId") String agentId,
            @JsonProperty("name") String name,
            @JsonProperty("model") String model,
            @JsonProperty("systemPrompt") String version,
            @JsonProperty("tools") List<Tools> tools,
            @JsonProperty("subAgents") List<PlanAgent> subAgents
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }

    public record PlanContent(
            @JsonProperty("planId") String planId,
            @JsonProperty("agentId") String agentId,
            @JsonProperty("name") String name,
            @JsonProperty("subAgents") List<PlanAgent> subAgents
    ) {
        @Override
        public String toString() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "JsonProcessingException in " + this.getClass().getSimpleName();
            }
        }
    }


    // Records for API endpoints

    public record VersionResponse(
        @JsonProperty("version") String version
    ) {}

    public record SessionResponse(
        @JsonProperty("sessionId") String sessionId
    ) {}

    public record ChatRequest(
        @JsonProperty("content") List<ContentListItem> content,
        @JsonProperty("isChunk") boolean isChunk
    ) {}

    public record CallbackRequest(
        @JsonProperty("id") String id,
        @JsonProperty("result") Map<String, Object> result
    ) {}

    public record StopResponse(
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("taskId") String taskId
    ) {}

    public record ClearResponse(
        @JsonProperty("id") String id
    ) {}

    // Records for different message content types
//    public static class TextMessage extends AgentMessage {
//        @JsonProperty("content")
//        private String content;
//        public String getContent() { return content; }
//        public void setContent(String content) { this.content = content; }
//    }
//
//    public static class ImageUrlMessage extends AgentMessage {
//        @JsonProperty("content")
//        private String content;
//        public String getContent() { return content; }
//        public void setContent(String content) { this.content = content; }
//    }
//
//    public static class ContentListMessage extends AgentMessage {
//        @JsonProperty("content")
//        private List<ContentListItem> content;
//        public List<ContentListItem> getContent() { return content; }
//        public void setContent(List<ContentListItem> content) { this.content = content; }
//    }
//
//    public static class ToolCallsMessage extends AgentMessage {
//        @JsonProperty("content")
//        private List<ToolCall> content;
//        public List<ToolCall> getContent() { return content; }
//        public void setContent(List<ToolCall> content) { this.content = content; }
//    }
//
//    public static class ToolReturnMessage extends AgentMessage {
//        @JsonProperty("content")
//        private ToolReturn content;
//        public ToolReturn getContent() { return content; }
//        public void setContent(ToolReturn content) { this.content = content; }
//    }
//
//    public static class DispatchMessage extends AgentMessage {
//        @JsonProperty("content")
//        private List<DispatchContent> content;
//        public List<DispatchContent> getContent() { return content; }
//        public void setContent(List<DispatchContent> content) { this.content = content; }
//    }
//
//    public static class ReflectionMessage extends AgentMessage {
//        @JsonProperty("content")
//        private ReflectionContent content;
//        public ReflectionContent getContent() { return content; }
//        public void setContent(ReflectionContent content) { this.content = content; }
//    }
//
//    public static class TaskStatusMessage extends AgentMessage {
//        @JsonProperty("content")
//        private TaskStatusContent content;
//        public TaskStatusContent getContent() { return content; }
//        public void setContent(TaskStatusContent content) { this.content = content; }
//    }
//
//    public static class FunctionCallMessage extends AgentMessage {
//        @JsonProperty("content")
//        private FunctionCallContent content;
//        public FunctionCallContent getContent() { return content; }
//        public void setContent(FunctionCallContent content) { this.content = content; }
//    }
//
//    // Record for SSE Chunk
//    public record MessageChunk(
//        @JsonProperty("sessionId") String sessionId,
//        @JsonProperty("taskId") String taskId,
//        @JsonProperty("role") Role role,
//        @JsonProperty("to") Role to,
//        @JsonProperty("type") String type,
//        @JsonProperty("part") String part,
//        @JsonProperty("completions") Completions completions,
//        @JsonProperty("createTime") String createTime
//    ) {}


}
