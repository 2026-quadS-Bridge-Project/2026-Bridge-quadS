package me.sogom.bridge.domain.member.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.MemberErrorCode;
import me.sogom.bridge.domain.member.MemberException;
import me.sogom.bridge.domain.member.dto.AuthResponse;
import me.sogom.bridge.domain.member.dto.LoginRequest;
import me.sogom.bridge.domain.member.dto.SignUpRequest;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import me.sogom.bridge.global.security.entity.AuthMember;
import me.sogom.bridge.global.security.entity.MemberRole;
import me.sogom.bridge.global.security.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ParentRepository parentRepository;
    private final ChildrenRepository childrenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse signUpParent(SignUpRequest request) {
        checkDuplicateEmail(request.email());
        Parent parent = Parent.builder()
                .name(request.name())
                .email(request.email())
                .hash(passwordEncoder.encode(request.password()))
                .build();
        parentRepository.save(parent);
        return new AuthResponse(jwtUtil.createAccessToken(new AuthMember(parent, MemberRole.PARENT)));
    }

    @Transactional
    public AuthResponse signUpChildren(SignUpRequest request) {
        checkDuplicateEmail(request.email());
        Children children = Children.builder()
                .name(request.name())
                .email(request.email())
                .hash(passwordEncoder.encode(request.password()))
                .build();
        childrenRepository.save(children);
        return new AuthResponse(jwtUtil.createAccessToken(new AuthMember(children, MemberRole.CHILDREN)));
    }

    @Transactional(readOnly = true)
    public AuthResponse loginParent(LoginRequest request) {
        Parent parent = parentRepository.findByEmail(request.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        validatePassword(request.password(), parent.getHash());
        return new AuthResponse(jwtUtil.createAccessToken(new AuthMember(parent, MemberRole.PARENT)));
    }

    @Transactional(readOnly = true)
    public AuthResponse loginChildren(LoginRequest request) {
        Children children = childrenRepository.findByEmail(request.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        validatePassword(request.password(), children.getHash());
        return new AuthResponse(jwtUtil.createAccessToken(new AuthMember(children, MemberRole.CHILDREN)));
    }

    private void checkDuplicateEmail(String email) {
        if (parentRepository.findByEmail(email).isPresent() || childrenRepository.findByEmail(email).isPresent()) {
            throw new MemberException(MemberErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new MemberException(MemberErrorCode.INVALID_PASSWORD);
        }
    }
}
