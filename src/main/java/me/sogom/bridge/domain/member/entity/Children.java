package me.sogom.bridge.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import me.sogom.bridge.domain.common.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "children")
public class Children extends BaseEntity implements Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "children_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(nullable = false)
    private String hash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column
    private LocalDateTime passwordChangedAt;

    @Column(length = 8, unique = true)
    private String code; // 자녀 연동 코드

    @Column(length = 4)
    private String birth; // 자녀 출생연도

    @Column(length = 100)
    private String profileImageUrl;

    // FCM 토큰 저장
    @Column(length = 500)
    private String fcmToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    public void changePassword(String newHash, LocalDateTime changedAt) {
        this.hash = newHash;
        this.passwordChangedAt = changedAt;
    }

    // FCM 토큰 저장 및 갱신
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void markDormant() {
        this.status = MemberStatus.DORMANT;
    }

    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.code = null;
        this.parent = null;
    }
}