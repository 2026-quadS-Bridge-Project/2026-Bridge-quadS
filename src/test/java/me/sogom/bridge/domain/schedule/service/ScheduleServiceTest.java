package me.sogom.bridge.domain.schedule.service;

import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.notification.entity.NotificationType;
import me.sogom.bridge.domain.notification.service.NotificationService;
import me.sogom.bridge.domain.policy.repository.TimePolicyRepository;
import me.sogom.bridge.domain.schedule.entity.WeeklyBudget;
import me.sogom.bridge.domain.schedule.entity.WeeklyTimeDistribution;
import me.sogom.bridge.domain.schedule.repository.DailyTimeAllocationRepository;
import me.sogom.bridge.domain.schedule.repository.WeeklyBudgetRepository;
import me.sogom.bridge.domain.schedule.repository.WeeklyRoutineRepository;
import me.sogom.bridge.domain.schedule.repository.WeeklyTimeDistributionRepository;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private WeeklyTimeDistributionRepository weeklyRepository;
    @Mock
    private DailyTimeAllocationRepository dailyRepository;
    @Mock
    private ChildrenRepository childrenRepository;
    @Mock
    private TimePolicyRepository timePolicyRepository;
    @Mock
    private WeeklyRoutineRepository routineRepository;
    @Mock
    private WeeklyBudgetRepository weeklyBudgetRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void completeTimePlanNotifiesParent() {
        Parent parent = Parent.builder()
                .id(11L)
                .name("parent")
                .email("parent@test.com")
                .hash("hash")
                .build();
        Children child = Children.builder()
                .id(22L)
                .name("하늘")
                .email("child@test.com")
                .hash("hash")
                .parent(parent)
                .build();

        when(weeklyBudgetRepository.findAllByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(List.of(WeeklyBudget.builder().child(child).yearMonth("2026-06").weekNumber(1).allocatedMinutes(60).build()));
        when(weeklyRepository.findAllByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(List.of(WeeklyTimeDistribution.builder().child(child).yearMonth("2026-06").weekNumber(1).dayOfWeek(DayOfWeek.MONDAY).baseMinutes(60).build()));
        when(childrenRepository.findById(22L)).thenReturn(Optional.of(child));

        scheduleService.completeTimePlan(22L, "2026-06");

        verify(notificationService).createNotification(
                eq(11L),
                eq(MemberRole.PARENT),
                eq("시간 계획 완료"),
                contains("하늘"),
                eq(NotificationType.GENERAL),
                eq(22L),
                isNull(),
                isNull(),
                eq("/today-time?childrenId=22")
        );
    }

    @Test
    void completeTimePlanRequiresSubmittedPlan() {
        when(weeklyBudgetRepository.findAllByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> scheduleService.completeTimePlan(22L, "2026-06"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("시간 계획");

        verify(notificationService, never()).createNotification(
                eq(11L),
                eq(MemberRole.PARENT),
                eq("시간 계획 완료"),
                contains("하늘"),
                eq(NotificationType.GENERAL),
                eq(22L),
                isNull(),
                isNull(),
                eq("/today-time?childrenId=22")
        );
    }
}
