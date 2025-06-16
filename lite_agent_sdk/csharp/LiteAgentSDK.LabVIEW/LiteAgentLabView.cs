using LiteAgentSDK.DotNet;
using LiteAgentSDK.DotNet.Models;
using Newtonsoft.Json;
using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;

namespace LiteAgentSDK.Labview
{
    public class LiteAgentLabView
    {
        private LiteAgentSdk _sdk;
        private readonly ConcurrentDictionary<string, TaskCompletionSource<ToolReturn>> _pendingCalls = new ConcurrentDictionary<string, TaskCompletionSource<ToolReturn>>();

        public LiteAgentLabView(string baseUrl, string apiKey)
        {
            _sdk = new LiteAgentSdk(baseUrl, apiKey);
        }

        public string GetVersion()
        {
            EnsureInitialized();

            var task = _sdk.GetVersion();
            task.Wait();
            if (task.Result == null) throw new InvalidOperationException("Result is null. SDK may not have responded correctly.");
            return JsonConvert.SerializeObject(task.Result);
        }

        public string InitSession(string agentId)
        {
            EnsureInitialized();

            var task = _sdk.InitSession(agentId);
            task.Wait();
            if (task.Result == null) throw new InvalidOperationException("Result is null. SDK may not have responded correctly.");
            return JsonConvert.SerializeObject(task.Result);
        }

        public void Chat(string sessionId, string userTaskJson, AgentMessageEventHandler handler)
        {
            EnsureInitialized();

            var session = new Session(sessionId);
            var labviewMessageHandler = new LabVIEWMessageHandler(this._pendingCalls, handler);
            var userTaskUtf8 = TryFixLabVIEWString(userTaskJson);
            var userTask = JsonConvert.DeserializeObject<UserTask>(userTaskUtf8);
            var task = _sdk.Chat(session, userTask, labviewMessageHandler);
            task.Wait();
        }

        public string GetHistory(string sessionId)
        {
            EnsureInitialized();

            var task = _sdk.GetHistory(sessionId);
            task.Wait();

            if (task.Result == null) throw new InvalidOperationException("Result is null. SDK may not have responded correctly.");

            return JsonConvert.SerializeObject(task.Result);
        }

        public void Stop(string sessionId, string taskId)
        {
            EnsureInitialized();

            string currTaskId = null;
            if(taskId != null &&  taskId.Length > 0) currTaskId = taskId;

            var task = _sdk.Stop(sessionId, currTaskId);
            task.Wait();
        }

        public void Clear(string sessionId)
        {
            EnsureInitialized();

            var task = _sdk.Clear(sessionId);
            task.Wait();
        }

        public void Callback(string toolReturnJson)
        {
            var toolReturn = JsonConvert.DeserializeObject<ToolReturn>(toolReturnJson);
            if (_pendingCalls.TryRemove(toolReturn.Id, out var tcs))
            {
                tcs.SetResult(toolReturn);
            }
        }

        private void EnsureInitialized()
        {
            if (_sdk == null) throw new InvalidOperationException("LiteAgentSdk is not initialized. Please call Init() first.");
        }

        private string TryFixLabVIEWString(string lvText)
        {
            // Step 1: Turn text to  bytes
            byte[] bytes = Encoding.Default.GetBytes(lvText); // Maybe GBK ro Latin-1

            // Step 2: decode by UTF-8
            return Encoding.UTF8.GetString(bytes);
        }

    }

    public class StringEventArgs : EventArgs
    {
        public string Json { get; set; }
    }

    public class AgentMessageEventHandler
    {
        public event EventHandler<StringEventArgs> OnEvent;

        public void EmitEvent(string json)
        {
            OnEvent?.Invoke(this, new StringEventArgs { Json = json });
        }
    }

    internal class LabVIEWMessageHandler : AgentMessageHandler
    {
        private readonly ConcurrentDictionary<string, TaskCompletionSource<ToolReturn>> _pendingCalls;
        private readonly AgentMessageEventHandler _handler;

        public LabVIEWMessageHandler(ConcurrentDictionary<string, TaskCompletionSource<ToolReturn>> pendingCalls, AgentMessageEventHandler handler)
        {
            _pendingCalls = pendingCalls;
            _handler = handler;
        }

        public override async Task<ToolReturn> OnFunctionCallAsync(string sessionId, FunctionCall functionCall)
        {
            var tcs = new TaskCompletionSource<ToolReturn>();

            _pendingCalls[functionCall.Id] = tcs;

            var json = JsonConvert.SerializeObject(new
            {
                type = "OnFunctionCall",
                sessionId,
                payload = functionCall
            });

            _handler.EmitEvent(json);

            return await tcs.Task;
        }

        public override async Task OnChunkAsync(string sessionId, AgentMessageChunk agentMessageChunk)
        {
            var json = JsonConvert.SerializeObject(new
            {
                type = "OnChunk",
                sessionId,
                payload = agentMessageChunk
            });
            _handler.EmitEvent(json);
            await Task.CompletedTask;
        }

        public override async Task OnDoneAsync()
        {
            var json = "{\"type\": \"OnDone\"}";
            _handler.EmitEvent(json);
            await Task.CompletedTask;
        }

        public override async Task OnErrorAsync(Exception e)
        {
            var json = JsonConvert.SerializeObject(new
            {
                type = "OnError",
                error = e.Message
            });

            _handler.EmitEvent(json);
            await Task.CompletedTask;
        }

        public override async Task OnMessageAsync(string sessionId, AgentMessage agentMessage)
        {
            var json = JsonConvert.SerializeObject(new
            {
                type = "OnMessage",
                sessionId,
                payload = agentMessage
            });
            _handler.EmitEvent(json);
            await Task.CompletedTask;
        }
    }

}
