package me.sogom.bridge.domain.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class PolicyReqDTO {

    public record SetTimePolicyRequest(

            @NotNull(message = "자녀 ID를 입력해 주세요.")
            Long childId,

            @NotBlank(message = "년월을 입력해 주세요.")
            @Pattern(
                    regexp = "\\d{4}-(0[1-9]|1[0-2])",
                    message = "년월은 yyyy-MM 형식이어야 합니다."
            )
            String yearMonth,

            @NotNull(message = "기본 시간을 입력해 주세요.")
            @Positive(message = "기본 시간은 0보다 커야 합니다.")
            Integer baseTime
    ) {}
}
