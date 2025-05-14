using Newtonsoft.Json;

namespace LiteAgentSDK.DotNet.Models
{
    public class ApiKey
    {
        [JsonProperty("type")]
        public ApiKeyType Type { get; set; }

        [JsonProperty("apiKey")]
        public string ApiKeyString { get; set; }

        public ApiKey(ApiKeyType type, string apiKey)
        {
            Type = type;
            ApiKeyString = apiKey;
        }
    }

    public enum ApiKeyType
    {
        Basic,
        Bearer,
        Original
    }
}