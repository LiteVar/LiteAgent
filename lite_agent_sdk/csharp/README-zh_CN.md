# LiteAgent SDK for C# (.NET Standard 和 .NET Framework)

[English](README.md) · 中文

LiteAgent的C# SDK，用于 .NET Standard 和 .NET Framework 应用程序中与LiteAgent进行交互。

## 功能

- 初始化Agent的会话
- 向Agent发送客户端消息
- 订阅Agent消息，包括：Agent消息、逐个吐字时的chunk消息、SSE的Done和Error、Function Call的回调请求
- 发送Function Call的Callback结果
- 停止当前会话
- 清除当前会话

## 安装

在 `NuGet` 文件中增加如下依赖：

```
LiteAgent
```

## 使用

- 实现AgentMessageHandler，用以订阅Agent各类推送消息

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