package me.sogom.bridge.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.code.MemberSuccessCode;
import me.sogom.bridge.domain.member.dto.req.MemberReqDTO;
import me.sogom.bridge.domain.member.service.AuthService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.security.entity.AuthMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "Member API")
public class MemberController {

    private final AuthService authService;

    @Operation(summary = "비밀번호 변경", description = "기존 비밀번호 검증 후 비밀번호를 변경하는 API")
    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid MemberReqDTO.ChangePasswordRequest request) {
        authService.changePassword(authMember, request);
        return ApiResponse.onSuccess(MemberSuccessCode.PASSWORD_CHANGE_SUCCESS, null);
    }

    @Operation(summary = "회원 탈퇴", description = "부모/자녀 계정을 탈퇴 처리하는 API")
    @DeleteMapping
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal AuthMember authMember) {
        authService.withdraw(authMember);
        return ApiResponse.onSuccess(MemberSuccessCode.MEMBER_WITHDRAW_SUCCESS, null);
    }
}
