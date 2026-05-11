package com.litevar.agent.rest.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

/**
 * @author reid
 * @since 2026/1/12
 */

@Component
public class OssUtil {
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    @Value("${aliyun.oss.region}")
    private String region;
    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;
    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;
    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;
    @Value("${aliyun.oss.custom-domain:}")
    private String customDomain;

    private volatile OSS ossClient;

    public void initClient() {
        boolean supportCustomDomain = StrUtil.isNotBlank(customDomain);
        String clientEndpoint = supportCustomDomain ? normalizeEndpoint(customDomain) : endpoint;
        ClientBuilderConfiguration clientConfig = new ClientBuilderConfiguration();
        clientConfig.setSupportCname(supportCustomDomain);
        ossClient = OSSClientBuilder.create()
                .endpoint(clientEndpoint)
                .clientConfiguration(clientConfig)
                .region(region)
                .credentialsProvider(new DefaultCredentialProvider(accessKeyId, accessKeySecret))
                .build();
    }

    public OSS getInstance() {
        if (ossClient == null) {
            synchronized (this) {
                if (ossClient == null) {
                    initClient();
                }
            }
        }
        return ossClient;
    }

    public String putObject(String fileKey, byte[] bytes) {
        getInstance().putObject(bucketName, fileKey, new ByteArrayInputStream(bytes));
        return fileKey;
    }

    public String putObject(String fileKey, InputStream inputStream) {
        getInstance().putObject(bucketName, fileKey, inputStream);
        return fileKey;
    }

    public String putObject(String ossPathPrefix, String filename, InputStream inputStream) {
        String key = ossPathPrefix + "/" + filename;
        getInstance().putObject(bucketName, key, inputStream);
        return key;
    }

    /**
     * 删除指定Key的OSS对象
     *
     * @param key OSS对象的Key
     */
    @Async
    public void deleteObject(String key) {
        getInstance().deleteObject(bucketName, key);
    }

    /**
     * 删除指定前缀的所有对象（相当于删除目录）
     *
     * @param prefix OSS对象的前缀
     */
    public void deleteDir(String prefix) {
        List<String> keys = getInstance().listObjects(bucketName, prefix).getObjectSummaries()
                .stream().map(OSSObjectSummary::getKey)
                .toList();

        if (!keys.isEmpty()) {
            DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName)
                    .withKeys(keys).withEncodingType("url");
            getInstance().deleteObjects(request);
        }
    }

    /**
     * 下载指定前缀的所有对象到本地目录
     *
     * @param prefix    OSS对象的前缀
     * @param targetDir 本地目标目录
     */
    public void downloadDir(String prefix, String targetDir) {
        getInstance().listObjects(bucketName, prefix).getObjectSummaries()
                .parallelStream()
                .forEach(summary -> {
                    String key = summary.getKey();
                    if (key.endsWith("/")) {
                        // 跳过目录对象
                        return;
                    }

                    Path targetPath = Path.of(targetDir, key.substring(prefix.length()));
                    getObject(key, targetPath);
                });
    }

    public List<String> listObjectKeys(String prefix) {
        return getInstance().listObjects(bucketName, prefix).getObjectSummaries()
                .parallelStream().map(OSSObjectSummary::getKey)
                .filter(key -> !key.endsWith("/")).toList();
    }

    /**
     * 获取OSS对象的字节内容
     *
     * @param key OSS对象的Key
     * @return 对象内容的字节数组
     * @throws IOException
     */
    public byte[] getObjectBytes(String key) throws IOException {
        OSSObject ossObject = getInstance().getObject(bucketName, key);
        try (InputStream inputStream = ossObject.getObjectContent()) {
            return inputStream.readAllBytes();
        }
    }

    public InputStream getObjectStream(String key) {
        OSSObject ossObject = getInstance().getObject(bucketName, key);
        return ossObject.getObjectContent();
    }

    /**
     * 下载OSS对象到本地路径
     *
     * @param key  OSS对象的Key
     * @param path 本地目标路径
     */
    public void getObject(String key, Path path) {
        FileUtil.mkParentDirs(path);

        GetObjectRequest request = new GetObjectRequest(bucketName, key);
        getInstance().getObject(request, path.toFile());
    }

    public String generatePresignedUrl(String key) {
        // 过期时间为1小时
        long expiration = 60 * 60 * 1000L;
        return generatePresignedUrl(key, expiration);
    }

    /**
     * 生成OSS文件的预签名URL
     *
     * @param key          文件key
     * @param expireMillis 过期时间，单位毫秒
     * @return 预签名URL
     */
    public String generatePresignedUrl(String key, long expireMillis) {
        Date expiration = new Date(new Date().getTime() + expireMillis);
        return getInstance().generatePresignedUrl(bucketName, key, expiration).toString();
    }

    private String normalizeEndpoint(String source) {
        String normalized = StrUtil.removePrefix(source.trim(), "http://");
        normalized = StrUtil.removePrefix(normalized, "https://");
        normalized = "https://" + normalized;
        return normalized.replaceAll("/+$", "");
    }

}
