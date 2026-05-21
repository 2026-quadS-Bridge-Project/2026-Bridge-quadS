package me.sogom.bridge.domain.fcm.service;

import lombok.extern.slf4j.Slf4j;
import me.sogom.bridge.domain.fcm.dto.message.FcmMessageDTO;
import me.sogom.bridge.global.security.entity.AuthMember;
import me.sogom.bridge.global.security.entity.MemberRole;

@Slf4j
//@Service  실제로 Firebase 연동 시 주석 처리 해제 및 MockFcmService의 @Service 해제
public class FirebaseFcmService implements FcmService {

    // 실제 Firebase 연동 예정 서비스
    // Firebase service-account.json 연결 후 구현 예정

    @Override
    public void sendPush(
            Long memberId,
            MemberRole memberRole,
            FcmMessageDTO message
    ) {

        log.info("Firebase 일반 푸시 전송 예정");
    }

    @Override
    public void sendSilentPush(
            Long memberId,
            MemberRole memberRole,
            String type
    ) {

        log.info("Firebase Silent Push 전송 예정");
    }

    // Firebase 토큰 저장 예정
    @Override
    public void saveFcmToken(
            AuthMember authMember,
            String fcmToken
    ) {

        log.info("Firebase FCM 토큰 저장 예정");
    }

}