package me.sogom.bridge.domain.file.controller;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.file.code.FileSuccessCode;
import me.sogom.bridge.domain.file.dto.FileResDTO;
import me.sogom.bridge.domain.file.dto.PhotoCategory;
import me.sogom.bridge.domain.file.service.FileService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.security.entity.AuthMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 사진 통합 업로드
     *  - category=PROFILE: 프로필 사진
     *  - category=MISSION: 미션 인증 사진
     */
    @PostMapping(value = "/photos", consumes = "multipart/form-data")
    public ApiResponse<FileResDTO.PhotoUploadResponse> uploadPhoto(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam("category") PhotoCategory category,
            @RequestPart("file") MultipartFile file
    ) {
        FileResDTO.PhotoUploadResponse response = fileService.uploadPhoto(authMember, category, file);
        return ApiResponse.onSuccess(FileSuccessCode.PHOTO_UPLOAD_SUCCESS, response);
    }

    /**
     * 저장된 사진(S3 key)에 대한 임시 조회 URL을 재발급
     */
    @GetMapping("/photos/presign")
    public ApiResponse<FileResDTO.PresignedUrlResponse> presignPhoto(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam("key") String key
    ) {
        FileResDTO.PresignedUrlResponse response = fileService.presignPhoto(key);
        return ApiResponse.onSuccess(FileSuccessCode.PHOTO_PRESIGN_SUCCESS, response);
    }
}
