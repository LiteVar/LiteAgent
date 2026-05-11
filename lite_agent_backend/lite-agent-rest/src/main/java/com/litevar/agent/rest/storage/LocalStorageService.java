package com.litevar.agent.rest.storage;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.storage.FileDigestService;
import com.litevar.agent.core.module.storage.SecretKeyService;
import com.litevar.agent.core.module.storage.StorageService;
import com.litevar.agent.rest.config.LitevarProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件系统存储
 *
 * @author uncle
 * @since 2025/12/29 09:58
 */
@Slf4j
@Component
public class LocalStorageService implements StorageService {

    private final FileDigestService fileDigestService;
    private final SecretKeyService secretKeyService;
    private final String uploadRootPath;
    private final String externalApiUrl;
    private final String contextPath;

    public LocalStorageService(LitevarProperties litevarProperties,
                               FileDigestService fileDigestService,
                               SecretKeyService secretKeyService,
                               @Value("${server.servlet.context-path:}") String contextPath) {
        this.fileDigestService = fileDigestService;
        this.secretKeyService = secretKeyService;
        this.uploadRootPath = litevarProperties.getUploadPath();
        this.externalApiUrl = litevarProperties.getExternalApiUrl();
        this.contextPath = contextPath;
    }

    @Override
    public FileDigestService getFileDigestService() {
        return fileDigestService;
    }

    @Override
    public SecretKeyService getSecretKeyService() {
        return secretKeyService;
    }

    @Override
    public String getExternalApiUrl() {
        return externalApiUrl;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String writeBytes(String uploadPath, String filename, byte[] fileContent, String contentType) throws IOException {
        Path target = resolveTargetPath(uploadPath, filename);
        if (Files.notExists(target)) {
            Files.createDirectories(target.getParent());
            Files.write(target, fileContent);
        }
        return encodeFilePathKey(uploadPath, filename);
    }

    @Override
    public String writeStream(String uploadPath, String filename, InputStream inputStream, String contentType, long size) throws IOException {
        Path target = resolveTargetPath(uploadPath, filename);
        if (Files.notExists(target)) {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target);
        }
        return encodeFilePathKey(uploadPath, filename);
    }

    @Override
    public String fileToBase64(String key) {
        Path basePath = getBasePath();
        try {
            String rawPath = decodeFilePathKey(key);
            Path target = basePath.resolve(rawPath).normalize();
            if (!Files.isRegularFile(target)) {
                throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
            }
            FileSystemResource resource = new FileSystemResource(target);
            MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
            String base64 = Base64.encode(Files.readAllBytes(target));
            return "data:" + mediaType + ";base64," + base64;
        } catch (ServiceException ex) {
            log.error("", ex);
            throw ex;
        } catch (IOException ex) {
            log.error("", ex);
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        } catch (Exception ex) {
            log.error("", ex);
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
    }

    @Override
    public String generateUrl(String key) {
        long expires = System.currentTimeMillis() + (10 * 60 * 1000L);
        return buildUrl(key, expires);
    }

    public FileSystemResource getFileResource(String key) {
        Path path = decodeKeyToPath(key);
        return new FileSystemResource(path);
    }

    /**
     * 解析key得到本地存储路径
     */
    private Path decodeKeyToPath(String key) {
        Path basePath = getBasePath();
        try {
            String rawPath = decodeFilePathKey(key);
            Path target = basePath.resolve(rawPath).normalize();
            if (!target.startsWith(basePath)) {
                throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
            }
            if (!Files.isRegularFile(target)) {
                throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
            }
            return target;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
    }

    private Path resolveTargetPath(String uploadPath, String filename) {
        Path basePath = getBasePath();
        Path dir = basePath.resolve(uploadPath).normalize();
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(basePath)) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
        return target;
    }

    private Path getBasePath() {
        return Paths.get(uploadRootPath).toAbsolutePath().normalize();
    }

    private String encodeFilePathKey(String uploadPath, String filename) {
        String rawPath = Paths.get(uploadPath, filename).normalize().toString().replace('\\', '/');
        //aes encrypt
        byte[] secretKey = secretKeyService.getSecretKey(SecretKeyService.LOCAL_FILE_PATH_KEY);
        byte[] aesByte = SecureUtil.aes(secretKey).encrypt(rawPath);
        //base64 encode
        return Base64.encode(aesByte);
    }

    private String decodeFilePathKey(String key) {
        //base64 decode
        byte[] base64Byte = Base64.decode(key);
        //aes decrypt
        byte[] secretKey = secretKeyService.getSecretKey(SecretKeyService.LOCAL_FILE_PATH_KEY);
        byte[] aesByte = SecureUtil.aes(secretKey).decrypt(base64Byte);
        return new String(aesByte, StandardCharsets.UTF_8);
    }
}
