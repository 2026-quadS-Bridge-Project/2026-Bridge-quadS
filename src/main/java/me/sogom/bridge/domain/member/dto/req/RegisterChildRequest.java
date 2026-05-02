package me.sogom.bridge.domain.member.dto.req;

import jakarta.validation.constraints.NotBlank;

public record RegisterChildRequest(

        @NotBlank(message = "자녀 이름을 입력해 주세요.")
        String childrenName,

        @NotBlank(message = "자녀 코드를 입력해 주세요.")
        String childrenCode,

        String profileUrl // optional
) {}

