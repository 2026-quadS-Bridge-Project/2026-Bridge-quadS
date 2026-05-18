package me.sogom.bridge.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.sogom.bridge.domain.common.BaseEntity;
import me.sogom.bridge.domain.member.entity.Children;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "weekly_routines")
public class WeeklyRoutine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weekly_routine_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "children_id", nullable = false)
    private Children child;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek; // MONDAY, TUESDAY ...

    @Column(nullable = false)
    private LocalTime startTime; // 예: 16:00

    @Column(nullable = false)
    private LocalTime endTime; // 예: 18:00

    @Column(nullable = false)
    private String title; // 예: "영어학원"

    @Builder
    public WeeklyRoutine(Children child, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, String title) {
        this.child = child;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
    }

    public void updateRoutine(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, String title) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
    }
}