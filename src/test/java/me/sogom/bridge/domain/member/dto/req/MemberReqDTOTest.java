package me.sogom.bridge.domain.member.dto.req;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberReqDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void registerChildRequestAcceptsFourDigitBirthYear() {
        MemberReqDTO.RegisterChildRequest request =
                new MemberReqDTO.RegisterChildRequest("하늘", "2016", "CHILD-CODE", null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void registerChildRequestRejectsFullDateBirth() {
        MemberReqDTO.RegisterChildRequest request =
                new MemberReqDTO.RegisterChildRequest("하늘", "2016-01-01", "CHILD-CODE", null);

        assertThat(validator.validate(request))
                .extracting(ConstraintViolation::getMessage)
                .contains("자녀 출생연도는 4자리 연도여야 합니다.");
    }

    @Test
    void registerChildRequestStillAllowsMissingBirthYear() {
        MemberReqDTO.RegisterChildRequest request =
                new MemberReqDTO.RegisterChildRequest("하늘", null, "CHILD-CODE", null);

        assertThat(validator.validate(request)).isEmpty();
    }
}
