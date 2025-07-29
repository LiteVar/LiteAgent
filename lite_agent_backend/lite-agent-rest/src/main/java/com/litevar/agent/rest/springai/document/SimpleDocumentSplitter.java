package com.litevar.agent.rest.springai.document;

import lombok.Getter;
import org.springframework.ai.document.Document;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Spring AI Document文本分隔器
 * 支持多级分割、智能合并和重叠处理
 */
@Getter
public class SimpleDocumentSplitter {

    // Getters
    private final String separator;
    private final int chunkSize;
    private final int overlapSize;

    /**
     * 构造函数
     * @param separator 分隔符
     * @param chunkSize 文本块大小
     * @param overlapSize 重叠大小（可选，默认为chunkSize/10）
     */
    public SimpleDocumentSplitter(String separator, int chunkSize, Integer overlapSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize必须大于0");
        }

        this.separator = separator != null ? separator : "\n";
        this.chunkSize = chunkSize;
        this.overlapSize = overlapSize != null ? overlapSize : Math.max(0, chunkSize / 10);

        if (this.overlapSize >= chunkSize) {
            throw new IllegalArgumentException("overlapSize不能大于或等于chunkSize");
        }
    }

    /**
     * 构造函数（使用默认重叠大小）
     */
    public SimpleDocumentSplitter(String separator, int chunkSize) {
        this(separator, chunkSize, null);
    }

    /**
     * 分割文档列表的主方法
     * @param documents 要分割的文档列表
     * @return 分割后的文档列表
     */
    public List<Document> split(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        List<Document> result = new ArrayList<>();

        for (Document document : documents) {
            List<Document> splitDocuments = splitDocument(document);
            result.addAll(splitDocuments);
        }

        return result;
    }

    /**
     * 分割单个文档
     * @param document 要分割的文档
     * @return 分割后的文档列表
     */
    public List<Document> splitDocument(Document document) {
        if (document == null || document.getText() == null || document.getText().isEmpty()) {
            return Collections.singletonList(document);
        }

        String text = document.getText();
        Map<String, Object> originalMetadata = document.getMetadata();

        // 分割文本内容
        List<String> textChunks = splitText(text);

        // 转换为Document对象列表
        List<Document> documentChunks = new ArrayList<>();

        for (String chunk : textChunks) {
            // 创建新的元数据
//            DocumentMetadata chunkMetadata = createChunkMetadata(originalMetadata, i, textChunks.size(), chunk);

            // 创建新的Document
            Document chunkDocument = new Document(chunk, originalMetadata);
            documentChunks.add(chunkDocument);
        }

        return documentChunks;
    }

//    /**
//     * 创建分块的元数据
//     * @param originalMetadata 原始文档元数据
//     * @param chunkIndex 分块索引
//     * @param totalChunks 总分块数
//     * @param content 分块内容
//     * @return 新的元数据
//     */
//    private Map<String, Object> createChunkMetadata(Map<String, Object> originalMetadata,
//                                                 int chunkIndex, int totalChunks, String content) {
//        Map<String, Object> metadataMap = new HashMap<>();
//
//        // 复制原始元数据
//        if (originalMetadata != null && originalMetadata.toMap() != null) {
//            metadataMap.putAll(originalMetadata.toMap());
//        }
//
//        // 添加分块相关元数据
//        metadataMap.put("chunk_index", chunkIndex);
////        metadataMap.put("chunk_total", totalChunks);
////        metadataMap.put("chunk_size", content.length());
////        metadataMap.put("chunk_id", generateChunkId(originalMetadata, chunkIndex));
////        metadataMap.put("is_chunked", true);
////        metadataMap.put("chunking_strategy", "semantic_overlap");
////        metadataMap.put("overlap_size", overlapSize);
////        metadataMap.put("separator", separator);
//
//        // 添加原始文档标识
//        if (originalMetadata != null && originalMetadata.get("id") != null) {
//            metadataMap.put("parent_document_id", originalMetadata.get("id"));
//        } else {
//            metadataMap.put("parent_document_id", "unknown");
//        }
//
//        return new DocumentMetadata(metadataMap);
//    }

//    /**
//     * 生成分块ID
//     */
//    private String generateChunkId(DocumentMetadata originalMetadata, int chunkIndex) {
//        String parentId = "doc";
//        if (originalMetadata != null && originalMetadata.get("id") != null) {
//            parentId = originalMetadata.get("id").toString();
//        }
//        return parentId + "_chunk_" + chunkIndex;
//    }

    /**
     * 分割文本的核心逻辑（从原有的字符串分隔器中提取）
     */
    private List<String> splitText(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        // 第一步：根据分隔符进行初始分割
        List<String> initialChunks = initialSplit(text);

        // 第二步：处理超长片段和合并短片段
//        List<String> processedChunks = processChunks(initialChunks);
        return processChunks(initialChunks);

//        // 第三步：添加重叠内容
//        return addOverlap(processedChunks);
    }

    // 以下方法与之前的TextSplitter实现相同
    private List<String> initialSplit(String text) {
        List<String> chunks = new ArrayList<>();

        if (separator.isEmpty()) {
            chunks.add(text);
            return chunks;
        }

        String[] parts = text.split(Pattern.quote(separator), -1);

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (!part.isEmpty()) {
                chunks.add(part);
            } else if (i < parts.length - 1) {
                chunks.add("");
            }
        }

        return chunks;
    }

    private List<String> processChunks(List<String> chunks) {
        List<String> result = new ArrayList<>();

        for (String chunk : chunks) {
            if (chunk.isEmpty()) {
                continue;
            }

            if (chunk.length() > chunkSize) {
                result.addAll(splitLongChunk(chunk));
            } else {
                result.add(chunk);
            }
        }

        return mergeShortChunks(result);
    }

    private List<String> splitLongChunk(String chunk) {
        List<String> subChunks = new ArrayList<>();
        List<String> sentences = splitBySentence(chunk);
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (sentence.length() > chunkSize) {
                if (currentChunk.length() > 0) {
                    subChunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                subChunks.addAll(forceSplit(sentence));
            } else if (currentChunk.length() + sentence.length() + 1 <= chunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            } else {
                if (currentChunk.length() > 0) {
                    subChunks.add(currentChunk.toString().trim());
                }
                currentChunk = new StringBuilder(sentence);
            }
        }

        if (currentChunk.length() > 0) {
            subChunks.add(currentChunk.toString().trim());
        }

        return subChunks;
    }

    private List<String> splitBySentence(String text) {
        String[] sentences = text.split("(?<=[.!?。！？])\\s+");
        List<String> result = new ArrayList<>();

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (!sentence.isEmpty()) {
                result.add(sentence);
            }
        }

        return result.isEmpty() ? Arrays.asList(text) : result;
    }

    private List<String> forceSplit(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            if (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
                int wordBoundary = text.lastIndexOf(' ', end);
                if (wordBoundary > start) {
                    end = wordBoundary;
                }
            }

            chunks.add(text.substring(start, end).trim());
            start = end;

            while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
        }

        return chunks;
    }

    private List<String> mergeShortChunks(List<String> chunks) {
        if (chunks.isEmpty()) {
            return chunks;
        }

        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String chunk : chunks) {
            if (chunk.isEmpty()) {
                continue;
            }

            if (current.length() == 0) {
                current.append(chunk);
            } else if (current.length() + chunk.length() + 1 <= chunkSize) {
                current.append(" ").append(chunk);
            } else {
                merged.add(current.toString());
                current = new StringBuilder(chunk);
            }
        }

        if (current.length() > 0) {
            merged.add(current.toString());
        }

        return merged;
    }

    private List<String> addOverlap(List<String> chunks) {
        if (chunks.size() <= 1 || overlapSize == 0) {
            return chunks;
        }

        List<String> result = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            StringBuilder overlappedChunk = new StringBuilder();

            // 添加前面的重叠内容
            if (i > 0) {
                String prevChunk = chunks.get(i - 1);
                String prevOverlap = extractOverlap(prevChunk, false);
                if (!prevOverlap.isEmpty()) {
                    overlappedChunk.append(prevOverlap).append(" ");
                }
            }

            // 添加当前块
            overlappedChunk.append(chunk);

            // 添加后面的重叠内容
            if (i < chunks.size() - 1) {
                String nextChunk = chunks.get(i + 1);
                String nextOverlap = extractOverlap(nextChunk, true);
                if (!nextOverlap.isEmpty()) {
                    overlappedChunk.append(" ").append(nextOverlap);
                }
            }

            result.add(overlappedChunk.toString());
        }

        return result;
    }

    private String extractOverlap(String chunk, boolean fromStart) {
        if (chunk.length() <= overlapSize) {
            return chunk;
        }

        if (fromStart) {
            int end = overlapSize;
            while (end < chunk.length() && !Character.isWhitespace(chunk.charAt(end))) {
                end++;
            }
            return chunk.substring(0, Math.min(end, chunk.length())).trim();
        } else {
            int start = chunk.length() - overlapSize;
            while (start > 0 && !Character.isWhitespace(chunk.charAt(start))) {
                start--;
            }
            return chunk.substring(Math.max(0, start), chunk.length()).trim();
        }
    }

}



//import cn.hutool.core.util.StrUtil;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.reader.tika.TikaDocumentReader;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.core.io.Resource;
//
//import java.util.*;
//import java.util.regex.Pattern;
//
///**
// * @author reid
// * @since 2025/7/9
// */
//
//public class SimpleDocumentSplitter {
//    private final String separator;
//    private final int chunkSize;
//    private final int overlapSize;
//
//    public SimpleDocumentSplitter(String separator, int chunkSize, int overlapSize) {
//        if (chunkSize <= 0) throw new IllegalArgumentException("Chunk size must be greater than 0.");
//        if (overlapSize < 0) throw new IllegalArgumentException("Overlap size must be >= 0.");
//        if (overlapSize >= chunkSize) throw new IllegalArgumentException("Overlap size must be < chunk size.");
//        this.separator = separator;
//        this.chunkSize = chunkSize;
//        this.overlapSize = overlapSize;
//    }
//
//    public SimpleDocumentSplitter(String separator, int chunkSize) {
//        this(separator, chunkSize, chunkSize / 10);
//    }
//
//    /**
//     * ⭐ 核心入口方法：接收一个文档列表，返回一个切分后的新文档列表。
//     */
//    public List<Document> split(List<Document> documents) {
//        List<Document> finalDocuments = new ArrayList<>();
//        for (Document doc : documents) {
//            if (StrUtil.isBlank(doc.getText())) {
//                continue;
//            }
//
//            // 1. 对当前文档的内容进行切分，得到净化后的字符串片段
//            List<String> cleanedTextChunks = splitText(doc.getText());
//
//            // 2. 为每个文本片段创建新的Document对象
//            for (int i = 0; i < cleanedTextChunks.size(); i++) {
//                String chunkContent = cleanedTextChunks.get(i);
//
//                // 3. 继承原始元数据，并添加额外信息
//                Map<String, Object> newMetadata = new HashMap<>(doc.getMetadata());
//                newMetadata.put("chunk_index", i);
////                newMetadata.put("original_document_id", doc.getId());
//
//                finalDocuments.add(new Document(chunkContent, newMetadata));
//            }
//        }
//        return finalDocuments;
//    }
//
//    /**
//     * 便捷方法，处理单个文档。
//     */
//    public List<Document> split(Document document) {
//        return this.split(List.of(document));
//    }
//
//
//    public List<String> splitText(String text) {
//        if (StrUtil.isBlank(text)) {
//            return Collections.emptyList();
//        }
//
//        // 阶段一：初步切分与规格化（基于净化后长度判断）
//        List<String> normalizedSegments = splitAndNormalize(text);
//
//        // 阶段二：合并与重叠处理（基于净化后长度判断）
//        return mergeAndOverlap(normalizedSegments);
//    }
//
//    private List<String> splitAndNormalize(String text) {
//        List<String> normalizedSegments = new ArrayList<>();
//        String[] initialSegments = text.split(Pattern.quote(this.separator));
//
//        for (String segment : initialSegments) {
//            if (segment.isBlank()) continue;
//
//            if (getCleanedLength(segment) > this.chunkSize) {
//                // 强制切分时传入原始文本，因为它需要保留格式来进行精确截取
//                normalizedSegments.addAll(forceSplit(segment));
//            } else {
//                // 列表中存储的始终是带格式的原始片段
//                normalizedSegments.add(segment);
//            }
//        }
//        return normalizedSegments;
//    }
//
//    /**
//     * 增强版：将长文本强制切分成多个块，确保每个块净化后的长度不超过chunkSize。
//     */
//    private List<String> forceSplit(String rawText) {
//        List<String> chunks = new ArrayList<>();
//        int rawIndex = 0;
//        while (rawIndex < rawText.length()) {
//            // 从当前位置开始，截取一段子串，使其净化后长度约等于chunkSize
//            int rawEndIndex = findEndIndexForCleanedLength(rawText, rawIndex, this.chunkSize);
//            String rawChunk = rawText.substring(rawIndex, rawEndIndex);
//            chunks.add(rawChunk);
//
//            // 计算下一个起始位置，需要回溯，确保重叠部分净化后长度约等于overlapSize
//            int overlapStartIndex = findEndIndexForCleanedLength(rawChunk, 0, getCleanedLength(rawChunk) - this.overlapSize);
//            rawIndex += overlapStartIndex;
//
//            // 安全检查，防止死循环
//            if (rawIndex >= rawEndIndex) {
//                rawIndex = rawEndIndex;
//            }
//        }
//        return chunks;
//    }
//
//    /**
//     * 核心辅助方法：从原始字符串的指定位置开始，找到一个结束位置，
//     * 使得子串[startIndex, endIndex)净化后的长度达到指定的cleanLength。
//     */
//    private int findEndIndexForCleanedLength(String rawText, int startIndex, int cleanLength) {
//        int endIndex = startIndex;
//        int currentCleanLength = 0;
//        while (endIndex < rawText.length() && currentCleanLength < cleanLength) {
//            if (!Character.isWhitespace(rawText.charAt(endIndex))) {
//                currentCleanLength++;
//            }
//            endIndex++;
//        }
//        return endIndex;
//    }
//
//    private List<String> mergeAndOverlap(List<String> segments) {
//        if (segments.isEmpty()) return Collections.emptyList();
//
//        List<String> finalCleanedChunks = new ArrayList<>();
//        StringBuilder currentChunkBuilder = new StringBuilder(); // 存储带格式的当前块
//        int currentCleanedLength = 0; // ⭐ 新增：跟踪当前块净化后的长度
//
//        for (String segment : segments) {
//            // 计算下一个片段净化后的长度，这是一个小计算，开销很低
//            int segmentCleanedLength = getCleanedLength(segment);
//            int separatorCleanedLength = currentChunkBuilder.isEmpty() ? 0 : getCleanedLength(this.separator);
//
//            // ⭐ 优化核心：基于计数值进行判断，而不是创建新字符串
//            if (currentCleanedLength + separatorCleanedLength + segmentCleanedLength > this.chunkSize && !currentChunkBuilder.isEmpty()) {
//                // 1. 当前块已满，净化后存入最终列表
//                String finalizedRawChunk = currentChunkBuilder.toString();
//                finalCleanedChunks.add(clean(finalizedRawChunk));
//
//                // 2. 计算并准备下一个块的重叠部分（原始格式）
//                int cleanedLength = currentCleanedLength; // 使用我们跟踪的长度
//                int cleanedOverlap = Math.min(this.overlapSize, cleanedLength);
//                int overlapStartIndex = findEndIndexForCleanedLength(finalizedRawChunk, 0, cleanedLength - cleanedOverlap);
//                String overlapRawText = finalizedRawChunk.substring(overlapStartIndex);
//
//                // 3. 重置当前块和其净化后长度
//                currentChunkBuilder = new StringBuilder(overlapRawText);
//                currentCleanedLength = getCleanedLength(overlapRawText);
//
//                // 4. 将当前无法合并的segment作为新块的延续
//                currentChunkBuilder.append(this.separator).append(segment);
//                currentCleanedLength += separatorCleanedLength + segmentCleanedLength;
//
//            } else {
//                // 5. 合并当前片段
//                currentChunkBuilder.append(currentChunkBuilder.isEmpty() ? "" : this.separator).append(segment);
//                currentCleanedLength += separatorCleanedLength + segmentCleanedLength;
//            }
//        }
//
//        // 添加最后一个正在构建的块
//        if (!currentChunkBuilder.isEmpty()) {
//            finalCleanedChunks.add(clean(currentChunkBuilder.toString()));
//        }
//
//        return finalCleanedChunks;
//    }
//
////    private List<String> mergeAndOverlap(List<String> segments) {
////        if (segments.isEmpty()) return Collections.emptyList();
////
////        List<String> finalCleanedChunks = new ArrayList<>();
////        StringBuilder currentChunkBuilder = new StringBuilder(); // 存储带格式的当前块
////
////        for (String segment : segments) {
////            String potentialChunk;
////            if (currentChunkBuilder.isEmpty()) {
////                potentialChunk = segment;
////            } else {
////                potentialChunk = currentChunkBuilder + this.separator + segment;
////            }
////
////            // 基于净化后的长度进行判断
////            if (clean(potentialChunk).length() > this.chunkSize && !currentChunkBuilder.isEmpty()) {
////                // 1. 当前块已满，净化后存入最终列表
////                String finalizedRawChunk = currentChunkBuilder.toString();
////                finalCleanedChunks.add(clean(finalizedRawChunk));
////
////                // 2. 计算并准备下一个块的重叠部分（原始格式）
////                int overlapStartIndex = findEndIndexForCleanedLength(finalizedRawChunk, 0, clean(finalizedRawChunk).length() - this.overlapSize);
////                currentChunkBuilder = new StringBuilder(finalizedRawChunk.substring(overlapStartIndex));
////
////                // 3. 将当前无法合并的segment作为新块的延续
////                currentChunkBuilder.append(this.separator).append(segment);
////            } else {
////                // 4. 合并当前片段
////                if (!currentChunkBuilder.isEmpty()) {
////                    currentChunkBuilder.append(this.separator);
////                }
////                currentChunkBuilder.append(segment);
////            }
////        }
////
////        // 添加最后一个正在构建的块
////        if (!currentChunkBuilder.isEmpty()) {
////            finalCleanedChunks.add(clean(currentChunkBuilder.toString()));
////        }
////
////        return finalCleanedChunks;
////    }
//
//    // 辅助方法，用于净化文本
//    private String clean(String text) {
//        return text.replaceAll("\\s", "");
//    }
//
//    private int getCleanedLength(String text) {
//        if (text == null) {
//            return 0;
//        }
//        int length = 0;
//        for (int i = 0; i < text.length(); i++) {
//            if (!Character.isWhitespace(text.charAt(i))) {
//                length++;
//            }
//        }
//        return length;
//    }
//
//    public static void main(String[] args) {
//        // ... main 方法可以保持不变，用于测试优化后的代码 ...
//        System.out.println("V5 - 内存优化版测试");
//        String separator = "\n\n";
//        String text = """
//            第一回 宴桃园豪杰三结义 斩黄巾英雄首立功
//
//            　　滚滚长江东逝水，浪花淘尽英雄。是非成败转头空。青山依旧在，几度夕阳红。白发渔樵江渚上，惯看秋月春风。一壶浊酒喜相逢。古今多少事，都付笑谈中。——调寄《临江仙》
//
//            　　话说天下大势，分久必合，合久必分。周末七国分争，并入于秦。及秦灭之后，楚、汉分争，又并入于汉。汉朝自高祖斩白蛇而起义，一统天下，后来光武中兴，传至献帝，遂分为三国。推其致乱之由，殆始于桓、灵二帝。桓帝禁锢善类，崇信宦官。及桓帝崩，灵帝即位，大将军窦武、太傅陈蕃共相辅佐。时有宦官曹节等弄权，窦武、陈蕃谋诛之，机事不密，反为所害，中涓自此愈横。
//
//            　　建宁二年四月望日，帝御温德殿。方升座，殿角狂风骤起。只见一条大青蛇，从梁上飞将下来，蟠于椅上。帝惊倒，左右急救入宫，百官俱奔避。须臾，蛇不见了。忽然大雷大雨，加以冰雹，落到半夜方止，坏却房屋无数。建宁四年二月，洛阳地震；又海水泛溢，沿海居民，尽被大浪卷入海中。光和元年，雌鸡化雄。六月朔，黑气十余丈，飞入温德殿中。秋七月，有虹现于玉堂；五原山岸，尽皆崩裂。种种不祥，非止一端。帝下诏问群臣以灾异之由，议郎蔡邕上疏，以为蜺堕鸡化，乃妇寺干政之所致，言颇切直。帝览奏叹息，因起更衣。曹节在后窃视，悉宣告左右；遂以他事陷邕于罪，放归田里。后张让、赵忠、封谞、段珪、曹节、侯览、蹇硕、程旷、夏恽、郭胜十人朋比为奸，号为“十常侍”。帝尊信张让，呼为“阿父”。朝政日非，以致天下人心思乱，盗贼蜂起。
//
//            　　时巨鹿郡有兄弟三人，一名张角，一名张宝，一名张梁。那张角本是个不第秀才，因入山采药，遇一老人，碧眼童颜，手执藜杖，唤角至一洞中，以天书三卷授之，曰：“此名《太平要术》，汝得之，当代天宣化，普救世人；若萌异心，必获恶报。”角拜问姓名。老人曰：“吾乃南华老仙也。”言讫，化阵清风而去。角得此书，晓夜攻习，能呼风唤雨，号为“太平道人”。
//
//            　　中平元年正月内，疫气流行，张角散施符水，为人治病，自称“大贤良师”。角有徒弟五百余人，云游四方，皆能书符念咒。次后徒众日多，角乃立三十六方，大方万余人，小方六七千，各立渠帅，称为将军；讹言：“苍天已死，黄天当立；岁在甲子，天下大吉。”令人各以白土书“甲子”二字于家中大门上。青、幽、徐、冀、荆、扬、兖、豫八州之人，家家侍奉大贤良师张角名字。角遣其党马元义，暗赍金帛，结交中涓封谞，以为内应。角与二弟商议曰：“至难得者，民心也。今民心已顺，若不乘势取天下，诚为可惜。”遂一面私造黄旗，约期举事；一面使弟子唐周，驰书报封谞。唐周乃径赴省中告变。帝召大将军何进调兵擒马元义，斩之；次收封谞等一干人下狱。
//
//            　　张角闻知事露，星夜举兵，自称“天公将军”，张宝称“地公将军”，张梁称“人公将军”。申言于众曰：“今汉运将终，大圣人出。汝等皆宜顺天从正，以乐太平。”四方百姓，裹黄巾从张角反者四五十万。贼势浩大，官军望风而靡。何进奏帝火速降诏，令各处备御，讨贼立功。一面遣中郎将卢植、皇甫嵩、朱儁，各引精兵、分三路讨之。
//
//            　　且说张角一军，前犯幽州界分。幽州太守刘焉，乃江夏竟陵人氏，汉鲁恭王之后也。当时闻得贼兵将至，召校尉邹靖计议。靖曰：“贼兵众，我兵寡，明公宜作速招军应敌。”刘焉然其说，随即出榜招募义兵。
//
//            　　榜文行到涿县，引出涿县中一个英雄。那人不甚好读书；性宽和，寡言语，喜怒不形于色；素有大志，专好结交天下豪杰；生得身长七尺五寸，两耳垂肩，双手过膝，目能自顾其耳，面如冠玉，唇若涂脂；中山靖王刘胜之后，汉景帝阁下玄孙，姓刘名备，字玄德。昔刘胜之子刘贞，汉武时封涿鹿亭侯，后坐酎金失侯，因此遗这一枝在涿县。玄德祖刘雄，父刘弘。弘曾举孝廉，亦尝作吏，早丧。玄德幼孤，事母至孝；家贫，贩屦织席为业。家住本县楼桑村。其家之东南，有一大桑树，高五丈余，遥望之，童童如车盖。相者云：“此家必出贵人。”玄德幼时，与乡中小儿戏于树下，曰：“我为天子，当乘此车盖。”叔父刘元起奇其言，曰：“此儿非常人也！”因见玄德家贫，常资给之。年十五岁，母使游学，尝师事郑玄、卢植，与公孙瓒等为友。
//
//            　　及刘焉发榜招军时，玄德年已二十八岁矣。当日见了榜文，慨然长叹。随后一人厉声言曰：“大丈夫不与国家出力，何故长叹？”玄德回视其人，身长八尺，豹头环眼，燕颔虎须，声若巨雷，势如奔马。玄德见他形貌异常，问其姓名。其人曰：“某姓张名飞，字翼德。世居涿郡，颇有庄田，卖酒屠猪，专好结交天下豪杰。恰才见公看榜而叹，故此相问。”玄德曰：“我本汉室宗亲，姓刘，名备。今闻黄巾倡乱，有志欲破贼安民，恨力不能，故长叹耳。”飞曰：“吾颇有资财，当招募乡勇，与公同举大事，如何。”玄德甚喜，遂与同入村店中饮酒。
//
//            　　正饮间，见一大汉，推着一辆车子，到店门首歇了，入店坐下，便唤酒保：“快斟酒来吃，我待赶入城去投军。”玄德看其人：身长九尺，髯长二尺；面如重枣，唇若涂脂；丹凤眼，卧蚕眉，相貌堂堂，威风凛凛。玄德就邀他同坐，叩其姓名。其人曰：“吾姓关名羽，字长生，后改云长，河东解良人也。因本处势豪倚势凌人，被吾杀了，逃难江湖，五六年矣。今闻此处招军破贼，特来应募。”玄德遂以己志告之，云长大喜。同到张飞庄上，共议大事。飞曰：“吾庄后有一桃园，花开正盛；明日当于园中祭告天地，我三人结为兄弟，协力同心，然后可图大事。”玄德、云长齐声应曰：“如此甚好。”
//
//            　　次日，于桃园中，备下乌牛白马祭礼等项，三人焚香再拜而说誓曰：“念刘备、关羽、张飞，虽然异姓，既结为兄弟，则同心协力，救困扶危；上报国家，下安黎庶。不求同年同月同日生，只愿同年同月同日死。皇天后土，实鉴此心，背义忘恩，天人共戮！”誓毕，拜玄德为兄，关羽次之，张飞为弟。祭罢天地，复宰牛设酒，聚乡中勇士，得三百余人，就桃园中痛饮一醉。来日收拾军器，但恨无马匹可乘。正思虑间，人报有两个客人，引一伙伴当，赶一群马，投庄上来。玄德曰：“此天佑我也！”三人出庄迎接。原来二客乃中山大商：一名张世平，一名苏双，每年往北贩马，近因寇发而回。玄德请二人到庄，置酒管待，诉说欲讨贼安民之意。二客大喜，愿将良马五十匹相送；又赠金银五百两，镔铁一千斤，以资器用。
//            """;
//
//        SimpleDocumentSplitter splitter = new SimpleDocumentSplitter(separator, 1000);
//
//        Resource textResource = new ByteArrayResource(text.getBytes());
//        TikaDocumentReader documentReader = new TikaDocumentReader(textResource);
//        List<Document> documents = documentReader.read();
//
//        List<Document> list = splitter.split(documents);
//        System.out.println(list);
//    }
//}
