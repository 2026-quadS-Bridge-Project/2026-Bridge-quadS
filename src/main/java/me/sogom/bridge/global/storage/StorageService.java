package me.sogom.bridge.global.storage;

import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.Duration;

public interface StorageService {

    /**
     * 파일을 저장소에 업로드한다.
     * @param file 업로드할 파일 (multipart)
     * @param keyPrefix S3 키 접두사 (예: "photos/profile")
     * @return 저장된 객체 키 (presigned URL 발급 시 사용)
     */
    String upload(MultipartFile file, String keyPrefix);

    /**
     * 저장된 객체의 임시 조회 URL(presigned GET)을 발급한다.
     * @param key 저장된 객체 키
     * @param ttl 유효기간
     */
    URL presignGet(String key, Duration ttl);

    /**
     * 저장된 객체를 삭제한다. (멱등)
     */
    void delete(String key);
}
