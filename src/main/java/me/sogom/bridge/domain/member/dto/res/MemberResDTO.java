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
        @Nullable String refreshToken,
        @Nullable Long memberId,
        @Nullable String name,
        @Nullable String childCode
    ) {}

    @Builder
    public record
    ChildrenInfoResponse(
            @JsonProperty("childrenId") Long childrenId,
            @JsonProperty("childCode") String childCode,
            @JsonProperty("name") String name,
            @JsonProperty("profileImageUrl") String profileImageUrl
    ) {
        public static ChildrenInfoResponse of(Children children, String profileImageUrl) {
            return ChildrenInfoResponse.builder()
                    .childrenId(children.getId())
                    .childCode(children.getCode())
                    .name(children.getName())
                    .profileImageUrl(profileImageUrl)
                    .build();
        }
    }
}
