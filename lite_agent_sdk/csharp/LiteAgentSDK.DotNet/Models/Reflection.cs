using System.Collections.Generic;
using Newtonsoft.Json;

namespace LiteAgentSDK.DotNet.Models
{
    public class ReflectScore
    {
        [JsonProperty("score")]
        public int Score { get; set; }

        [JsonProperty("description")]
        public string Description { get; set; }

        public ReflectScore(int score, string description = null)
        {
            Score = score;
            Description = description;
        }
    }

    public class MessageScore
    {
        [JsonProperty("content")]
        public List<Content> Content { get; set; }

        [JsonProperty("messageType")]
        public string MessageType { get; set; }

        [JsonProperty("message")]
        public string Message { get; set; }

        [JsonProperty("reflectScoreList")]
        public List<ReflectScore> ReflectScoreList { get; set; }

        public MessageScore(List<Content> content, string messageType, string message, List<ReflectScore> reflectScoreList)
        {
            Content = content;
            MessageType = messageType;
            Message = message;
            ReflectScoreList = reflectScoreList;
        }
    }

    public class Reflection
    {
        [JsonProperty("isPass")]
        public bool IsPass { get; set; }

        [JsonProperty("messageScore")]
        public MessageScore MessageScore { get; set; }

        [JsonProperty("passScore")]
        public int PassScore { get; set; }

        [JsonProperty("count")]
        public int Count { get; set; }

        [JsonProperty("maxCount")]
        public int MaxCount { get; set; }

        public Reflection(bool isPass, MessageScore messageScore, int passScore, int count, int maxCount)
        {
            IsPass = isPass;
            MessageScore = messageScore;
            PassScore = passScore;
            Count = count;
            MaxCount = maxCount;
        }
    }
}