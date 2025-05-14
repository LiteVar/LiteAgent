using System.Collections.Generic;
using System.Threading.Tasks;
using LiteAgentSDK.DotNet.Models;
using Refit;

namespace LiteAgentSDK.DotNet.Clients
{
    public interface IHttpService
    {
        // Get version
        [Get("/version")]
        Task<VersionModel> GetVersion([Header("Accept")] string accept = "application/json");

        // Initialize session
        [Post("/initSession")]
        Task<Session> InitSession([Query("agentId")] string agentId, [Header("Accept")] string accept = "application/json");

        // Callback
        [Post("/callback")]
        Task Callback([Query("sessionId")] string sessionId, [Body] ToolReturn toolReturn);

        // Get history
        [Get("/history")]
        Task<List<AgentMessage>> GetHistory([Query("sessionId")] string sessionId, [Header("Accept")] string accept = "application/json");

        // Stop
        [Get("/stop")]
        Task Stop([Query("sessionId")] string sessionId, [Query("taskId")] string taskId);

        // Clear
        [Get("/clear")]
        Task Clear([Query("sessionId")] string sessionId);
    }
}