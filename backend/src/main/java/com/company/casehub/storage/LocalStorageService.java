package com.company.casehub.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageService implements StorageService {

    private final Path root;

    @Autowired
    public LocalStorageService(@Value("${casehub.storage-root:var/storage}") String root) {
        this(Path.of(root));
    }

    public LocalStorageService(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public Path resolve(String storageKey) {
        if (storageKey == null || !storageKey.matches("(?:temp|final|trash)/[A-Za-z0-9._/-]+")
                || storageKey.contains("..") || storageKey.startsWith("/")) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return resolved;
    }

    @Override
    public String writeTemp(InputStream input, String filename) throws IOException {
        String safeName = filename == null ? "upload.bin" : filename.replaceAll("[^A-Za-z0-9._-]", "_");
        String key = "temp/" + UUID.randomUUID() + "-" + safeName;
        Path path = resolve(key);
        Files.createDirectories(path.getParent());
        Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    @Override
    public void move(String sourceKey, String targetKey) throws IOException {
        Path source = resolve(sourceKey);
        Path target = resolve(targetKey);
        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(source, target);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    @Override
    public String sha256(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            input.transferTo(new java.io.OutputStream() {
                @Override public void write(int b) { digest.update((byte) b); }
                @Override public void write(byte[] b, int off, int len) { digest.update(b, off, len); }
            });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
