package me.sogom.bridge.domain.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.code.MemberSuccessCode;
import me.sogom.bridge.domain.member.dto.req.RegisterChildRequest;
import me.sogom.bridge.domain.member.service.ParentService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.security.entity.AuthMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<Void> registerChild(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid RegisterChildRequest request) {
        Long parentId = authMember.getMember().getId();
        parentService.registerChild(parentId, request);
        return ApiResponse.onSuccess(MemberSuccessCode.CHILDREN_REGISTER_SUCCESS, null);
    }
}

