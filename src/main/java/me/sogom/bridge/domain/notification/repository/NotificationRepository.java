package me.sogom.bridge.domain.notification.repository;

import me.sogom.bridge.domain.notification.entity.Notification;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 회원의 알림 목록 조회
    List<Notification> findAllByMemberIdAndMemberRoleOrderByCreatedAtDesc(
            Long memberId,
            MemberRole memberRole
    );
}