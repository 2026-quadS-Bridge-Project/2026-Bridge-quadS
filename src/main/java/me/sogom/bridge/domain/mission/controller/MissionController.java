package me.sogom.bridge.domain.mission.controller;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.mission.dto.AiVerificationResponse;
import me.sogom.bridge.domain.mission.service.MissionVerificationAiService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<AiVerificationResponse> testVerification(
            @RequestParam("image") MultipartFile image,
            @RequestParam("prompt") String prompt) throws IOException {

        // 아직 DB 연결 전이므로, 포스트맨에서 프롬프트를 직접 입력받아 테스트
        AiVerificationResponse response = aiService.verifyMissionImage(image, prompt);
        return ResponseEntity.ok(response);
    }
}