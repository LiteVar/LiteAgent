using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using LaunchDarkly.EventSource;
using LiteAgentSDK.DotNet.Models;
using Newtonsoft.Json;

namespace LiteAgentSDK.DotNet.Clients
{
    public class SseService
    {
        private readonly string _baseUrl;
        private readonly string _apiKey;
        private EventSource _eventSource;

        public SseService(string baseUrl, string apiKey = null)
        {
            _baseUrl = baseUrl;
            _apiKey = apiKey;
        }

        public async Task ChatAsync(string sessionId, UserTask userTask, Func<MessageEvent, Task> onEvent, Func<Exception, Task> onError, Func<Task> onClosed)
        {
            var uri = new Uri($"{_baseUrl}/chat?sessionId={Uri.EscapeDataString(sessionId)}");

            var headers = new Dictionary<string, string>
            {
                ["Accept"] = "text/event-stream"
            };
            if (!string.IsNullOrEmpty(_apiKey))
            {
                headers["Authorization"] = $"Bearer {_apiKey}";
            }

            var body = JsonConvert.SerializeObject(userTask);

            var config = Configuration.Builder(uri)
                .Method(HttpMethod.Post)
                .RequestHeaders(headers)
                .RequestBody(body, "application/json")
                .ResponseStartTimeout(TimeSpan.FromMinutes(10))
                .Build();
            _eventSource = new EventSource(config);

            _eventSource.MessageReceived += async (sender, e) =>
            {
                await onEvent(e.Message);
            };

            _eventSource.Error += async (sender, e) =>
            {
                await onError(e.Exception);
            };

            _eventSource.Closed += async (sender, e) =>
            {
                if (_eventSource.ReadyState == ReadyState.Closed)
                {
                    await onClosed();
                    _eventSource.Dispose();
                }
            };

            await _eventSource.StartAsync();
        }
    }
}