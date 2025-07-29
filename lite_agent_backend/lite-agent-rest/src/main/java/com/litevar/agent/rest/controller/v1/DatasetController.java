package com.litevar.agent.rest.controller.v1;

import com.litevar.agent.auth.annotation.IgnoreAuth;
import com.litevar.agent.auth.annotation.WorkspaceRole;
import com.litevar.agent.auth.util.JwtUtil;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.entity.DatasetDocument;
import com.litevar.agent.base.entity.DatasetRetrieveHistory;
import com.litevar.agent.base.entity.DocumentSegment;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.vo.*;
import com.litevar.agent.rest.service.DatasetRetrieveHistoryService;
import com.litevar.agent.rest.service.DatasetService;
import com.litevar.agent.rest.service.DocumentService;
import com.litevar.agent.rest.service.SegmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.util.List;

/**
 * 知识库
 *
 * @author reid
 * @since 2/19/25
 */
@Validated
@RestController
@RequestMapping("/v1/dataset")
public class DatasetController {

    @Autowired
    private DatasetService datasetService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private SegmentService segmentService;
    @Autowired
    private DatasetRetrieveHistoryService retrieveHistoryService;

    /**
     * 创建一个新的知识库。
     *
     * @param workspaceId 工作空间标识符
     * @param form        要创建的知识库
     * @return 创建的知识库
     */
    @PostMapping("/add")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<Dataset> createDataset(
        @RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
        @Validated @RequestBody DatasetCreateForm form,
        HttpServletRequest request
    ) {
        return ResponseData.success(datasetService.createDataset(workspaceId, form, request));
    }

    /**
     * 通过ID获取知识库。
     *
     * @param id 知识库ID
     * @return 知识库详情
     */
    @GetMapping("/{id}")
    public ResponseData<DatasetsVO> getDataset(@PathVariable String id) {
        return ResponseData.success(datasetService.info(id));
    }

    /**
     * 获取知识库的分页列表。
     *
     * @param workspaceId 工作空间标识符
     * @param query       查询关键字
     * @param pageNo      页码
     * @param pageSize    每页大小
     * @return 知识库的分页列表
     */
    @GetMapping("/list")
    public ResponseData<PageModel<DatasetsVO>> listDatasets(
        @RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
        @RequestParam(required = false, defaultValue = "") String query,
        @RequestParam(defaultValue = "0") Integer pageNo,
        @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return ResponseData.success(datasetService.listDatasets(workspaceId, query, pageNo, pageSize));
    }

    /**
     * 更新现有知识库。
     *
     * @param id   知识库ID
     * @param form 更新的知识库信息
     * @return 更新后的知识库
     */
    @PutMapping("/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<Dataset> updateDataset(
        @PathVariable String id,
        @Validated @RequestBody DatasetCreateForm form) {
        return ResponseData.success(datasetService.updateDataset(id, form));
    }

    /**
     * 通过ID删除知识库。
     *
     * @param id 知识库ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> deleteDataset(@PathVariable String id) {
        datasetService.deleteDataset(id);
        return ResponseData.success();
    }

    /**
     * 切换知识库共享状态。
     *
     * @param id 知识库ID
     * @return 成功响应
     */
    @PutMapping("/{id}/share")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> toggleShare(@PathVariable String id) {
        datasetService.toggleShare(id);
        return ResponseData.success();
    }

    /**
     * 召回测试
     *
     * @param datasetId 知识库ID
     * @param query   要检索的内容
     * @return 检索到的片段
     */
    @GetMapping("/{id}/retrieve")
    public ResponseData<List<SegmentVO>> retrieveTest(
        @PathVariable("id") String datasetId,
        @RequestParam String query
    ) {
        return ResponseData.success(datasetService.retrieve(datasetId, query, ""));
    }

    @IgnoreAuth
    @GetMapping("/{id}/retrieve/external")
    public ResponseData<List<SegmentVO>> retrieveExternal(
        @PathVariable("id") String datasetId,
        @RequestParam String query,
        HttpServletRequest request
    ) {
        String token = JwtUtil.getApikeyFromRequest(request);
        return ResponseData.success(datasetService.retrieve(datasetId, query, token));
    }

    /**
     * 检索记录
     *
     * @param datasetId 知识库ID
     * @param pageNo    页码
     * @param pageSize  每页大小
     * @return 响应数据
     */
    @GetMapping("/{id}/retrieve/history")
    public ResponseData<PageModel<DatasetRetrieveHistory>> retrieveHistory(
        @PathVariable("id") String datasetId,
        @RequestParam(defaultValue = "0") Integer pageNo,
        @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return ResponseData.success(retrieveHistoryService.getHistoryByDatasetId(datasetId, pageNo, pageSize));
    }

    /**
     * 检索记录 片段详情
     *
     * @param historyId id
     * @return
     */
    @GetMapping("/retrieve/history/{id}")
    public ResponseData<List<SegmentVO>> retrieveHistoryDetail(@PathVariable("id") String historyId) {
        return ResponseData.success(retrieveHistoryService.historyDetail(historyId));
    }

    /**
     * 生成API Key
     * @param id 知识库ID
     * @return Dataset
     */
    @GetMapping("/{id}/apiKey/generate")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<Dataset> generateApiKey(
        @PathVariable("id") String id,
        HttpServletRequest request
    ) {
        return ResponseData.success(datasetService.generateApiKey(id, request));
    }

    /**
     * 创建新文档。
     *
     * @param datasetId 知识库ID
     * @param form      要创建的文档
     * @return 文档
     */
    @PostMapping("/{datasetId}/documents")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<DatasetDocument> createDocument(
        @PathVariable String datasetId,
        @RequestBody DocumentCreateForm form
    ) throws MalformedURLException {
        return ResponseData.success(documentService.createDocument(datasetId, form));
    }

    /**
     * 预览将文档分割为片段。
     *
     * @param form       文档表单
     * @return 成功响应
     */
    @PostMapping("/documents/split")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<List<String>> splitDocumentPreview(
        @RequestBody DocumentCreateForm form
    ) throws MalformedURLException {
        List<Document> segments = documentService.splitDocument(form, true, "", "");
        return ResponseData.success(segments.stream().map(Document::getText).toList());
    }

    /**
     * 重命名文档。
     *
     * @param documentId 文档ID
     * @param name       新名称
     * @return 更新后的文档
     */
    @PutMapping("/documents/{documentId}/rename")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<DatasetDocument> updateDocumentName(
        @PathVariable String documentId,
        @RequestParam String name
    ) {
        return ResponseData.success(documentService.renameDocument(documentId, name));
    }

    /**
     * 通过知识库ID获取文档。
     *
     * @param datasetId 知识库ID
     * @param pageNo    页码
     * @param pageSize  每页大小
     * @return 文档的分页列表
     */
    @GetMapping("/{datasetId}/documents")
    public ResponseData<PageModel<DatasetDocument>> listDocuments(
        @PathVariable String datasetId,
        @RequestParam(defaultValue = "0") Integer pageNo,
        @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return ResponseData.success(documentService.listDocuments(datasetId, pageNo, pageSize));
    }

    /**
     * 通过ID删除文档。
     *
     * @param documentId 文档ID
     * @return 成功响应
     */
    @DeleteMapping("/documents/{documentId}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> deleteDocument(@PathVariable String documentId) {
        documentService.deleteDocument(documentId);
        return ResponseData.success();
    }

    /**
     * 批量删除文档。
     *
     * @param documentIds 要删除的文档ID列表
     * @return 成功响应
     */
    @DeleteMapping("/documents/batchDelete")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> batchDeleteDocuments(@RequestBody List<String> documentIds) {
        documentService.batchDeleteDocuments(documentIds);
        return ResponseData.success();
    }

    /**
     * 通过文档ID获取文档片段，带分页。
     *
     * @param documentId 文档ID
     * @param pageNo     页码
     * @param pageSize   每页大小
     * @return 文档片段的分页列表
     */
    @GetMapping("/documents/{documentId}/segments")
    public ResponseData<PageModel<DocumentSegment>> listSegments(
        @PathVariable String documentId,
        @RequestParam String query,
        @RequestParam(defaultValue = "0") Integer pageNo,
        @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return ResponseData.success(segmentService.listSegments(documentId, query, pageNo, pageSize));
    }

    /**
     * 切换文档启用状态。
     *
     * @param documentId 文档ID
     * @return 成功响应
     */
    @PutMapping("/documents/{documentId}/enable")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> toggleDocumentEnable(@PathVariable String documentId) {
        documentService.toggleDocumentEnable(documentId);
        return ResponseData.success();
    }

    /**
     * 切换片段启用状态。
     *
     * @param segmentId 片段ID
     * @return 成功响应
     */
    @PutMapping("/segments/{segmentId}/enable")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> toggleSegmentEnable(@PathVariable String segmentId) {
        segmentService.toggleEnableFlag(segmentId);
        return ResponseData.success();
    }

    /**
     * 创建新片段。
     *
     * @param documentId 文档ID
     * @param form       要创建的片段
     * @return 创建的片段
     */
    @PostMapping("/documents/{documentId}/segments")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<DocumentSegment> createSegment(
        @PathVariable String documentId,
        @Validated @RequestBody SegmentUpdateForm form
    ) {
        return ResponseData.success(segmentService.createSegment(documentId, form));
    }

    /**
     * 通过ID更新片段。
     *
     * @param segmentId 片段ID
     * @param form      更新表单
     * @return 更新后的片段
     */
    @PutMapping("/segments/{segmentId}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<DocumentSegment> updateSegment(
        @PathVariable String segmentId,
        @Validated @RequestBody SegmentUpdateForm form
    ) {
        return ResponseData.success(segmentService.updateSegment(segmentId, form));
    }

    /**
     * 通过ID删除片段。
     *
     * @param segmentId 片段ID
     * @return 成功响应
     */
    @DeleteMapping("/segments/{segmentId}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> deleteSegment(@PathVariable String segmentId) {
        segmentService.deleteSegment(segmentId);
        return ResponseData.success();
    }

    /**
     * 批量删除片段。
     *
     * @param segmentIds 要删除的片段ID列表
     * @return 成功响应
     */
    @DeleteMapping("/segments/batchDelete")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> batchDeleteSegments(@RequestBody List<String> segmentIds) {
        segmentService.deleteSegments(segmentIds);
        return ResponseData.success();
    }

}
