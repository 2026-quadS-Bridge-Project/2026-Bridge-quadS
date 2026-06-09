package me.sogom.bridge.domain.policy.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyReqDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void setTimePolicyRequestAcceptsIsoYearMonth() {
        PolicyReqDTO.SetTimePolicyRequest request =
                new PolicyReqDTO.SetTimePolicyRequest(2L, "2026-06", 600);

        Set<ConstraintViolation<PolicyReqDTO.SetTimePolicyRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void setTimePolicyRequestRejectsNonIsoYearMonth() {
        PolicyReqDTO.SetTimePolicyRequest shortMonth =
                new PolicyReqDTO.SetTimePolicyRequest(2L, "2026-6", 600);
        PolicyReqDTO.SetTimePolicyRequest invalidMonth =
                new PolicyReqDTO.SetTimePolicyRequest(2L, "2026-13", 600);

        assertThat(validator.validate(shortMonth))
                .extracting(ConstraintViolation::getMessage)
                .contains("년월은 yyyy-MM 형식이어야 합니다.");
        assertThat(validator.validate(invalidMonth))
                .extracting(ConstraintViolation::getMessage)
                .contains("년월은 yyyy-MM 형식이어야 합니다.");
    }
}
