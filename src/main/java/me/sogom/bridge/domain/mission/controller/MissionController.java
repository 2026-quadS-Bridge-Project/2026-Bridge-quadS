package me.sogom.bridge.domain.mission.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.mission.code.MissionSuccessCode;
import me.sogom.bridge.domain.mission.dto.AiVerificationResponse;
import me.sogom.bridge.domain.mission.dto.req.MissionReqDTO;
import me.sogom.bridge.domain.mission.dto.res.MissionPerformanceResDTO;
import me.sogom.bridge.domain.mission.dto.res.MissionResDTO;
import me.sogom.bridge.domain.mission.dto.res.MissionSummaryResponse;
import me.sogom.bridge.domain.mission.exception.MissionErrorCode;
import me.sogom.bridge.domain.mission.exception.MissionException;
import me.sogom.bridge.domain.mission.service.MissionPerformanceService;
import me.sogom.bridge.domain.mission.service.MissionService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.apiPayload.code.GeneralErrorCode;
import me.sogom.bridge.global.apiPayload.code.GeneralSuccessCode;
import me.sogom.bridge.global.apiPayload.exception.ProjectException;
import me.sogom.bridge.global.security.entity.AuthMember;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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
    public ApiResponse<MissionResDTO.MissionResponse> createMission(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid MissionReqDTO.CreateMissionRequest request) {

        Long parentId = authMember.asParent().getId();
        MissionResDTO.MissionResponse response = missionService.createMission(parentId, request);
        return ApiResponse.onSuccess(MissionSuccessCode.MISSION_CREATE_SUCCESS, response);
    }

    //부모/자녀의 미션 목록 조회 API
    @GetMapping
    public ApiResponse<List<MissionSummaryResponse>> getMissionList(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(value = "childId", required = false) Long childId) {

        List<MissionSummaryResponse> response;

        if (authMember.getRole() == MemberRole.PARENT) {

            if (childId == null) {
                throw new MissionException(MissionErrorCode.CHILD_PARENT_MISMATCH);
            }

            Long parentId = authMember.asParent().getId();
            response = missionService.getParentMissionSummaries(parentId, childId);

        } else if (authMember.getRole() == MemberRole.CHILDREN) {

            Long childrenId = authMember.asChildren().getId();
            response = missionService.getChildrenMissionSummaries(childrenId);

        } else {
            throw new ProjectException(GeneralErrorCode.FORBIDDEN);
        }

        return ApiResponse.onSuccess(MissionSuccessCode.MISSION_LIST_SUCCESS, response);
    }

    //미션 상세정보 조회 API
    @GetMapping("/{missionId}")
    public ApiResponse<MissionResDTO.MissionResponse> getMissionDetail(
            @PathVariable("missionId") Long missionId) {

        MissionResDTO.MissionResponse response = missionService.getMissionDetail(missionId);
        return ApiResponse.onSuccess(MissionSuccessCode.MISSION_LIST_SUCCESS, response);
    }

    //자녀의 미션 수행 인증 API
    @PostMapping(value = "/{missionId}/performances", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AiVerificationResponse> performMission(
            @PathVariable("missionId") Long missionId,      // 어떤 미션인지 (경로 변수)
            @AuthenticationPrincipal AuthMember authMember, // member JWT token
            @RequestParam("image") MultipartFile image ) throws IOException {   // 인증 사진
        Long childId = authMember.asChildren().getId();     // 수행하는 자녀가 누구인지 token에서 가져옴
        // Service에서 권한 검증 -> AI 판독 -> DB 저장(reason 포함)을 한 번에 처리 후 결과 반환
        AiVerificationResponse result = performanceService.verifyAndSaveMission( missionId, childId, image );
        // APIResponse 포맷으로 반환
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, result);
    }

    // 미션 수행 내역 조회 API
    @GetMapping("/{missionId}/performance")
    public ApiResponse<MissionPerformanceResDTO.MissionPerformanceResponse> getMissionPerformance(
            @PathVariable("missionId") Long missionId,
            @AuthenticationPrincipal AuthMember authMember) {

        Long parentId = authMember.asParent().getId();
        MissionPerformanceResDTO.MissionPerformanceResponse response = performanceService.getMissionPerformance(missionId, parentId);
        return ApiResponse.onSuccess(MissionSuccessCode.MISSION_PERFORMANCE_RETRIEVE_SUCCESS, response);
    }

    // 부모의 미션 승인 API
    @PatchMapping("/performances/{performanceId}/approve")
    public ApiResponse<String> approveMission(
            @PathVariable Long performanceId,
            @AuthenticationPrincipal AuthMember authMember) {

        Long parentId = authMember.asParent().getId();

        performanceService.approveMission(performanceId, parentId);

        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                "미션 승인 완료"
        );
    }

    // 부모의 미션 거절 API
    @PatchMapping("/performances/{performanceId}/reject")
    public ApiResponse<String> rejectMission(
            @PathVariable Long performanceId,
            @AuthenticationPrincipal AuthMember authMember) {

        Long parentId = authMember.asParent().getId();

        performanceService.rejectMission(performanceId, parentId);

        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                "미션 거절 완료"
        );
    }

}