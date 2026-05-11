package com.litevar.agent.rest.agentflow;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.litevar.agent.base.dto.TokenReportDTO;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.openai.ObjectMapperSingleton;
import com.litevar.agent.openai.RequestExecutor;
import com.litevar.agent.openai.completion.CompletionRequestParam;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.message.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 上下文摘要与压缩
 *
 * @author uncle
 * @since 2026/01/22 16:54
 */
@Slf4j
public class ContextCompactor {
    private static final String SUMMARY_PREFIX = "[summary]";
    private static final String VISION_PROMPT = "Describe the image/video content concisely in the same language as the user. Output plain text only.";
    private static final String SUMMARY_PLACEHOLDER_USER_MESSAGE = "Please continue based on the current conversation summary.";
    private static final String SUMMARY_PROMPT_TEMPLATE = """
            You are a conversation summarizer.
            Input is a JSON object with fields: summary, history (array of {role, content}).
            Write in the same language as the conversation.
            Do not add new instructions or tool call requests.
            Put tool results into "Facts".
            Output only the summary body without the [summary] prefix, using this format:
            Facts:
            - ...
            Goals:
            - ...
            Constraints/Preferences:
            - ...
            Decisions:
            - ...
            TODO:
            - ...
            Key Data/IDs/Paths:
            - ...
            Keep within 1500 characters.
            """;

    /**
     * Build a compact context with rolling summary and recent turns.
     *
     * @param messages   full history messages
     * @param turnsCount keep latest N user turns
     * @param model      model to use for summarization
     * @return compacted messages for sending and storing
     */
    public List<Message> compress(List<Message> messages, int turnsCount, LlmModel model, TokenReportDTO tokenReport) {
        StringBuilder oldSummary = new StringBuilder();
        //保留最新turns轮会话
        List<Message> reserve = new ArrayList<>();
        //要摘要的消息
        List<Message> toSummarize = new ArrayList<>();

        pickMessage(messages, turnsCount, reserve, toSummarize, oldSummary);

        String newSummary = startSummary(oldSummary.toString(), toSummarize, model, tokenReport);
        if (StrUtil.isBlank(newSummary)) {
            log.info("摘要失败,返回原来的消息");
            return messages;
        }

        //摘要后结果: reserve + AssistantMessage(new summary content)
        List<Message> result = new ArrayList<>(reserve);
        AssistantMessage summaryMessage = new AssistantMessage();
        summaryMessage.setContent(SUMMARY_PREFIX + newSummary);
        result.add(summaryMessage);
        return result;
    }

    /**
     * 压缩当前task上下文,不保留原始turn,全部历史都进入摘要。
     *
     * @param messages         task历史消息
     * @param model            model to use for summarization
     * @param tokenUsageRecord token usage
     * @return compacted messages for current task
     */
    public List<Message> compressTask(List<Message> messages, LlmModel model, TokenReportDTO tokenReport) {
        if (messages.isEmpty()) {
            log.info("task上下文无可摘要消息,不再进行压缩");
            return null;
        }

        String newSummary = startSummary("", messages, model, tokenReport);
        if (StrUtil.isBlank(newSummary)) {
            log.info("task上下文摘要失败");
            return null;
        }

        AssistantMessage summaryMessage = new AssistantMessage();
        summaryMessage.setContent(newSummary);
        List<Message> list = new ArrayList<>();
        list.add(UserMessage.of("please continue"));
        list.add(summaryMessage);
        return list;
    }

    /**
     *
     * @param originMsg
     * @param turnsCount  保留N轮会话
     * @param reserve     保留最新N轮的会话
     * @param toSummarize 要摘要的消息
     */
    private void pickMessage(List<Message> originMsg, int turnsCount, List<Message> reserve,
                             List<Message> toSummarize, StringBuilder oldSummary) {
        int turns = turnsCount;
        DeveloperMessage systemMessage = null;

        for (int index = originMsg.size() - 1; index >= 0; index--) {
            Message message = originMsg.get(index);
            if (message instanceof DeveloperMessage developerMessage) {
                //system prompt不参与摘要
                systemMessage = developerMessage;
                continue;
            }
            if (message instanceof AssistantMessage assistantMessage) {
                String content = assistantMessage.getContent();
                if (StrUtil.isNotBlank(content) && content.trim().startsWith(SUMMARY_PREFIX)) {
                    // 仅保留最新的摘要消息，旧摘要不会进入摘要输入
                    oldSummary.append(StrUtil.removePrefix(content.trim(), SUMMARY_PREFIX));
                    continue;
                }
            }
            if (turns > 0) {
                reserve.add(message);
                if (message instanceof UserMessage) {
                    turns--;
                }
            } else {
                toSummarize.add(message);
            }
        }
        if (toSummarize.isEmpty()) {
            log.info("保留{}轮会话后,要摘要的消息为空,说明上下文要超了,保留的会话也要做摘要", turnsCount - turns);
            //这种情况摘要完成后,要补一条user message占位,不然会后面如果是tool call,会报错,No user query found in messages.
            toSummarize.addAll(reserve);
            reserve.clear();
            reserve.add(UserMessage.of(SUMMARY_PLACEHOLDER_USER_MESSAGE));
        }
        if (systemMessage != null) {
            reserve.add(systemMessage);
        }
        Collections.reverse(toSummarize);
        Collections.reverse(reserve);
    }

    private String startSummary(String oldSummary, List<Message> toSummarize, LlmModel model, TokenReportDTO tokenReport) {
        //图片/视频转文字
        transformMultiMedia(toSummarize, model, tokenReport);

        Dict input = Dict.create().set("summary", oldSummary).set("history", buildSummaryItems(toSummarize));

        String payload;
        try {
            payload = ObjectMapperSingleton.getObjectMapper().writeValueAsString(input);
        } catch (JsonProcessingException e) {
            log.error("summary json failure", e);
            return null;
        }

        log.info("开始摘要,输入内容:{}", payload);

        CompletionRequestParam param = new CompletionRequestParam();
        param.setModel(model.getName());
        param.setMessages(List.of(DeveloperMessage.of(SUMMARY_PROMPT_TEMPLATE),
                UserMessage.of(payload)));
        param.setTokenReport(tokenReport);

        try {
            CompletionResponse response = RequestExecutor.doRequest(param, model.getBaseUrl(), model.getApiKey());
            if (!StrUtil.equals(response.getChoices().get(0).getFinishReason(), CompletionResponse.FinishReason.STOP)) {
                log.error("[摘要]失败,LLM输出结束:{}", response.getChoices().get(0).getFinishReason());
                return null;
            }
            String content = response.getChoices().get(0).getMessage().getContent();
            if (StrUtil.isBlank(content)) {
                return null;
            }
            String summary = content.trim();
            summary = StrUtil.removePrefix(summary, SUMMARY_PREFIX);
            log.info("摘要结果:{}", summary);
            return summary;
        } catch (Exception ex) {
            log.error("摘要失败", ex);
            return null;
        }
    }

    private List<SummaryItem> buildSummaryItems(List<Message> messages) {
        List<SummaryItem> items = new ArrayList<>();
        for (Message msg : messages) {
            SummaryItem item;
            if (msg instanceof UserMessage userMessage) {
                item = new SummaryItem("user", userMessage.getContent().toString());
            } else if (msg instanceof AssistantMessage assistantMessage) {
                String content;
                if (assistantMessage.hasToolCalls()) {
                    content = JSONUtil.toJsonStr(assistantMessage.getToolCalls());
                } else {
                    content = assistantMessage.getContent();
                }
                item = new SummaryItem("assistant", content);
            } else if (msg instanceof ToolMessage toolMessage) {
                item = new SummaryItem("tool", toolMessage.getContent());
            } else {
                continue;
            }
            items.add(item);
        }
        return items;
    }

    /**
     * 把图片/视频消息转为文字
     */
    private void transformMultiMedia(List<Message> toSummarize, LlmModel model, TokenReportDTO tokenReport) {
        for (Message msg : toSummarize) {
            if (msg instanceof UserMessage userMessage && userMessage.getContent() instanceof List<?> list) {
                List<UserMessage.ContentType> contents = new ArrayList<>();
                for (Object obj : list) {
                    if (obj instanceof UserMessage.TextContentType textType && StrUtil.isNotBlank(textType.getText())) {
                        contents.add(textType);
                    } else if (obj instanceof UserMessage.ImageContentType imageType) {
                        contents.add(imageType);
                    } else if (obj instanceof UserMessage.VideoContentType videoType) {
                        contents.add(videoType);
                    }
                }
                if (!contents.isEmpty()) {
                    log.info("检测到有图片/视频,摘要前转为文字");
                    CompletionRequestParam param = new CompletionRequestParam();
                    param.setModel(model.getName());
                    param.setMessages(List.of(DeveloperMessage.of(VISION_PROMPT), UserMessage.of(contents)));
                    param.setTokenReport(tokenReport);
                    CompletionResponse response = RequestExecutor.doRequest(param, model.getBaseUrl(), model.getApiKey());
                    if (!StrUtil.equals(response.getChoices().get(0).getFinishReason(), CompletionResponse.FinishReason.STOP)) {
                        log.error("[摘要]图片/视频转文字失败,LLM输出结束:{}", response.getChoices().get(0).getFinishReason());
                        return;
                    }
                    String text = response.getChoices().get(0).getMessage().getContent();
                    //替换为文本
                    userMessage.setContent("[Media]" + text);
                }
            }
        }
    }

    private record SummaryItem(String role, String content) {
    }
}
