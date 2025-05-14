using Newtonsoft.Json;

namespace LiteAgentSDK.DotNet.Models
{
    public class VersionModel
    {
        [JsonProperty("version")]
        public string Version { get; set; }

        public VersionModel(string version)
        {
            Version = version;
        }
    }
}