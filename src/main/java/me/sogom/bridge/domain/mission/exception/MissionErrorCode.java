package me.sogom.bridge.domain.mission.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.global.apiPayload.code.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MissionErrorCode implements BaseErrorCode {

    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION404", "해당 미션을 찾을 수 없습니다."),
    AI_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "MISSION500", "AI 판독 결과를 처리하는 중 오류가 발생했습니다.");
    // 필요할 때마다 여기에 추가 예정

    private final HttpStatus status;
    private final String code;
    private final String message;
}