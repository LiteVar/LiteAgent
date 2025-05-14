
using System;

namespace LiteAgentSDK.DotNet
{
    public class FunctionNotSupportedException : Exception
    {
        private readonly string _functionName;

        public FunctionNotSupportedException(string functionName)
            : base($"Function '{functionName}' not supported.")
        {
            _functionName = functionName;
        }

        public string ToJson()
        {
            return $"{{ \"error\": \"Function '{_functionName}' not supported.\" }}";
        }
    }
}