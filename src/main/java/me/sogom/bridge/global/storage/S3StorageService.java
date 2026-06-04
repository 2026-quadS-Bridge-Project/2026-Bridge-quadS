package me.sogom.bridge.global.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.sogom.bridge.global.apiPayload.exception.ProjectException;
import me.sogom.bridge.global.config.S3Properties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    @Override
    public String upload(MultipartFile file, String keyPrefix) {
        validate(file);

        String key = buildKey(keyPrefix, file.getOriginalFilename());

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception e) {
            log.error("S3 upload failed. key={}, bucket={}", key, properties.bucket(), e);
            throw new ProjectException(StorageErrorCode.UPLOAD_FAILED);
        }
        return key;
    }

    @Override
    public URL presignGet(String key, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build();
        try {
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(req);
            return presigned.url();
        } catch (S3Exception e) {
            log.error("S3 presign failed. key={}", key, e);
            throw new ProjectException(StorageErrorCode.PRESIGN_FAILED);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            log.warn("S3 delete failed (ignored). key={}", key, e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ProjectException(StorageErrorCode.EMPTY_FILE);
        }
        if (file.getSize() > properties.maxFileSizeBytes()) {
            throw new ProjectException(StorageErrorCode.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new ProjectException(StorageErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private String buildKey(String keyPrefix, String originalFilename) {
        String ext = extractExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String prefix = (keyPrefix == null || keyPrefix.isBlank()) ? "misc" : keyPrefix.replaceAll("^/+|/+$", "");
        return prefix + "/" + uuid + (ext.isEmpty() ? "" : "." + ext);
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        String ext = filename.substring(dot + 1).toLowerCase();
        // 영숫자만 허용 (path traversal 등 차단)
        return ext.matches("^[a-z0-9]{1,8}$") ? ext : "";
    }
}
