package me.sogom.bridge.domain.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.code.MemberSuccessCode;
import me.sogom.bridge.domain.member.dto.req.MemberReqDTO;
import me.sogom.bridge.domain.member.dto.res.MemberResDTO;
import me.sogom.bridge.domain.member.service.ParentService;
import me.sogom.bridge.domain.policy.dto.PolicyReqDTO;
import me.sogom.bridge.domain.schedule.dto.TimeSummaryResponse;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.security.entity.AuthMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    /**
     * 부모가 자녀 등록
     * @param authMember 인증된 부모 사용자
     * @param request 자녀 등록 요청
     * @return 자녀 등록 성공 응답
     */
    @PostMapping("/children")
    public ApiResponse<MemberResDTO.ChildrenInfoResponse> registerChild(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid MemberReqDTO.RegisterChildRequest request) {
        Long parentId = authMember.getMember().getId();
        MemberResDTO.ChildrenInfoResponse response = parentService.registerChild(parentId, request);
        return ApiResponse.onSuccess(MemberSuccessCode.CHILDREN_REGISTER_SUCCESS, response);
    }

    /**
     * 부모와 연결된 자녀 목록 조회
     * @param authMember 인증된 부모 사용자
     * @return 자녀 정보 목록 (자녀 ID, 이름, 프로필 URL)
     */
    @GetMapping("/children")
    public ApiResponse<List<MemberResDTO.ChildrenInfoResponse>> getChildrenList(
            @AuthenticationPrincipal AuthMember authMember) {
        Long parentId = authMember.getMember().getId();
        List<MemberResDTO.ChildrenInfoResponse> childrenList = parentService.getChildrenList(parentId);
        return ApiResponse.onSuccess(MemberSuccessCode.CHILDREN_LIST_SUCCESS, childrenList);
    }

    /**
     * 자녀의 총시간(TimePolicy) 설정
     * @param authMember 인증된 부모 사용자
     * @param request 시간 정책 설정 요청 (자녀 ID, 년월, 기본 시간)
     * @return 설정 성공 응답
     */
    @PostMapping("/time-policy")
    public ApiResponse<Void> setTimePolicy(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid PolicyReqDTO.SetTimePolicyRequest request) {
        Long parentId = authMember.getMember().getId();
        parentService.setTimePolicy(parentId, request);
        return ApiResponse.onSuccess(MemberSuccessCode.TIME_POLICY_SET_SUCCESS, null);
    }

    @GetMapping("/children/{childId}/time-summary")
    public ApiResponse<TimeSummaryResponse> getChildTimeSummary(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long childId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long parentId = authMember.asParent().getId();
        LocalDate targetDate = date != null ? date : LocalDate.now();
        TimeSummaryResponse response = parentService.getChildTimeSummary(parentId, childId, targetDate);
        return ApiResponse.onSuccess(MemberSuccessCode.CHILDREN_LIST_SUCCESS, response);
    }
}
