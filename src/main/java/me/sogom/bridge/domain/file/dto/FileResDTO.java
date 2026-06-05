package me.sogom.bridge.domain.file.dto;

public class FileResDTO {

    public record PhotoUploadResponse(
            String key,
            String url,
            long expiresInSeconds
    ) {}

    public record PresignedUrlResponse(
            String key,
            String url,
            long expiresInSeconds
    ) {}
}
