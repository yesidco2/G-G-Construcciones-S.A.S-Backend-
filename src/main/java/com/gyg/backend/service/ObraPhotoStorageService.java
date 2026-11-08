package com.gyg.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;

@Service
public class ObraPhotoStorageService {

    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 16_000_000L;

    private final Path storageDirectory;

    public ObraPhotoStorageService(
            @Value("${app.uploads.path:${APP_UPLOADS_DIR:./var/uploads/obra-fotos}}") String path) {
        this.storageDirectory = Path.of(path).toAbsolutePath().normalize();
    }

    public StoredPhoto store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("La imagen debe tener contenido y no superar 5 MB.");
        }

        byte[] source = file.getBytes();
        detectFormat(source);
        BufferedImage decoded = decodeImage(source);
        BufferedImage cleanImage = new BufferedImage(
                decoded.getWidth(), decoded.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = cleanImage.createGraphics();
        try {
            graphics.drawImage(decoded, 0, 0, null);
        } finally {
            graphics.dispose();
            decoded.flush();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(cleanImage, "png", output)) {
            throw new IOException("No fue posible reprocesar la imagen.");
        }
        cleanImage.flush();

        String key = UUID.randomUUID() + ".png";
        Files.createDirectories(storageDirectory);
        Path destination = resolveKey(key);
        Files.write(destination, output.toByteArray());
        return new StoredPhoto(key, "image/png");
    }

    public byte[] read(String key) throws IOException {
        return Files.readAllBytes(resolveKey(key));
    }

    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolveKey(key));
    }

    private BufferedImage decodeImage(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new IllegalArgumentException("El archivo no contiene una imagen compatible.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("El archivo no contiene una imagen compatible.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("Las dimensiones de la imagen no son válidas.");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("El archivo no contiene una imagen compatible.");
                }
                return decoded;
            } finally {
                reader.dispose();
            }
        }
    }

    private String detectFormat(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P'
                && bytes[2] == 'N' && bytes[3] == 'G' && bytes[4] == 0x0D
                && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "png";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W'
                && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        throw new IllegalArgumentException("Solo se permiten imágenes JPEG, PNG o WebP.");
    }

    private Path resolveKey(String key) {
        if (key == null || !key.matches("[0-9a-fA-F-]{36}\\.png")) {
            throw new IllegalArgumentException("La referencia de imagen no es válida.");
        }
        Path resolved = storageDirectory.resolve(key).normalize();
        if (!resolved.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("La referencia de imagen no es válida.");
        }
        return resolved;
    }

    public record StoredPhoto(String key, String contentType) {
    }
}
