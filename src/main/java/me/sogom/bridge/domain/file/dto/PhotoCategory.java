package me.sogom.bridge.domain.file.dto;

/**
 * 사진 업로드 카테고리.
 * S3 키 접두사를 결정
 */
public enum PhotoCategory {
    PROFILE("photos/profile"),
    MISSION("photos/mission");

    private final String basePrefix;

    PhotoCategory(String basePrefix) {
        this.basePrefix = basePrefix;
    }

    public String keyPrefix(String role, Long memberId) {
        return basePrefix + "/" + role.toLowerCase() + "/" + memberId;
    }
}
