package com.litevar.agent.rest.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IOUtil {
    public static String baseName(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    public static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "";
    }

    public static String sha1Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path writeBytes(Path root, String relative, byte[] data) throws IOException {
        Path p = root.resolve(relative);
        Files.createDirectories(p.getParent());
        Files.write(p, data);
        return p;
    }

    public static byte[] readAll(InputStream is) throws IOException {
        return is.readAllBytes();
    }
}

