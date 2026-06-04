package me.sogom.bridge.domain.mission.exception;

import me.sogom.bridge.global.apiPayload.code.BaseErrorCode;
import me.sogom.bridge.global.apiPayload.exception.ProjectException;

public class MissionException extends ProjectException {
    public MissionException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
