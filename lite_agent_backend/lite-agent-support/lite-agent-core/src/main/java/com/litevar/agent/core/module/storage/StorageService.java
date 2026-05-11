package com.litevar.agent.core.module.storage;

import cn.hutool.crypto.SecureUtil;
import com.litevar.agent.base.entity.FileDigest;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * 文件存储
 *
 * @author uncle
 * @since 2025/12/29 09:50
 */
public interface StorageService {
    /**
     * 语音文件
     */
    String PATH_AGENT_AUDIO = "multimedia/audio/%s/%s";

    String RESOURCE_PATH = "/v1/file/resource/download";

    FileDigestService getFileDigestService();

    SecretKeyService getSecretKeyService();

    String getExternalApiUrl();

    String getContextPath();

    String writeBytes(String uploadPath, String filename, byte[] fileContent, String contentType) throws IOException;

    String writeStream(String uploadPath,
                       String filename,
                       InputStream inputStream,
                       String contentType,
                       long size) throws IOException;

    /**
     * 根据key生成可读的临时URL
     */
    String generateUrl(String key);

    /**
     * 生成文件的base64编码
     * 用于大模型访问不了文件,只能通过base64方式传参
     *
     * @param key
     * @return 返回格式: data:[MIME_type];base64,{base64_image}
     */
    String fileToBase64(String key);

    /**
     * 保存文件
     *
     * @param uploadPath  文件上传相对路径
     * @param filename    文件名
     * @param fileContent 文件二进制内容
     * @param contentType 文件类型
     * @return 存储key
     */
    default String store(String uploadPath, String filename, byte[] fileContent, String contentType) {
        String fileHash = DigestUtils.md5Hex(fileContent);

        FileDigest existing = findDuplicateFile(uploadPath, fileHash, fileContent.length);
        if (existing != null) {
            return existing.getFileKey();
        }
        try {
            String storedFilename = buildStoredFilename(filename, fileHash);
            String fileKey = writeBytes(uploadPath, storedFilename, fileContent, contentType);
            return saveDigest(uploadPath, filename, fileHash, fileContent.length, fileKey);
        } catch (IOException e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
    }

    /**
     * 流式保存文件
     *
     * @param uploadPath  文件上传相对路径
     * @param filename    文件名
     * @param inputStream 文件输入流
     * @param contentType 文件类型
     * @return 存储key
     */
    default String store(String uploadPath, String filename, InputStream inputStream, String contentType) {
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
        FileDigest existing = findDuplicateFile(uploadPath, fileHash, size);
        if (existing != null) {
            deleteTempFile(tempFile);
            return existing.getFileKey();
        }
        try (InputStream tempIn = Files.newInputStream(tempFile)) {
            String storedFilename = buildStoredFilename(filename, fileHash);
            String fileKey = writeStream(uploadPath, storedFilename, tempIn, contentType, size);
            return saveDigest(uploadPath, filename, fileHash, size, fileKey);
        } catch (IOException e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    /**
     * 根据key生成不会过期的URL(指定本服务的接口)
     */
    default String generateUrlNoExpire(String key) {
        return buildUrl(key, -1);
    }

    /**
     * 查询是否存在重复文件
     */
    default FileDigest findDuplicateFile(String uploadPath, String fileHash, long fileSize) {
        //md5去重
        FileDigest one = getFileDigestService().lambdaQuery()
                .eq(FileDigest::getFilePath, uploadPath)
                .eq(FileDigest::getFileHash, fileHash)
                .one();
        if (one != null && one.getFileSize() != fileSize) {
            throw new ServiceException(1000, "MD5碰撞");
        }
        return one;
    }

    default String sign(String payload, byte[] secretKey) {
        return SecureUtil.hmacSha256(secretKey).digestHex(payload);
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

    private String buildStoredFilename(String filename, String fileHash) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > -1 && dotIndex < filename.length() - 1) {
            return fileHash + filename.substring(dotIndex);
        }
        return fileHash;
    }

    default String buildUrl(String key, long expires) {
        String baseUrl = getExternalApiUrl();
        byte[] fileSecretKey = getSecretKeyService().getSecretKey(SecretKeyService.FILE_READ_KEY);
        String sign = sign(key + ":" + expires, fileSecretKey);
        UriComponentsBuilder builder = baseUrl != null && !baseUrl.isBlank()
                ? UriComponentsBuilder.fromUriString(baseUrl)
                : UriComponentsBuilder.newInstance();
        String contextPath = getContextPath();
        if (contextPath != null && !contextPath.isBlank()) {
            builder.path(contextPath);
        }
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        String encodedSign = URLEncoder.encode(sign, StandardCharsets.UTF_8);
        UriComponentsBuilder urlBuilder = builder.path(RESOURCE_PATH)
                .queryParam("key", encodedKey)
                .queryParam("sign", encodedSign);
        if (expires != -1) {
            String encodedExpires = URLEncoder.encode(String.valueOf(expires), StandardCharsets.UTF_8);
            urlBuilder.queryParam("expires", encodedExpires);
        }
        return urlBuilder.build(true).toUriString();
    }

    private String saveDigest(String uploadPath, String filename, String fileHash, long fileSize, String fileKey) {
        FileDigest digest = new FileDigest();
        digest.setFilePath(uploadPath);
        digest.setFilename(filename);
        digest.setFileHash(fileHash);
        digest.setFileKey(fileKey);
        digest.setFileSize(fileSize);
        getFileDigestService().save(digest);
        return fileKey;
    }
}
