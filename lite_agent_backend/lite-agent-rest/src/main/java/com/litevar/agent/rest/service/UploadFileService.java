package com.litevar.agent.rest.service;

import cn.hutool.core.io.FileUtil;
import com.litevar.agent.base.entity.UploadFile;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.rest.config.LitevarProperties;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Service for handling uploaded files metadata persistence.
 */
@Slf4j
@Service
public class UploadFileService extends ServiceImpl<UploadFile> {

    @Autowired
    private LitevarProperties litevarProperties;

    public UploadFile saveUploadedFile(String datasetId, MultipartFile file) throws IOException {
        String md5Hex = DigestUtils.md5Hex(file.getInputStream());
        UploadFile existing = searchByDatasetIdAndMd5(datasetId, md5Hex);
        if (existing != null) {
            throw  new ServiceException(ServiceExceptionEnum.DUPLICATE_FILE);
        }

        String filename = file.getOriginalFilename();
        byte[] content = file.getBytes();
        String contentType = file.getContentType();
        return saveFile(datasetId, filename, content, contentType);
    }

    public UploadFile saveFile(String datasetId, String filename, byte[] content, String contentType) throws IOException {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("file content must not be empty");
        }

        String fileMd5 = DigestUtils.md5Hex(content);

        String originalName = FilenameUtils.getName(Objects.requireNonNullElse(filename, ""));
        if (originalName.isBlank()) {
            originalName = fileMd5;
        }

        // 保存路径: uploadPath/datasets/{datasetId}/fileMd5/
        Path dir = Paths.get(litevarProperties.getUploadPath(), "datasets", datasetId, fileMd5);
        Files.createDirectories(dir);

        Path target = dir.resolve(originalName);
        Files.write(target, content);

        String extension = FilenameUtils.getExtension(originalName);
        UploadFile uf = new UploadFile();
        uf.setName(originalName);
        uf.setPath(target.toAbsolutePath().toString());
        uf.setMimeType(Objects.requireNonNullElse(contentType, ""));
        uf.setExtension(extension);
        uf.setSize((long) content.length);
        uf.setMd5Hash(fileMd5);
        uf.setUserId(LoginContext.currentUserId());
        uf.setDatasetId(datasetId);

        if ("md".equalsIgnoreCase(extension)) {
            uf.setMarkdownName(originalName);
            uf.setMarkdownPath(target.toAbsolutePath().toString());
        }

        this.save(uf);
        return uf;
    }

    /**
     * Search for an uploaded file by its MD5 hash.
     *
     * @param md5 MD5 hash of the file
     * @return UploadFile entity if found, else null
     */
    public UploadFile searchByMd5(String md5) {
        return new LambdaQueryChainWrapper<>(getBaseMapper(), UploadFile.class)
                .eq(UploadFile::getMd5Hash, md5)
                .one();
    }

    public UploadFile searchByDatasetIdAndMd5(String datasetId, String md5) {
        return new LambdaQueryChainWrapper<>(getBaseMapper(), UploadFile.class)
                .eq(UploadFile::getDatasetId, datasetId)
                .eq(UploadFile::getMd5Hash, md5)
                .one();
    }

    /**
     * 删除文件并清理磁盘上的资源。
     *
     * @param fileId 文件ID
     * @return 删除是否成功
     */
    public boolean deleteFileById(String fileId) {
        UploadFile uploadFile = getById(fileId);
        if (uploadFile == null) {
            return false;
        }

        boolean removed = this.removeById(fileId);
        if (!removed) {
            log.warn("Failed to delete upload file, fileId={}", fileId);
            return false;
        }

        removeLocalFile(uploadFile);
        return true;
    }

    private void removeLocalFile(UploadFile uploadFile) {
        Path path = Path.of(uploadFile.getPath()).getParent();
        FileUtil.del(path);
    }

}
