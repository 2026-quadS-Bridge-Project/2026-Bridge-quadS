package me.sogom.bridge.domain.mission.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.global.apiPayload.code.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MissionSuccessCode implements BaseSuccessCode {

    MISSION_CREATE_SUCCESS(HttpStatus.CREATED, "MISSION201_CREATE", "미션 등록에 성공했습니다."),
    MISSION_LIST_SUCCESS(HttpStatus.OK, "MISSION200", "자녀 미션 조회에 성공했습니다."),
    MISSION_PERFORMANCE_RETRIEVE_SUCCESS(HttpStatus.OK, "MISSION200_PERFORMANCE", "미션 수행 내역 조회에 성공했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
