package me.sogom.bridge.domain.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import me.sogom.bridge.domain.common.BaseEntity;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "time_policy")
public class TimePolicy extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_policy_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "children_id", nullable = false) // 자녀 테이블 PK 이름에 맞춤
    private Children child;

    @Column(nullable = false)
    private int baseTime; // 부모님이 설정한 기본 시간 (분)

    @Column(nullable = false)
    private int accumulatedRewardTime; // 미션 성공으로 쌓인 총 보상 시간 (분)

    @Column(nullable = false, length = 7)
    private String yearMonth; // 해당 정책의 년월 (예: "2026-04")

    // 보상 시간 추가 메서드
    public void addReward(int rewardMinutes) {
        this.accumulatedRewardTime += rewardMinutes;
    }

    // 자녀가 조회할 '사용 가능한 총 시간' 계산
    public int getTotalAvailableTime() {
        return this.baseTime + this.accumulatedRewardTime;
    }
}