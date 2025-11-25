package com.litevar.agent.rest.util;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.*;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.enums.ExecuteMode;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.tool.ToolFunctionService;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.rest.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * agent导出
 *
 * @author uncle
 * @since 2025/9/23 11:32
 */
@Slf4j
@Component
public class AgentExportUtil {
    static final String MULTI_AGENT_DIR = "multiagent/";
    static final String MODELS_DIR = "models/";
    static final String TOOLS_DIR = "tools/";
    public static final String IMAGES_DIR = "imgs/";
    public static final String KNOWLEDGE_BASE_DIR = "knowledge_bases/";
    static final String METADATA_FILE = "metadata.json";
    private static final DateTimeFormatter METADATA_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern RESOURCE_IMAGE_PATTERN = Pattern.compile("(!\\[[^\\]]*\\]\\()(/resources[^)]*?/md/imgs/)([^)]+)(\\))");

    @Autowired
    private ModelService modelService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private AgentDatasetRelaService agentDatasetRelaService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private SegmentService segmentService;
    @Autowired
    private DatasetService datasetService;
    @Autowired
    private ToolFunctionService toolFunctionService;
    @Autowired
    private ToolService toolService;
    @Autowired
    private UploadFileService uploadFileService;

    public byte[] exportAgent(Agent agent, boolean plainText) throws IOException {
        Map<String, Agent> allSubAgents = new LinkedHashMap<>();
        extraAgent(agent, allSubAgents);

        Set<String> allAgentIds = new HashSet<>(allSubAgents.keySet());
        allAgentIds.add(agent.getId());
        //{agentId,[DatasetId]}
        Map<String, List<String>> agentDatasetIdMap = loadAgentDatasetIds(allAgentIds);
        Set<String> allDatasetId = agentDatasetIdMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        Set<String> allModelId = new HashSet<>();
        //{filename,fileByte[]}
        Map<String, byte[]> knowledgeFolder = exportKnowledge(allDatasetId, allModelId);

        allSubAgents.forEach((agentId, subAgent) -> extraModel(subAgent, allModelId));
        extraModel(agent, allModelId);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            //root folder: metadata.json
            writeJsonEntry(zos, METADATA_FILE, buildMetadata());

            Set<String> allToolId = new HashSet<>();

            //root folder: agentName.json
            exportAgentJson(agent, agentDatasetIdMap.get(agent.getId()), agent.getName() + ".json", zos, allToolId);

            //multiagent folder
            for (Agent subAgent : allSubAgents.values()) {
                exportAgentJson(subAgent, agentDatasetIdMap.get(subAgent.getId()), MULTI_AGENT_DIR + subAgent.getId() + ".json", zos, allToolId);
            }

            //models folder
            exportModel(allModelId, zos, plainText);

            //tools folder
            exportTool(allToolId, zos, plainText);

            //knowledge_bases folder
            for (Map.Entry<String, byte[]> entry : knowledgeFolder.entrySet()) {
                writeEntry(zos, KNOWLEDGE_BASE_DIR + entry.getKey(), entry.getValue());
            }

            zos.finish();
            return outputStream.toByteArray();
        }
    }

    private Dict buildMetadata() {
        String username = "";
        try {
            username = LoginContext.me().getUsername();
        } catch (Exception ex) {

        }
        return Dict.create()
                .set("agent", "LiteAgent")
                .set("version", "2.0.0")
                .set("author", username)
                .set("createTime", LocalDateTime.now().format(METADATA_TIME_FORMATTER));
    }

    private Map<String, List<String>> loadAgentDatasetIds(Set<String> agentIds) {
        if (ObjectUtil.isEmpty(agentIds)) {
            return Collections.emptyMap();
        }
        return agentDatasetRelaService.lambdaQuery()
                .in(AgentDatasetRela::getAgentId, agentIds)
                .list()
                .stream()
                .collect(Collectors.groupingBy(AgentDatasetRela::getAgentId,
                        LinkedHashMap::new,
                        Collectors.mapping(AgentDatasetRela::getDatasetId, Collectors.toList())));
    }

    public Map<String, byte[]> exportKnowledge(Set<String> allDatasetId, Set<String> allModelId) {
        if (allDatasetId.isEmpty()) {
            return Collections.emptyMap();
        }

        //{datasetId,dataset}
        Map<String, Dataset> datasetMap = datasetService.getByIds(allDatasetId)
                .stream().collect(Collectors.toMap(Dataset::getId, i -> i));
        //{datasetId,{docId,[segment]}}
        Map<String, Map<String, List<DocumentSegment>>> segmentMap = segmentService.lambdaQuery()
                .projectDisplay(DocumentSegment::getDatasetId, DocumentSegment::getDocumentId, DocumentSegment::getContent, DocumentSegment::getFileId)
                .in(DocumentSegment::getDatasetId, allDatasetId)
                .eq(DocumentSegment::getEnableFlag, Boolean.TRUE).list()
                .parallelStream()
                .collect(Collectors.groupingBy(DocumentSegment::getDatasetId, Collectors.groupingBy(DocumentSegment::getDocumentId)));
        //{docId,document}
        Map<String, DatasetDocument> docInfoMap = documentService.lambdaQuery()
                .projectDisplay(DatasetDocument::getId, DatasetDocument::getName, DatasetDocument::getSeparator)
                .in(DatasetDocument::getDatasetId, allDatasetId).list()
                .stream().collect(Collectors.toMap(DatasetDocument::getId, doc -> doc));

        Set<String> fileIds = segmentMap.values().stream()
                .flatMap(m -> m.values().stream())
                .flatMap(Collection::stream)
                .map(DocumentSegment::getFileId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());

        Map<String, UploadFile> uploadFileMap = fileIds.isEmpty() ?
                Collections.emptyMap() :
                uploadFileService.getByIds(fileIds).stream().collect(Collectors.toMap(UploadFile::getId, uploadFile -> uploadFile));

        Map<String, byte[]> knowledgeFolder = new HashMap<>();
        datasetMap.forEach((datasetId, dataset) -> {
            //add model
            if (StrUtil.isNotBlank(dataset.getEmbeddingModel())) {
                allModelId.add(dataset.getEmbeddingModel());
            }
            if (StrUtil.isNotBlank(dataset.getLlmModelId())) {
                allModelId.add(dataset.getLlmModelId());
            }

            //datasetName/metadata.json
            Dict metadata = Dict.create()
                    .set("id", datasetId)
                    .set("name", dataset.getName())
                    .set("description", dataset.getDescription())
                    .set("embeddingModelId", dataset.getEmbeddingModel())
                    .set("llmModelId", dataset.getLlmModelId())
                    .set("topK", dataset.getRetrievalTopK())
                    .set("maxDistance", dataset.getRetrievalScoreThreshold());
            knowledgeFolder.put(dataset.getId() + "/" + METADATA_FILE, JSONUtil.toJsonPrettyStr(metadata).getBytes(StandardCharsets.UTF_8));

            Map<String, List<DocumentSegment>> docSegmentMap = segmentMap.get(datasetId);
            if (ObjectUtil.isEmpty(docSegmentMap)) {
                return;
            }

            docSegmentMap.forEach((docId, docSegments) -> {
                DatasetDocument document = docInfoMap.get(docId);
                if (document == null) {
                    return;
                }

                String docBasePath = dataset.getId() + "/" + docId + "/";

                //将DocumentSegment中content字段内容用分隔符拼接在一起写入markdown文件
                String mdContent = docSegments.stream().map(DocumentSegment::getContent).collect(Collectors.joining(document.getSeparator()));
                mdContent = normalizeResourceImagePath(mdContent);
                String mdName = document.getName().endsWith(".md") ? document.getName() : document.getName() + ".md";
                knowledgeFolder.put(docBasePath + mdName, mdContent.getBytes(StandardCharsets.UTF_8));

                Set<String> docFileIds = docSegments.stream()
                        .map(DocumentSegment::getFileId)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                exportDocumentImages(docBasePath, docFileIds, uploadFileMap, knowledgeFolder);
                Dict docMetadata = Dict.create()
                        .set("name", document.getName())
                        .set("separator", document.getSeparator());
                if (StrUtil.isNotBlank(dataset.getLlmModelId())) {
                    String summary = documentService.getDocumentSummary(docId);
                    docMetadata.set("summary", summary);
                }
                knowledgeFolder.put(docBasePath + METADATA_FILE, JSONUtil.toJsonPrettyStr(docMetadata).getBytes(StandardCharsets.UTF_8));
            });
        });
        return knowledgeFolder;
    }

    public static String normalizeResourceImagePath(String content) {
        Matcher matcher = RESOURCE_IMAGE_PATTERN.matcher(content);
        StringBuilder formatted = new StringBuilder();
        while (matcher.find()) {
            String replacement = matcher.group(1) + IMAGES_DIR + matcher.group(3) + matcher.group(4);
            matcher.appendReplacement(formatted, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(formatted);
        return formatted.toString();
    }

    private void exportDocumentImages(String docBasePath,
                                      Set<String> docFileIds,
                                      Map<String, UploadFile> uploadFileMap,
                                      Map<String, byte[]> knowledgeFolder) {
        if (ObjectUtil.isEmpty(docFileIds)) {
            return;
        }
        for (String fileId : docFileIds) {
            UploadFile uploadFile = uploadFileMap.get(fileId);
            if (uploadFile == null || StrUtil.isBlank(uploadFile.getMarkdownPath())) {
                continue;
            }
            Path markdownPath = Path.of(uploadFile.getMarkdownPath());
            Path markdownDir = markdownPath.getParent();
            if (markdownDir == null) {
                continue;
            }
            Path imagesDir = markdownDir.resolve("imgs");
            if (!Files.isDirectory(imagesDir)) {
                continue;
            }
            try (Stream<Path> imageStream = Files.walk(imagesDir)) {
                List<Path> imageFiles = imageStream
                        .filter(Files::isRegularFile)
                        .toList();
                for (Path imageFile : imageFiles) {
                    Path relativePath = imagesDir.relativize(imageFile);
                    String relativeName = relativePath.toString().replace('\\', '/');
                    String zipEntryName = docBasePath + IMAGES_DIR + relativeName;
                    byte[] bytes = Files.readAllBytes(imageFile);
                    knowledgeFolder.put(zipEntryName, bytes);
                }
            } catch (IOException e) {
                log.warn("导出imgs目录失败, fileId={}, imagesDir={}", fileId, imagesDir, e);
            }
        }
    }

    public void exportModel(Set<String> ids, ZipOutputStream zos, boolean plainText) throws IOException {
        for (String id : ids) {
            byte[] modelBytes = modelService.exportModel(id, plainText);
            writeEntry(zos, MODELS_DIR + id + ".json", modelBytes);
        }
    }

    private void exportTool(Set<String> toolIds, ZipOutputStream zos, boolean plainText) throws IOException {
        for (String id : toolIds) {
            byte[] toolBytes = toolService.exportTool(id, plainText);
            writeEntry(zos, TOOLS_DIR + id + ".json", toolBytes);
        }
    }

    public void extraAgent(Agent agent, Map<String, Agent> allSubAgents) {
        if (ObjectUtil.isEmpty(agent.getSubAgentIds())) {
            return;
        }
        List<String> ids = agent.getSubAgentIds().stream()
                .filter(StrUtil::isNotBlank)
                .filter(id -> !allSubAgents.containsKey(id))
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        List<Agent> subAgentList = agentService.getByIds(ids);
        subAgentList.forEach(child -> {
            allSubAgents.put(child.getId(), child);
            extraAgent(child, allSubAgents);
        });
    }

    private void extraModel(Agent agent, Set<String> allModelId) {
        if (StrUtil.isNotEmpty(agent.getLlmModelId())) {
            allModelId.add(agent.getLlmModelId());
        }
        if (StrUtil.isNotEmpty(agent.getAsrModelId())) {
            allModelId.add(agent.getAsrModelId());
        }
        if (StrUtil.isNotEmpty(agent.getTtsModelId())) {
            allModelId.add(agent.getTtsModelId());
        }
    }

    private void exportAgentJson(Agent agent,
                                 List<String> datasetIds,
                                 String filename,
                                 ZipOutputStream zos,
                                 Set<String> allToolId) throws IOException {
        Dict agentJson = Dict.create()
                .set("id", agent.getId())
                .set("name", agent.getName())
                .set("description", agent.getDescription())
                .set("prompt", agent.getPrompt())
                .set("type", AgentType.of(agent.getType()).toString())
                .set("mode", ExecuteMode.of(agent.getMode()).toString())
                .set("modelId", agent.getLlmModelId())
                .set("temperature", agent.getTemperature())
                .set("topP", agent.getTopP())
                .set("maxTokens", agent.getMaxTokens())
                .set("ttsModelId", agent.getTtsModelId())
                .set("asrModelId", agent.getAsrModelId())
                .set("subAgentIds", agent.getSubAgentIds())
                .set("knowledgeBaseIds", datasetIds);

        if (ObjectUtil.isNotEmpty(agent.getFunctionList())) {
            Set<String> functionIds = agent.getFunctionList().stream()
                    .map(Agent.AgentFunction::getFunctionId).collect(Collectors.toSet());
            Map<String, Integer> functionMode = agent.getFunctionList().stream()
                    .collect(Collectors.toMap(Agent.AgentFunction::getFunctionId, Agent.AgentFunction::getMode));
            List<Dict> functionList = toolFunctionService.lambdaQuery()
                    .projectDisplay(ToolFunction::getId, ToolFunction::getToolId, ToolFunction::getRequestMethod, ToolFunction::getResource)
                    .in(ToolFunction::getId, functionIds).list()
                    .parallelStream().map(function -> {
                        allToolId.add(function.getToolId());
                        String functionRawId = function.getRequestMethod() + ":" + function.getResource();
                        return Dict.create().set("toolId", function.getToolId())
                                .set("functionId", Base64.getEncoder().encodeToString(functionRawId.getBytes(StandardCharsets.UTF_8)))
                                .set("mode", ExecuteMode.of(functionMode.get(function.getId())).toString());
                    }).toList();
            agentJson.set("functionList", functionList);
        }

        writeJsonEntry(zos, filename, agentJson);
    }

    private void writeJsonEntry(ZipOutputStream zos, String entryName, Object data) throws IOException {
        byte[] jsonBytes = JSONUtil.toJsonPrettyStr(data).getBytes(StandardCharsets.UTF_8);
        writeEntry(zos, entryName, jsonBytes);
    }

    public void writeEntry(ZipOutputStream zos, String entryName, byte[] content) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(content);
        zos.closeEntry();
    }
}
