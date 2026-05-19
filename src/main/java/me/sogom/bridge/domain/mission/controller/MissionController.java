package me.sogom.bridge.domain.mission.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.mission.code.MissionSuccessCode;
import me.sogom.bridge.domain.mission.dto.AiVerificationResponse;
import me.sogom.bridge.domain.mission.dto.req.MissionReqDTO;
import me.sogom.bridge.domain.mission.dto.res.MissionResDTO;
import me.sogom.bridge.domain.mission.entity.MissionCategory;
import me.sogom.bridge.domain.mission.service.MissionPerformanceService; // 변경: 통합 서비스 임포트
import me.sogom.bridge.domain.mission.service.MissionService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.apiPayload.code.GeneralSuccessCode;
import me.sogom.bridge.global.security.entity.AuthMember;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*; // PathVariable 등을 위해 추가
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class MissionController {

    // 변경: AI 전담 서비스 대신, DB 저장까지 책임지는 MissionPerformanceService를 주입
    private final MissionPerformanceService performanceService;
    private final MissionService missionService;

    //부모의 미션 등록 API
    @PostMapping
    public ApiResponse<MissionResDTO.CreateMissionResponse> createMission(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid MissionReqDTO.CreateMissionRequest request) {

        Long parentId = authMember.asParent().getId();
        MissionResDTO.CreateMissionResponse response = missionService.createMission(parentId, request);
        return ApiResponse.onSuccess(MissionSuccessCode.MISSION_CREATE_SUCCESS, response);
    }

    //부모의 미션 목록 조회 API
    @GetMapping
    public ApiResponse<List<MissionResDTO.MissionSummaryResponse>> getMissionList(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam("childId") Long childId) {

        Long parentId = authMember.asParent().getId();
        List<MissionResDTO.MissionSummaryResponse> response = missionService.getParentMissionSummaries(parentId, childId);
        return ApiResponse.onSuccess(MissionSuccessCode.MISSION_LIST_SUCCESS, response);
    }

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