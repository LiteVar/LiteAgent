using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;
using LiteAgentSDK.DotNet.Clients;
using LiteAgentSDK.DotNet.Models;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Refit;

namespace LiteAgentSDK.DotNet
{
    public class LiteAgentSdk
    {
        private readonly IHttpService _httpService;
        private readonly SseService _sseService;

        public LiteAgentSdk(string baseUrl, string apiKey = null)
        {
            ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls | SecurityProtocolType.Tls11 | SecurityProtocolType.Tls12;
            
            var httpClient = new HttpClient
            {
                BaseAddress = new Uri(baseUrl)
            };
        
            if (!string.IsNullOrEmpty(apiKey))
            {
                httpClient.DefaultRequestHeaders.Add("Authorization", $"Bearer {apiKey}");
            }
            
            var refitSettings = new RefitSettings
            {
                ContentSerializer = new NewtonsoftJsonContentSerializer()
            };
        
            _httpService = RestService.For<IHttpService>(httpClient, refitSettings);
            _sseService = new SseService(baseUrl, apiKey);
        }

        public async Task<VersionModel> GetVersion()
        {
            var version = await _httpService.GetVersion();
            return version;
        }

        public async Task<Session> InitSession(string agentId)
        {
            return await _httpService.InitSession(agentId);
        }

        public async Task Chat(Session session, UserTask userTask, AgentMessageHandler agentMessageHandler)
        {
            await _sseService.ChatAsync(session.SessionId, userTask, 
                onEvent: async (eventMessage) =>
                {
                    if (eventMessage.Name == SseEventType.Message)
                    {
                        var agentMessage = JsonConvert.DeserializeObject<AgentMessage>(eventMessage.Data);
                        if (agentMessage != null) await agentMessageHandler.OnMessageAsync(session.SessionId, agentMessage);
                    }
                    else if (eventMessage.Name == SseEventType.Chunk)
                    {
                        var agentMessageChunk = JsonConvert.DeserializeObject<AgentMessageChunk>(eventMessage.Data);
                        if (agentMessageChunk != null) await agentMessageHandler.OnChunkAsync(session.SessionId, agentMessageChunk);
                    }
                    else if (eventMessage.Name == SseEventType.FunctionCall)
                    {
                        var agentMessage = JsonConvert.DeserializeObject<AgentMessage>(eventMessage.Data);
                        
                        if (agentMessage.Type == AgentMessageType.FunctionCall)
                        {
                            if (agentMessage.Content is JObject content)
                            {
                                var functionCall = content.ToObject<FunctionCall>();
                                var toolReturn = await agentMessageHandler.OnFunctionCallAsync(session.SessionId, functionCall);
                                await _httpService.Callback(session.SessionId, toolReturn);
                            }
                        }
                    }
                },
                onError: async exception =>
                {
                    await agentMessageHandler.OnErrorAsync(exception);
                },
                  onClosed: async () =>
                {
                    await agentMessageHandler.OnDoneAsync();
                }
            );
        }

        public async Task<List<AgentMessage>> GetHistory(string sessionId)
        {
            return await _httpService.GetHistory(sessionId: sessionId);
        }
        
        public async Task Stop(string sessionId, string taskId)
        {
            await _httpService.Stop(sessionId: sessionId, taskId: taskId);
        }
        
        public async Task Clear(string sessionId)
        {
            await _httpService.Clear(sessionId: sessionId);
        }
    }
}