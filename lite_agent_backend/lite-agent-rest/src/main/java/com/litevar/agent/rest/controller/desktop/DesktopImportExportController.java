package com.litevar.agent.rest.controller.desktop;

import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.rest.util.AgentExportUtil;
import com.litevar.agent.rest.util.AgentImportUtil;
import com.litevar.agent.rest.util.FileDownloadUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

/**
 * desktop导入导出接口
 *
 * @author uncle
 * @since 2025/10/30 17:41
 */
@RestController
@RequestMapping("/desktop/import")
public class DesktopImportExportController {

    @Autowired
    private AgentImportUtil agentImportUtil;
    @Autowired
    private AgentExportUtil agentExportUtil;

    /**
     * 上传知识库文件压缩包
     * 读取文件并返回数据供用户预览
     *
     * @param file 压缩包
     * @return
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseData<AgentImportUtil.ImportDescriptor> importKnowledgeBase(@RequestParam("file") MultipartFile file,
                                                                              @RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId) {
        AgentImportUtil.ImportDescriptor importDescriptor = agentImportUtil.previewKnowledge(file, workspaceId);
        return ResponseData.success(importDescriptor);
    }

    /**
     * 保存知识库数据
     *
     * @param workspaceId 工作空间id
     * @param token       上一步返回的数据中token字段值
     * @param param       修改后的数据
     * @return
     */
    @PostMapping("/save/{token}")
    public ResponseData<Map<String, String>> saveData(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                                      @RequestBody @Validated(value = AddAction.class) AgentImportUtil.ImportDescriptor param,
                                                      @PathVariable("token") String token) {
        Map<String, String> datasetIdMap = agentImportUtil.importKnowledge(workspaceId, token, param);
        return ResponseData.success(datasetIdMap);
    }

    /**
     * 导出知识库
     *
     * @param plainText  是否明文,默认为false
     * @param datasetIds 知识库id
     * @param response
     * @throws IOException
     */
    @GetMapping("/exportKnowledge")
    public void exportKnowledge(@RequestParam(value = "plainText", defaultValue = "false") boolean plainText,
                                @RequestParam("datasetIds") Set<String> datasetIds,
                                HttpServletResponse response) throws IOException {
        Set<String> allModelIds = new HashSet<>();
        //{filename,fileByte[]}
        Map<String, byte[]> knowledgeFolder = agentExportUtil.exportKnowledge(datasetIds, allModelIds);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            //models folder
            agentExportUtil.exportModel(allModelIds, zos, plainText);

            //knowledge_bases folder
            for (Map.Entry<String, byte[]> entry : knowledgeFolder.entrySet()) {
                agentExportUtil.writeEntry(zos, AgentExportUtil.KNOWLEDGE_BASE_DIR + entry.getKey(), entry.getValue());
            }

            zos.finish();
            byte[] byteArray = outputStream.toByteArray();
            FileDownloadUtil.download(response, "knowledge.zip", byteArray);
        }
    }
}
