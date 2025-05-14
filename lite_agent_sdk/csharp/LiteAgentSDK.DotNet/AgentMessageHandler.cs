using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using LiteAgentSDK.DotNet.Models;

namespace LiteAgentSDK.DotNet
{
    public abstract class AgentMessageHandler
    {
        private ToolReturn _toolReturn;

        /// <summary>
        /// When the agent calls a native function, this will be called.
        /// </summary>
        public virtual async Task<ToolReturn> OnFunctionCallAsync(string sessionId, FunctionCall functionCall)
        {
            return await Task.Run(() =>
            {
                var result = new Dictionary<string, object>
                {
                    { "error", new FunctionNotSupportedException(functionCall.Name).ToJson() }
                };

                _toolReturn = new ToolReturn(functionCall.Id, result);
                return _toolReturn;
            });
        }

        /// <summary>
        /// When the SSE connection receives a message, this will be called.
        /// </summary>
        public abstract Task OnMessageAsync(string sessionId, AgentMessage agentMessage);

        /// <summary>
        /// When the SSE connection receives a chunk, this will be called.
        /// </summary>
        public abstract Task OnChunkAsync(string sessionId, AgentMessageChunk agentMessageChunk);

        /// <summary>
        /// When the SSE connection is done, this will be called.
        /// </summary>
        public abstract Task OnDoneAsync();

        /// <summary>
        /// When there is an SSE error, this will be called.
        /// </summary>
        public abstract Task OnErrorAsync(Exception e);
    }
}