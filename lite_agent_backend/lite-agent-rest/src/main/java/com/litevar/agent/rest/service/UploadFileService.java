package com.litevar.agent.rest.service;

import cn.hutool.core.io.FileUtil;
import com.litevar.agent.base.entity.FileDigest;
import com.litevar.agent.base.entity.UploadFile;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.core.module.storage.FileDigestService;
import com.litevar.agent.core.module.storage.StorageService;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Service for handling uploaded files metadata persistence.
 */
@Slf4j
@Service
public class UploadFileService extends ServiceImpl<UploadFile> {

    @Autowired
    private StorageService storageService;
    @Autowired
    private FileDigestService fileDigestService;

    public UploadFile saveFile(String datasetId, String filename, byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("file content must not be empty");
        }

        String md5Hex = DigestUtils.md5Hex(content);

        UploadFile existing = searchByDatasetIdAndMd5(datasetId, md5Hex);
        if (existing != null) {
            throw new ServiceException(ServiceExceptionEnum.DUPLICATE_FILE);
        }

        String fileKey = storageService.store("/datasets/" + datasetId, filename, content, "");

        FileDigest fileDigest = fileDigestService.lambdaQuery()
            .eq(FileDigest::getFileKey, fileKey)
            .one();

        String extension = FilenameUtils.getExtension(filename);
        UploadFile uf = new UploadFile();
        uf.setName(filename);
        uf.setPath(fileDigest.getFilePath());
        uf.setMimeType(Objects.requireNonNullElse(contentType, ""));
        uf.setExtension(extension);
        uf.setSize((long) content.length);
        uf.setMd5Hash(md5Hex);
        uf.setUserId(LoginContext.currentUserId());
        uf.setDatasetId(datasetId);

        if ("md".equalsIgnoreCase(extension)) {
            uf.setMarkdownName(filename);
            uf.setMarkdownPath(fileDigest.getFilePath());
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
