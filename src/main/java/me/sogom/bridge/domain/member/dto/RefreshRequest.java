package me.sogom.bridge.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "리프레시 토큰을 입력해 주세요.")
        String refreshToken
) {}
