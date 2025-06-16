using System.Collections.Generic;
using Newtonsoft.Json;

namespace LiteAgentSDK.DotNet.Models
{
    public class UserTask
    {
        [JsonProperty("taskId")]
        public string TaskId { get; set; }

        [JsonProperty("content")]
        public List<Content> Content { get; set; }

        [JsonProperty("isChunk")]
        public bool? IsChunk { get; set; }

        public UserTask(string taskId, List<Content> content, bool? isChunk = null)
        {
            TaskId = taskId;
            Content = content;
            IsChunk = isChunk;
        }
    }
    
    public static class ContentType
    {
        public static readonly string Text = "text";
        public static readonly string ImageUrl = "imageUrl";
    }

    public class Content
    {
        [JsonProperty("type")]
        public string Type { get; set; }

        [JsonProperty("message")]
        public string Message { get; set; }

        public Content(string type, string message)
        {
            Type = type;
            Message = message;
        }
    }
}