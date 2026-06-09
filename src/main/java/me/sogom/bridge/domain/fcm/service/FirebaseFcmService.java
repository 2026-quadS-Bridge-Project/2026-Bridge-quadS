package me.sogom.bridge.domain.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.sogom.bridge.domain.fcm.dto.message.FcmMessageDTO;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import me.sogom.bridge.global.security.entity.AuthMember;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseFcmService implements FcmService {

    private final ParentRepository parentRepository;
    private final ChildrenRepository childrenRepository;

    @Override
    public void saveFcmToken(
            AuthMember authMember,
            String fcmToken
    ) {

        if (authMember.getRole() == MemberRole.PARENT) {
            Parent parent = authMember.asParent();
            parent.updateFcmToken(fcmToken);
            parentRepository.save(parent);
            return;
        }

        else if (authMember.getRole() == MemberRole.CHILDREN) {
            Children children = authMember.asChildren();
            children.updateFcmToken(fcmToken);
            childrenRepository.save(children);
            return;
        }
    }

    @Override
    public void sendPush(
            Long memberId,
            MemberRole memberRole,
            FcmMessageDTO message
    ) {

        try {

            String token = getFcmToken(memberId, memberRole);
            if (token == null || token.isBlank()) {
                return;
            }
            Message.Builder firebaseMessageBuilder = Message.builder()
                    .setToken(token);
            applyVisibleNotification(firebaseMessageBuilder, message);
            message.dataPayload().forEach(firebaseMessageBuilder::putData);
            Message firebaseMessage = firebaseMessageBuilder.build();
            FirebaseMessaging.getInstance().send(firebaseMessage);
            log.info("Firebase Push 전송 완료");
        } catch (Exception e) {
            log.error("Firebase Push 전송 실패", e);
        }
    }

    @Override
    public void sendSilentPush(
            Long memberId,
            MemberRole memberRole,
            String type
    ) {

        try {

            String token = getFcmToken(memberId, memberRole);
            if (token == null || token.isBlank()) {
                return;
            }
            Message firebaseMessage = Message.builder()
                    .setToken(token)
                    .putData("type", type)
                    .build();
            FirebaseMessaging.getInstance().send(firebaseMessage);
            log.info("Firebase Silent Push 전송 완료");
        } catch (Exception e) {
            log.error("Firebase Silent Push 전송 실패", e);
        }
    }

    private String getFcmToken(
            Long memberId,
            MemberRole memberRole
    ) {

        if (memberRole == MemberRole.PARENT) {
            Parent parent = parentRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("부모 회원을 찾을 수 없습니다."));
            return parent.getFcmToken();
        }

        else if (memberRole == MemberRole.CHILDREN) {
            Children children = childrenRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("자녀 회원을 찾을 수 없습니다."));
            return children.getFcmToken();
        }

        return null;
    }

    private void applyVisibleNotification(
            Message.Builder firebaseMessageBuilder,
            FcmMessageDTO message
    ) {

        if (isBlank(message.title()) && isBlank(message.body())) {
            return;
        }

        com.google.firebase.messaging.Notification.Builder notificationBuilder =
                com.google.firebase.messaging.Notification.builder();
        if (!isBlank(message.title())) {
            notificationBuilder.setTitle(message.title());
        }
        if (!isBlank(message.body())) {
            notificationBuilder.setBody(message.body());
        }

        firebaseMessageBuilder.setNotification(notificationBuilder.build());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
