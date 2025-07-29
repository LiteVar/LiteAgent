package com.litevar.agent.openai.completion;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import lombok.Data;

import java.util.List;

/**
 * @author uncle
 * @since 2025/2/13 15:29
 */
@Data
public class CompletionResponse {
    private String id;
    private String model;
    private List<Choices> choices;
    private Usage usage;

    @Data
    public static class Choices {
        /**
         * 结束原因
         * stop: if the model hit a natural stop point or a provided stop sequence;
         * length: if the maximum number of tokens specified in the request was reached;
         * content_filter: if content was omitted due to a flag from our content filters;
         * tool_calls: if the model called a tool.
         *
         * @see FinishReason
         */
        private String finishReason;
        /**
         * stream=false时,返回的字段名为message
         * stream=true时,返回的字段名为delta
         */
        @JsonAlias({"delta"})
        private AssistantMessage message;
    }

    @Data
    public static class Usage {
        private Integer completionTokens;
        private Integer promptTokens;
        private Integer totalTokens;
    }

    public static class FinishReason {
        public static final String STOP = "stop";
        public static final String LENGTH = "length";
        public static final String CONTENT_FILTER = "content_filter";
        public static final String TOOL_CALLS = "tool_calls";
    }

    public boolean isFunctionCalling() {
        return StrUtil.equals(choices.get(0).finishReason, FinishReason.TOOL_CALLS);
    }
}
