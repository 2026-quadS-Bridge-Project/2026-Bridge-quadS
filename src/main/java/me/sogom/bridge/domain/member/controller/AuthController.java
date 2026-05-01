package me.sogom.bridge.domain.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.dto.AuthResponse;
import me.sogom.bridge.domain.member.dto.LoginRequest;
import me.sogom.bridge.domain.member.dto.SignUpRequest;
import me.sogom.bridge.domain.member.service.AuthService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.apiPayload.code.GeneralSuccessCode;
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
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, authService.signUpParent(request));
    }

    @PostMapping("/children/signup")
    public ApiResponse<AuthResponse> signUpChildren(@RequestBody @Valid SignUpRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, authService.signUpChildren(request));
    }

    @PostMapping("/parent/login")
    public ApiResponse<AuthResponse> loginParent(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, authService.loginParent(request));
    }

    @PostMapping("/children/login")
    public ApiResponse<AuthResponse> loginChildren(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, authService.loginChildren(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }
}
