package me.sogom.bridge.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.sogom.bridge.domain.common.BaseEntity;
import me.sogom.bridge.domain.member.entity.Children;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.DayOfWeek;

@Entity
@Getter
@EnableJpaAuditing
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "weekly_time_distributions")
public class WeeklyTimeDistribution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Children Entity import
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Children child;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek; // MONDAY, TUESDAY ... SUNDAY

    @Column(name = "base_minutes", nullable = false)
    private int baseMinutes; // 요일별 기본 세팅 시간 (분 단위)

    private String yearMonth; // 예: "2026-05"
    private int weekNumber;   // 1~5 주차

    @Builder
    public WeeklyTimeDistribution(Children child, DayOfWeek dayOfWeek, String yearMonth, int weekNumber, int baseMinutes) {
        this.child = child;
        this.dayOfWeek = dayOfWeek;
        this.yearMonth = yearMonth;
        this.weekNumber = weekNumber;
        this.baseMinutes = baseMinutes;
    }

    public void updateBaseMinutes(int minutes) {
        this.baseMinutes = minutes;
    }
}