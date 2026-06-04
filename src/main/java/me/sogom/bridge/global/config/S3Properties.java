package me.sogom.bridge.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloud.aws.s3")
public record S3Properties(
        String bucket,
        String region,
        String accessKey,
        String secretKey,
        long presignedGetExpirationSeconds,
        long maxFileSizeBytes
) {
    public S3Properties {
        if (presignedGetExpirationSeconds <= 0) presignedGetExpirationSeconds = 3600;
        if (maxFileSizeBytes <= 0) maxFileSizeBytes = 10L * 1024 * 1024;
        if (region == null || region.isBlank()) region = "ap-northeast-2";
    }
}
