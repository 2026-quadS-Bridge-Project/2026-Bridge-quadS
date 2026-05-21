package me.sogom.bridge.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.sogom.bridge.domain.common.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 알림 수신 회원 ID
    @Column(nullable = false)
    private Long memberId;

    // 알림 제목
    @Column(nullable = false)
    private String title;

    // 알림 내용
    @Column(nullable = false)
    private String content;

    // 알림 읽음 여부
    @Column(nullable = false)
    private Boolean isRead;

    // 알림 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Builder
    public Notification(
            Long memberId,
            String title,
            String content,
            Boolean isRead,
            NotificationType notificationType
    ) {
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        this.isRead = isRead;
        this.notificationType = notificationType;
    }

    // 알림 읽음 처리
    public void read() {
        this.isRead = true;
    }
}