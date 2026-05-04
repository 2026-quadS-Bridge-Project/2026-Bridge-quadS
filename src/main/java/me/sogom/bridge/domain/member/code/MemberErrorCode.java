package me.sogom.bridge.domain.member.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.global.apiPayload.code.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "MEMBER409", "이미 사용 중인 이메일입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404", "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "MEMBER401", "비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "MEMBER402", "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "MEMBER403", "만료된 리프레시 토큰입니다."),
    CHILDREN_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404_CHILDREN", "해당 자녀 코드를 찾을 수 없습니다."),
    CHILDREN_ALREADY_REGISTERED(HttpStatus.CONFLICT, "MEMBER409_CHILDREN", "이미 다른 부모와 연결된 자녀입니다."),
    CHILDREN_PARENT_MISMATCH(HttpStatus.UNAUTHORIZED, "MEMBER401_CHILDREN", "자녀와 부모의 연결이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
