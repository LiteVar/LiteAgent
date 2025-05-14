# LiteAgent SDK for C# (.NET Standard and .NET Framework)

English · [中文](README-zh_CN.md)

The LiteAgent C# SDK is used for interacting with LiteAgent in .NET Standard and .NET Framework applications.

## Features

- Initialize an Agent session
- Send client messages to the Agent
- Subscribe to Agent messages, including: Agent messages, chunk messages during word-by-word typing, SSE Done and Error, and Function Call callback requests
- Send Function Call callback results
- Stop the current session
- Clear the current session

## Installation

Add the following dependency in your `NuGet`:

```
LiteAgent
```

## Usage

- Implement AgentMessageHandler to subscribe to various Agent push messages

```csharp
namespace SdkExample
{
    internal static class Program
    {
        private static async Task Main(string[] args)
        {
            const string baseUrl = "<BaseUrl, eg. https://www.liteagent.cn/liteAgent/v1>";
            const string apiKey = "<Agent access apiKey from LiteAgent>";
            const string userPrompt = "Hi";
            const string agentId = "<AgentId from LiteAgent>";

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
```