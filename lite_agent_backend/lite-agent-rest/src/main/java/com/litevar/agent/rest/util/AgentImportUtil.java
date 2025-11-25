package com.litevar.agent.rest.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.*;
import com.litevar.agent.base.enums.*;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.base.vo.*;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.tool.ToolFunctionService;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.rest.config.LitevarProperties;
import com.litevar.agent.rest.service.*;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.litevar.agent.rest.util.AgentExportUtil.*;

/**
 * agent导入
 *
 * @author uncle
 * @since 2025/9/24
 */
@Slf4j
@Component
public class AgentImportUtil {
    private static final String JSON_SUFFIX = ".json";
    private static final String DESCRIPTOR_FILE = "descriptor.json";
    private static final long PREVIEW_TTL_MINUTES = 60L;
    private final String FLAG_LINK = "link_";

    @Autowired
    private AgentService agentService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ToolService toolService;
    @Autowired
    private ToolFunctionService toolFunctionService;
    @Autowired
    private DatasetService datasetService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private SegmentService segmentService;
    @Autowired
    private Validator validator;
    @Autowired
    private UploadFileService uploadFileService;
    @Autowired
    private LitevarProperties litevarProperties;
    @Autowired
    private AgentImportProgressPublisher agentImportProgressPublisher;

    @PostConstruct
    public void init() {
        Runnable task = () -> {
            String uploadRoot = StrUtil.emptyToDefault(litevarProperties.getUploadPath(), System.getProperty("java.io.tmpdir"));
            Path agentBaseDir = Paths.get(uploadRoot, "agent");
            if (!Files.exists(agentBaseDir) || !Files.isDirectory(agentBaseDir)) {
                return;
            }

            try (Stream<Path> tokenDirs = Files.list(agentBaseDir)) {
                tokenDirs.filter(Files::isDirectory).forEach(tokenDir -> {
                    String token = tokenDir.getFileName().toString();
                    if (StrUtil.isBlank(token)) {
                        return;
                    }
                    try {
                        if (!RedisUtil.exists(buildCacheKey(token))) {
                            cleanupImportContext(token, tokenDir.toString());
                        }
                    } catch (Exception ex) {
                        log.error("清理导入缓存目录{}失败", tokenDir, ex);
                    }
                });
            } catch (IOException e) {
                log.error("扫描导入缓存目录{}失败", agentBaseDir, e);
            }
        };
        Executors.newScheduledThreadPool(1)
                .scheduleWithFixedDelay(task, 3, 15, TimeUnit.MINUTES);
    }

    public ImportDescriptor previewAgent(MultipartFile file, String workspaceId) {
        assertAgentFile(file);
        String token = UUID.fastUUID().toString(true);
        Path tempDir = createTempDirectory(token);

        //读取数据
        ImportDescriptor descriptor = readArchive(file, tempDir);
        descriptor.setTempDir(tempDir.toString());
        descriptor.setToken(token);

        //校验数据
        validateDescriptor(descriptor);

        //查找相同的数据
        findSimilarData(descriptor, workspaceId);

        //暂存数据
        persistDescriptor(token, descriptor);
        return descriptor;
    }

    public ImportDescriptor previewKnowledge(MultipartFile archive, String workspaceId) {
        if (archive == null || archive.isEmpty()) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "导入失败，上传文件不能为空");
        }
        String token = UUID.fastUUID().toString(true);
        Path tempDir = createTempDirectory(token);

        //读取数据
        ImportDescriptor descriptor = readArchive(archive, tempDir);
        descriptor.setTempDir(tempDir.toString());
        descriptor.setToken(token);

        Set<String> availableModelIds = descriptor.getModelMap().keySet();
        //校验数据
        validateKnowledgeBases(descriptor.getKnowledgeBaseMap(), availableModelIds);

        //查找相同的数据
        findSimilarData(descriptor, workspaceId);

        //暂存数据
        persistDescriptor(token, descriptor);

        return descriptor;
    }

    private void assertAgentFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "上传文件不能为空");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (!filename.endsWith(".agent")) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "仅支持agent格式的导入文件");
        }
    }

    private ImportDescriptor readArchive(MultipartFile file, Path tempDir) {
        ImportDescriptor descriptor = new ImportDescriptor();
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || StrUtil.equals(entry.getName(), METADATA_FILE)) {
                    continue;
                }
                String entryName = entry.getName();
                byte[] bytes = IoUtil.readBytes(zis, false);
                if (bytes.length == 0) {
                    continue;
                }

                if (entryName.startsWith(MODELS_DIR) && entryName.endsWith(JSON_SUFFIX)) {
                    //models/modelId.json
                    String modelId = trimEntryId(entryName, MODELS_DIR);
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    ModelVO model = JSONUtil.toBean(content, ModelVO.class);
                    validateModel(model);
                    descriptor.getModelMap().put(modelId, model);

                } else if (entryName.startsWith(TOOLS_DIR) && entryName.endsWith(JSON_SUFFIX)) {
                    //tools/toolId.json
                    String toolId = trimEntryId(entryName, TOOLS_DIR);
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    ToolVO tool = JSONUtil.toBean(content, ToolVO.class);
                    validateTool(tool);
                    descriptor.getToolMap().put(toolId, tool);

                } else if (entryName.startsWith(MULTI_AGENT_DIR) && entryName.endsWith(JSON_SUFFIX)) {
                    //multiagent/subAgentId.json
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    AgentDefinition agent = JSONUtil.toBean(content, AgentDefinition.class);
                    descriptor.getSubAgentMap().put(agent.getId(), agent);

                } else if (!entryName.contains("/") && entryName.endsWith(JSON_SUFFIX)) {
                    //agentName.json
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    AgentDefinition agent = JSONUtil.toBean(content, AgentDefinition.class);
                    descriptor.setMainAgent(agent);

                } else if (entryName.startsWith(KNOWLEDGE_BASE_DIR)) {
                    //knowledge_bases
                    handleKnowledgeBaseEntry(descriptor, entryName, bytes, tempDir);
                }
            }
        } catch (IOException e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "读取导入文件失败:" + e.getMessage());
        }
        return descriptor;
    }

    /**
     * datasetId/metadata.json
     * datasetId/docId/metadata.json
     * datasetId/docId/<document>.md
     * datasetId/docId/imgs/<image files>
     */
    private void handleKnowledgeBaseEntry(ImportDescriptor descriptor, String entryName, byte[] bytes, Path tempDir) {
        String relativePath = StrUtil.removePrefix(entryName, KNOWLEDGE_BASE_DIR);
        if (StrUtil.isBlank(relativePath)) {
            return;
        }

        String[] datasetSplit = relativePath.split("/");
        if (datasetSplit.length < 2) {
            return;
        }

        String datasetId = datasetSplit[0];

        KnowledgeBaseDescriptor kbDescriptor = descriptor.getKnowledgeBaseMap()
                .computeIfAbsent(datasetId, i -> new KnowledgeBaseDescriptor());

        if (datasetSplit.length == 2 && StrUtil.equals(datasetSplit[1], METADATA_FILE)) {
            //知识库的metadata.json
            KnowledgeBaseMetadata metadata = JSONUtil.toBean(new String(bytes, StandardCharsets.UTF_8), KnowledgeBaseMetadata.class);
            kbDescriptor.setMetadata(metadata);

        } else if (datasetSplit.length >= 3) {
            String docId = datasetSplit[1];
            KnowledgeDocument document = kbDescriptor.getDocuments().computeIfAbsent(docId, i -> new KnowledgeDocument());
            if (datasetSplit.length == 3 && StrUtil.equals(datasetSplit[2], METADATA_FILE)) {
                //文档的metadata.json
                JSONObject metadata = JSONUtil.parseObj(new String(bytes, StandardCharsets.UTF_8));
                document.setName(metadata.getStr("name"));
                document.setSeparator(metadata.getStr("separator"));
                document.setSummary(metadata.getStr("summary", ""));
                return;
            }
            //markdown文档和imgs文件夹图片
            int prefixLength = (datasetId + "/" + docId + "/").length();
            if (relativePath.length() <= prefixLength) {
                return;
            }
            String docRelativePath = relativePath.substring(prefixLength);
            Path target = tempDir.resolve(entryName);
            try {
                Files.createDirectories(target.getParent());
                Files.write(target, bytes);
            } catch (IOException e) {
                throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                        StrUtil.format("读取导入文件失败，无法写入临时文件:{}", e.getMessage()));
            }
            document.getFilePathMap().put(docRelativePath, target.toString());
        }
    }

    private void validateDescriptor(ImportDescriptor descriptor) {
        List<AgentDefinition> allAgents = new ArrayList<>();
        if (descriptor.getMainAgent() == null) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "导入文件中缺少主agent配置");
        }
        allAgents.add(descriptor.getMainAgent());
        allAgents.addAll(descriptor.getSubAgentMap().values());

        Set<String> knowledgeIds = descriptor.getKnowledgeBaseMap().keySet();
        Set<String> availableModelIds = descriptor.getModelMap().keySet();
        Set<String> availableToolIds = descriptor.getToolMap().keySet();

        for (AgentDefinition agentDefinition : allAgents) {
            String agentIdentifier = StrUtil.emptyToDefault(agentDefinition.getName(), agentDefinition.getId());
            ensureModelDefined(agentDefinition.getModelId(), availableModelIds,
                    () -> StrUtil.format("导入失败，Agent {} 引用的主模型不存在: {}", agentIdentifier, agentDefinition.getModelId()));
            ensureModelDefined(agentDefinition.getTtsModelId(), availableModelIds,
                    () -> StrUtil.format("导入失败，Agent {} 引用的TTS模型不存在: {}", agentIdentifier, agentDefinition.getTtsModelId()));
            ensureModelDefined(agentDefinition.getAsrModelId(), availableModelIds,
                    () -> StrUtil.format("导入失败，Agent {} 引用的ASR模型不存在: {}", agentIdentifier, agentDefinition.getAsrModelId()));

            if (CollUtil.isNotEmpty(agentDefinition.getFunctionList())) {
                for (AgentFunctionDefinition functionDefinition : agentDefinition.getFunctionList()) {
                    String toolId = functionDefinition.getToolId();
                    if (StrUtil.isBlank(toolId)) {
                        throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                                StrUtil.format("导入失败，Agent {} 的函数缺少工具配置", agentIdentifier));
                    }
                    if (!availableToolIds.contains(toolId)) {
                        throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                                StrUtil.format("导入失败，Agent {} 引用的工具不存在: {}", agentIdentifier, toolId));
                    }
                }
            }
        }

        allAgents.parallelStream()
                .filter(i -> !CollUtil.isEmpty(i.getKnowledgeBaseIds()))
                .flatMap(i -> i.getKnowledgeBaseIds().stream())
                .filter(id -> !knowledgeIds.contains(id))
                .findAny().ifPresent(id -> {
                    throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "导入失败,未找到知识库:" + id);
                });

        validateKnowledgeBases(descriptor.getKnowledgeBaseMap(), availableModelIds);
    }

    private void validateKnowledgeBases(Map<String, KnowledgeBaseDescriptor> knowledgeBaseMap,
                                        Set<String> availableModelIds) {
        for (Map.Entry<String, KnowledgeBaseDescriptor> entry : knowledgeBaseMap.entrySet()) {
            String knowledgeId = entry.getKey();
            KnowledgeBaseDescriptor kbDescriptor = entry.getValue();
            KnowledgeBaseMetadata metadata = kbDescriptor.getMetadata();
            if (metadata == null) {
                throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                        StrUtil.format("导入失败，知识库{}缺少metadata.json", knowledgeId));
            }
            if (StrUtil.isBlank(metadata.getName())) {
                throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                        StrUtil.format("导入失败，知识库{}的名称为空", knowledgeId));
            }
            if (StrUtil.isBlank(metadata.getEmbeddingModelId())) {
                throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                        StrUtil.format("导入失败，知识库{}缺少向量模型配置", knowledgeId));
            }
            ensureModelDefined(metadata.getEmbeddingModelId(), availableModelIds,
                    () -> StrUtil.format("导入失败，知识库{}引用的模型不存在: {}", metadata.getName(), metadata.getEmbeddingModelId()));
            ensureModelDefined(metadata.getLlmModelId(), availableModelIds,
                    () -> StrUtil.format("导入失败，知识库{}引用的模型不存在: {}", metadata.getName(), metadata.getLlmModelId()));

            for (Map.Entry<String, KnowledgeDocument> docEntry : kbDescriptor.getDocuments().entrySet()) {
                KnowledgeDocument document = docEntry.getValue();
                if (document == null) {
                    continue;
                }
                if (StrUtil.isBlank(document.getName())) {
                    throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                            StrUtil.format("导入失败，知识库{}的文档{}缺少metadata.json", metadata.getName(), docEntry.getKey()));
                }
                boolean hasMarkdown = document.getFilePathMap().keySet().stream()
                        .anyMatch(path -> StrUtil.endWithIgnoreCase(path, ".md"));
                if (!hasMarkdown) {
                    throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                            StrUtil.format("导入失败，知识库{}的文档{}缺少Markdown文件", metadata.getName(), document.getName()));
                }
            }
        }
    }

    private void findSimilarData(ImportDescriptor descriptor, String workspaceId) {
        if (descriptor.getMainAgent() != null) {
            Agent similarAgent = agentService.lambdaQuery().projectDisplay(Agent::getId)
                    .eq(Agent::getWorkspaceId, workspaceId).eq(Agent::getName, descriptor.getMainAgent().getName()).one();
            if (similarAgent != null) {
                descriptor.getMainAgent().setSimilarId(similarAgent.getId());
            }
        }
        if (!descriptor.getSubAgentMap().isEmpty()) {
            descriptor.getSubAgentMap().forEach((agentId, agent) -> {
                Agent similarAgent = agentService.lambdaQuery().projectDisplay(Agent::getId)
                        .eq(Agent::getWorkspaceId, workspaceId).eq(Agent::getName, agent.getName()).one();
                if (similarAgent != null) {
                    agent.setSimilarId(similarAgent.getId());
                }
            });
        }
        if (!descriptor.getModelMap().isEmpty()) {
            List<String> modelAliasList = descriptor.getModelMap().values().stream().map(ModelVO::getAlias).toList();
            Map<String, String> similarModelMap = modelService.lambdaQuery().projectDisplay(LlmModel::getId, LlmModel::getAlias)
                    .eq(LlmModel::getWorkspaceId, workspaceId).in(LlmModel::getAlias, modelAliasList).list()
                    .stream().collect(Collectors.toMap(LlmModel::getAlias, LlmModel::getId));
            descriptor.getModelMap().forEach((modelId, model) -> {
                String similarModelId = similarModelMap.get(model.getAlias());
                if (similarModelId != null) {
                    model.setSimilarId(similarModelId);
                }
            });
        }
        if (!descriptor.getToolMap().isEmpty()) {
            List<String> toolNameList = descriptor.getToolMap().values().stream().map(ToolVO::getName).toList();
            Map<String, String> similarToolMap = toolService.lambdaQuery().projectDisplay(ToolProvider::getId, ToolProvider::getName)
                    .eq(ToolProvider::getWorkspaceId, workspaceId).in(ToolProvider::getName, toolNameList).list()
                    .stream().collect(Collectors.toMap(ToolProvider::getName, ToolProvider::getId));
            descriptor.getToolMap().forEach((toolId, tool) -> {
                String similarToolId = similarToolMap.get(tool.getName());
                if (similarToolId != null) {
                    tool.setSimilarId(similarToolId);
                }
            });
        }
        if (!descriptor.getKnowledgeBaseMap().isEmpty()) {
            descriptor.getKnowledgeBaseMap().forEach((kId, dataset) -> {
                Dataset similarDataset = datasetService.lambdaQuery().projectDisplay(Dataset::getId)
                        .eq(Dataset::getWorkspaceId, workspaceId).eq(Dataset::getName, dataset.getMetadata().getName()).one();
                if (similarDataset != null) {
                    dataset.setSimilarId(similarDataset.getId());
                }
            });
        }
    }

    private Path createTempDirectory(String token) {
        try {
            String uploadRoot = StrUtil.emptyToDefault(litevarProperties.getUploadPath(), System.getProperty("java.io.tmpdir"));
            Path agentBaseDir = Paths.get(uploadRoot, "agent");
            Files.createDirectories(agentBaseDir);
            Path tokenDir = agentBaseDir.resolve(token);
            Files.createDirectories(tokenDir);
            return tokenDir;
        } catch (IOException e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "创建导入临时目录失败:" + e.getMessage());
        }
    }

    private void persistDescriptor(String token, ImportDescriptor descriptor) {
        try {
            Path descriptorPath = Paths.get(descriptor.getTempDir(), DESCRIPTOR_FILE);
            Files.writeString(descriptorPath, JSONUtil.toJsonStr(descriptor), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            RedisUtil.setValue(buildCacheKey(token), descriptorPath.toString(), PREVIEW_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (IOException e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "缓存导入数据失败:" + e.getMessage());
        }
    }

    public String importAgent(String workspaceId, String token, ImportDescriptor param) {
        ImportDescriptor descriptor = loadDescriptor(token);
        if (descriptor == null) {
            agentImportProgressPublisher.publishError(token, "【失败】导入信息已过期,请重新上传");
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "导入信息已过期，请重新上传");
        }
        Map<String, String> modelIdMap = new HashMap<>(descriptor.getModelMap().size());
        Map<String, String> datasetIdMap = new HashMap<>(descriptor.getKnowledgeBaseMap().size());
        Map<String, String> toolIdMap = new HashMap<>(descriptor.getToolMap().size());
        Map<String, String> importedAgents = new HashMap<>(descriptor.getSubAgentMap().size() + 1);

        try {
            //更新用户确认后的修改
            applyOverrides(descriptor, param);
            //保存数据
            importModels(workspaceId, descriptor.getModelMap(), modelIdMap, token);

            importKnowledgeBases(workspaceId, descriptor, modelIdMap, datasetIdMap, token);

            importTools(workspaceId, descriptor.getToolMap(), toolIdMap, token);
            Map<String, String> functionHashMap = buildFunctionHashMap(toolIdMap.values());

            saveAgents(workspaceId, descriptor, modelIdMap, functionHashMap, datasetIdMap, importedAgents, token);
            String mainAgentId = importedAgents.get(descriptor.getMainAgent().getId());
            cleanupImportContext(token, descriptor.getTempDir());
            return StrUtil.removePrefix(mainAgentId, FLAG_LINK);
        } catch (Exception ex) {
            //delete data
            datasetIdMap.forEach((oldId, newId) -> {
                if (!newId.startsWith(FLAG_LINK)) {
                    datasetService.deleteDataset(newId);
                }
            });
            modelIdMap.forEach((oldId, newId) -> {
                if (!newId.startsWith(FLAG_LINK)) {
                    modelService.removeModel(newId);
                }
            });
            toolIdMap.forEach((oldId, newId) -> {
                if (!newId.startsWith(FLAG_LINK)) {
                    toolService.deleteTool(newId);
                }
            });
            importedAgents.forEach((oldId, newId) -> {
                if (!newId.startsWith(FLAG_LINK)) {
                    agentService.removeAgent(newId);
                }
            });

            String failReason = Optional.ofNullable(ex.getMessage()).filter(StrUtil::isNotBlank)
                    .orElseGet(() -> ex.getClass().getSimpleName());
            publishProgress(token, "【失败】" + failReason);
            throw ex;
        } finally {
            agentImportProgressPublisher.complete(token);
        }
    }

    public Map<String, String> importKnowledge(String workspaceId, String token, ImportDescriptor param) {
        ImportDescriptor descriptor = loadDescriptor(token);
        if (descriptor == null) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "导入信息已过期，请重新上传");
        }

        Map<String, String> datasetIdMap = new HashMap<>(descriptor.getKnowledgeBaseMap().size());
        Map<String, String> modelIdMap = new HashMap<>(descriptor.getModelMap().size());
        try {
            //更新用户确认后的修改
            applyOverrides(descriptor, param);
            importModels(workspaceId, descriptor.getModelMap(), modelIdMap, token);
            importKnowledgeBases(workspaceId, descriptor, modelIdMap, datasetIdMap, token);
            cleanupImportContext(token, descriptor.getTempDir());
            datasetIdMap.entrySet().forEach(entry ->
                    entry.setValue(StrUtil.removePrefix(entry.getValue(), FLAG_LINK)));
            return datasetIdMap;
        } catch (Exception ex) {
            //delete data
            datasetIdMap.forEach((oldId, newId) -> {
                if (!newId.startsWith(FLAG_LINK)) {
                    datasetService.deleteDataset(newId);
                }
            });
            modelIdMap.forEach((oldId, newId) -> {
                if (!newId.startsWith(FLAG_LINK)) {
                    modelService.removeModel(newId);
                }
            });
            throw ex;
        }
    }

    private ImportDescriptor loadDescriptor(String token) {
        String cacheKey = buildCacheKey(token);
        Object cachePath = RedisUtil.getValue(cacheKey);
        if (cachePath == null) {
            return null;
        }
        Path descriptorPath = Paths.get(String.valueOf(cachePath));
        if (!Files.exists(descriptorPath)) {
            return null;
        }
        try {
            String json = Files.readString(descriptorPath, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return null;
            }
            return JSONUtil.toBean(json, ImportDescriptor.class);
        } catch (IOException e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "读取导入缓存失败:" + e.getMessage());
        }
    }

    private void cleanupImportContext(String token, String tempDir) {
        log.info("清理agent导入缓存目录{}", tempDir);
        RedisUtil.delKey(buildCacheKey(token));
        if (StrUtil.isNotBlank(tempDir)) {
            FileUtil.del(tempDir);
        }
    }

    public boolean hasImportContext(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        return RedisUtil.exists(buildCacheKey(token));
    }

    private String buildCacheKey(String token) {
        return String.format(CacheKey.AGENT_IMPORT_PREVIEW, token);
    }

    private void publishProgress(String token, String message) {
        if (StrUtil.isBlank(token) || StrUtil.isBlank(message)) {
            return;
        }
        agentImportProgressPublisher.publish(token, message);
    }

    private void applyOverrides(ImportDescriptor descriptor, ImportDescriptor param) {
        if (CollUtil.isNotEmpty(param.getModelMap())) {
            Map<String, ModelVO> modelMap = descriptor.getModelMap();
            if (param.getModelMap().size() != modelMap.size()) {
                throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "导入失败，模型数量不匹配");
            }

            for (Map.Entry<String, ModelVO> paramModelEntry : param.getModelMap().entrySet()) {
                ModelVO modelVO = modelMap.get(paramModelEntry.getKey());
                if (modelVO == null) {
                    continue;
                }
                modelMap.put(paramModelEntry.getKey(), paramModelEntry.getValue());
            }
        }
        param.getToolMap().forEach((toolId, tool) -> {
            ToolVO originTool = descriptor.getToolMap().get(toolId);
            if (originTool != null) {
                originTool.setOperate(tool.getOperate());
            }
        });
        if (param.getMainAgent() != null) {
            descriptor.getMainAgent().setOperate(param.getMainAgent().getOperate());
        }
        param.getSubAgentMap().forEach((agentId, agent) -> {
            AgentDefinition originAgent = descriptor.getSubAgentMap().get(agentId);
            if (originAgent != null) {
                originAgent.setOperate(agent.getOperate());
            }
        });
        param.getKnowledgeBaseMap().forEach((kId, dataset) -> {
            KnowledgeBaseDescriptor originDataset = descriptor.getKnowledgeBaseMap().get(kId);
            if (originDataset != null) {
                originDataset.setOperate(dataset.getOperate());
            }
        });
    }

    private String trimEntryId(String entryName, String prefix) {
        String id = StrUtil.removePrefix(entryName, prefix);
        return StrUtil.removeSuffix(id, JSON_SUFFIX);
    }

    private void importModels(String workspaceId, Map<String, ModelVO> modelMap, Map<String, String> idMap, String token) {
        if (modelMap.isEmpty()) {
            publishProgress(token, "【跳过】未检测到需要创建的大模型");
            return;
        }
        publishProgress(token, "【任务】正在创建大模型...");
        for (Map.Entry<String, ModelVO> entry : modelMap.entrySet()) {
            ModelVO modelVO = entry.getValue();
            String modelId;
            if (modelVO.getOperate() == OperateTypeEnum.INSERT.getOperate()) {
                //insert
                //存在相同别名,别名要加后缀
                String aliasName = StrUtil.isNotBlank(modelVO.getSimilarId()) ? modelVO.getAlias() + "_1" : modelVO.getAlias();
                modelVO.setAlias(aliasName);
                modelId = modelService.createModel(workspaceId, modelVO).getId();
            } else if (modelVO.getOperate() == OperateTypeEnum.UPDATE.getOperate()) {
                //update
                modelVO.setId(modelVO.getSimilarId());
                modelService.updateModel(modelVO);
                modelId = FLAG_LINK + modelVO.getSimilarId();
            } else {
                //直接引用,不update
                modelId = FLAG_LINK + modelVO.getSimilarId();
            }
            idMap.put(entry.getKey(), modelId);
        }
        publishProgress(token, "【完成】大模型创建完成");
    }

    private void importTools(String workspaceId, Map<String, ToolVO> toolMap, Map<String, String> idMap, String token) {
        if (toolMap.isEmpty()) {
            publishProgress(token, "【跳过】未检测到需要创建的工具");
            return;
        }
        publishProgress(token, "【任务】正在创建工具...");
        for (Map.Entry<String, ToolVO> entry : toolMap.entrySet()) {
            ToolVO toolVO = entry.getValue();
            String toolId;
            if (toolVO.getOperate() == OperateTypeEnum.INSERT.getOperate()) {
                String name = StrUtil.isNotBlank(toolVO.getSimilarId()) ? toolVO.getName() + "_1" : toolVO.getName();
                toolVO.setName(name);
                toolId = toolService.addTool(toolVO, workspaceId).getId();
            } else if (toolVO.getOperate() == OperateTypeEnum.UPDATE.getOperate()) {
                toolVO.setId(toolVO.getSimilarId());
                toolService.updateTool(toolVO);
                toolId = FLAG_LINK + toolVO.getSimilarId();
            } else {
                toolId = FLAG_LINK + toolVO.getSimilarId();
            }
            idMap.put(entry.getKey(), toolId);
        }
        publishProgress(token, "【完成】工具创建完成");
    }

    private void importKnowledgeBases(String workspaceId,
                                      ImportDescriptor descriptor,
                                      Map<String, String> modelIdMap,
                                      Map<String, String> datasetIdMap,
                                      String token) {
        if (descriptor.getKnowledgeBaseMap().isEmpty()) {
            publishProgress(token, "【跳过】未检测到需要创建的知识库");
            return;
        }

        for (Map.Entry<String, KnowledgeBaseDescriptor> entry : descriptor.getKnowledgeBaseMap().entrySet()) {
            KnowledgeBaseDescriptor kbDescriptor = entry.getValue();
            KnowledgeBaseMetadata metadata = kbDescriptor.getMetadata();
            String embeddingModelId = resolveId(metadata.getEmbeddingModelId(), modelIdMap);
            String llmModelId = resolveId(metadata.getLlmModelId(), modelIdMap);
            publishProgress(token, "【任务】正在创建知识库：" + metadata.getName());
            Dataset dataset;
            String datasetId;
            if (kbDescriptor.getOperate() == OperateTypeEnum.INSERT.getOperate()) {
                String name = StrUtil.isNotBlank(kbDescriptor.getSimilarId()) ? metadata.getName() + "_1" : metadata.getName();
                metadata.setName(name);
                DatasetCreateForm form = buildDatasetCreateForm(metadata, embeddingModelId, llmModelId);
                dataset = datasetService.createDataset(workspaceId, form);
                datasetId = dataset.getId();
            } else {
                dataset = datasetService.getById(kbDescriptor.getSimilarId());
                datasetId = FLAG_LINK + kbDescriptor.getSimilarId();
            }

            datasetIdMap.put(entry.getKey(), datasetId);

            boolean canEmbedding = canEmbedding(embeddingModelId, llmModelId);
            if (canEmbedding) {
                publishProgress(token, "【任务】开始进行文件向量化，向量化可能花费的时间比较久，请不要关闭此界面");
            } else {
                publishProgress(token, "【警告】⚠️知识库：" + metadata.getName() + "由于模型原因，无法向量化，请在智能体创建成功后，在知识库中重新选择正确的模型进行向量化处理");
            }

            importKnowledgeDocuments(dataset, kbDescriptor, canEmbedding);
        }
        publishProgress(token, "【完成】所有知识库创建完成");
    }

    private boolean canEmbedding(String embeddingModelId, String llmModelId) {
        LlmModel embeddingModel = modelService.findById(embeddingModelId);
        boolean flag = cn.hutool.core.lang.Validator.isUrl(embeddingModel.getBaseUrl()) &&
                !StrUtil.equals(embeddingModel.getApiKey(), "{{<APIKEY>}}");
        if (flag && StrUtil.isNotEmpty(llmModelId)) {
            LlmModel model = modelService.findById(llmModelId);
            return cn.hutool.core.lang.Validator.isUrl(model.getBaseUrl())
                    && !StrUtil.equals(model.getApiKey(), "{{<APIKEY>}}");
        }
        return flag;
    }

    private DatasetCreateForm buildDatasetCreateForm(KnowledgeBaseMetadata metadata,
                                                     String embeddingModelId,
                                                     String llmModelId) {
        DatasetCreateForm form = new DatasetCreateForm();
        form.setName(metadata.getName());
        form.setDescription(StrUtil.nullToEmpty(metadata.getDescription()));
        form.setIcon("");
        form.setLlmModelId(llmModelId);
        form.setEmbeddingModel(embeddingModelId);
        form.setRetrievalTopK(Optional.ofNullable(metadata.getTopK()).orElse(10));
        form.setRetrievalScoreThreshold(Optional.ofNullable(metadata.getMaxDistance()).orElse(0.5D));
        return form;
    }

    private void importKnowledgeDocuments(Dataset dataset, KnowledgeBaseDescriptor kbDescriptor, boolean canEmbedding) {
        if (kbDescriptor.getDocuments().isEmpty() || kbDescriptor.getOperate() == OperateTypeEnum.SKIP.getOperate()) {
            return;
        }
        boolean updateMode = OperateTypeEnum.UPDATE.getOperate().equals(kbDescriptor.getOperate());
        Map<String, List<DatasetDocument>> existDocumentMap = Collections.emptyMap();
        if (updateMode) {
            existDocumentMap = documentService.lambdaQuery()
                    .eq(DatasetDocument::getDatasetId, dataset.getId())
                    .list()
                    .stream()
                    .collect(Collectors.groupingBy(DatasetDocument::getName,
                            LinkedHashMap::new,
                            Collectors.toList()));
        }

        for (KnowledgeDocument document : kbDescriptor.getDocuments().values()) {
            String documentName = document.getName();
            if (!updateMode) {
                createDocument(dataset, document, canEmbedding);
                continue;
            }
            List<DatasetDocument> sameNameDocs = existDocumentMap.get(documentName);
            if (CollUtil.isEmpty(sameNameDocs)) {
                createDocument(dataset, document, canEmbedding);
                continue;
            }

            String markdownTmpPath = document.getFilePathMap().entrySet().stream()
                    .filter(entry -> StrUtil.endWithIgnoreCase(entry.getKey(), ".md"))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .get();
            String markdownContent = FileUtil.readUtf8String(markdownTmpPath);
            String importMd5 = DigestUtils.md5Hex(markdownContent);

            DatasetDocument matchedDoc = null;
            List<String> duplicateIds = new ArrayList<>();
            for (DatasetDocument existDoc : sameNameDocs) {
                if (matchedDoc == null && compareDocument(importMd5, existDoc)) {
                    matchedDoc = existDoc;
                } else {
                    duplicateIds.add(existDoc.getId());
                }
            }

            if (matchedDoc != null) {
                documentService.batchDeleteDocuments(duplicateIds);
                existDocumentMap.remove(documentName);
                continue;
            }

            List<String> deleteIds = sameNameDocs.stream()
                    .map(DatasetDocument::getId)
                    .toList();
            documentService.batchDeleteDocuments(deleteIds);
            existDocumentMap.remove(documentName);
            createDocument(dataset, document, canEmbedding);
        }

        if (updateMode) {
            List<String> redundantDocIds = existDocumentMap.values().stream()
                    .flatMap(Collection::stream)
                    .map(DatasetDocument::getId)
                    .toList();
            if (!redundantDocIds.isEmpty()) {
                documentService.batchDeleteDocuments(redundantDocIds);
            }
        }
    }

    private boolean compareDocument(String importMd5, DatasetDocument document) {
        String dbDoc = segmentService.lambdaQuery().projectDisplay(DocumentSegment::getContent)
                .eq(DocumentSegment::getDocumentId, document.getId()).list()
                .stream().map(DocumentSegment::getContent).collect(Collectors.joining(document.getSeparator()));
        String dbMd5 = DigestUtils.md5Hex(dbDoc);
        return StrUtil.equals(dbMd5, importMd5);
    }

    private void createDocument(Dataset dataset, KnowledgeDocument document, boolean canEmbedding) {
        String documentName = document.getName();
        String separator = StrUtil.emptyToDefault(document.getSeparator(), "\n\n");

        DocumentCreateForm form = new DocumentCreateForm();
        form.setWorkspaceId(dataset.getWorkspaceId());
        form.setName(documentName);
        form.setSeparator(separator);
        form.setDataSourceType(DatasetSourceType.FILE.getValue());

        try {
            Map.Entry<String, String> markdownEntry = document.getFilePathMap().entrySet().stream().filter(entry -> StrUtil.endWithIgnoreCase(entry.getKey(), ".md")).findFirst().get();
            Path markdownFile = Paths.get(markdownEntry.getValue());
            byte[] markdownBytes = Files.readAllBytes(markdownFile);
            //写markdown文件
            UploadFile uploadFile = uploadFileService.saveFile(dataset.getId(), markdownEntry.getKey(), markdownBytes, "text/markdown");
            updateMarkdownImageLinks(uploadFile);
            //写图片
            writeDocumentImages(uploadFile, document, dataset.getName(), documentName);

            form.setFileId(uploadFile.getId());

            if (canEmbedding) {
                documentService.createDocument(dataset.getId(), form);
            } else {
                persistDocumentForm(dataset.getId(), form);
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("导入失败,知识库文档创建失败", ex);
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    StrUtil.format("导入失败，知识库{}的文档{}创建失败: {}",
                            dataset.getName(), documentName, ex.getMessage()));
        }
    }

    private void writeDocumentImages(UploadFile uploadFile,
                                     KnowledgeDocument document,
                                     String datasetName,
                                     String documentName) {
        Path markdownDir = Paths.get(uploadFile.getMarkdownPath()).getParent();
        for (Map.Entry<String, String> entry : document.getFilePathMap().entrySet()) {
            String relativePath = entry.getKey();
            if (!relativePath.startsWith(IMAGES_DIR)) {
                continue;
            }
            Path source = Paths.get(entry.getValue());
            if (!Files.exists(source)) {
                continue;
            }
            Path target = markdownDir.resolve(relativePath);
            try {
                Files.createDirectories(target.getParent());
                Files.write(target, Files.readAllBytes(source));
            } catch (IOException e) {
                log.error("导入失败,知识库文档写入图片失败", e);
                throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                        StrUtil.format("导入失败，知识库{}的文档{}写入图片{}失败: {}",
                                datasetName, documentName, relativePath, e.getMessage()));
            }
        }
    }

    private void updateMarkdownImageLinks(UploadFile uploadFile) {
        Path markdownPath = Paths.get(uploadFile.getPath());
        try {
            String content = Files.readString(markdownPath, StandardCharsets.UTF_8);
            Pattern pattern = Pattern.compile("(!\\[[^\\]]*]\\()(imgs[^)\\s]+)([^)]*)\\)");
            Matcher matcher = pattern.matcher(content);
            if (!matcher.find()) {
                return;
            }
            String resourcesPrefix = buildResourcesPrefix(markdownPath.getParent());
            StringBuilder buffer = new StringBuilder();
            do {
                String relativePath = matcher.group(2);
                String normalizedRelative = StrUtil.removePrefix(relativePath, "/");
                String newPath = resourcesPrefix + "/" + normalizedRelative;
                matcher.appendReplacement(buffer,
                        Matcher.quoteReplacement(matcher.group(1) + newPath + matcher.group(3) + ")"));
            } while (matcher.find());
            matcher.appendTail(buffer);
            Files.writeString(markdownPath, buffer.toString(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("更新Markdown图片链接失败, 文件: {}", uploadFile.getPath(), e);
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    "导入失败，Markdown图片链接处理异常");
        }
    }

    private String buildResourcesPrefix(Path markdownDir) {
        if (markdownDir == null) {
            return "/resources";
        }
        String parentPath = markdownDir.toString().replace("\\", "/");
        String datasetPrefix = Paths.get(StrUtil.emptyToDefault(litevarProperties.getUploadPath(), ""), "datasets")
                .toString()
                .replace("\\", "/");
        if (StrUtil.startWith(parentPath, datasetPrefix)) {
            parentPath = parentPath.substring(datasetPrefix.length());
        }
        parentPath = StrUtil.addPrefixIfNot(parentPath, "/");
        parentPath = StrUtil.removeSuffix(parentPath, "/");
        if (StrUtil.isBlank(parentPath) || StrUtil.equals(parentPath, "/")) {
            return "/resources";
        }
        return "/resources" + parentPath;
    }

    private void persistDocumentForm(String datasetId, DocumentCreateForm form) {
        try {
            log.info("知识库id:{},文档:{}暂不做向量化操作,开始缓存文档数据", datasetId, form.getName());
            String uploadRoot = StrUtil.emptyToDefault(litevarProperties.getUploadPath(), System.getProperty("java.io.tmpdir"));
            Path dir = Paths.get(uploadRoot, "tmpDocument", datasetId);
            Files.createDirectories(dir);

            Path target = dir.resolve(form.getFileId() + ".json");

            Files.writeString(target, JSONUtil.toJsonStr(form), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("缓存知识库文档缓存数据失败", e);
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "缓存知识库文档缓存数据失败:" + e.getMessage());
        }
    }

    private Map<String, String> buildFunctionHashMap(Collection<String> toolIds) {
        if (CollUtil.isEmpty(toolIds)) {
            return Collections.emptyMap();
        }
        List<String> ids = toolIds.stream().map(i -> StrUtil.removePrefix(i, FLAG_LINK)).toList();
        List<ToolFunction> functions = toolFunctionService.list(toolFunctionService.lambdaQuery()
                .projectDisplay(ToolFunction::getId, ToolFunction::getResource, ToolFunction::getRequestMethod)
                .in(ToolFunction::getToolId, ids));
        Map<String, String> mapping = new HashMap<>(functions.size());
        Base64.Encoder encoder = Base64.getEncoder();
        for (ToolFunction function : functions) {
            String raw = function.getRequestMethod() + ":" + function.getResource();
            String key = encoder.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            mapping.put(key, function.getId());
        }
        return mapping;
    }

    private void saveAgents(String workspaceId,
                            ImportDescriptor descriptor,
                            Map<String, String> modelIdMap,
                            Map<String, String> functionHashMap,
                            Map<String, String> datasetIdMap,
                            Map<String, String> result,
                            String token) {
        List<AgentDefinition> allAgents = new ArrayList<>();
        if (descriptor.getMainAgent() != null) {
            allAgents.add(descriptor.getMainAgent());
        }
        allAgents.addAll(descriptor.getSubAgentMap().values());

        publishProgress(token, "【任务】正在创建智能体...");
        String userId = LoginContext.currentUserId();
        for (AgentDefinition definition : allAgents) {
            if (definition.getOperate() == OperateTypeEnum.INSERT.getOperate()) {
                AgentCreateForm form = new AgentCreateForm();
                String name = StrUtil.isNotBlank(definition.getSimilarId()) ? definition.getName() + "_1" : definition.getName();
                form.setName(name);
                definition.setName(name);
                form.setDescription(StrUtil.nullToEmpty(definition.getDescription()));
                Agent agent = agentService.addAgent(workspaceId, form, userId);
                result.put(definition.getId(), agent.getId());
            } else {
                result.put(definition.getId(), FLAG_LINK + definition.getSimilarId());
            }
        }

        for (AgentDefinition definition : allAgents) {
            if (definition.getOperate() == OperateTypeEnum.SKIP.getOperate()) {
                continue;
            }

            List<String> knowledgeBaseIds = convertKnowledgeBaseIds(definition.getKnowledgeBaseIds(), datasetIdMap);
            List<String> mappedSubAgentIds = mapSubAgentIds(definition.getSubAgentIds(), result);

            AgentUpdateForm updateForm = new AgentUpdateForm();
            updateForm.setName(definition.getName());
            updateForm.setDescription(definition.getDescription());
            updateForm.setPrompt(StrUtil.nullToEmpty(definition.getPrompt()));
            updateForm.setType(parseAgentType(definition.getType()));
            updateForm.setMode(parseExecuteMode(definition.getMode()));
            updateForm.setTemperature(definition.getTemperature());
            updateForm.setTopP(definition.getTopP());
            updateForm.setMaxTokens(definition.getMaxTokens());
            updateForm.setLlmModelId(resolveId(definition.getModelId(), modelIdMap));
            updateForm.setTtsModelId(resolveId(definition.getTtsModelId(), modelIdMap));
            updateForm.setAsrModelId(resolveId(definition.getAsrModelId(), modelIdMap));
            updateForm.setFunctionList(convertFunctionList(definition.getFunctionList(), functionHashMap));
            if (CollUtil.isNotEmpty(knowledgeBaseIds)) {
                updateForm.setDatasetIds(knowledgeBaseIds);
            }
            if (CollUtil.isNotEmpty(mappedSubAgentIds)) {
                updateForm.setSubAgentIds(mappedSubAgentIds);
            }

            String agentId = StrUtil.removePrefix(result.get(definition.getId()), FLAG_LINK);
            agentService.updateAgent(agentId, updateForm);
        }
        publishProgress(token, "【完成】所有智能体创建完成");
    }

    private List<Agent.AgentFunction> convertFunctionList(List<AgentFunctionDefinition> functions,
                                                          Map<String, String> functionHashMap) {
        if (CollUtil.isEmpty(functions)) {
            return null;
        }
        List<Agent.AgentFunction> list = new ArrayList<>(functions.size());
        for (AgentFunctionDefinition definition : functions) {
            String functionId = functionHashMap.get(definition.getFunctionId());
            if (StrUtil.isEmpty(functionId)) {
                throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                        StrUtil.format("导入失败，未找到函数:{}", definition.getFunctionId()));
            }
            Agent.AgentFunction function = new Agent.AgentFunction();
            function.setFunctionId(functionId);
            function.setMode(parseExecuteMode(definition.getMode()));
            list.add(function);
        }
        return list;
    }

    private List<String> convertKnowledgeBaseIds(List<String> knowledgeBaseIds, Map<String, String> datasetIdMap) {
        if (CollUtil.isEmpty(knowledgeBaseIds)) {
            return Collections.emptyList();
        }
        List<String> mapped = new ArrayList<>(knowledgeBaseIds.size());
        for (String oldId : knowledgeBaseIds) {
            String newId = StrUtil.removePrefix(datasetIdMap.get(oldId), FLAG_LINK);
            if (StrUtil.isBlank(newId)) {
                throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                        StrUtil.format("导入失败，未找到知识库映射:{}", oldId));
            }
            mapped.add(newId);
        }
        return mapped;
    }

    private List<String> mapSubAgentIds(List<String> subAgentIds, Map<String, String> agentMap) {
        if (CollUtil.isEmpty(subAgentIds)) {
            return Collections.emptyList();
        }
        List<String> mapped = new ArrayList<>(subAgentIds.size());
        for (String oldId : subAgentIds) {
            String newId = StrUtil.removePrefix(agentMap.get(oldId), FLAG_LINK);
            if (newId != null) {
                mapped.add(newId);
            }
        }
        return mapped;
    }

    private Integer parseAgentType(String type) {
        if (StrUtil.isBlank(type)) {
            return AgentType.GENERAL.getType();
        }
        try {
            return AgentType.valueOf(type).getType();
        } catch (Exception ex) {
            return AgentType.GENERAL.getType();
        }
    }

    private Integer parseExecuteMode(String mode) {
        if (StrUtil.isBlank(mode)) {
            return ExecuteMode.PARALLEL.getMode();
        }
        try {
            return ExecuteMode.valueOf(mode).getMode();
        } catch (Exception ex) {
            return ExecuteMode.PARALLEL.getMode();
        }
    }

    private String resolveId(String originId, Map<String, String> idMap) {
        if (StrUtil.isBlank(originId)) {
            return "";
        }
        String id = idMap.getOrDefault(originId, "");
        return StrUtil.removePrefix(id, FLAG_LINK);
    }

    private void ensureModelDefined(String modelId, Set<String> availableModelIds, Supplier<String> messageSupplier) {
        if (StrUtil.isBlank(modelId)) {
            return;
        }
        if (!availableModelIds.contains(modelId)) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), messageSupplier.get());
        }
    }

    private void validateModel(ModelVO modelVO) {
        Set<ConstraintViolation<ModelVO>> violations = validator.validate(modelVO, AddAction.class);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    StrUtil.format("模型配置校验失败: {}", message));
        }
    }

    private void validateTool(ToolVO toolVO) {
        Set<ConstraintViolation<ToolVO>> violations = validator.validate(toolVO, AddAction.class);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(),
                    StrUtil.format("工具配置校验失败: {}", message));
        }
    }

    @Getter
    @Setter
    public static class ImportDescriptor {
        private AgentDefinition mainAgent;
        private Map<String, AgentDefinition> subAgentMap = new LinkedHashMap<>();
        private Map<String, ModelVO> modelMap = new LinkedHashMap<>();
        private Map<String, ToolVO> toolMap = new LinkedHashMap<>();
        private Map<String, KnowledgeBaseDescriptor> knowledgeBaseMap = new LinkedHashMap<>();
        private String tempDir;
        private String token;
    }

    @Getter
    @Setter
    public static class AgentDefinition {
        private String id;
        private String name;
        private String description;
        private String prompt;
        private String type;
        private String mode;
        private String modelId;
        private Double temperature;
        private Double topP;
        private Integer maxTokens;
        private String ttsModelId;
        private String asrModelId;
        private List<String> subAgentIds;
        private List<AgentFunctionDefinition> functionList;
        private List<String> knowledgeBaseIds;

        /**
         * 相同的数据id
         */
        private String similarId;

        /**
         * 数据操作类型
         *
         * @see com.litevar.agent.base.enums.OperateTypeEnum
         */
        private Integer operate = 0;
    }

    @Setter
    @Getter
    public static class AgentFunctionDefinition {
        private String toolId;
        private String functionId;
        private String mode;
    }

    @Getter
    @Setter
    public static class KnowledgeBaseDescriptor {
        private KnowledgeBaseMetadata metadata;
        private Map<String, KnowledgeDocument> documents = new LinkedHashMap<>();

        /**
         * 相同的数据id
         */
        private String similarId;

        /**
         * 数据操作类型
         *
         * @see com.litevar.agent.base.enums.OperateTypeEnum
         */
        private Integer operate = 0;
    }

    @Getter
    @Setter
    public static class KnowledgeBaseMetadata {
        /**
         * 知识库名字
         */
        private String name;
        /**
         * 描述
         */
        private String description;
        /**
         * 向量模型id
         */
        private String embeddingModelId;
        /**
         * 摘要模型id
         */
        private String llmModelId;
        private Integer topK;
        private Double maxDistance;
    }

    @Getter
    @Setter
    public static class KnowledgeDocument {
        /**
         * 文档名字
         */
        @Setter
        private String name;
        /**
         * 分隔符
         */
        @Setter
        private String separator;
        /**
         * 文档摘要
         */
        private String summary;
        /**
         * 文档内容路径信息
         */
        private Map<String, String> filePathMap = new LinkedHashMap<>();
    }
}
