package me.sogom.bridge.domain.member.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.code.MemberErrorCode;
import me.sogom.bridge.domain.member.MemberException;
import me.sogom.bridge.domain.member.dto.AuthResponse;
import me.sogom.bridge.domain.member.dto.LoginRequest;
import me.sogom.bridge.domain.member.dto.RefreshRequest;
import me.sogom.bridge.domain.member.dto.SignUpRequest;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.entity.RefreshToken;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import me.sogom.bridge.domain.member.repository.RefreshTokenRepository;
import me.sogom.bridge.global.security.entity.AuthMember;
import me.sogom.bridge.global.security.entity.MemberRole;
import me.sogom.bridge.global.security.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ParentRepository parentRepository;
    private final ChildrenRepository childrenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
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
        return new AuthResponse(null, null);
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
        return new AuthResponse(null, null);
    }

    @Transactional
    public AuthResponse loginParent(LoginRequest request) {
        Parent parent = parentRepository.findByEmail(request.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        validatePassword(request.password(), parent.getHash());
        return issueTokens(new AuthMember(parent, MemberRole.PARENT));
    }

    @Transactional
    public AuthResponse loginChildren(LoginRequest request) {
        Children children = childrenRepository.findByEmail(request.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        validatePassword(request.password(), children.getHash());
        return issueTokens(new AuthMember(children, MemberRole.CHILDREN));
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenValue = request.refreshToken();

        if (!jwtUtil.isValid(tokenValue)) {
            throw new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken stored = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new MemberException(MemberErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        AuthMember authMember = loadAuthMember(stored.getEmail(), stored.getRole());
        return issueAccessTokenOnly(authMember);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.deleteByToken(request.refreshToken());
    }

    private AuthResponse issueTokens(AuthMember authMember) {
        String accessToken = jwtUtil.createAccessToken(authMember);
        String refreshTokenValue = jwtUtil.createRefreshToken(authMember);

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtUtil.getRefreshExpirationMillis() / 1000);

        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshTokenValue)
                .email(authMember.getUsername())
                .role(authMember.getRole())
                .expiresAt(expiresAt)
                .build());

        return new AuthResponse(accessToken, refreshTokenValue);
    }

    private AuthResponse issueAccessTokenOnly(AuthMember authMember) {
        String accessToken = jwtUtil.createAccessToken(authMember);
        return new AuthResponse(accessToken, null);
    }

    private AuthMember loadAuthMember(String email, MemberRole role) {
        if (role == MemberRole.PARENT) {
            Parent parent = parentRepository.findByEmail(email)
                    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
            return new AuthMember(parent, MemberRole.PARENT);
        }
        Children children = childrenRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        return new AuthMember(children, MemberRole.CHILDREN);
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
