package me.sogom.bridge.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.sogom.bridge.domain.common.BaseEntity;
import me.sogom.bridge.domain.member.entity.Children;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDate;

@Entity
@Getter
@EnableJpaAuditing
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "daily_time_allocations")
public class DailyTimeAllocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Children child;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate; // 연장이 일어나는 구체적인 날짜 (예: 2026-05-16)

    @Column(name = "base_minutes", nullable = false)
    private int baseMinutes; // 그날 요일의 템플릿에서 복사해온 기본 시간

    @Column(name = "extended_minutes", nullable = false)
    private int extendedMinutes; // 자녀가 보상이나 여분으로 추가 연장한 시간

    @Builder
    public DailyTimeAllocation(Children child, LocalDate targetDate, int baseMinutes, int extendedMinutes) {
        this.child = child;
        this.targetDate = targetDate;
        this.baseMinutes = baseMinutes;
        this.extendedMinutes = extendedMinutes;
    }

    // 객체지향 비즈니스 메서드: 오늘의 시간 추가 연장
    public void addExtraTime(int minutes) {
        this.extendedMinutes += minutes;
    }

    // 최종 자녀 폰에 적용될 제한 시간 계산용 메서드
    public int getTotalAvailableTime() {
        return this.baseMinutes + this.extendedMinutes;
    }
}