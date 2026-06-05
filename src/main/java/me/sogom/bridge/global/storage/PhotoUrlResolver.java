package me.sogom.bridge.global.storage;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.global.config.S3Properties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * S3 key를 응답용 presigned URL로 변환
 */
@Component
@RequiredArgsConstructor
public class PhotoUrlResolver {

    private final StorageService storageService;
    private final S3Properties properties;

    public String resolveOrNull(String key) {
        if (key == null || key.isBlank()) return null;
        Duration ttl = Duration.ofSeconds(properties.presignedGetExpirationSeconds());
        return storageService.presignGet(key, ttl).toString();
    }
}
