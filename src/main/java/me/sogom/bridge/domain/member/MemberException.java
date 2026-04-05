package me.sogom.bridge.domain.member;

import me.sogom.bridge.global.apiPayload.code.BaseErrorCode;
import me.sogom.bridge.global.apiPayload.exception.ProjectException;

public class MemberException extends ProjectException {

    public MemberException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
