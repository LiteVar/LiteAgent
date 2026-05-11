package com.litevar.agent.rest.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.openai.completion.message.AssistantMessage;
import com.litevar.agent.openai.completion.message.DeveloperMessage;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.openai.completion.message.ToolMessage;
import com.litevar.agent.openai.completion.message.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Message token counting
 *
 * @author uncle
 * @since 2026/01/23 17:20
 */
public class MessageTokenUtil {

    public static int countTokens(List<Message> messages) {
        int total = 0;
        for (Message msg : messages) {
            String content = extractContentForToken(msg);
            total += countTokens(content);
        }
        return total;
    }

    public static int countTokens(String content) {
        if (StrUtil.isBlank(content)) {
            return 0;
        }
        return TikToken.countTokens(content);
    }

    private static String extractContentForToken(Message msg) {
        if (msg instanceof UserMessage userMessage) {
            Object content = userMessage.getContent();
            if (content == null) {
                return "";
            }
            if (content instanceof String) {
                return (String) content;
            }
            if (content instanceof List<?> list) {
                List<String> userContentList = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof UserMessage.TextContentType textContentType) {
                        String textContent = textContentType.getText().trim();
                        if (StrUtil.isNotBlank(textContent)) {
                            userContentList.add(textContent);
                        }
                    } else if (item instanceof UserMessage.ImageContentType imageContentType) {
                        if (imageContentType.getImageUrl() != null) {
                            userContentList.add(imageContentType.getImageUrl().getUrl());
                        }
                    } else if (item instanceof UserMessage.VideoContentType videoContentType) {
                        if (videoContentType.getVideoUrl() != null) {
                            userContentList.add(videoContentType.getVideoUrl().getUrl());
                        }
                    }
                }
                return StrUtil.join("\n", userContentList);
            }
            return content.toString();
        }
        if (msg instanceof AssistantMessage assistantMessage) {
            String toolCall = "";
            if (assistantMessage.hasToolCalls()) {
                toolCall = JSONUtil.toJsonStr(assistantMessage.getToolCalls());
            }
            return assistantMessage.getContent() + toolCall;
        }
        if (msg instanceof ToolMessage toolMessage) {
            return toolMessage.getContent();
        }
        if (msg instanceof DeveloperMessage developerMessage) {
            return developerMessage.getContent();
        }
        return "";
    }
}
