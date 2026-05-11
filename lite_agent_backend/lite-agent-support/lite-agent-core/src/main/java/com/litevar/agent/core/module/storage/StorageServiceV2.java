package com.litevar.agent.core.module.storage;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * @author reid
 * @since 2026/1/23
 */

public interface StorageServiceV2 {

    String writeFile(String fileKey, byte[] bytes);

    String writeFile(String fileKey, InputStream inputStream);

    byte[] readFile(String fileKey);

    InputStream readFileStream(String fileKey);

    Path downloadFile(String fileKey, String targetDir);

    void deleteFile(String fileKey);

    void deleteDir(String dir);

    void downloadDir(String dirKey, String targetDir);

    Map<String, byte[]> downloadDir(String dirKey);

    List<String> listFileKeys(String dirKey);
}
