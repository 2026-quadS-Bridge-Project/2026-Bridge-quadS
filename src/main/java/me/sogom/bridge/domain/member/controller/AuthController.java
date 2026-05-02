package me.sogom.bridge.domain.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.code.MemberSuccessCode;
import me.sogom.bridge.domain.member.dto.res.AuthResponse;
import me.sogom.bridge.domain.member.dto.req.LoginRequest;
import me.sogom.bridge.domain.member.dto.req.RefreshRequest;
import me.sogom.bridge.domain.member.dto.req.SignUpRequest;
import me.sogom.bridge.domain.member.service.AuthService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/parent/signup")
    public ApiResponse<AuthResponse> signUpParent(@RequestBody @Valid SignUpRequest request) {
        return ApiResponse.onSuccess(MemberSuccessCode.PARENT_SIGNUP_SUCCESS, authService.signUpParent(request));
    }

    @PostMapping("/children/signup")
    public ApiResponse<AuthResponse> signUpChildren(@RequestBody @Valid SignUpRequest request) {
        return ApiResponse.onSuccess(MemberSuccessCode.CHILDREN_SIGNUP_SUCCESS, authService.signUpChildren(request));
    }

    @PostMapping("/parent/login")
    public ApiResponse<AuthResponse> loginParent(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.onSuccess(MemberSuccessCode.PARENT_LOGIN_SUCCESS, authService.loginParent(request));
    }

    @PostMapping("/children/login")
    public ApiResponse<AuthResponse> loginChildren(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.onSuccess(MemberSuccessCode.CHILDREN_LOGIN_SUCCESS, authService.loginChildren(request));
    }

    @PostMapping("/token/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ApiResponse.onSuccess(MemberSuccessCode.REFRESH_TOKEN_SUCCESS, authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid RefreshRequest request) {
        authService.logout(request);
        return ApiResponse.onSuccess(MemberSuccessCode.LOGOUT_SUCCESS, null);
    }
}
