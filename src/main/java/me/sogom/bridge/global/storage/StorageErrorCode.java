package me.sogom.bridge.global.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.global.apiPayload.code.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StorageErrorCode implements BaseErrorCode {

    EMPTY_FILE(HttpStatus.BAD_REQUEST, "STORAGE400_EMPTY", "업로드할 파일이 비어있습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "STORAGE413_SIZE", "허용된 파일 크기를 초과했습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "STORAGE415_TYPE", "지원하지 않는 파일 형식입니다."),
    UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE500_UPLOAD", "파일 업로드에 실패했습니다."),
    PRESIGN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE500_PRESIGN", "임시 URL 발급에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
