package me.sogom.bridge.domain.policy.controller;

import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.policy.dto.PolicyResponse;
import me.sogom.bridge.domain.policy.service.ChildPolicyService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.apiPayload.code.GeneralErrorCode;
import me.sogom.bridge.global.apiPayload.exception.ProjectException;
import me.sogom.bridge.global.security.entity.AuthMember;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChildPolicyControllerTest {

    private final ChildPolicyService childPolicyService = mock(ChildPolicyService.class);
    private final ChildPolicyController controller = new ChildPolicyController(childPolicyService);

    @Test
    void getChildPolicyAllowsAuthenticatedChildToReadOwnPolicy() {
        PolicyResponse policy = PolicyResponse.builder()
                .baseTime(600)
                .accumulatedRewardTime(30)
                .totalAvailableTime(630)
                .blockedApps(List.of())
                .build();
        when(childPolicyService.getChildPolicy(22L)).thenReturn(policy);

        ApiResponse<PolicyResponse> response = controller.getChildPolicy(authChild(22L), 22L);

        assertThat(response.getData()).isSameAs(policy);
        verify(childPolicyService).getChildPolicy(22L);
    }

    @Test
    void getChildPolicyRejectsOtherChildPolicyLookup() {
        assertThatThrownBy(() -> controller.getChildPolicy(authChild(22L), 33L))
                .isInstanceOf(ProjectException.class)
                .satisfies(exception ->
                        assertThat(((ProjectException) exception).getErrorCode())
                                .isEqualTo(GeneralErrorCode.FORBIDDEN));

        verify(childPolicyService, never()).getChildPolicy(33L);
    }

    private AuthMember authChild(Long childId) {
        Children child = Children.builder()
                .id(childId)
                .name("child")
                .email("child@test.com")
                .hash("hash")
                .build();
        return new AuthMember(child, MemberRole.CHILDREN);
    }
}
