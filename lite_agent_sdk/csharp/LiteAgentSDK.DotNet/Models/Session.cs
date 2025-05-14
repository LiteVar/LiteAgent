using Newtonsoft.Json;

namespace LiteAgentSDK.DotNet.Models
{
    public class Session
    {
        [JsonProperty("sessionId")]
        public string SessionId { get; set; }

        public Session(string sessionId)
        {
            SessionId = sessionId;
        }
    }

    public class SessionName : Session
    {
        [JsonProperty("name", NullValueHandling = NullValueHandling.Ignore)]
        public string Name { get; set; }

        public SessionName(string sessionId, string name = null) : base(sessionId)
        {
            Name = name;
        }
    }

    public class SessionTask
    {
        [JsonProperty("sessionId")]
        public string SessionId { get; set; }

        [JsonProperty("taskId", NullValueHandling = NullValueHandling.Ignore)]
        public string TaskId { get; set; }

        public SessionTask(string sessionId, string taskId = null)
        {
            SessionId = sessionId;
            TaskId = taskId;
        }
    }
}