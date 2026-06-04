package me.sogom.bridge.global.security.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Member;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.global.apiPayload.code.GeneralErrorCode;
import me.sogom.bridge.global.apiPayload.exception.ProjectException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class AuthMember implements UserDetails {

    private final Member member;
    private final MemberRole role;

    public Parent asParent() {
        if (role != MemberRole.PARENT) {
            throw new ProjectException(GeneralErrorCode.FORBIDDEN);
        }
        if (!(member instanceof Parent)) {
            throw new ProjectException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
        return (Parent) member;
    }
    
    public Children asChildren() {
        if (role != MemberRole.CHILDREN) {
            throw new ProjectException(GeneralErrorCode.FORBIDDEN);
        }
        if (!(member instanceof Children)) {
            throw new ProjectException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
        return (Children) member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return member.getHash();
    }

    @Override
    public String getUsername() {
        return member.getEmail();
    }
}
