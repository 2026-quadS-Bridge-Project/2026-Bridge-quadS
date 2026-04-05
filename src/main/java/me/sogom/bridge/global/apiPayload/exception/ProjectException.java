package me.sogom.bridge.global.apiPayload.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.global.apiPayload.code.BaseErrorCode;

@Getter
@RequiredArgsConstructor
public class ProjectException extends RuntimeException {

    private final BaseErrorCode errorCode;

}
