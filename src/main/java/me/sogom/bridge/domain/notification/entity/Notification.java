package me.sogom.bridge.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.sogom.bridge.domain.common.BaseEntity;
import me.sogom.bridge.global.security.entity.MemberRole;

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

    // 알림 수신 회원 역할
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole memberRole;

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

    private Long childId;

    private Long missionId;

    private Long performanceId;

    private String targetRoute;

    @Builder
    public Notification(
            Long memberId,
            MemberRole memberRole,
            String title,
            String content,
            Boolean isRead,
            NotificationType notificationType,
            Long childId,
            Long missionId,
            Long performanceId,
            String targetRoute
    ) {
        this.memberId = memberId;
        this.memberRole = memberRole;
        this.title = title;
        this.content = content;
        this.isRead = isRead;
        this.notificationType = notificationType;
        this.childId = childId;
        this.missionId = missionId;
        this.performanceId = performanceId;
        this.targetRoute = targetRoute;
    }

    // 알림 읽음 처리
    public void read() {
        this.isRead = true;
    }
}
