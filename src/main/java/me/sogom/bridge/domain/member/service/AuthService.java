package me.sogom.bridge.domain.member.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.code.MemberErrorCode;
import me.sogom.bridge.domain.member.MemberException;
import me.sogom.bridge.domain.member.dto.req.MemberReqDTO;
import me.sogom.bridge.domain.member.dto.res.MemberResDTO;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Member;
import me.sogom.bridge.domain.member.entity.MemberStatus;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.entity.RefreshToken;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import me.sogom.bridge.domain.member.repository.RefreshTokenRepository;
import me.sogom.bridge.domain.member.util.ChildrenCodeGenerator;
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
    public MemberResDTO.AuthResponse signUpParent(MemberReqDTO.SignUpRequest request) {
        checkDuplicateEmail(request.email());
        Parent parent = Parent.builder()
                .name(request.name())
                .email(request.email())
                .hash(passwordEncoder.encode(request.password()))
                .build();
        parentRepository.save(parent);
        return new MemberResDTO.AuthResponse(null, null, null, null);
    }

    @Transactional
    public MemberResDTO.AuthResponse signUpChildren(MemberReqDTO.SignUpRequest request) {
        checkDuplicateEmail(request.email());

        String code = ChildrenCodeGenerator.generateCode();
        Children children = Children.builder()
                .name(request.name())
                .email(request.email())
                .hash(passwordEncoder.encode(request.password()))
                .code(code)
                .build();
        childrenRepository.save(children);
        return new MemberResDTO.AuthResponse(null, null, null, null);
    }

    @Transactional
    public MemberResDTO.AuthResponse loginParent(MemberReqDTO.LoginRequest request) {
        Parent parent = parentRepository.findByEmail(request.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        validateActive(parent.getStatus());
        validatePassword(request.password(), parent.getHash());
        return issueTokens(new AuthMember(parent, MemberRole.PARENT));
    }

    @Transactional
    public MemberResDTO.AuthResponse loginChildren(MemberReqDTO.LoginRequest request) {
        Children children = childrenRepository.findByEmail(request.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        validateActive(children.getStatus());
        validatePassword(request.password(), children.getHash());
        return issueTokens(new AuthMember(children, MemberRole.CHILDREN));
    }

    @Transactional
    public MemberResDTO.AuthResponse refresh(MemberReqDTO.RefreshRequest request) {
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
    public void logout(MemberReqDTO.RefreshRequest request) {
        refreshTokenRepository.deleteByToken(request.refreshToken());
    }

    private MemberResDTO.AuthResponse issueTokens(AuthMember authMember) {
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

        Member member = authMember.getMember();
        return new MemberResDTO.AuthResponse(accessToken, refreshTokenValue, member.getId(), member.getName());
    }

    private MemberResDTO.AuthResponse issueAccessTokenOnly(AuthMember authMember) {
        String accessToken = jwtUtil.createAccessToken(authMember);
        Member member = authMember.getMember();
        return new MemberResDTO.AuthResponse(accessToken, null, member.getId(), member.getName());
    }

    private AuthMember loadAuthMember(String email, MemberRole role) {
        if (role == MemberRole.PARENT) {
            Parent parent = parentRepository.findByEmail(email)
                    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
            validateActive(parent.getStatus());
            return new AuthMember(parent, MemberRole.PARENT);
        }
        Children children = childrenRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        validateActive(children.getStatus());
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

    private void validateActive(MemberStatus status) {
        if (status != null && status != MemberStatus.ACTIVE) {
            throw new MemberException(MemberErrorCode.MEMBER_INACTIVE);
        }
    }

    @Transactional
    public void changePassword(AuthMember authMember, MemberReqDTO.ChangePasswordRequest request) {
        if (authMember.getRole() == MemberRole.PARENT) {
            Parent parent = parentRepository.findByEmail(authMember.getUsername())
                    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
            validatePassword(request.oldPassword(), parent.getHash());
            validateNewPasswordNotSame(request.newPassword(), parent.getHash());
            validatePasswordChangeCooldown(parent.getPasswordChangedAt());
            parent.changePassword(passwordEncoder.encode(request.newPassword()), LocalDateTime.now());
            return;
        }
        Children children = childrenRepository.findByEmail(authMember.getUsername())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        validatePassword(request.oldPassword(), children.getHash());
        validateNewPasswordNotSame(request.newPassword(), children.getHash());
        validatePasswordChangeCooldown(children.getPasswordChangedAt());
        children.changePassword(passwordEncoder.encode(request.newPassword()), LocalDateTime.now());
    }

    @Transactional
    public void withdraw(AuthMember authMember) {
        if (authMember.getRole() == MemberRole.PARENT) {
            Parent parent = parentRepository.findByEmail(authMember.getUsername())
                    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

            detachChildrenAsDormant(parent);
            parent.withdraw();
            refreshTokenRepository.deleteByEmail(parent.getEmail());
            return;
        }
        Children children = childrenRepository.findByEmail(authMember.getUsername())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        detachFromParent(children);
        children.withdraw();
        refreshTokenRepository.deleteByEmail(children.getEmail());
    }

    private void validateNewPasswordNotSame(String newPassword, String encodedPassword) {
        if (passwordEncoder.matches(newPassword, encodedPassword)) {
            throw new MemberException(MemberErrorCode.PASSWORD_SAME_AS_OLD);
        }
    }

    private void validatePasswordChangeCooldown(LocalDateTime lastChangedAt) {
        if (lastChangedAt != null && lastChangedAt.isAfter(LocalDateTime.now().minusHours(24))) {
            throw new MemberException(MemberErrorCode.PASSWORD_CHANGE_TOO_SOON);
        }
    }

    // 부모 탈퇴 시, 자녀는 부모와의 연결 해제 + 코드 초기화 + 휴면 상태로 변경
    private void detachChildrenAsDormant(Parent parent) {
        for (Children child : java.util.List.copyOf(parent.getChildren())) {
            child.setParent(null);
            child.setCode(null);
            child.markDormant();
        }
        parent.getChildren().clear();
    }

    // 자녀 탈퇴 시, 부모와의 연결 해제
    private void detachFromParent(Children child) {
        Parent parent = child.getParent();
        if (parent != null) {
            parent.getChildren().remove(child);
            child.setParent(null);
        }
    }
}
