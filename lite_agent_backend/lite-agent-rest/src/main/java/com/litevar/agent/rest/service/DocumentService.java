package com.litevar.agent.rest.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.entity.DatasetDocument;
import com.litevar.agent.base.entity.DocumentSegment;
import com.litevar.agent.base.enums.DatasetSourceType;
import com.litevar.agent.base.enums.EmbedStatus;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.DocumentCreateForm;
import com.litevar.agent.rest.config.LocalStorageProperties;
import com.litevar.agent.rest.springai.document.SimpleDocumentSplitter;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.conditions.update.LambdaUpdateChainWrapper;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.impl.ServiceImpl;
import kotlin.collections.ArrayDeque;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation for Document management.
 */
@Slf4j
@Service
public class DocumentService extends ServiceImpl<DatasetDocument> {
    @Autowired
    private LocalStorageProperties localStorageProperties;
    @Autowired
    private DatasetService datasetService;
    @Autowired
    private SegmentService segmentService;

    /**
     * Creates a new document.
     *
     * @param datasetId the ID of the workspace
     * @param form      the document to create
     * @return the created document
     */
    public DatasetDocument createDocument(String datasetId, DocumentCreateForm form) throws MalformedURLException {
        String userId = LoginContext.currentUserId();
        Dataset dataset = datasetService.getDataset(datasetId);

        DatasetDocument document = BeanUtil.toBean(form, DatasetDocument.class, CopyOptions.create().setIgnoreNullValue(true));
        document.setUserId(userId);
        document.setDatasetId(datasetId);
        save(document);

        document = getById(document.getId());

//        if (file != null) {
//            String saveFile = LocalFileUtil.saveFile(file, localStorageProperties.getDatasetFilePath());
//            document.setMd5Hash(saveFile);
//            document.setFilePath(localStorageProperties.getDatasetFilePath() + saveFile);
//
//            // Calculate word count from file content
//            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
//            int wordCount = content.trim().split("\\s+").length;
//            document.setWordCount(wordCount);
//        }

        List<Document> segments = splitDocument(form, false, datasetId, document.getId());
        List<DocumentSegment> documentSegments = segmentService.embedSegments(dataset.getWorkspaceId(), datasetId, document.getId(), segments);

        document.setWordCount(documentSegments.stream().mapToInt(DocumentSegment::getWordCount).sum());
        document.setTokenCount(documentSegments.stream().mapToInt(DocumentSegment::getTokenCount).sum());
        document.setEmbedStatus(EmbedStatus.SUCCESS.getValue());
        saveOrUpdate(document);

        return document;
    }

    /**
     * Retrieves a document by its ID.
     *
     * @param id the ID of the document
     * @return the retrieved document
     */
    public DatasetDocument getDocument(String id) {
        return getById(id);
    }

    /**
     * Lists documents in a dataset with pagination.
     *
     * @param datasetId the ID of the dataset
     * @param pageNo    the page number
     * @param pageSize  the page size
     * @return a page model containing the documents
     */
    public PageModel<DatasetDocument> listDocuments(String datasetId, Integer pageNo, Integer pageSize) {
        LambdaQueryChainWrapper<DatasetDocument> wrapper = this.lambdaQuery()
            .eq(DatasetDocument::getDatasetId, datasetId);

        wrapper.orderByDesc(DatasetDocument::getCreateTime);

        PageResult<DatasetDocument> pageResult = this.page(wrapper, pageNo, pageSize);

        return new PageModel<>(pageNo, pageSize, pageResult.getTotalSize(), pageResult.getContentData());
    }

    public Map<String, List<DatasetDocument>> listDocumentsBatch(List<String> datasetIds) {
        LambdaQueryChainWrapper<DatasetDocument> wrapper = this.lambdaQuery()
            .in(DatasetDocument::getDatasetId, datasetIds);
        List<DatasetDocument> documents = wrapper.list();
        return documents.stream().collect(Collectors.groupingBy(DatasetDocument::getDatasetId));
    }

    public long countDocumentsByDatasetId(String datasetId) {
        LambdaQueryChainWrapper<DatasetDocument> wrapper = this.lambdaQuery()
            .eq(DatasetDocument::getDatasetId, datasetId);
        return this.count(wrapper);
    }

    /**
     * Deletes a document by its ID.
     *
     * @param id the ID of the document to delete
     */
    public void deleteDocument(String id) {
        removeById(id);
        segmentService.deleteSegmentsByDocumentId(id);
    }

    /**
     * Deletes documents by dataset ID.
     *
     * @param datasetId the ID of the dataset
     */
    public void deleteDocumentsByDatasetId(String datasetId) {
        LambdaUpdateChainWrapper<DatasetDocument> wrapper = this.lambdaUpdate()
            .eq(DatasetDocument::getDatasetId, datasetId);
        this.remove(wrapper);
    }

    /**
     * Toggles the enable flag of a document.
     *
     * @param id the ID of the document
     */
    public void toggleDocumentEnable(String id) {
        DatasetDocument document = getById(id);

        boolean flag = !document.getEnableFlag();
        document.setEnableFlag(flag);
        saveOrUpdate(document);

        segmentService.toggleEnableFlagByDocument(id, flag);
    }

    /**
     * Splits a document into segments.
     *
     * @param form the document
     * @return the list of segments
     */
    public List<Document> splitDocument(
        DocumentCreateForm form, boolean previewFlag, String datasetId, String documentId
    ) throws MalformedURLException {
        List<Document> documents = new ArrayDeque<>();

        if (form.getDataSourceType().equalsIgnoreCase(DatasetSourceType.INPUT.getValue())) {
            Resource textResource = new ByteArrayResource(form.getContent().getBytes());
            TikaDocumentReader documentReader = new TikaDocumentReader(textResource);
            List<Document> docList = documentReader.get();
            docList.stream().forEach(d -> d.getMetadata().remove("source"));
            documents.addAll(docList);
        }
        if (form.getDataSourceType().equalsIgnoreCase(DatasetSourceType.HTML.getValue())) {
            for (String url : form.getHtmlUrl()) {
                Resource urlResource = new UrlResource(url);
                TikaDocumentReader documentReader = new TikaDocumentReader(urlResource);
                documents.addAll(documentReader.get());
            }
        }
        if (form.getDataSourceType().equalsIgnoreCase(DatasetSourceType.FILE.getValue())) {
            //TODO
        }

        documents.forEach(document -> {
            if (StrUtil.isNotBlank(datasetId)) {
                document.getMetadata().put("datasetId", datasetId);
            }
            if (StrUtil.isNotBlank(documentId)) {
                document.getMetadata().put("documentId", documentId);
            }

            if (StrUtil.isNotBlank(form.getMetadata())) {
                JSONObject jsonObject = JSONUtil.parseObj(form.getMetadata());
                for (String key : jsonObject.keySet()) {
                    document.getMetadata().put(key, jsonObject.getStr(key));
                }
            }
        });

//        DocumentSplitter splitter;
//        if (StrUtil.isBlank(form.getSeparator())) {
//            splitter = DocumentSplitterFactory.createRecursive(500, 50);
//        } else {
//            splitter = DocumentSplitterFactory.createCustom(form.getChunkSize(), 50, form.getSeparator());
//        }
//
//        List<Document> segments = splitter.splitAll(documents);

        List<Document> segments = new SimpleDocumentSplitter(form.getSeparator(), form.getChunkSize()).split(documents);

        if (previewFlag) {
            return segments.stream().limit(5).collect(Collectors.toList());
        }

        return segments;
    }

    public void batchDeleteDocuments(List<String> documentIds) {
        removeBatchByIds(documentIds);
        segmentService.deleteSegmentsByDocumentIds(documentIds);
    }

    public DatasetDocument renameDocument(String documentId, String name) {
        DatasetDocument document = getById(documentId);

        document.setName(name);
        updateById(document);
        return document;
    }

}