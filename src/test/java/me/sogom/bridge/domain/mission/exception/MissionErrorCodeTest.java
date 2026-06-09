package me.sogom.bridge.domain.mission.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MissionErrorCodeTest {

    @Test
    void missionReviewErrorsExposeDistinctWireCodes() {
        assertThat(MissionErrorCode.MISSION_ALREADY_COMPLETED.getCode())
                .isEqualTo("MISSION_ALREADY_COMPLETED");
        assertThat(MissionErrorCode.MISSION_ALREADY_SUBMITTED.getCode())
                .isEqualTo("MISSION_ALREADY_SUBMITTED");
        assertThat(MissionErrorCode.INVALID_MISSION_STATE.getCode())
                .isEqualTo("INVALID_MISSION_STATE");
    }
}
