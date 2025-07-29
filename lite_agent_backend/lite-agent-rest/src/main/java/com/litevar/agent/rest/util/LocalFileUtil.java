package com.litevar.agent.rest.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

/**
 * @author reid
 * @since 2024/10/25
 */
public class LocalFileUtil {
    public static PathMatcher glob(String glob) {
        return FileSystems.getDefault().getPathMatcher("glob:" + glob);
    }

    public static Path toPath(String relativePath) {
        try {
            URL fileUrl = LocalFileUtil.class.getClassLoader().getResource(relativePath);
            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Save uploaded file to specified path with MD5 hash as filename
     *
     * @param file     the uploaded file
     * @param path the directory path to save file
     * @return the saved filename (MD5 hash + original suffix)
     * @throws IOException if an I/O error occurs
     */
    public static String saveFile(MultipartFile file, String path) throws IOException {
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'));
        String digest = DigestUtils.md5Hex(file.getInputStream());
        File f = new File(path + digest + suffix);
        if (!f.exists()) {
            cn.hutool.core.io.FileUtil.writeFromStream(file.getInputStream(), f);
        }
        return f.getName();
    }
}
