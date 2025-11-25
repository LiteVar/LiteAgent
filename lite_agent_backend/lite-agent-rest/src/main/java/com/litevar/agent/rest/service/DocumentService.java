package com.litevar.agent.rest.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.entity.DatasetDocument;
import com.litevar.agent.base.entity.DocumentSegment;
import com.litevar.agent.base.entity.UploadFile;
import com.litevar.agent.base.enums.DatasetSourceType;
import com.litevar.agent.base.enums.EmbedStatus;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.DocumentCreateForm;
import com.litevar.agent.rest.springai.document.DocumentSplitter;
import com.litevar.agent.rest.springai.document.DocumentSplitterFactory;
import com.litevar.agent.rest.vector.MilvusService;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.impl.ServiceImpl;
import kotlin.collections.ArrayDeque;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Service implementation for Document management.
 */
@Slf4j
@Service
public class DocumentService extends ServiceImpl<DatasetDocument> {
    @Autowired
    private DatasetService datasetService;
    @Autowired
    private SegmentService segmentService;
    @Autowired
    private UploadFileService uploadFileService;
    @Autowired
    private FileSummaryService fileSummaryService;
    @Autowired
    private MilvusService milvusService;
    @Autowired
    @Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;

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

        List<Document> segments = splitDocument(form, false, datasetId, document.getId());
        List<DocumentSegment> documentSegments = segmentService.embedSegments(
            dataset.getWorkspaceId(), datasetId, document.getId(), document.getFileId(), segments
        );

        document.setWordCount(documentSegments.stream().mapToInt(DocumentSegment::getWordCount).sum());
        document.setTokenCount(documentSegments.stream().mapToInt(DocumentSegment::getTokenCount).sum());
        document.setEmbedStatus(EmbedStatus.SUCCESS.getValue());
        saveOrUpdate(document);

        //摘要
        fileSummaryService.summarizeDocument(datasetId, document);

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

    public String getDocumentSummary(String documentId) {
        DatasetDocument document = Optional.ofNullable(getById(documentId)).orElseThrow();
        Dataset dataset = datasetService.getDataset(document.getDatasetId());
        if (StrUtil.isBlank(dataset.getSummaryCollectionName())) {
            return "";
        }

        return milvusService.getSummaryText(dataset.getSummaryCollectionName(), documentId).orElse("");
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
     * Deletes documents by dataset ID.
     *
     * @param datasetId the ID of the dataset
     */
    public void deleteDocumentsByDatasetId(String datasetId) {
        List<DatasetDocument> list = this.lambdaQuery().eq(DatasetDocument::getDatasetId, datasetId).list();
        deleteDocuments(list);
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
        document.setNeedSummary(Boolean.TRUE);
        saveOrUpdate(document);

        List<DocumentSegment> segmentList = segmentService.lambdaQuery()
                .eq(DocumentSegment::getDocumentId, id).list();

        segmentService.toggleEnableFlag(segmentList, flag);

        //如果有摘要,也要同步该文档摘要的enable字段
        milvusService.toggleEnableFlag("summary_" + document.getDatasetId(), List.of(id), flag);
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
            UploadFile uploadFile = uploadFileService.getById(form.getFileId());
            Resource fileResource = new FileSystemResource(uploadFile.getMarkdownPath());
            TikaDocumentReader documentReader = new TikaDocumentReader(fileResource);
            List<Document> docList = documentReader.get();
            docList.stream().forEach(d -> d.getMetadata().remove("source"));
            documents.addAll(docList);
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

        DocumentSplitter splitter = DocumentSplitterFactory.createDelimiterMerging(form.getChunkSize(), 0, form.getSeparator());
        List<Document> segments = splitter.split(documents.get(0));

        if (previewFlag) {
            return segments.stream().limit(5).collect(Collectors.toList());
        }

        return segments;
    }

    public void batchDeleteDocuments(List<String> documentIds) {
        if (documentIds.isEmpty()) {
            return;
        }
        List<DatasetDocument> documentList = this.getByIds(documentIds);

        deleteDocuments(documentList);
    }

    private void deleteDocuments(List<DatasetDocument> docList) {
        if (docList.isEmpty()) {
            return;
        }
        List<String> docIds = docList.stream().map(DatasetDocument::getId).toList();
        this.removeBatchByIds(docIds);
        segmentService.deleteSegmentsByDocumentIds(docIds);

        //删除文档,如果有摘要,也要删除
        milvusService.removeEmbeddings("summary_" + docList.get(0).getDatasetId(), docIds);

        docList.forEach(doc -> {
            if (StrUtil.isNotBlank(doc.getFileId())) {
                uploadFileService.deleteFileById(doc.getFileId());
            }
        });
    }

    public DatasetDocument renameDocument(String documentId, String name) {
        DatasetDocument document = getById(documentId);

        document.setName(name);
        updateById(document);
        return document;
    }

    public void summarizeNeedSummaryDocuments(String datasetId) {
        List<DatasetDocument> documents = this.lambdaQuery()
                .eq(DatasetDocument::getDatasetId, datasetId)
                .eq(DatasetDocument::getEnableFlag, Boolean.TRUE)
                .eq(DatasetDocument::getNeedSummary, Boolean.TRUE)
                .list();

        if (documents.isEmpty()) {
            return;
        }

        documents.forEach(document ->
                CompletableFuture.runAsync(() -> fileSummaryService.summarizeDocument(datasetId, document), asyncTaskExecutor)
                        .exceptionally(ex -> {
                            log.error("文档摘要失败, datasetId={}, documentId={}", datasetId, document.getId(), ex);
                            return null;
                        })
        );
    }

}
