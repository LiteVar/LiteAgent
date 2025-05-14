using System.Collections.Generic;
using Newtonsoft.Json;

namespace LiteAgentSDK.DotNet.Models
{
    public class FunctionCall
    {
        [JsonProperty("id")]
        public string Id { get; set; }

        [JsonProperty("name")]
        public string Name { get; set; }

        [JsonProperty("arguments")]
        public Dictionary<string, object> Arguments { get; set; }

        public FunctionCall(string id, string name, Dictionary<string, object> arguments)
        {
            Id = id;
            Name = name;
            Arguments = arguments;
        }
    }
    
    public class ToolReturn
    {
        [JsonProperty("id")]
        public string Id { get; set; }

        [JsonProperty("result")]
        public Dictionary<string, object> Result { get; set; }

        public ToolReturn(string id, Dictionary<string, object> result)
        {
            Id = id;
            Result = result;
        }
    }
}