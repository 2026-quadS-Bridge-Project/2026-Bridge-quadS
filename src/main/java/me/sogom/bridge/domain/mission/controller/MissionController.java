package me.sogom.bridge.domain.mission.controller;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.mission.dto.AiVerificationResponse;
import me.sogom.bridge.domain.mission.entity.MissionCategory;
import me.sogom.bridge.domain.mission.service.MissionPerformanceService; // 변경: 통합 서비스 임포트
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.apiPayload.code.GeneralSuccessCode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*; // PathVariable 등을 위해 추가
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class MissionController {

    // 변경: AI 전담 서비스 대신, DB 저장까지 책임지는 MissionPerformanceService를 주입
    private final MissionPerformanceService performanceService;

    //자녀의 미션 수행 인증 API
    @PostMapping(value = "/{missionId}/performances", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AiVerificationResponse> performMission(
            @PathVariable("missionId") Long missionId,      // 어떤 미션인지 (경로 변수)
            @RequestParam("childId") Long childId,          // 수행하는 자녀가 누구인지
            @RequestParam("image") MultipartFile image,     // 인증 사진
            @RequestParam("category") MissionCategory category, //카테고리
            @RequestParam("prompt") String prompt) throws IOException {

        // Service에서 권한 검증 -> AI 판독 -> DB 저장(reason 포함)을 한 번에 처리 후 결과 반환
        AiVerificationResponse result = performanceService.verifyAndSaveMission(missionId, childId, image, prompt, category);
        // APIResponse 포맷으로 반환
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, result);
    }
}