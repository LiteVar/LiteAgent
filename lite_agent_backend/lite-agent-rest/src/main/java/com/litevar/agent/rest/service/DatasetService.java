package com.litevar.agent.rest.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.AgentDatasetRela;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.entity.DatasetDocument;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.DatasetCreateForm;
import com.litevar.agent.base.vo.DatasetsVO;
import com.litevar.agent.base.vo.DocumentCreateForm;
import com.litevar.agent.base.vo.SegmentVO;
import com.litevar.agent.rest.config.LitevarProperties;
import com.litevar.agent.rest.util.PermissionUtil;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service implementation for Dataset management.
 */
@Slf4j
@Service
public class DatasetService extends ServiceImpl<Dataset> {
    @Autowired
    private LitevarProperties litevarProperties;

    @Autowired
    private DocumentService documentService;
    @Autowired
    private SegmentService segmentService;
    @Autowired
    private AgentDatasetRelaService agentDatasetRelaService;

    /**
     * Creates a new dataset in the specified workspace.
     *
     * @param workspaceId The ID of the workspace to create the dataset in.
     * @param form        The form containing dataset creation details.
     * @return The created Dataset object.
     */
    public Dataset createDataset(String workspaceId, DatasetCreateForm form) {
        Dataset existDataset = this.lambdaQuery()
                .projectDisplay(Dataset::getId)
                .eq(Dataset::getWorkspaceId, workspaceId)
                .eq(Dataset::getName, form.getName()).one();
        if (existDataset != null) {
            throw new ServiceException(ServiceExceptionEnum.DATASET_NAME_DUPLICATE);
        }
        String userId = LoginContext.currentUserId();

        Dataset dataset = BeanUtil.toBean(form, Dataset.class, CopyOptions.create().setIgnoreNullValue(true));
        dataset.setUserId(userId);
        dataset.setWorkspaceId(workspaceId);

        save(dataset);

        // Generate a unique vector collection name for the dataset
        updateVectorCollectionName(dataset.getId());
        // Generate an API key for the dataset
        generateApiKey(dataset.getId());

        return getById(dataset.getId());
    }

    /**
     * Retrieves a Dataset by its ID.
     *
     * @param id The ID of the dataset to retrieve.
     * @return The Dataset object if found, otherwise null.
     */
    public Dataset getDataset(String id) {
        Dataset dataset = getById(id);
        return Optional.ofNullable(dataset).orElseThrow(() -> new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD));
    }

    public DatasetsVO info(String id) {
        Dataset dataset = getDataset(id);
        DatasetsVO bean = BeanUtil.toBean(dataset, DatasetsVO.class);
        bean.setCanEdit(PermissionUtil.getEditPermission(dataset.getUserId(), dataset.getWorkspaceId()));
        bean.setCanDelete(PermissionUtil.getEditPermission(dataset.getUserId(), dataset.getWorkspaceId()));

        return bean;
    }

    /**
     * Lists datasets within a workspace with pagination.
     *
     * @param workspaceId The ID of the workspace to list datasets from.
     * @param pageNo      The page number for pagination.
     * @param pageSize    The number of datasets per page.
     * @return PageModel containing DatasetsVO objects and pagination information.
     */
    public PageModel<DatasetsVO> listDatasets(String workspaceId, String query, Integer pageNo, Integer pageSize) {
        String userId = LoginContext.currentUserId();

        LambdaQueryChainWrapper<Dataset> wrapper = this.lambdaQuery()
            .eq(Dataset::getWorkspaceId, workspaceId);

        if (StrUtil.isNotBlank(query)) {
            wrapper.like(Dataset::getName, query);
        }

        wrapper.orderByDesc(Dataset::getCreateTime);

        PageResult<Dataset> pageResult = this.page(wrapper, pageNo, pageSize);
        if (pageResult.getContentData().isEmpty()) {
            return new PageModel<>(pageNo, pageSize, 0L, Collections.emptyList());
        }

        Map<String, List<DatasetDocument>> documents = documentService.listDocumentsBatch(pageResult.getContentData()
            .stream().map(Dataset::getId).collect(Collectors.toList()));

        String datasetKey = CacheKey.AGENT_DATASET_DRAFT;
        Set<String> keys = RedisUtil.keys(datasetKey.substring(0, datasetKey.lastIndexOf(":")) + "*");
        Map<String, Set<String>> datasetAgentMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(keys)) {
            keys.forEach(key -> {
                String agentId = key.substring(key.lastIndexOf(":") + 1);
                Object value = RedisUtil.getValue(key);
                if (value == null) {
                    return;
                }
                List<String> dsIds = (List<String>) value;
                dsIds.forEach(id -> {
                    Set<String> agentSet = datasetAgentMap.computeIfAbsent(id, k -> new HashSet<>());
                    agentSet.add(agentId);
                });
            });
        }

        List<String> dsIds = pageResult.getContentData().parallelStream().map(Dataset::getId).toList();
        List<AgentDatasetRela> agentDatasetRelas = agentDatasetRelaService.list(agentDatasetRelaService.lambdaQuery()
            .in(AgentDatasetRela::getDatasetId, dsIds));
        agentDatasetRelas.forEach(r -> {
            Set<String> agentSet = datasetAgentMap.computeIfAbsent(r.getDatasetId(), k -> new HashSet<>());
            agentSet.add(r.getAgentId());
        });

        List<DatasetsVO> datasets = new ArrayList<>(pageResult.getContentData().size());
        pageResult.getContentData().forEach(v -> {
            DatasetsVO dataset = BeanUtil.toBean(v, DatasetsVO.class);
            dataset.setDocCount(documents.getOrDefault(v.getId(), Collections.emptyList()).size());
            dataset.setWordCount(
                documents.getOrDefault(v.getId(), Collections.emptyList()).stream()
                    .mapToInt(DatasetDocument::getWordCount).sum()
            );
            Set<String> agentIds = datasetAgentMap.get(v.getId());
            dataset.setAgentCount(ObjectUtil.isEmpty(agentIds) ? 0 : agentIds.size());
            dataset.setCanEdit(PermissionUtil.getEditPermission(v.getUserId(), v.getWorkspaceId()));
            dataset.setCanDelete(PermissionUtil.getEditPermission(v.getUserId(), v.getWorkspaceId()));

            datasets.add(dataset);
        });

        return new PageModel<>(pageNo, pageSize, pageResult.getTotalSize(), datasets);
    }

    /**
     * Searches datasets by a collection of IDs.
     *
     * @param ids the collection of dataset IDs
     * @return the list of datasets
     */
    public List<Dataset> searchDatasetsByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryChainWrapper<Dataset> wrapper = this.lambdaQuery()
            .in(Dataset::getId, ids);

        return wrapper.list();
    }

    public List<Dataset> searchDatasetsByLlmModelId(String embeddingModelId) {
        if (StrUtil.isBlank(embeddingModelId)) {
            return Collections.emptyList();
        }

        LambdaQueryChainWrapper<Dataset> wrapper = this.lambdaQuery()
                .eq(Dataset::getEmbeddingModel, embeddingModelId);

        return wrapper.list();
    }

    /**
     * Updates an existing dataset.
     *
     * @param id   The ID of the dataset to update.
     * @param form The form containing updated dataset details.
     * @return The updated Dataset object.
     */
    public Dataset updateDataset(String id, DatasetCreateForm form) {
        Dataset dataset = getById(id);
        if (!dataset.getName().equals(form.getName())) {
            Dataset existDataset = this.lambdaQuery().projectDisplay(Dataset::getId)
                    .eq(Dataset::getWorkspaceId, dataset.getWorkspaceId())
                    .eq(Dataset::getName, form.getName()).one();
            if (existDataset != null) {
                throw new ServiceException(ServiceExceptionEnum.DATASET_NAME_DUPLICATE);
            }
        }

        //之前没有选择摘要模型,新选择了模型,要帮needSummary的文档做一次摘要
        boolean firstSummary = StrUtil.isBlank(dataset.getLlmModelId())
                && StrUtil.isNotBlank(form.getLlmModelId());

        BeanUtil.copyProperties(
                form, dataset,
                CopyOptions.create().setIgnoreNullValue(true).setIgnoreProperties("embeddingModel")
        );

        if (StrUtil.isNotBlank(form.getEmbeddingModel()) && !StrUtil.equalsIgnoreCase(form.getEmbeddingModel(), dataset.getEmbeddingModel())) {
            long cnt = documentService.countDocumentsByDatasetId(id);
            if (cnt == 0) {
                dataset.setEmbeddingModel(form.getEmbeddingModel());
            }
        }
        String summaryCollectionName = StrUtil.isNotBlank(dataset.getLlmModelId()) ? "summary_" + dataset.getId() : "";
        dataset.setSummaryCollectionName(summaryCollectionName);

        updateById(dataset);

        //处理未向量化的文档
        processPendingDocuments(id);

        if (firstSummary) {
            documentService.summarizeNeedSummaryDocuments(id);
        }

        return dataset;
    }

    private void processPendingDocuments(String datasetId) {
        String uploadRoot = StrUtil.emptyToDefault(litevarProperties.getUploadPath(), System.getProperty("java.io.tmpdir"));
        Path datasetDir = Paths.get(uploadRoot, "tmpDocument", datasetId);
        //判断当前文件夹是否为空,如果不为空,读取里面的json文件,转为DocumentCreateForm对象,调用createDocument
        if (!Files.exists(datasetDir) || !Files.isDirectory(datasetDir)) {
            return;
        }

        List<Path> pendingFiles;
        try (Stream<Path> stream = Files.list(datasetDir)) {
            pendingFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> StrUtil.endWithIgnoreCase(path.getFileName().toString(), ".json"))
                    .toList();
        } catch (IOException e) {
            log.error("读取知识库{}缓存文档列表失败", datasetId, e);
            return;
        }

        for (Path pendingFile : pendingFiles) {
            try {
                String json = Files.readString(pendingFile, StandardCharsets.UTF_8);
                if (StrUtil.isBlank(json)) {
                    Files.deleteIfExists(pendingFile);
                    continue;
                }

                DocumentCreateForm form = JSONUtil.toBean(json, DocumentCreateForm.class);
                documentService.createDocument(datasetId, form);
                Files.deleteIfExists(pendingFile);
            } catch (Exception ex) {
                log.error("处理知识库{}缓存文档{}失败", datasetId, pendingFile.getFileName(), ex);
            }
        }

        try (Stream<Path> stream = Files.list(datasetDir)) {
            boolean hasPendingFiles = stream
                    .filter(Files::isRegularFile)
                    .anyMatch(path -> StrUtil.endWithIgnoreCase(path.getFileName().toString(), ".json"));
            if (hasPendingFiles) {
                return;
            }
        } catch (IOException e) {
            log.warn("检查知识库{}缓存文档列表失败", datasetId, e);
            return;
        }

        try (Stream<Path> stream = Files.list(datasetDir)) {
            boolean directoryEmpty = stream.findAny().isEmpty();
            if (directoryEmpty) {
                Files.deleteIfExists(datasetDir);
            }
        } catch (IOException e) {
            log.warn("删除知识库{}缓存目录失败", datasetId, e);
        }
    }

    /**
     * Deletes a dataset and its associated documents and segments.
     *
     * @param id The ID of the dataset to delete.
     */
    public void deleteDataset(String id) {
        removeById(id);
        documentService.deleteDocumentsByDatasetId(id);
        segmentService.deleteSegmentsByDatasetId(id);

        //删除agent与数据集的关联
        agentDatasetRelaService.removeByColumn(AgentDatasetRela::getDatasetId, id);
    }

    /**
     * Toggles the share flag of a dataset.
     *
     * @param id The ID of the dataset to toggle sharing for.
     */
    public void toggleShare(String id) {
        Dataset dataset = getById(id);
        dataset.setShareFlag(!dataset.getShareFlag());
        updateById(dataset);
    }

    /**
     * 召回测试
     */
    public List<SegmentVO> retrieve(String datasetId, String content, String apikey) {
        Dataset dataset = getById(datasetId);

        if (StrUtil.isBlank(dataset.getEmbeddingModel())) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_NOT_EXIST_OR_NOT_SHARE);
        }

        if (StrUtil.isNotBlank(apikey) && !apikey.equalsIgnoreCase(dataset.getApiKey())) {
            throw new ServiceException(ServiceExceptionEnum.INVALID_APIKEY);
        }

        return segmentService.retrieveSegments("", Collections.singletonList(datasetId), content);
    }

    public Dict retrieve(List<String> datasetIds, String content) {
        return segmentService.retrieve("", datasetIds, content);
    }

    public Dataset generateApiKey(String id) {
        Dataset dataset = getDataset(id);

        if (!PermissionUtil.getEditPermission(dataset.getUserId(), dataset.getWorkspaceId())) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }

        String apiKey = "sk-" + UUID.fastUUID().toString(true);
        dataset.setApiKey(apiKey);

        if (StrUtil.isBlank(dataset.getApiUrl())) {
            String apiUrl = litevarProperties.getExternalApiUrl()
                + "/liteAgent/v1/dataset/"
                + dataset.getId()
                + "/retrieve/external?content=%s";
            dataset.setApiUrl(apiUrl);
        }

        saveOrUpdate(dataset);
        return dataset;
    }

    private void updateVectorCollectionName(String id) {
        Dataset dataset = getById(id);
        dataset.setVectorCollectionName("vector_" + dataset.getId());
        //文档摘要向量库
        if (StrUtil.isNotBlank(dataset.getLlmModelId())) {
            dataset.setSummaryCollectionName("summary_" + dataset.getId());
        }
        updateById(dataset);
    }

}
