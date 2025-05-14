using System;
using Newtonsoft.Json;

namespace LiteAgentSDK.DotNet.Models
{
    public static class AgentRoleType
    {
        public static readonly string System = "developer"; // system prompt
        public static readonly string User = "user";       // user
        public static readonly string Agent = "agent";     // agent
        public static readonly string Llm = "assistant";   // llm
        public static readonly string Tool = "tool";       // external tools
        public static readonly string Client = "client";   // external caller
    }

    public static class AgentMessageType
    {
        public static readonly string Text = "text";        // String
        public static readonly string ImageUrl = "imageUrl"; // String
        // public static readonly string DISPATCH = "dispatch"; // Dispatch
        public static readonly string ToolCalls = "toolCalls"; // List<FunctionCall>
        public static readonly string ToolReturn = "toolReturn"; // ToolReturn
        public static readonly string ContentList = "contentList"; // List<Content>
        public static readonly string Reflection = "reflection"; // Reflection
        public static readonly string TaskStatus = "taskStatus"; // TaskStatus
        public static readonly string FunctionCall = "functionCall"; // FunctionCall
    }

    public static class SseEventType {
        public static readonly string Message = "message";
        public static readonly string Chunk = "chunk";
        public static readonly string FunctionCall = "functionCall";
    }

    public class AgentMessage
    {
        [JsonProperty("sessionId")]
        public string SessionId { get; set; }

        [JsonProperty("taskId")]
        public string TaskId { get; set; }

        [JsonProperty("role")]
        public string Role { get; set; }

        [JsonProperty("to")]
        public string To { get; set; }

        [JsonProperty("type")]
        public string Type { get; set; }

        [JsonProperty("content")]
        public object Content { get; set; }

        [JsonProperty("createTime")]
        public DateTime CreateTime { get; set; }

        [JsonProperty("completions")]
        public Completions Completions { get; set; }
        
        public AgentMessage(
            string sessionId,
            string taskId,
            string role,
            string to,
            string type,
            object content,
            DateTime createTime,
            Completions completions = null)
        {
            SessionId = sessionId;
            TaskId = taskId;
            Role = role;
            To = to;
            Type = type;
            Content = content;
            CreateTime = createTime;
            Completions = completions;
        }
    }

    public class Completions
    {
        [JsonProperty("usage")]
        public TokenUsage Usage { get; set; }

        [JsonProperty("id")]
        public string Id { get; set; }

        [JsonProperty("model")]
        public string Model { get; set; }
        
        public Completions(TokenUsage usage, string id, string model)
        {
            Usage = usage;
            Id = id;
            Model = model;
        }
    }

    public class TokenUsage
    {
        [JsonProperty("promptTokens")]
        public int PromptTokens { get; set; }

        [JsonProperty("completionTokens")]
        public int CompletionTokens { get; set; }

        [JsonProperty("totalTokens")]
        public int TotalTokens { get; set; }
        
        public TokenUsage(int promptTokens, int completionTokens, int totalTokens)
        {
            PromptTokens = promptTokens;
            CompletionTokens = completionTokens;
            TotalTokens = totalTokens;
        }
    }
}