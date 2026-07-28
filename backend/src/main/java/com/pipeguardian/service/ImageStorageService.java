package com.pipeguardian.service;

import com.pipeguardian.config.PipeGuardianProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".webp", ".bmp");

    private final PipeGuardianProperties properties;

    public Path store(MultipartFile image, Long pipeId, String category) {
        validateImage(image);

        Path root = Path.of(properties.getStorage().getRoot()).toAbsolutePath().normalize();
        Path pipeDirectory = root.resolve("pipe-" + pipeId).resolve(category).normalize();
        if (!pipeDirectory.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 저장 경로입니다.");
        }

        String extension = safeExtension(image.getOriginalFilename());
        Path target = pipeDirectory.resolve(UUID.randomUUID() + extension).normalize();
        if (!target.startsWith(pipeDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일명입니다.");
        }

        try {
            Files.createDirectories(pipeDirectory);
            try (var input = image.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "이미지를 저장할 수 없습니다.",
                    exception
            );
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일이 필요합니다.");
        }

        String contentType = image.getContentType();
        if (contentType != null && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "이미지 형식만 업로드할 수 있습니다.");
        }
    }

    private String safeExtension(String originalFilename) {
        if (originalFilename == null) {
            return ".bin";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0) {
            return ".bin";
        }
        String extension = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.contains(extension) ? extension : ".bin";
    }
}
