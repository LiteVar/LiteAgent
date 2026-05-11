package com.litevar.agent.rest.util;

import lombok.SneakyThrows;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import cn.hutool.core.util.StrUtil;

/**
 * @author reid
 * @since 2025/6/24
 */

public class AudioUtil {

    @SneakyThrows
    public static String savePcmData(DataBuffer dataBuffers) {
        String pcmFilePath = System.getProperty("user.dir") + "/tmp/audio/" + System.currentTimeMillis() + ".pcm";
        Path path = Paths.get(pcmFilePath);

        // 确保目录存在
        Files.createDirectories(path.getParent());

        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE)) {
            byte[] bytes = new byte[dataBuffers.readableByteCount()];
            dataBuffers.read(bytes);
            outputStream.write(bytes);
            // 释放DataBuffer
            DataBufferUtils.release(dataBuffers);
        }

        return pcmFilePath;
    }

    /**
     * 将PCM文件转换为WAV文件
     *
     * @param pcmFilePath PCM文件路径
     * @return WAV文件路径
     */
    @SneakyThrows
    public static String convertPcmToWav(String pcmFilePath) {
        String wavFilePath = System.getProperty("user.dir") + "/tmp/audio/" + System.currentTimeMillis() + ".wav";

        // PCM音频格式参数（根据实际API返回的格式调整）
        AudioFormat audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED, // 编码格式
                24000, // 采样率 (Hz)
                16, // 采样精度 (bits)
                1, // 声道数 (1=单声道, 2=立体声)
                2, // 帧大小 (bytes)
                16000, // 帧率 (fps)
                false // 字节序 (false=小端序)
        );

        byte[] pcmData = Files.readAllBytes(Paths.get(pcmFilePath));

        try (ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
                AudioInputStream audioInputStream = new AudioInputStream(bais, audioFormat,
                        pcmData.length / audioFormat.getFrameSize());
                FileOutputStream fos = new FileOutputStream(wavFilePath)) {

            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, fos);
        }

        // 删除临时PCM文件
        Files.deleteIfExists(Paths.get(pcmFilePath));

        return wavFilePath;
    }

    public static String saveToWav(DataBuffer dataBuffer, String extension) throws IOException {
        Path audioDir = Paths.get(System.getProperty("user.dir"), "tmp", "audio");
        Files.createDirectories(audioDir);

        byte[] audioData = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(audioData);

        try {
            // 验证是否有有效的音频数据
            if (audioData.length == 0) {
                throw new IOException("音频数据为空");
            }

            String fileExtension = "wav".equalsIgnoreCase(extension) ? ".wav" : "." + extension;
            Path tempFile = Files.createTempFile(audioDir, String.valueOf(System.currentTimeMillis()), fileExtension);
            Files.write(tempFile, audioData);
            return tempFile.toAbsolutePath().toString();
        } finally {
            // 关键：释放 DataBuffer
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * 将PCM转换为MP3（需要额外的MP3编码库，如LAME FFmpeg）
     */
    public static String convertPcmToMp3(String pcmFilePath, String outputFileName) {
        // 这里需要使用第三方库如 JLayer 或调用系统的LAME编码器
        // 由于MP3编码较复杂，建议优先使用WAV格式
        throw new UnsupportedOperationException("MP3转换需要额外的编码库支持");
    }

    /**
     * 计算音频时长（秒，向上取整）
     * 支持 pcm, mp3, opus, aac, flac, wav
     *
     * @param data   音频数据
     * @param format 音频格式
     * @return 时长（秒）
     */
    public static int calculateDuration(byte[] data, String format) {
        if (data == null || data.length == 0) {
            return 0;
        }

        // OpenAI PCM defaults: 24kHz, 16-bit, mono
        // bytes per second = 24000 * (16/8) * 1 = 48000
        if ("pcm".equalsIgnoreCase(format)) {
            return (int) Math.ceil(data.length / 48000.0);
        }

        try (java.io.InputStream is = new java.io.ByteArrayInputStream(data)) {
            Metadata metadata = new Metadata();
            Parser parser = new AutoDetectParser();
            // -1 limit for BodyContentHandler to avoid write limit exception, though we
            // don't care about content
            parser.parse(is, new BodyContentHandler(-1), metadata, new ParseContext());

            String durationStr = metadata.get("xmpDM:duration");
            if (StrUtil.isNotBlank(durationStr)) {
                return (int) Math.ceil(Double.parseDouble(durationStr));
            }
        } catch (Exception e) {
            // 忽略异常，返回0
        }

        return 0;
    }
}
