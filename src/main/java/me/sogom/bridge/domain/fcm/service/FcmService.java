package me.sogom.bridge.domain.fcm.service;

import me.sogom.bridge.domain.fcm.dto.message.FcmMessageDTO;
import me.sogom.bridge.global.security.entity.AuthMember;
import me.sogom.bridge.global.security.entity.MemberRole;

public interface FcmService {

    // FCM 토큰 저장 및 갱신
    void saveFcmToken(
            AuthMember authMember,
            String fcmToken
    );

    // 일반 푸시 알림 전송
    void sendPush(
            Long memberId,
            MemberRole memberRole,
            FcmMessageDTO message
    );

    // Silent Push 전송
    void sendSilentPush(
            Long memberId,
            MemberRole memberRole,
            String type
    );
}