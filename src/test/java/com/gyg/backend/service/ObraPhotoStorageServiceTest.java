package com.gyg.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObraPhotoStorageServiceTest {

    @TempDir
    Path storageDirectory;

    @Test
    void shouldReprocessPngAndStoreWithGeneratedName() throws Exception {
        ObraPhotoStorageService storage = new ObraPhotoStorageService(storageDirectory.toString());
        MockMultipartFile upload = new MockMultipartFile(
                "archivo", "../../tracking-code.png", "image/png", createPng());

        ObraPhotoStorageService.StoredPhoto stored = storage.store(upload);

        assertEquals("image/png", stored.contentType());
        assertTrue(stored.key().matches("[0-9a-fA-F-]{36}\\.png"));
        assertTrue(ImageIO.read(storageDirectory.resolve(stored.key()).toFile()) != null);
        assertTrue(storage.read(stored.key()).length > 8);
    }

    @Test
    void shouldRejectFilesWithoutAnAllowedImageSignature() {
        ObraPhotoStorageService storage = new ObraPhotoStorageService(storageDirectory.toString());
        MockMultipartFile upload = new MockMultipartFile(
                "archivo", "image.png", "image/png", "not an image".getBytes());

        assertThrows(IllegalArgumentException.class, () -> storage.store(upload));
    }

    @Test
    void shouldRejectPathTraversalKeys() {
        ObraPhotoStorageService storage = new ObraPhotoStorageService(storageDirectory.toString());

        assertThrows(IllegalArgumentException.class, () -> storage.read("../../private.png"));
    }

    private byte[] createPng() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF336699);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
