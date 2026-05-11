package com.litevar.agent.rest.controller.v2;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.auth.annotation.IgnoreAuth;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.MarkdownConversionProgressDTO;
import com.litevar.agent.base.entity.UploadFileV2;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.core.module.storage.StorageServiceV2;
import com.litevar.agent.rest.service.MarkdownConversionService;
import com.litevar.agent.rest.service.SegmentService;
import com.litevar.agent.rest.service.UploadFileServiceV2;
import com.litevar.agent.rest.util.AgentExportUtil;
import com.litevar.agent.rest.util.FileDownloadUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件v2
 *
 * @author uncle
 * @since 2026/1/20 15:44
 */

@Slf4j
@RestController
@RequestMapping("/v2/file")
public class FileControllerV2 {

    @Resource
    private StorageServiceV2 storageService;
    @Resource
    private UploadFileServiceV2 fileService;
    @Resource
    private MarkdownConversionService markdownConversionService;
    @Resource
    private SegmentService segmentService;

    /**
     * 文件上传
     *
     * @param datasetId 知识库id(可选)
     * @param file      文件
     * @return 文件id
     * @throws IOException
     */
    @PostMapping("/upload")
    public ResponseData<String> upload(
        @RequestParam(value = "datasetId", required = false) String datasetId,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        String result;
        if (StrUtil.isNotBlank(datasetId)) {
            UploadFileV2 uploadFile = fileService.uploadFile(file.getInputStream(), file.getOriginalFilename(), datasetId);

            if (!uploadFile.getExtension().equalsIgnoreCase("md")) {
                markdownConversionService.convertToMarkdownAsync(uploadFile);
            }
            result = uploadFile.getId();
        } else {
            String path = fileService.resolvePath(file.getContentType(), "");
            String fileKey = fileService.uploadFile(file.getInputStream(), path, file.getOriginalFilename(), file.getContentType());
            result = fileService.generateNoExpireFileUrl(fileKey);
        }

        return ResponseData.success(result);
    }

    /**
     * 删除文件
     *
     * @param id 文件id
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseData<String> deleteFile(@PathVariable String id) {
        fileService.deleteFile(id);
        return ResponseData.success();
    }

    /**
     * 文件预览
     *
     * @param fileKey   文件key
     * @param timestamp 时间戳
     * @param sign      签名
     * @param nonce     随机数
     */
    @IgnoreAuth
    @GetMapping("/preview")
    public ResponseEntity<StreamingResponseBody> preview(
        @RequestParam String fileKey,
        @RequestParam String timestamp,
        @RequestParam String sign,
        @RequestParam String nonce
    ) {
        fileService.verifySign(fileKey, timestamp, nonce, sign);
        StreamingResponseBody streamingResponseBody = outputStream -> {
            try (InputStream in = storageService.readFileStream(fileKey)) {
                StreamUtils.copy(in, outputStream);
                outputStream.flush();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException("文件预览失败", e);
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline");
        headers.add(HttpHeaders.CACHE_CONTROL, "public, max-age=259200");
        //浏览器缓存3天
        headers.setExpires(System.currentTimeMillis() + 259200000L);
        return ResponseEntity.ok().headers(headers).body(streamingResponseBody);
    }

    /**
     * 文件下载
     *
     * @param fileId 文件id
     */
    @GetMapping("/download")
    public void downloadFile(
        @RequestParam String fileId,
        HttpServletResponse response
    ) {
        UploadFileV2 uploadFile = fileService.getById(fileId);
        if (uploadFile == null) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }

        byte[] bytes = storageService.readFile(uploadFile.getFileKey());
        FileDownloadUtil.download(response, uploadFile.getFilename(), bytes);
    }

    /**
     * 下载markdown zip
     *
     * @param fileId
     * @param response
     */
    @GetMapping("/download/markdown")
    public void downloadMarkdown(
        @RequestParam String fileId,
        HttpServletResponse response
    ) {
        UploadFileV2 uf = fileService.getById(fileId);
        if (uf == null || StrUtil.isBlank(uf.getMarkdownKey())) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }

        String markdownKey = uf.getMarkdownKey();
        Path markdownPath = Path.of(markdownKey);
        String markdownFilename = markdownPath.getFileName().toString();
        String zipName = FilenameUtils.removeExtension(markdownFilename) + ".zip";

        try (ServletOutputStream outputStream = response.getOutputStream();
             ZipOutputStream zos = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            FileDownloadUtil.resetHeader(response, zipName);

            // 1) markdown 流式写入 zip
            zos.putNextEntry(new ZipEntry(markdownFilename));
            try (InputStream markdownIn = storageService.readFileStream(markdownKey)) {
                StreamUtils.copy(markdownIn, zos);
            }
            zos.closeEntry();

            // 2) images 逐个文件流式写入 zip
            String imageDirKey = markdownPath.getParent().resolve("imgs").normalize().toString();
            List<String> imageKeys = storageService.listFileKeys(imageDirKey);
            for (String imageKey : imageKeys) {
                String relativeName = Path.of(imageDirKey).relativize(Path.of(imageKey)).toString().replace('\\', '/');
                String entryName = AgentExportUtil.IMAGES_DIR + relativeName;

                zos.putNextEntry(new ZipEntry(entryName));
                try (InputStream imageIn = storageService.readFileStream(imageKey)) {
                    StreamUtils.copy(imageIn, zos);
                }
                zos.closeEntry();
            }

            zos.finish();
        } catch (IOException e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
    }

    /**
     * 查询异步markdown转换进度
     */
    @GetMapping("/markdown/progress")
    public ResponseData<MarkdownConversionProgressDTO> getMarkdownConversionProgress(
        @RequestParam("fileId") String fileId
    ) {
        UploadFileV2 uf = fileService.getById(fileId);
        if (uf == null) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }

        String key = String.format(CacheKey.MARKDOWN_CONVERSION_PROGRESS, fileId);
        Object cached = RedisUtil.getValue(key);
        MarkdownConversionProgressDTO progress = null;
        if (cached instanceof MarkdownConversionProgressDTO dto) {
            progress = dto;
        } else if (cached != null) {
            progress = BeanUtil.toBean(cached, MarkdownConversionProgressDTO.class);
        }

        if (progress == null) {
            boolean completed = StrUtil.isNotBlank(uf.getMarkdownKey());
            if (completed) {
                progress = new MarkdownConversionProgressDTO(fileId, 100d, "COMPLETE", "Conversion completed", MarkdownConversionService.STATUS_COMPLETED);
            } else {
                progress = new MarkdownConversionProgressDTO(fileId, 0d, "FAILED", "Conversion failed", MarkdownConversionService.STATUS_FAILED);
            }
        }

        return ResponseData.success(progress);
    }

    /**
     * 预览markdown文件内容
     *
     * @param fileId 文件id
     * @return markdown内容
     */
    @GetMapping("/markdown/preview")
    public ResponseData<String> previewMarkdown(@RequestParam("fileId") String fileId) {
        UploadFileV2 uf = fileService.getById(fileId);
        if (uf == null || StrUtil.isBlank(uf.getMarkdownKey())) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }

        byte[] markdownBytes = storageService.readFile(uf.getMarkdownKey());
        String content = new String(markdownBytes, StandardCharsets.UTF_8);
        String updated = segmentService.replaceImageUrls(content, uf.getId(), SegmentService.MARKDOWN_IMAGE_PATTERN, 1);
        updated = segmentService.replaceImageUrls(updated, uf.getId(), SegmentService.HTML_IMAGE_PATTERN, 2);
        return ResponseData.success(updated);
    }
}
