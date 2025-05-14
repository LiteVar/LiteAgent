using System;
using Newtonsoft.Json;

namespace LiteAgentSDK.DotNet.Models
{
    public class AgentMessageChunk
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

        [JsonProperty("part")]
        public string Part { get; set; }

        [JsonProperty("createTime")]
        public DateTime CreateTime { get; set; }

        public AgentMessageChunk(
            string sessionId,
            string taskId,
            string role,
            string to,
            string type,
            string part,
            DateTime createTime)
        {
            SessionId = sessionId;
            TaskId = taskId;
            Role = role;
            To = to;
            Type = type;
            Part = part;
            CreateTime = createTime;
        }
    }
}