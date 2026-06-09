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

    public void updateBaseTime(int baseTime) { this.baseTime = baseTime; }

    // 기본 제공 시간과 보상 시간을 합쳐서 사용할 시간을 차감한다.
    // 기본 시간이 먼저 소진되고, 부족한 부분만 보상 시간에서 차감된다.
    public void deductAvailableTime(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("사용할 시간은 0보다 커야 합니다.");
        }
        int totalAvailable = this.baseTime + this.accumulatedRewardTime;
        if (totalAvailable < minutes) {
            throw new IllegalArgumentException("사용 가능한 총 시간(기본+보상)이 부족하여 연장할 수 없습니다.");
        }

        if (this.baseTime >= minutes) {
            this.baseTime -= minutes;
        }
        else {
            int remainder = minutes - this.baseTime;
            this.baseTime = 0;
            this.accumulatedRewardTime -= remainder;
        }
    }

    //자녀가 당일에 쓰지 않고 남긴 시간을 보상 시간 풀로 다시 환불(적립)해주는 메서드
    public void refundUnusedTime(int unusedMinutes) {
        if (unusedMinutes < 0) {
            throw new IllegalArgumentException("적립할 시간은 음수일 수 없습니다.");
        }
        this.accumulatedRewardTime += unusedMinutes;
    }
}
