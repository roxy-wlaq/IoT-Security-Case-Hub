package com.company.casehub.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface StorageService {

    Path resolve(String storageKey);

    String writeTemp(InputStream input, String filename) throws IOException;

    void move(String sourceKey, String targetKey) throws IOException;

    void delete(String storageKey) throws IOException;

    String sha256(Path path) throws IOException;
}
