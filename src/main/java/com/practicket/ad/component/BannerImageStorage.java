package com.practicket.ad.component;

import com.practicket.ad.exception.AdException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 배너 이미지를 서버 로컬 디렉토리에 저장하는 컴포넌트.
 * docs/ad-admin-system.md Q2: 파일 업로드 → 서버 로컬 저장(홈서버 단일이라 충분, 커지면 S3).
 *
 * 저장 후 반환하는 imagePath는 "/ad-images/{파일명}" 형태의 웹 접근 경로다.
 * 이 경로가 실제로 서빙되려면 정적 리소스 매핑이 필요 — 최종 보고 참고.
 */
@Slf4j
@Component
public class BannerImageStorage {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024; // 2MB
    private static final String WEB_PATH_PREFIX = "/ad-images/";

    @Value("${app.ad.image-dir:/data/practicket/ad-images}")
    private String imageDir;

    /**
     * 업로드 파일을 검증 후 저장하고, 웹에서 접근 가능한 경로를 반환한다.
     *
     * @throws AdException 빈 파일, 용량 초과, 허용되지 않는 확장자, 저장 실패 시
     */
    public String store(MultipartFile file) {
        validate(file);

        String extension = extractExtension(file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + "." + extension;

        try {
            Path dir = Paths.get(imageDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(storedFilename);
            file.transferTo(target);
        } catch (IOException e) {
            log.error("배너 이미지 저장 실패: {}", e.getMessage(), e);
            throw new AdException("이미지 저장 중 오류가 발생했습니다.");
        }

        return WEB_PATH_PREFIX + storedFilename;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AdException("업로드할 이미지 파일이 없습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AdException("이미지 파일 용량은 2MB를 초과할 수 없습니다.");
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new AdException("파일 확장자를 확인할 수 없습니다.");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new AdException("허용되지 않는 이미지 확장자입니다. (jpg, jpeg, png, gif만 가능)");
        }
        return extension;
    }
}
