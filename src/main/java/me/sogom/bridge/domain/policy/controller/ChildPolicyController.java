package me.sogom.bridge.domain.policy.controller;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.policy.dto.PolicyResponse;
import me.sogom.bridge.domain.policy.service.ChildPolicyService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.apiPayload.code.GeneralSuccessCode;
import me.sogom.bridge.global.apiPayload.code.GeneralErrorCode;
import me.sogom.bridge.global.apiPayload.exception.ProjectException;
import me.sogom.bridge.global.security.entity.AuthMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/children")
@RequiredArgsConstructor
public class ChildPolicyController {

    private final ChildPolicyService childPolicyService;

    /*
     자녀 앱용 정책 조회 API
     자녀 앱 구동 시 필요한 시간 제한 및 앱 차단 리스트를 반환
     */
    @GetMapping("/{childId}/policies")
    public ApiResponse<PolicyResponse> getChildPolicy(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable("childId") Long childId) {

        Children authenticatedChild = authMember.asChildren();
        if (!authenticatedChild.getId().equals(childId)) {
            throw new ProjectException(GeneralErrorCode.FORBIDDEN);
        }

        // Service를 호출하여 이번 달 정책과 차단 앱 목록을 가져옴
        PolicyResponse response = childPolicyService.getChildPolicy(childId);

        // ApiResponse 형식에 담아서 반환
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }
}
