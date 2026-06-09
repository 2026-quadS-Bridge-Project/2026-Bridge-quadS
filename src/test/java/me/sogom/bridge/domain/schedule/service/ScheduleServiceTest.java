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
import me.sogom.bridge.domain.schedule.entity.WeeklyRoutine;
import me.sogom.bridge.domain.schedule.entity.WeeklyTimeDistribution;
import me.sogom.bridge.domain.schedule.repository.DailyTimeAllocationRepository;
import me.sogom.bridge.domain.schedule.repository.WeeklyBudgetRepository;
import me.sogom.bridge.domain.schedule.repository.WeeklyRoutineRepository;
import me.sogom.bridge.domain.schedule.repository.WeeklyTimeDistributionRepository;
import me.sogom.bridge.global.apiPayload.code.GeneralErrorCode;
import me.sogom.bridge.global.apiPayload.exception.ProjectException;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    void getOrCreateDailyAllocationUsesFourthWeekTemplateForMonthEnd() {
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
                .accumulatedRewardTime(0)
                .build();
        LocalDate targetDate = LocalDate.of(2026, 6, 30);
        WeeklyTimeDistribution template = WeeklyTimeDistribution.builder()
                .child(child)
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
        when(timePolicyRepository.findByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(Optional.of(policy));
        when(childrenRepository.findById(22L)).thenReturn(Optional.of(child));
        when(dailyRepository.save(any(DailyTimeAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyTimeAllocation allocation = scheduleService.getOrCreateDailyAllocation(22L, targetDate);

        assertThat(allocation.getTargetDate()).isEqualTo(targetDate);
        assertThat(allocation.getBaseMinutes()).isEqualTo(45);
        assertThat(allocation.getExtendedMinutes()).isZero();
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
    void extendDailyTimeConsumesRewardPoolWithoutChangingMonthlyBase() {
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
                .accumulatedRewardTime(45)
                .build();
        LocalDate targetDate = LocalDate.of(2026, 6, 9);
        DailyTimeAllocation allocation = DailyTimeAllocation.builder()
                .child(child)
                .targetDate(targetDate)
                .baseMinutes(60)
                .extendedMinutes(5)
                .build();

        when(timePolicyRepository.findByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(Optional.of(policy));
        when(dailyRepository.findByChildIdAndTargetDate(22L, targetDate))
                .thenReturn(Optional.of(allocation));

        DailyTimeAllocation updated = scheduleService.extendDailyTime(22L, targetDate, 15);

        assertThat(updated.getBaseMinutes()).isEqualTo(60);
        assertThat(updated.getExtendedMinutes()).isEqualTo(20);
        assertThat(policy.getBaseTime()).isEqualTo(600);
        assertThat(policy.getAccumulatedRewardTime()).isEqualTo(30);
    }

    @Test
    void extendDailyTimeRejectsWhenRewardPoolIsInsufficient() {
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
                .accumulatedRewardTime(10)
                .build();
        LocalDate targetDate = LocalDate.of(2026, 6, 9);

        when(timePolicyRepository.findByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> scheduleService.extendDailyTime(22L, targetDate, 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("보상 시간");

        assertThat(policy.getBaseTime()).isEqualTo(600);
        assertThat(policy.getAccumulatedRewardTime()).isEqualTo(10);
        verify(dailyRepository, never()).findByChildIdAndTargetDate(anyLong(), any());
    }

    @Test
    void extendDailyTimeRejectsInvalidRequestValuesBeforeRepositoryLookup() {
        assertThatThrownBy(() -> scheduleService.extendDailyTime(22L, null, 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("날짜");

        assertThatThrownBy(() -> scheduleService.extendDailyTime(
                22L,
                LocalDate.of(2026, 6, 9),
                0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1분");

        verify(timePolicyRepository, never()).findByChildIdAndYearMonth(anyLong(), anyString());
        verify(dailyRepository, never()).findByChildIdAndTargetDate(anyLong(), any());
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
                .thenReturn(List.of(
                        weeklyBudget(child, 1, 60),
                        weeklyBudget(child, 2, 60),
                        weeklyBudget(child, 3, 60),
                        weeklyBudget(child, 4, 60)
                ));
        when(weeklyRepository.findAllByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(List.of(
                        weeklyTemplate(child, 1, DayOfWeek.MONDAY, 60),
                        weeklyTemplate(child, 2, DayOfWeek.MONDAY, 60),
                        weeklyTemplate(child, 3, DayOfWeek.MONDAY, 60),
                        weeklyTemplate(child, 4, DayOfWeek.MONDAY, 60)
                ));
        when(timePolicyRepository.findByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(Optional.of(TimePolicy.builder()
                        .parent(parent)
                        .child(child)
                        .yearMonth("2026-06")
                        .baseTime(240)
                        .accumulatedRewardTime(0)
                        .build()));
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
    void completeTimePlanRequiresBudgetTotalToMatchParentPolicy() {
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
                .thenReturn(List.of(
                        weeklyBudget(child, 1, 60),
                        weeklyBudget(child, 2, 60),
                        weeklyBudget(child, 3, 60),
                        weeklyBudget(child, 4, 60)
                ));
        when(weeklyRepository.findAllByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(List.of(
                        weeklyTemplate(child, 1, DayOfWeek.MONDAY, 60),
                        weeklyTemplate(child, 2, DayOfWeek.MONDAY, 60),
                        weeklyTemplate(child, 3, DayOfWeek.MONDAY, 60),
                        weeklyTemplate(child, 4, DayOfWeek.MONDAY, 60)
                ));
        when(timePolicyRepository.findByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(Optional.of(TimePolicy.builder()
                        .parent(parent)
                        .child(child)
                        .yearMonth("2026-06")
                        .baseTime(300)
                        .accumulatedRewardTime(0)
                        .build()));

        assertThatThrownBy(() -> scheduleService.completeTimePlan(22L, "2026-06"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("시간 계획");

        verify(childrenRepository, never()).findById(22L);
        verify(notificationService, never()).createNotification(
                anyLong(),
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
    void completeTimePlanRequiresWeeklyTemplateTotalsToMatchBudgets() {
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
                .thenReturn(List.of(
                        weeklyBudget(child, 1, 60),
                        weeklyBudget(child, 2, 60),
                        weeklyBudget(child, 3, 60),
                        weeklyBudget(child, 4, 60)
                ));
        when(weeklyRepository.findAllByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(List.of(
                        weeklyTemplate(child, 1, DayOfWeek.MONDAY, 60),
                        weeklyTemplate(child, 2, DayOfWeek.MONDAY, 60),
                        weeklyTemplate(child, 3, DayOfWeek.MONDAY, 60),
                        weeklyTemplate(child, 4, DayOfWeek.MONDAY, 30)
                ));
        assertThatThrownBy(() -> scheduleService.completeTimePlan(22L, "2026-06"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("시간 계획");

        verify(childrenRepository, never()).findById(22L);
        verify(notificationService, never()).createNotification(
                anyLong(),
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
    void completeTimePlanRequiresAllFourBudgetAndTemplateWeeks() {
        Children child = Children.builder()
                .id(22L)
                .name("하늘")
                .email("child@test.com")
                .hash("hash")
                .build();

        when(weeklyBudgetRepository.findAllByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(List.of(
                        weeklyBudget(child, 1, 60),
                        weeklyBudget(child, 2, 60),
                        weeklyBudget(child, 3, 60),
                        weeklyBudget(child, 4, 60)
                ));
        when(weeklyRepository.findAllByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(List.of(
                        weeklyTemplate(child, 1, DayOfWeek.MONDAY, 60),
                        weeklyTemplate(child, 2, DayOfWeek.MONDAY, 60),
                        weeklyTemplate(child, 3, DayOfWeek.MONDAY, 60)
                ));

        assertThatThrownBy(() -> scheduleService.completeTimePlan(22L, "2026-06"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("시간 계획");

        verify(notificationService, never()).createNotification(
                anyLong(),
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
                List.of(
                        weeklyBudgetRequest(1, 30),
                        weeklyBudgetRequest(2, 30),
                        weeklyBudgetRequest(3, 30),
                        weeklyBudgetRequest(4, 31)
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("한 달 총량");

        verify(weeklyBudgetRepository, never()).saveAll(any());
    }

    @Test
    void createWeeklyBudgetsRejectsWhenMonthlyBaseTimeUnderfilled() {
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
                List.of(
                        weeklyBudgetRequest(1, 30),
                        weeklyBudgetRequest(2, 30),
                        weeklyBudgetRequest(3, 30),
                        weeklyBudgetRequest(4, 20)
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("한 달 총량");

        verify(weeklyBudgetRepository, never()).saveAll(any());
    }

    @Test
    void createWeeklyBudgetsRequiresAllFourPositiveWeeks() {
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
                List.of(
                        weeklyBudgetRequest(1, 30),
                        weeklyBudgetRequest(2, 30),
                        weeklyBudgetRequest(3, 30)
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1~4주차");

        assertThatThrownBy(() -> scheduleService.createWeeklyBudgets(
                22L,
                "2026-06",
                List.of(
                        weeklyBudgetRequest(1, 30),
                        weeklyBudgetRequest(2, 30),
                        weeklyBudgetRequest(3, 30),
                        weeklyBudgetRequest(4, 0)
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0분보다 커야");

        assertThatThrownBy(() -> scheduleService.createWeeklyBudgets(
                22L,
                "2026-06",
                Arrays.asList(
                        weeklyBudgetRequest(1, 30),
                        weeklyBudgetRequest(2, 30),
                        weeklyBudgetRequest(3, 30),
                        null
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1~4주차");

        verify(weeklyBudgetRepository, never()).saveAll(any());
    }

    @Test
    void createWeeklyBudgetsClearsExistingBudgetsAndTemplatesBeforeSaving() {
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
                .baseTime(240)
                .accumulatedRewardTime(0)
                .build();
        List<WeeklyBudget> oldBudgets = List.of(weeklyBudget(child, 1, 60));
        List<WeeklyTimeDistribution> oldTemplates = List.of(
                weeklyTemplate(child, 1, DayOfWeek.MONDAY, 60)
        );

        when(timePolicyRepository.findByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(Optional.of(policy));
        when(weeklyBudgetRepository.findAllByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(oldBudgets);
        when(weeklyRepository.findAllByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(oldTemplates);
        when(childrenRepository.findById(22L)).thenReturn(Optional.of(child));

        scheduleService.createWeeklyBudgets(
                22L,
                "2026-06",
                List.of(
                        weeklyBudgetRequest(1, 60),
                        weeklyBudgetRequest(2, 60),
                        weeklyBudgetRequest(3, 60),
                        weeklyBudgetRequest(4, 60)
                )
        );

        verify(weeklyBudgetRepository).deleteAll(oldBudgets);
        verify(weeklyRepository).deleteAll(oldTemplates);
        verify(weeklyBudgetRepository).saveAll(any());
    }

    @Test
    void clearChildPlanDeletesMonthlyBudgetsTemplatesAndDailyAllocations() {
        Children child = Children.builder()
                .id(22L)
                .name("하늘")
                .email("child@test.com")
                .hash("hash")
                .build();
        List<WeeklyBudget> oldBudgets = List.of(weeklyBudget(child, 1, 60));
        List<WeeklyTimeDistribution> oldTemplates = List.of(
                weeklyTemplate(child, 1, DayOfWeek.MONDAY, 60)
        );
        List<DailyTimeAllocation> oldAllocations = List.of(
                DailyTimeAllocation.builder()
                        .child(child)
                        .targetDate(LocalDate.of(2026, 6, 9))
                        .baseMinutes(60)
                        .extendedMinutes(15)
                        .build()
        );

        when(weeklyBudgetRepository.findAllByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(oldBudgets);
        when(weeklyRepository.findAllByChildIdAndYearMonth(22L, "2026-06"))
                .thenReturn(oldTemplates);
        when(dailyRepository.findAllByChildIdAndTargetDateBetween(
                22L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        )).thenReturn(oldAllocations);

        scheduleService.clearChildPlan(22L, "2026-06");

        verify(weeklyBudgetRepository).deleteAll(oldBudgets);
        verify(weeklyRepository).deleteAll(oldTemplates);
        verify(dailyRepository).deleteAll(oldAllocations);
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
    void updateWeeklyTemplateRejectsInvalidTemplateRequestValues() {
        assertThatThrownBy(() -> scheduleService.updateWeeklyTemplate(
                22L,
                weeklyTemplateRequest("2026-06", 5, DayOfWeek.MONDAY, 40)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1~4주차");

        assertThatThrownBy(() -> scheduleService.updateWeeklyTemplate(
                22L,
                weeklyTemplateRequest("2026-06", 1, DayOfWeek.MONDAY, -1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("음수");

        assertThatThrownBy(() -> scheduleService.updateWeeklyTemplate(
                22L,
                weeklyTemplateRequest("", 1, DayOfWeek.MONDAY, 40)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("올바르지");

        verify(weeklyBudgetRepository, never())
                .findByChildIdAndYearMonthAndWeekNumber(anyLong(), anyString(), anyInt());
        verify(weeklyRepository, never()).save(any(WeeklyTimeDistribution.class));
    }

    @Test
    void deleteRoutineAllowsOwningChildRoutine() {
        Children child = Children.builder()
                .id(22L)
                .name("하늘")
                .email("child@test.com")
                .hash("hash")
                .build();
        WeeklyRoutine routine = weeklyRoutine(child);

        when(routineRepository.findById(700L)).thenReturn(Optional.of(routine));

        scheduleService.deleteRoutine(22L, 700L);

        verify(routineRepository).delete(routine);
    }

    @Test
    void deleteRoutineRejectsOtherChildRoutine() {
        Children otherChild = Children.builder()
                .id(33L)
                .name("바다")
                .email("other-child@test.com")
                .hash("hash")
                .build();
        WeeklyRoutine routine = weeklyRoutine(otherChild);

        when(routineRepository.findById(700L)).thenReturn(Optional.of(routine));

        assertThatThrownBy(() -> scheduleService.deleteRoutine(22L, 700L))
                .isInstanceOf(ProjectException.class)
                .satisfies(exception ->
                        assertThat(((ProjectException) exception).getErrorCode())
                                .isEqualTo(GeneralErrorCode.FORBIDDEN));

        verify(routineRepository, never()).delete(any(WeeklyRoutine.class));
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

    @Test
    void settleDailyTimeRejectsActualUsedAboveAllocatedTime() {
        LocalDate targetDate = LocalDate.of(2026, 6, 9);
        DailyTimeAllocation allocation = DailyTimeAllocation.builder()
                .targetDate(targetDate)
                .baseMinutes(60)
                .extendedMinutes(10)
                .build();

        when(dailyRepository.findByChildIdAndTargetDate(22L, targetDate))
                .thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> scheduleService.settleDailyTime(22L, targetDate, 71))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("초과");

        assertThat(allocation.getBaseMinutes()).isEqualTo(60);
        assertThat(allocation.getExtendedMinutes()).isEqualTo(10);
    }

    @Test
    void settleDailyTimeRejectsNegativeActualUsed() {
        LocalDate targetDate = LocalDate.of(2026, 6, 9);

        assertThatThrownBy(() -> scheduleService.settleDailyTime(22L, targetDate, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("음수");

        verify(dailyRepository, never()).findByChildIdAndTargetDate(anyLong(), any());
    }

    private WeeklyBudgetRequest weeklyBudgetRequest(int weekNumber, int allocatedMinutes) {
        WeeklyBudgetRequest request = new WeeklyBudgetRequest();
        ReflectionTestUtils.setField(request, "weekNumber", weekNumber);
        ReflectionTestUtils.setField(request, "allocatedMinutes", allocatedMinutes);
        return request;
    }

    private WeeklyBudget weeklyBudget(Children child, int weekNumber, int allocatedMinutes) {
        return WeeklyBudget.builder()
                .child(child)
                .yearMonth("2026-06")
                .weekNumber(weekNumber)
                .allocatedMinutes(allocatedMinutes)
                .build();
    }

    private WeeklyTimeDistribution weeklyTemplate(
            Children child,
            int weekNumber,
            DayOfWeek dayOfWeek,
            int baseMinutes
    ) {
        return WeeklyTimeDistribution.builder()
                .child(child)
                .yearMonth("2026-06")
                .weekNumber(weekNumber)
                .dayOfWeek(dayOfWeek)
                .baseMinutes(baseMinutes)
                .build();
    }

    private WeeklyRoutine weeklyRoutine(Children child) {
        return WeeklyRoutine.builder()
                .child(child)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .title("고정 시간")
                .build();
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
