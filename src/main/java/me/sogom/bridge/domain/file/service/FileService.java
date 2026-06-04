package me.sogom.bridge.domain.file.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.file.dto.FileResDTO;
import me.sogom.bridge.domain.file.dto.PhotoCategory;
import me.sogom.bridge.global.config.S3Properties;
import me.sogom.bridge.global.security.entity.AuthMember;
import me.sogom.bridge.global.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FileService {

    private final StorageService storageService;
    private final S3Properties properties;

    public FileResDTO.PhotoUploadResponse uploadPhoto(AuthMember authMember, PhotoCategory category, MultipartFile file) {
        String prefix = category.keyPrefix(authMember.getRole().name(), authMember.getMember().getId());
        String key = storageService.upload(file, prefix);

        Duration ttl = Duration.ofSeconds(properties.presignedGetExpirationSeconds());
        String url = storageService.presignGet(key, ttl).toString();

        return new FileResDTO.PhotoUploadResponse(key, url, ttl.toSeconds());
    }

    public FileResDTO.PresignedUrlResponse presignPhoto(String key) {
        Duration ttl = Duration.ofSeconds(properties.presignedGetExpirationSeconds());
        String url = storageService.presignGet(key, ttl).toString();
        return new FileResDTO.PresignedUrlResponse(key, url, ttl.toSeconds());
    }
}
