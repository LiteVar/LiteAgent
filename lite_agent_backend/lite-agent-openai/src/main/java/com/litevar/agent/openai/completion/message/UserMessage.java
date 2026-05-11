package com.litevar.agent.openai.completion.message;

import com.litevar.agent.openai.completion.Role;
import lombok.Data;

import java.util.List;

/**
 * 用户消息
 *
 * @author uncle
 * @since 2025/2/13 12:20
 */
@Data
public class UserMessage implements Message {
    private final Role role = Role.USER;
    private Object content;
    private String name;

    @Override
    public Role getRole() {
        return role;
    }

    public static UserMessage of(String content) {
        UserMessage msg = new UserMessage();
        msg.setContent(content);
        return msg;
    }

    public static UserMessage of(List<ContentType> content) {
        UserMessage msg = new UserMessage();
        msg.setContent(content);
        return msg;
    }

    public interface ContentType {

    }

    @Data
    public static class TextContentType implements ContentType {
        private String type = "text";
        private String text;

        public static TextContentType of(String text) {
            TextContentType textContentType = new TextContentType();
            textContentType.text = text;
            return textContentType;
        }
    }

    @Data
    public static class ImageContentType implements ContentType {
        private String type = "image_url";
        private UrlBody imageUrl;

        public static ImageContentType of(String imageUrl) {
            ImageContentType imageContentType = new ImageContentType();
            imageContentType.imageUrl = UrlBody.of(imageUrl);
            return imageContentType;
        }
    }

    @Data
    public static class VideoContentType implements ContentType {
        private String type = "video_url";
        private UrlBody videoUrl;

        public static VideoContentType of(String videoUrl) {
            VideoContentType videoContentType = new VideoContentType();
            videoContentType.videoUrl = UrlBody.of(videoUrl);
            return videoContentType;
        }
    }

    @Data
    public static class UrlBody {
        private String url;

        public static UrlBody of(String url) {
            UrlBody urlBody = new UrlBody();
            urlBody.url = url;
            return urlBody;
        }
    }
}
