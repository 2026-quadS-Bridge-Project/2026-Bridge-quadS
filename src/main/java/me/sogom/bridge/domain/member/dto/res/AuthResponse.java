package me.sogom.bridge.domain.member.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.lang.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
    @Nullable String accessToken,
    @Nullable String refreshToken
) {}
