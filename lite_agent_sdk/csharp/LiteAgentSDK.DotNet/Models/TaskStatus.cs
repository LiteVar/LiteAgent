using System.Collections.Generic;
using Newtonsoft.Json;

namespace LiteAgentSDK.DotNet.Models
{
    public static class TaskStatusType
    {
        public static readonly string Start = "start";
        public static readonly string Stop = "stop";
        public static readonly string Done = "done";
        public static readonly string Exception = "exception";
        public static readonly string ToolsStart = "toolsStart";
        public static readonly string ToolsDone = "toolsDone";
    }

    public class TaskStatus
    {
        [JsonProperty("status")]
        public string Status { get; set; }

        [JsonProperty("taskId")]
        public string TaskId { get; set; }

        [JsonProperty("description", NullValueHandling = NullValueHandling.Ignore)]
        public Dictionary<string, object> Description { get; set; }

        public TaskStatus(string status, string taskId, Dictionary<string, object> description = null)
        {
            Status = status;
            TaskId = taskId;
            Description = description;
        }
    }
}