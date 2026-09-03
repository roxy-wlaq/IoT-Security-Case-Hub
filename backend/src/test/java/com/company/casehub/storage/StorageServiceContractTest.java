package com.company.casehub.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

class StorageServiceContractTest {

    @TempDir
    Path tempDir;

    @Test
    void localStorageRejectsPathTraversalKeys() throws Exception {
        Class<?> serviceType = Class.forName("com.company.casehub.storage.LocalStorageService");
        Object service = serviceType.getConstructor(Path.class).newInstance(Path.of("/tmp/casehub-storage-test"));
        Method resolve = serviceType.getMethod("resolve", String.class);

        assertThatThrownBy(() -> resolve.invoke(service, "../outside.txt"))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void storageServiceExposesTheTempFinalTrashBoundary() throws Exception {
        Class<?> serviceType = Class.forName("com.company.casehub.storage.LocalStorageService");
        Object service = serviceType.getConstructor(Path.class).newInstance(Path.of("/tmp/casehub-storage-test"));
        Method resolve = serviceType.getMethod("resolve", String.class);

        assertThat(resolve.invoke(service, "temp/upload.bin").toString())
                .endsWith("/temp/upload.bin");
        assertThat(resolve.invoke(service, "final/upload.bin").toString())
                .endsWith("/final/upload.bin");
        assertThat(resolve.invoke(service, "trash/upload.bin").toString())
                .endsWith("/trash/upload.bin");
    }

    @Test
    void uploadIsHashedInTempThenMovedToFinal() throws Exception {
        LocalStorageService service = new LocalStorageService(tempDir);
        String key = service.writeTemp(new ByteArrayInputStream("proof".getBytes()), "proof.txt");
        Path temp = service.resolve(key);

        assertThat(Files.readString(temp)).isEqualTo("proof");
        assertThat(service.sha256(temp)).isEqualTo("c1cda26362828b69266512052b97cb3729e3b052e4ade47c0a1e3383defe73c7");
        service.move(key, "final/evidence/proof.bin");
        assertThat(Files.exists(service.resolve(key))).isFalse();
        assertThat(Files.readString(service.resolve("final/evidence/proof.bin"))).isEqualTo("proof");
    }

    @Test
    void invalidKeysCannotEscapeStorageRoot() {
        LocalStorageService service = new LocalStorageService(tempDir);
        for (String key : new String[]{"../outside", "temp/../../outside", "/final/outside", "other/file"}) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.resolve(key))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
