package me.sogom.bridge.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.code.MemberSuccessCode;
import me.sogom.bridge.domain.member.dto.res.MemberResDTO;
import me.sogom.bridge.domain.member.dto.req.MemberReqDTO;
import me.sogom.bridge.domain.member.service.AuthService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "부모 회원가입", description = "부모 회원가입 API")
    @PostMapping("/parent/signup")
    public ApiResponse<MemberResDTO.AuthResponse> signUpParent(@RequestBody @Valid MemberReqDTO.SignUpRequest request) {
        return ApiResponse.onSuccess(MemberSuccessCode.PARENT_SIGNUP_SUCCESS, authService.signUpParent(request));
    }

    @Operation(summary = "자녀 회원가입", description = "자녀 회원가입 API")
    @PostMapping("/children/signup")
    public ApiResponse<MemberResDTO.AuthResponse> signUpChildren(@RequestBody @Valid MemberReqDTO.SignUpRequest request) {
        return ApiResponse.onSuccess(MemberSuccessCode.CHILDREN_SIGNUP_SUCCESS, authService.signUpChildren(request));
    }

    @Operation(summary = "부모 로그인", description = "부모 로그인 API")
    @PostMapping("/parent/login")
    public ApiResponse<MemberResDTO.AuthResponse> loginParent(@RequestBody @Valid MemberReqDTO.LoginRequest request) {
        return ApiResponse.onSuccess(MemberSuccessCode.PARENT_LOGIN_SUCCESS, authService.loginParent(request));
    }

    @Operation(summary = "자녀 로그인", description = "자녀 로그인 API")
    @PostMapping("/children/login")
    public ApiResponse<MemberResDTO.AuthResponse> loginChildren(@RequestBody @Valid MemberReqDTO.LoginRequest request) {
        return ApiResponse.onSuccess(MemberSuccessCode.CHILDREN_LOGIN_SUCCESS, authService.loginChildren(request));
    }

    @Operation(summary = "토큰 재발급", description = "액세스 토큰 만료 시, 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받는 API")
    @PostMapping("/token/refresh")
    public ApiResponse<MemberResDTO.AuthResponse> refresh(@RequestBody @Valid MemberReqDTO.RefreshRequest request) {
        return ApiResponse.onSuccess(MemberSuccessCode.REFRESH_TOKEN_SUCCESS, authService.refresh(request));
    }

    @Operation(summary = "로그아웃", description = "사용자 로그아웃 시, 리프레시 토큰 무효화 API")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid MemberReqDTO.RefreshRequest request) {
        authService.logout(request);
        return ApiResponse.onSuccess(MemberSuccessCode.LOGOUT_SUCCESS, null);
    }
}
