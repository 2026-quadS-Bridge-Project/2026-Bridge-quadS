package me.sogom.bridge.domain.fcm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.sogom.bridge.domain.fcm.dto.message.FcmMessageDTO;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import me.sogom.bridge.global.security.entity.AuthMember;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class MockFcmService implements FcmService {

    private final ParentRepository parentRepository;
    private final ChildrenRepository childrenRepository;

    // 일반 푸시 알림 Mock 전송
    @Override
    public void sendPush(
            Long memberId,
            MemberRole memberRole,
            FcmMessageDTO message
    ) {

        String fcmToken = getFcmToken(memberId, memberRole);

        // 토큰이 없는 경우 전송하지 않음
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }

        log.info("푸시 알림 전송");
        log.info("FCM Token : {}", fcmToken);
        log.info("Title : {}", message.title());
        log.info("Body : {}", message.body());
        log.info("Type : {}", message.type());
        log.info("TargetId : {}", message.targetId());
    }

    // Silent Push Mock 전송
    @Override
    public void sendSilentPush(
            Long memberId,
            MemberRole memberRole,
            String type
    ) {

        String fcmToken = getFcmToken(memberId, memberRole);

        // 토큰이 없는 경우 전송하지 않음
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }

        log.info("Silent Push 전송");
        log.info("FCM Token : {}", fcmToken);
        log.info("Type : {}", type);
    }

    // 회원 역할에 따라 FCM 토큰 조회
    private String getFcmToken(
            Long memberId,
            MemberRole memberRole
    ) {

        // 부모 회원 조회
        if (memberRole == MemberRole.PARENT) {

            Parent parent = parentRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("부모 회원을 찾을 수 없습니다."));

            return parent.getFcmToken();
        }

        // 자녀 회원 조회
        if (memberRole == MemberRole.CHILDREN) {

            Children children = childrenRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("자녀 회원을 찾을 수 없습니다."));

            return children.getFcmToken();
        }

        return null;
    }

    // FCM 토큰 저장 및 갱신
    @Override
    public void saveFcmToken(
            AuthMember authMember,
            String fcmToken
    ) {

        // 부모 계정인 경우
        if (authMember.getRole() == MemberRole.PARENT) {

            Parent parent = authMember.asParent();

            parent.updateFcmToken(fcmToken);

            parentRepository.save(parent);

            return;
        }

        // 자녀 계정인 경우
        if (authMember.getRole() == MemberRole.CHILDREN) {

            Children children = authMember.asChildren();

            children.updateFcmToken(fcmToken);

            childrenRepository.save(children);
        }
    }

}