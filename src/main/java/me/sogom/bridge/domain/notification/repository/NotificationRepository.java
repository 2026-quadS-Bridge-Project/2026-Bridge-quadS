package me.sogom.bridge.domain.notification.repository;

import me.sogom.bridge.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 회원의 알림 목록 조회
    List<Notification> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);
}