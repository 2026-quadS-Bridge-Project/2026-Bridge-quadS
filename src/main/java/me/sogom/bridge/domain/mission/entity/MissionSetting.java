package me.sogom.bridge.domain.mission.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
//mission setting entity
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mission_setting")
public class MissionSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_setting_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false, unique = true)
    private Mission mission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "reset_cycle", nullable = false)
    private ResetCycle resetCycle; // 미션 초기화 주기 (DAILY, WEEKLY, MONTHLY)

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false)
    private VerificationType verificationType; // 확인 방식 (AI, CHILD, PARENT)

    @Column(nullable = false)
    private int reward; // 지급 시간 (분)

    @Column(length = 500)
    private String description; // 미션 상세 설명

    @Column(length = 100)
    private String prompt; // 부모가 작성한 AI 검증 기준

    @Column(name = "last_reset_at", nullable = false)
    private LocalDateTime lastResetAt;


    public void markReset(LocalDateTime resetAt) {
        this.lastResetAt = resetAt;
    }
}
