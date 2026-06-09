package me.sogom.bridge.domain.schedule.service;

import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.notification.entity.NotificationType;
import me.sogom.bridge.domain.notification.service.NotificationService;
import me.sogom.bridge.domain.policy.entity.TimePolicy;
import me.sogom.bridge.domain.policy.repository.TimePolicyRepository;
import me.sogom.bridge.domain.schedule.dto.DailyScheduleResponse;
import me.sogom.bridge.domain.schedule.dto.WeeklyBudgetRequest;
import me.sogom.bridge.domain.schedule.dto.WeeklyTemplateRequest;
import me.sogom.bridge.domain.schedule.entity.DailyTimeAllocation;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    void weekNumberOfCapsMonthEndToFourthWeek() {
        assertThat(scheduleService.weekNumberOf(LocalDate.of(2026, 6, 30))).isEqualTo(4);
    }

    @Test
    void findDailySchedulePreviewUsesFourthWeekTemplateForMonthEnd() {
        LocalDate targetDate = LocalDate.of(2026, 6, 30);
        WeeklyTimeDistribution template = WeeklyTimeDistribution.builder()
                .yearMonth("2026-06")
                .weekNumber(4)
                .dayOfWeek(targetDate.getDayOfWeek())
                .baseMinutes(45)
                .build();

        when(dailyRepository.findByChildIdAndTargetDate(22L, targetDate))
                .thenReturn(Optional.empty());
        when(weeklyRepository.findByChildIdAndDayOfWeekAndYearMonthAndWeekNumber(
                22L, targetDate.getDayOfWeek(), "2026-06", 4
        )).thenReturn(Optional.of(template));

        Optional<DailyScheduleResponse> response = scheduleService.findDailySchedulePreview(22L, targetDate);

        assertThat(response).isPresent();
        assertThat(response.get().getBaseMinutes()).isEqualTo(45);
        assertThat(response.get().getTargetDate()).isEqualTo(targetDate);
    }

    @Test
    void getOrCreateDailyAllocationDoesNotDeductMonthlyBasePolicy() {
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
        TimePolicy policy = TimePolicy.builder()
                .parent(parent)
                .child(child)
                .yearMonth("2026-06")
                .baseTime(600)
                .accumulatedRewardTime(30)
                .build();
        LocalDate targetDate = LocalDate.of(2026, 6, 9);
        WeeklyTimeDistribution template = WeeklyTimeDistribution.builder()
                .child(child)
                .yearMonth("2026-06")
                .weekNumber(2)
                .dayOfWeek(targetDate.getDayOfWeek())
                .baseMinutes(60)
                .build();

        when(dailyRepository.findByChildIdAndTargetDate(22L, targetDate))
                .thenReturn(Optional.empty());
        when(weeklyRepository.findByChildIdAndDayOfWeekAndYearMonthAndWeekNumber(
                22L, targetDate.getDayOfWeek(), "2026-06", 2
        )).thenReturn(Optional.of(template));
        when(timePolicyRepository.findByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(Optional.of(policy));
        when(childrenRepository.findById(22L)).thenReturn(Optional.of(child));
        when(dailyRepository.save(any(DailyTimeAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyTimeAllocation allocation = scheduleService.getOrCreateDailyAllocation(22L, targetDate);

        assertThat(allocation.getBaseMinutes()).isEqualTo(60);
        assertThat(policy.getBaseTime()).isEqualTo(600);
        assertThat(policy.getAccumulatedRewardTime()).isEqualTo(30);
        verify(timePolicyRepository, never()).save(any(TimePolicy.class));
    }

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

    @Test
    void createWeeklyBudgetsRejectsWhenMonthlyBaseTimeExceeded() {
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
        TimePolicy policy = TimePolicy.builder()
                .parent(parent)
                .child(child)
                .yearMonth("2026-06")
                .baseTime(120)
                .accumulatedRewardTime(0)
                .build();

        when(timePolicyRepository.findByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> scheduleService.createWeeklyBudgets(
                22L,
                "2026-06",
                List.of(weeklyBudgetRequest(1, 60), weeklyBudgetRequest(2, 90))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("한 달 총량");

        verify(weeklyBudgetRepository, never()).saveAll(any());
    }

    @Test
    void updateWeeklyTemplateRejectsWhenWeekBudgetExceeded() {
        Children child = Children.builder()
                .id(22L)
                .name("하늘")
                .email("child@test.com")
                .hash("hash")
                .build();
        WeeklyBudget budget = WeeklyBudget.builder()
                .child(child)
                .yearMonth("2026-06")
                .weekNumber(1)
                .allocatedMinutes(100)
                .build();
        WeeklyTimeDistribution existingTemplate = WeeklyTimeDistribution.builder()
                .child(child)
                .yearMonth("2026-06")
                .weekNumber(1)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .baseMinutes(70)
                .build();

        when(weeklyBudgetRepository.findByChildIdAndYearMonthAndWeekNumber(22L, "2026-06", 1))
                .thenReturn(Optional.of(budget));
        when(weeklyRepository.findAllByChildIdAndYearMonthAndWeekNumber(22L, "2026-06", 1))
                .thenReturn(List.of(existingTemplate));

        assertThatThrownBy(() -> scheduleService.updateWeeklyTemplate(
                22L,
                weeklyTemplateRequest("2026-06", 1, DayOfWeek.MONDAY, 40)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("초과");

        verify(weeklyRepository, never()).save(any(WeeklyTimeDistribution.class));
    }

    @Test
    void settleDailyTimeLocksActualUsedWithoutRewardRefund() {
        LocalDate targetDate = LocalDate.of(2026, 6, 9);
        DailyTimeAllocation allocation = DailyTimeAllocation.builder()
                .targetDate(targetDate)
                .baseMinutes(60)
                .extendedMinutes(10)
                .build();

        when(dailyRepository.findByChildIdAndTargetDate(22L, targetDate))
                .thenReturn(Optional.of(allocation));

        DailyTimeAllocation settled = scheduleService.settleDailyTime(22L, targetDate, 30);

        assertThat(settled.getBaseMinutes()).isEqualTo(30);
        assertThat(settled.getExtendedMinutes()).isZero();
        verify(timePolicyRepository, never()).findByChildIdAndYearMonth(anyLong(), anyString());
    }

    private WeeklyBudgetRequest weeklyBudgetRequest(int weekNumber, int allocatedMinutes) {
        WeeklyBudgetRequest request = new WeeklyBudgetRequest();
        ReflectionTestUtils.setField(request, "weekNumber", weekNumber);
        ReflectionTestUtils.setField(request, "allocatedMinutes", allocatedMinutes);
        return request;
    }

    private WeeklyTemplateRequest weeklyTemplateRequest(
            String yearMonth,
            int weekNumber,
            DayOfWeek dayOfWeek,
            int baseMinutes
    ) {
        WeeklyTemplateRequest request = new WeeklyTemplateRequest();
        ReflectionTestUtils.setField(request, "yearMonth", yearMonth);
        ReflectionTestUtils.setField(request, "weekNumber", weekNumber);
        ReflectionTestUtils.setField(request, "dayOfWeek", dayOfWeek);
        ReflectionTestUtils.setField(request, "baseMinutes", baseMinutes);
        return request;
    }
}
