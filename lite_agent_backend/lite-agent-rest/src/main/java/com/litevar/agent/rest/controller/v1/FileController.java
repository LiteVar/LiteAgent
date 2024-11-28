package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.io.FileUtil;
import com.litevar.agent.auth.annotation.IgnoreAuth;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.ResponseData;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件
 *
 * @author uncle
 * @since 2024/10/14 17:35
 */
@RestController
@RequestMapping("/v1/file")
public class FileController {
    @Value("${file.save-path}")
    private String FILE_PATH;

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
        File f = new File(FILE_PATH + digest + suffix);
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
        File file = new File(FILE_PATH + filename);
        if (!file.exists()) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }
        reset(response, file.getName());

        try (ServletOutputStream outputStream = response.getOutputStream()) {
            outputStream.write(FileUtil.readBytes(file));
            outputStream.flush();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void reset(HttpServletResponse response, String filename) {
        filename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setCharacterEncoding(StandardCharsets.ISO_8859_1.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename*=UTF-8''" + filename);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
    }
}
