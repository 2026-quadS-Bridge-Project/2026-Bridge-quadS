package me.sogom.bridge.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.sogom.bridge.domain.member.entity.Children;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "weekly_budgets")
public class WeeklyBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    private Children child;

    private String yearMonth; // 예: "2026-05"
    private int weekNumber;   // 1, 2, 3, 4 주차
    private int allocatedMinutes; // 할당된 시간

    @Builder
    public WeeklyBudget(Children child, String yearMonth, int weekNumber, int allocatedMinutes) {
        this.child = child;
        this.yearMonth = yearMonth;
        this.weekNumber = weekNumber;
        this.allocatedMinutes = allocatedMinutes;
    }
}