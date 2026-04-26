package me.sogom.bridge.domain.mission.controller;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.mission.dto.AiVerificationResponse;
import me.sogom.bridge.domain.mission.service.MissionVerificationAiService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.apiPayload.code.GeneralSuccessCode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionVerificationAiService aiService;

    @PostMapping(value = "/verify-test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AiVerificationResponse> testVerification( //return type 변경
            @RequestParam("image") MultipartFile image,
            @RequestParam("prompt") String prompt) throws IOException {

        AiVerificationResponse result = aiService.verifyMissionImage(image, prompt);
        // APIResponse 포맷으로 반환
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, result);
    }
}