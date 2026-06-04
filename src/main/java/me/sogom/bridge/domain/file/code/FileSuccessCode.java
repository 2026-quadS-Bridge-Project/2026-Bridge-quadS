package me.sogom.bridge.domain.file.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.global.apiPayload.code.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FileSuccessCode implements BaseSuccessCode {

    PHOTO_UPLOAD_SUCCESS(HttpStatus.CREATED, "FILE201_PHOTO", "사진을 업로드했습니다."),
    PHOTO_PRESIGN_SUCCESS(HttpStatus.OK, "FILE200_PRESIGN", "임시 조회 URL을 발급했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
