package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.auth.annotation.IgnoreAuth;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.MarkdownConversionProgressDTO;
import com.litevar.agent.base.entity.UploadFile;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.rest.config.LitevarProperties;
import com.litevar.agent.rest.service.MarkdownConversionService;
import com.litevar.agent.rest.service.UploadFileService;
import com.litevar.agent.rest.util.AgentExportUtil;
import com.litevar.agent.rest.util.FileDownloadUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件
 *
 * @author uncle
 * @since 2024/10/14 17:35
 */
@Slf4j
@RestController
@RequestMapping("/v1/file")
public class FileController {

    @Autowired
    private UploadFileService uploadFileService;
    @Autowired
    private MarkdownConversionService markdownConversionService;
    @Autowired
    private LitevarProperties litevarProperties;

    /**
     * 文件上传
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public ResponseData<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        int index = filename.lastIndexOf('.');
        String suffix = filename.substring(index);
        InputStream inputStream = file.getInputStream();
        String digest = DigestUtils.md5Hex(file.getInputStream());
        File f = new File(litevarProperties.getIconPath() + digest + suffix);
        if (!f.exists()) {
            FileUtil.writeFromStream(inputStream, f);
        }

        return ResponseData.success(f.getName());
    }

    /**
     * 文件下载
     *
     * @param response
     * @param filename
     */
    @IgnoreAuth
    @RequestMapping(value = "/download", method = {RequestMethod.GET, RequestMethod.POST})
    public void download(HttpServletResponse response,
                         @RequestParam("filename") String filename) {
        File file = new File(litevarProperties.getIconPath() + filename);
        if (!file.exists()) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }

        FileDownloadUtil.download(response, file.getName(), FileUtil.readBytes(file));
    }

    /**
     * 知识库上传文件
     *
     * @param file 上传的文件
     * @return 上传后的文件元数据
     */
    @PostMapping("/dataset/upload")
    public ResponseData<String> uploadFileDataset(
        @RequestParam("datasetId") String datasetId,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        UploadFile uf = uploadFileService.saveUploadedFile(datasetId, file);

        if (!uf.getExtension().equalsIgnoreCase("md")) {
            // 异步执行文件转markdown，输出目录为 UploadFile.path/md
            markdownConversionService.convertToMarkdownAsync(uf);
        }

        return ResponseData.success(uf.getId());
    }

    /**
     * 删除文件
     *
     * @param id 文件id
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseData<String> deleteFile(@PathVariable String id) {
        uploadFileService.deleteFileById(id);
        return ResponseData.success();
    }

    /**
     * 知识库下载原文件(用户上传的文件)
     *
     * @param fileId 需要下载的文件id
     */
    @GetMapping("/dataset/file/download")
    public void downloadFileDataset(
        @RequestParam("fileId") String fileId,
        HttpServletResponse response
    ) {
        UploadFile uploadFile = uploadFileService.getById(fileId);
        if (uploadFile == null) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }

        File file = new File(uploadFile.getPath());
        if (!file.exists()) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }

        FileDownloadUtil.download(response, file.getName(), FileUtil.readBytes(file));
    }

    /**
     * 下载知识库markdown文件(转换后的markdown文件及资源)
     *
     * @param fileId   需要下载的文件id
     * @param response http response
     */
    @GetMapping("/dataset/markdown/download")
    public void downloadFolder(
        @RequestParam("fileId") String fileId,
        HttpServletResponse response
    ) {
        UploadFile uf = uploadFileService.getById(fileId);
        if (uf == null) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }
        File markdownFile = new File(uf.getMarkdownPath());
        if (!markdownFile.exists()) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }

        Map<String, byte[]> outputMap = new HashMap<>();
        try {
            //markdown
            String originContent = FileUtil.readString(markdownFile, StandardCharsets.UTF_8);
            String markdownContent = AgentExportUtil.normalizeResourceImagePath(originContent);
            outputMap.put(markdownFile.getName(), markdownContent.getBytes(StandardCharsets.UTF_8));

            //images
            Path markdownDir = Paths.get(markdownFile.getParent());
            Path imagesDir = markdownDir.resolve("imgs");
            if (Files.isDirectory(imagesDir)) {
                try (Stream<Path> imageStream = Files.walk(imagesDir)) {
                    List<Path> imagesFiles = imageStream.filter(Files::isRegularFile).toList();
                    for (Path imagesFile : imagesFiles) {
                        Path imagesPath = imagesDir.relativize(imagesFile);
                        outputMap.put(AgentExportUtil.IMAGES_DIR + imagesPath.toFile().getName(), Files.readAllBytes(imagesFile));
                    }
                }
            }

        } catch (IOException e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }

        String zipName = FilenameUtils.removeExtension(markdownFile.getName()) + ".zip";
        try (ServletOutputStream outputStream = response.getOutputStream();
             ZipOutputStream zos = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            FileDownloadUtil.resetHeader(response, zipName);
            for (Map.Entry<String, byte[]> entry : outputMap.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
            zos.finish();
        } catch (IOException e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
    }

    /**
     * 查询异步markdown转换进度。
     */
    @GetMapping("/dataset/markdown/progress")
    public ResponseData<MarkdownConversionProgressDTO> getMarkdownConversionProgress(
        @RequestParam("fileId") String fileId
    ) {
        UploadFile uf = uploadFileService.getById(fileId);
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
            boolean completed = StrUtil.isNotBlank(uf.getMarkdownPath());
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
     * @param fileId 需要预览的文件id
     * @return markdown文本内容
     */
    @GetMapping("/dataset/markdown/preview")
    public ResponseData<String> previewMarkdown(@RequestParam("fileId") String fileId) {
        UploadFile uploadFile = uploadFileService.getById(fileId);
        if (uploadFile == null || StrUtil.isBlank(uploadFile.getMarkdownPath())) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }

        File markdownFile = new File(uploadFile.getMarkdownPath());
        if (!markdownFile.exists() || !markdownFile.isFile()) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }

        String content = FileUtil.readString(markdownFile, StandardCharsets.UTF_8);

        return ResponseData.success(content);
    }
}
