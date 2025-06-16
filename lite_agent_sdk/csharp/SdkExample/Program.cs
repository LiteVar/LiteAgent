using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using LiteAgentSDK.DotNet;
using LiteAgentSDK.DotNet.Models;
using Newtonsoft.Json;

namespace SdkExample
{
    internal static class Program
    {
        private static async Task Main(string[] args)
        {
            const string baseUrl = "https://www.liteagent.cn/liteAgent/v1";//"<BaseUrl, eg. https://www.liteagent.cn/liteAgent/v1>";
            const string apiKey = "sk-07d063af326d4975b4e5c9bde939a5bc";//"<Agent access apiKey from LiteAgent>";
            const string userPrompt = "你能做什么？";//"Hi";
            const string agentId = "1920035450164170754";//"<AgentId from LiteAgent>";

            var liteAgent = new LiteAgentSdk(baseUrl, apiKey);
            var version = await liteAgent.GetVersion();
            Console.WriteLine(" version:" + JsonConvert.SerializeObject(version));
            var session = await liteAgent.InitSession(agentId);
            var userTask = new UserTask(null, new List<Content>() { new Content("text",userPrompt) },true);
            Console.WriteLine(" userTask:" + JsonConvert.SerializeObject(userTask));
            var agentMessageHandler = new AgentMessageHandlerImpl();
            await liteAgent.Chat(session, userTask, agentMessageHandler);

            await SleepAsync(20);
            
            var agentMessages = await liteAgent.GetHistory(sessionId: session.SessionId);
            Console.WriteLine(" agentMessageHistory:" + JsonConvert.SerializeObject(agentMessages));
        }

        private static async Task SleepAsync(int seconds)
        {
            for (var i = seconds; i > 0; i--)
            {
                Console.WriteLine(i);
                await Task.Delay(1000);
            }
        }
    }

    public class AgentMessageHandlerImpl : AgentMessageHandler
    {
        public override async Task OnDoneAsync()
        {
            Console.WriteLine("[onDone]");
        }

        public override async Task OnErrorAsync(Exception e)
        {
            Console.WriteLine($"[onError]: {e}");
        }

        public override async Task OnMessageAsync(string sessionId, AgentMessage agentMessage)
        {
            Console.WriteLine($"sessionId: {sessionId}, agentMessage: {JsonConvert.SerializeObject(agentMessage)}");
        }

        public override async Task OnChunkAsync(string sessionId, AgentMessageChunk agentMessageChunk)
        {
            Console.WriteLine($"sessionId: {sessionId}, agentMessageChunk: {JsonConvert.SerializeObject(agentMessageChunk)}");
        }

        public override async Task<ToolReturn> OnFunctionCallAsync(string sessionId, FunctionCall functionCall)
        {
            Console.WriteLine($"sessionId: {sessionId}, functionCall: {JsonConvert.SerializeObject(functionCall)}");
            var result = new Dictionary<string, object>
            {
                { "result", "success" }
            };
            var toolReturn = new ToolReturn(functionCall.Id, result);
            return toolReturn;
        }
    }
}