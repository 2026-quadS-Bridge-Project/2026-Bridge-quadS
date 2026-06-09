package me.sogom.bridge.domain.mission.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.global.apiPayload.code.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MissionErrorCode implements BaseErrorCode {

    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION404", "해당 미션을 찾을 수 없습니다."),
    AI_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "MISSION500", "AI 판독 결과를 처리하는 중 오류가 발생했습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "MISSION403", "해당 미션에 대한 수행 권한이 없습니다."), //권한 검증 실패 시 사용
    CHILD_PARENT_MISMATCH(HttpStatus.FORBIDDEN, "MISSION403_CHILD", "해당 자녀에게 미션을 부여할 권한이 없습니다."),
    MISSION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "MISSION400", "이미 완료된 미션입니다."),
    MISSION_ALREADY_SUBMITTED(HttpStatus.BAD_REQUEST, "MISSION_ALREADY_SUBMITTED", "이미 확인 대기 중인 미션입니다."),
    INVALID_MISSION_STATE(HttpStatus.BAD_REQUEST, "INVALID_MISSION_STATE", "대기 상태인 부모 확인 미션만 승인/반려할 수 있습니다.");
    // 필요할 때마다 여기에 추가 예정

    private final HttpStatus status;
    private final String code;
    private final String message;
}
