package me.sogom.bridge.domain.member.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.global.apiPayload.code.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberSuccessCode implements BaseSuccessCode {

    PARENT_SIGNUP_SUCCESS(HttpStatus.CREATED, "MEMBER201_PARENT_SIGNUP", "부모 회원가입에 성공했습니다."),
    CHILDREN_SIGNUP_SUCCESS(HttpStatus.CREATED, "MEMBER201_CHILDREN_SIGNUP", "자녀 회원가입에 성공했습니다."),
    PARENT_LOGIN_SUCCESS(HttpStatus.OK, "MEMBER200_PARENT_LOGIN", "부모 로그인에 성공했습니다."),
    CHILDREN_LOGIN_SUCCESS(HttpStatus.OK, "MEMBER200_CHILDREN_LOGIN", "자녀 로그인에 성공했습니다."),
    REFRESH_TOKEN_SUCCESS(HttpStatus.OK, "MEMBER200_REFRESH", "토큰이 성공적으로 갱신되었습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "MEMBER200_LOGOUT", "로그아웃에 성공했습니다."),
    CHILDREN_REGISTER_SUCCESS(HttpStatus.CREATED, "MEMBER201_CHILDREN_REGISTER", "자녀 등록에 성공했습니다."),
    CHILDREN_LIST_SUCCESS(HttpStatus.OK, "MEMBER200_CHILDREN_LIST", "자녀 목록 조회에 성공했습니다."),
    PASSWORD_CHANGE_SUCCESS(HttpStatus.OK, "MEMBER200_PASSWORD_CHANGE", "비밀번호 변경에 성공했습니다."),
    MEMBER_WITHDRAW_SUCCESS(HttpStatus.OK, "MEMBER200_WITHDRAW", "회원 탈퇴에 성공했습니다."),
    TIME_POLICY_SET_SUCCESS(HttpStatus.CREATED, "MEMBER201_TIME_POLICY_SET", "자녀 총시간 설정에 성공했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
