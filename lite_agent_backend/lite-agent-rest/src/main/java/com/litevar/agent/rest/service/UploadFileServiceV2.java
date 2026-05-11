package com.litevar.agent.rest.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.litevar.agent.base.entity.UploadFileV2;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.core.module.storage.StorageServiceV2;
import com.litevar.agent.rest.config.LitevarProperties;
import com.litevar.agent.rest.config.StorageProperties;
import com.mongoplus.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author reid
 * @since 2026/1/23
 */

@Slf4j
@Service
public class UploadFileServiceV2 extends ServiceImpl<UploadFileV2> {
    private static final String FILE_PREVIEW_PATH = "/v2/file/preview";
    private static final String SIGN_KEY = "NnyagRqQ0prvizTa";

    private static final Set<String> DOCUMENT_MIME = Set.of(
        "application/json",
        "application/xml",
        "application/yaml",
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Autowired
    private ServerProperties serverProperties;
    @Autowired
    private LitevarProperties litevarProperties;
    @Autowired
    private StorageProperties storageProperties;
    @Autowired
    private StorageServiceV2 storageService;

    public String uploadFile(InputStream inputStream, String resourcePath, String filename, String contentType) {
        MessageDigest md = DigestUtils.getMd5Digest();
        long size = 0;
        Path tempFile = null;
        try (InputStream in = inputStream) {
            tempFile = Files.createTempFile("upload-", ".tmp");
            try (OutputStream out = Files.newOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    md.update(buffer, 0, len);
                    size += len;
                }
            }
        } catch (IOException e) {
            deleteTempFile(tempFile);
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
        String fileHash = Hex.encodeHexString(md.digest());
        String ext = FilenameUtils.getExtension(filename);
        //文件名替换为hash值
        filename = fileHash + "." + ext;
        String fileKey = buildFileKey(resourcePath, filename);
        UploadFileV2 existing = findDuplicateFile(fileKey, size);
        if (existing != null) {
            deleteTempFile(tempFile);
            return existing.getFileKey();
        }

        try (InputStream tempIn = Files.newInputStream(tempFile)) {
            storageService.writeFile(fileKey, tempIn);

            UploadFileV2 uploadFileV2 = new UploadFileV2();
            uploadFileV2.setFilename(filename);
            uploadFileV2.setFileKey(fileKey);
            uploadFileV2.setMd5Hash(fileHash);
            uploadFileV2.setSize(size);
            uploadFileV2.setExtension(ext);
            uploadFileV2.setStorageType(storageProperties.getType());
            uploadFileV2.setMimeType(contentType);
            this.save(uploadFileV2);
            return fileKey;
        } catch (IOException e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignore) {
        }
    }

    private UploadFileV2 findDuplicateFile(String fileKey, long size) {
        UploadFileV2 exist = this.lambdaQuery().eq(UploadFileV2::getFileKey, fileKey).one();
        if (exist != null && exist.getSize() != size) {
            throw new ServiceException(1000, "MD5碰撞");
        }
        return exist;
    }

    /**
     * 保存上传文件
     *
     * @param datasetId 知识库ID，可选
     * @return 文件ID
     * @throws IOException
     */
    public UploadFileV2 uploadFile(InputStream inputStream, String filename, String datasetId) throws IOException {
        String userId = LoginContext.currentUserId();

        byte[] bytes = inputStream.readAllBytes();
        String md5Hash = DigestUtils.md5Hex(bytes);
        String suffixPath = userId;

        if (datasetId != null) {
            UploadFileV2 exist = searchByDatasetAndMd5(datasetId, md5Hash);
            if (exist != null) {
                throw new ServiceException(ServiceExceptionEnum.DUPLICATE_FILE);
            }

            suffixPath = datasetId + "/" + md5Hash;
        }

        String mimeType = getMimeType(bytes);
        String path = resolvePath(mimeType, suffixPath);

        //避免用户上传的文件名过长,使用md5值作为文件名
        String extension = FilenameUtils.getExtension(filename);
        String newFilename = md5Hash + "." + extension;
        String fileKey = buildFileKey(path, newFilename);

        storageService.writeFile(fileKey, bytes);

        UploadFileV2 uploadFile = new UploadFileV2();
        uploadFile.setUserId(userId);
        uploadFile.setDatasetId(datasetId);
        uploadFile.setFilename(filename);
        uploadFile.setFileKey(fileKey);
        uploadFile.setMimeType(mimeType);
        uploadFile.setExtension(extension);
        uploadFile.setSize((long) bytes.length);
        uploadFile.setMd5Hash(md5Hash);
        uploadFile.setStorageType(storageProperties.getType());

        if (extension.equalsIgnoreCase("md")) {
            uploadFile.setMarkdownKey(fileKey);
        }

        this.save(uploadFile);
        return uploadFile;
    }

    /**
     * 保存转换后的Markdown文件信息
     *
     * @param fileId       文件id
     * @param markdownPath markdown文件路径
     */
    public void saveConvertedMarkdown(String fileId, Path markdownPath, String content) {
        UploadFileV2 uf = this.getById(fileId);

        String markdownKey = StrUtil.removePrefix(markdownPath.toAbsolutePath().toString(), storageProperties.getBasePathLocal() + "/");

        if (!content.isBlank()) {
            storageService.writeFile(markdownKey, content.getBytes(StandardCharsets.UTF_8));
        } else {
            // content为空,扫描版pdf,使用oss存储时,需要将markdown文件上传到oss
            if (storageProperties.getType().equalsIgnoreCase("oss")) {
                storageService.writeFile(markdownKey, FileUtil.readBytes(markdownPath));
            }
        }

        uf.setMarkdownKey(markdownKey);
        saveOrUpdate(uf);
    }

    /**
     * 保存Markdown图片
     *
     * @param bytes     图片数据
     * @param filename  图片文件名
     * @param srcDir    图片所在目录
     */
    public UploadFileV2 saveMarkdownImage(byte[] bytes, String filename, String srcDir) {
        String keyPrefix = StrUtil.removePrefix(srcDir, storageProperties.getBasePathLocal() + "/");
        String imageKey = keyPrefix + "/" + filename;
        storageService.writeFile(imageKey, bytes);

        return null;
    }

    /**
     * 保存扫描版pdf转换的markdown图片
     *
     * @param fileId    用户上传的原始文件id
     * @param imageDir  DolphinPdfMarkdownClient回传图片的本地目录
     */
    public void saveDolphinMarkdownImage(String fileId, Path imageDir) {
        if (storageProperties.getType().equalsIgnoreCase("local")) {
            return;
        }

        try (Stream<Path> imageStream = Files.walk(imageDir)) {
            List<Path> images = imageStream.filter(Files::isRegularFile).toList();
            images.parallelStream().forEach(image -> {
                try {
                    String imageKey = StrUtil.removePrefix(image.toAbsolutePath().toString(), storageProperties.getBasePathLocal() + "/");
                    storageService.writeFile(imageKey, FileUtil.readBytes(image));
                } catch (Exception ex) {
                    log.error("Failed to save markdown image for fileId={}, image={}", fileId, image, ex);
                }
            });
        } catch (IOException e) {
            log.error("Failed to save markdown images for fileId={}, imageDir={}", fileId, imageDir, e);
        }
    }

    public void deleteFile(String fileId) {
        UploadFileV2 uf = this.getById(fileId);
        storageService.deleteFile(uf.getFileKey());
        if (StrUtil.isNotBlank(uf.getMarkdownKey())) {
            //知识库文件还需要删除markdown和图片
            storageService.deleteDir(uf.getMarkdownKey());
        }

        this.removeById(fileId);
    }

    public UploadFileV2 searchByDatasetAndMd5(String datasetId, String md5) {
        return this.lambdaQuery()
            .eq(UploadFileV2::getDatasetId, datasetId)
            .eq(UploadFileV2::getMd5Hash, md5)
            .one();
    }

    public String generateFileUrl(String fileKey) {
        return UriComponentsBuilder.fromUriString(litevarProperties.getExternalApiUrl())
            .path(serverProperties.getServlet().getContextPath())
            .path(FILE_PREVIEW_PATH)
            .queryParam("fileKey", fileKey)
            .build()
            .toUriString();
    }

    public String generateSignFileUrl(String fileKey) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000 + storageProperties.getAccessTimeout());
        return generateUrlInternal(fileKey, timestamp);
    }

    public String generateNoExpireFileUrl(String fileKey) {
        return generateUrlInternal(fileKey, "-1");
    }

    private String generateUrlInternal(String fileKey, String timestamp) {
        Map<String, String> queryParams = sign(fileKey, timestamp);
        return UriComponentsBuilder.fromUriString(litevarProperties.getExternalApiUrl())
            .path(serverProperties.getServlet().getContextPath())
            .path(FILE_PREVIEW_PATH)
            .queryParam("fileKey", fileKey)
            .queryParam("sign", queryParams.get("sign"))
            .queryParam("nonce", queryParams.get("nonce"))
            .queryParam("timestamp", queryParams.get("timestamp"))
            .build()
            .toUriString();
    }

    private String getMimeType(byte[] bytes) {
        Tika tika = new Tika();
        return tika.detect(bytes).toLowerCase();
    }

    /**
     * Resolve storage path based on MIME type and userId/datasetId.
     *
     * @param mimeType   mime type of the file
     * @param suffixPath userId or datasetId
     * @return resolved storage path
     */
    public String resolvePath(String mimeType, String suffixPath) {
        String path = "";
        if (mimeType.startsWith("image/")) {
            path = storageProperties.getImagePath();
        } else if (mimeType.startsWith("audio/")) {
            path = storageProperties.getAudioPath();
        } else if (mimeType.startsWith("video/")) {
            path = storageProperties.getVideoPath();
        } else if (mimeType.startsWith("text/") || DOCUMENT_MIME.contains(mimeType)) {
            path = storageProperties.getDocumentPath();
        } else {
            throw new RuntimeException("Unsupported mime type: " + mimeType);
        }
        return Path.of(path, suffixPath).normalize().toString();
    }

    public String buildFileKey(String path, String filename) {
        return Path.of(path, filename).normalize().toString();
    }

    public Map<String, String> sign(String fileKey, String timestamp) {
        String nonce = RandomUtil.randomString(16);

        String dataToSign = String.format("file-preview|%s|%s|%s", fileKey, timestamp, nonce);
        String sign = Base64.getUrlEncoder().encodeToString(
            SecureUtil.hmacSha256(SIGN_KEY).digestHex(dataToSign).getBytes(StandardCharsets.UTF_8)
        );

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("timestamp", timestamp);
        queryParams.put("nonce", nonce);
        queryParams.put("sign", sign);

        return queryParams;
    }

    public void verifySign(String fileKey, String timestamp, String nonce, String sign) {
        String dataToSign = String.format("file-preview|%s|%s|%s", fileKey, timestamp, nonce);
        String expectedSign = Base64.getUrlEncoder().encodeToString(
            SecureUtil.hmacSha256(SIGN_KEY).digestHex(dataToSign).getBytes(StandardCharsets.UTF_8)
        );

        if (!expectedSign.equals(sign)) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }

        long ts = Long.parseLong(timestamp);
        long now = System.currentTimeMillis() / 1000;
        if (ts > 0 && Math.abs(now - ts) > storageProperties.getAccessTimeout()) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
    }

    public String fileBase64Str(String fileKey) {
        byte[] bytes = storageService.readFile(fileKey);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "data:" + getMimeType(bytes) + ";base64," + base64;
    }

}
