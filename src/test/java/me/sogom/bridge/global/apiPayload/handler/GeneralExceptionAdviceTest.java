package me.sogom.bridge.global.apiPayload.handler;

import me.sogom.bridge.global.apiPayload.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GeneralExceptionAdviceTest {

    private final GeneralExceptionAdvice advice = new GeneralExceptionAdvice();

    @Test
    void illegalArgumentExceptionReturnsBadRequestWithDetailMessage() {
        ResponseEntity<ApiResponse<String>> response = advice.handleIllegalArgumentException(
                new IllegalArgumentException("정책이 없습니다.")
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("COMMON400");
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getData()).isEqualTo("정책이 없습니다.");
    }
}
