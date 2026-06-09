package me.sogom.bridge.domain.member.controller;

import me.sogom.bridge.domain.member.dto.req.MemberReqDTO;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.service.ParentService;
import me.sogom.bridge.domain.policy.dto.PolicyReqDTO;
import me.sogom.bridge.global.apiPayload.code.GeneralErrorCode;
import me.sogom.bridge.global.apiPayload.exception.ProjectException;
import me.sogom.bridge.global.security.entity.AuthMember;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ParentControllerTest {

    private final ParentService parentService = mock(ParentService.class);
    private final ParentController parentController = new ParentController(parentService);

    @Test
    void registerChildRejectsChildrenPrincipalBeforeServiceCall() {
        AuthMember childPrincipal = childPrincipal();

        assertThatThrownBy(() -> parentController.registerChild(
                childPrincipal,
                new MemberReqDTO.RegisterChildRequest("하늘", "2016", "CHILD-CODE", null)
        ))
                .isInstanceOf(ProjectException.class)
                .satisfies(exception -> assertThat(((ProjectException) exception).getErrorCode())
                        .isEqualTo(GeneralErrorCode.FORBIDDEN));

        verify(parentService, never()).registerChild(anyLong(), any());
    }

    @Test
    void getChildrenListRejectsChildrenPrincipalBeforeServiceCall() {
        AuthMember childPrincipal = childPrincipal();

        assertThatThrownBy(() -> parentController.getChildrenList(childPrincipal))
                .isInstanceOf(ProjectException.class)
                .satisfies(exception -> assertThat(((ProjectException) exception).getErrorCode())
                        .isEqualTo(GeneralErrorCode.FORBIDDEN));

        verify(parentService, never()).getChildrenList(anyLong());
    }

    @Test
    void setTimePolicyRejectsChildrenPrincipalBeforeServiceCall() {
        AuthMember childPrincipal = childPrincipal();

        assertThatThrownBy(() -> parentController.setTimePolicy(
                childPrincipal,
                new PolicyReqDTO.SetTimePolicyRequest(2L, "2026-06", 600)
        ))
                .isInstanceOf(ProjectException.class)
                .satisfies(exception -> assertThat(((ProjectException) exception).getErrorCode())
                        .isEqualTo(GeneralErrorCode.FORBIDDEN));

        verify(parentService, never()).setTimePolicy(anyLong(), any());
    }

    private AuthMember childPrincipal() {
        Children child = Children.builder()
                .id(2L)
                .name("하늘")
                .email("child@test.com")
                .hash("hash")
                .build();
        return new AuthMember(child, MemberRole.CHILDREN);
    }
}
