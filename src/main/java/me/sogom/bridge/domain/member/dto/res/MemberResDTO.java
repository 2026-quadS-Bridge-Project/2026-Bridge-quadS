package me.sogom.bridge.domain.member.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import me.sogom.bridge.domain.member.entity.Children;
import org.springframework.lang.Nullable;

public class MemberResDTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AuthResponse(
        @Nullable String accessToken,
        @Nullable String refreshToken
    ) {}

    @Builder
    public record ChildrenInfoResponse(
            @JsonProperty("childrenId") Long childrenId,
            @JsonProperty("name") String name,
            @JsonProperty("profileImageUrl") String profileImageUrl
    ) {
        public static ChildrenInfoResponse of(Children children) {
            return ChildrenInfoResponse.builder()
                    .childrenId(children.getId())
                    .name(children.getName())
                    .profileImageUrl(children.getProfileImageUrl())
                    .build();
        }
    }
}

