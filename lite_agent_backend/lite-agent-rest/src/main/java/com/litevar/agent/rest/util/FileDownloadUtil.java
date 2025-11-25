package com.litevar.agent.rest.util;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件下载工具类
 *
 * @author uncle
 * @since 2025/9/1 12:07
 */
public class FileDownloadUtil {

    public static void download(HttpServletResponse response, String filename, byte[] bytes) {
        resetHeader(response, filename);
        write(response, bytes);
    }

    public static void resetHeader(HttpServletResponse response, String filename) {
        filename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setCharacterEncoding(StandardCharsets.ISO_8859_1.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename*=UTF-8''" + filename);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
    }

    private static void write(HttpServletResponse response, byte[] bytes) {
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
