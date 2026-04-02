package me.sogom.bridge.domain.mission.dto;

// AI가 JSON 형태로 뱉어줄 응답 구조
public record AiVerificationResponse(
        boolean isAccepted,
        String reason // 통과/실패 이유 (자녀에게 피드백용)
) {}
