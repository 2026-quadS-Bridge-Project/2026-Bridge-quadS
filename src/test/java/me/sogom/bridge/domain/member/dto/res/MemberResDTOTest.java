package me.sogom.bridge.domain.member.dto.res;

import me.sogom.bridge.domain.member.entity.Children;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberResDTOTest {

    @Test
    void authResponseCarriesChildCodeForChildMembers() {
        MemberResDTO.AuthResponse response = new MemberResDTO.AuthResponse(
                null,
                null,
                1L,
                "Child",
                "AB12CD34"
        );

        assertThat(response.childCode()).isEqualTo("AB12CD34");
    }

    @Test
    void childrenInfoResponseIncludesChildCode() {
        Children children = Children.builder()
                .id(1L)
                .name("Child")
                .email("child@example.com")
                .hash("hash")
                .code("AB12CD34")
                .build();

        MemberResDTO.ChildrenInfoResponse response =
                MemberResDTO.ChildrenInfoResponse.of(children, null);

        assertThat(response.childrenId()).isEqualTo(1L);
        assertThat(response.childCode()).isEqualTo("AB12CD34");
        assertThat(response.name()).isEqualTo("Child");
    }
}
