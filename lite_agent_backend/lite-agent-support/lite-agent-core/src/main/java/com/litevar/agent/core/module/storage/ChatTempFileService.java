package com.litevar.agent.core.module.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Chat temp file handling
 *
 * @author uncle
 * @since 2026/1/8 11:40
 */
@Slf4j
@Service
public class ChatTempFileService {
    private static final String TEMP_DIR_NAME = "tmpChatUpload";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Value("${litevar.upload.path:}")
    private String uploadPath;

    public String saveTempFile(MultipartFile file) throws IOException {
        String date = LocalDate.now().format(DATE_FORMATTER);
        Path dir = getDateDir(date);
        Files.createDirectories(dir);
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename == null ? "" : "." + FilenameUtils.getExtension(originalFilename);
        String filename = IdUtil.getSnowflakeNextIdStr() + extension;
        Path target = dir.resolve(filename);
        file.transferTo(target);
        return date + filename;
    }

    public TempFileData openTempFile(String fileId) {
        if (fileId.length() <= 8) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
        String date = fileId.substring(0, 8);
        String filename = fileId.substring(8);
        Path baseDir = getBaseDir().normalize();
        Path dir = baseDir.resolve(date).normalize();
        if (!dir.startsWith(baseDir)) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }
        try {
            String mime = MediaTypeFactory.getMediaType(filename)
                    .map(MediaType::toString)
                    .orElse("application/octet-stream");
            InputStream inputStream = Files.newInputStream(target);
            return new TempFileData(filename, mime, inputStream);
        } catch (IOException e) {
            log.error("读取临时文件失败, fileId={}", fileId, e);
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
    }

    @Scheduled(cron = "0 10 0 * * ?")
    public void clearBeforeYesterdayFiles() {
        LocalDate today = LocalDate.now();
        String todayDir = today.format(DATE_FORMATTER);
        String yesterdayDir = today.minusDays(1).format(DATE_FORMATTER);
        Path baseDir = getBaseDir().normalize();
        if (!Files.isDirectory(baseDir)) {
            return;
        }
        try (var paths = Files.list(baseDir)) {
            paths.filter(Files::isDirectory)
                    .filter(path -> {
                        String dirName = path.getFileName().toString();
                        return !dirName.equals(todayDir) && !dirName.equals(yesterdayDir);
                    })
                    .forEach(path -> FileUtil.del(path.toFile()));
        } catch (IOException e) {
            log.warn("清理临时文件目录失败, dir={}", baseDir, e);
        }
    }

    private Path getDateDir(String date) {
        return getBaseDir().resolve(date);
    }

    private Path getBaseDir() {
        String uploadRoot = StrUtil.emptyToDefault(uploadPath, System.getProperty("java.io.tmpdir"));
        return Paths.get(uploadRoot, TEMP_DIR_NAME);
    }

    public record TempFileData(String filename, String mime, InputStream inputStream) {
    }
}
