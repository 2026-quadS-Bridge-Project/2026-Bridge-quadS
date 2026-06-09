package me.sogom.bridge.domain.member.service;

import me.sogom.bridge.domain.fcm.service.FcmService;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import me.sogom.bridge.domain.notification.service.NotificationService;
import me.sogom.bridge.domain.policy.entity.TimePolicy;
import me.sogom.bridge.domain.policy.repository.TimePolicyRepository;
import me.sogom.bridge.domain.schedule.dto.DailyScheduleResponse;
import me.sogom.bridge.domain.schedule.dto.TimeSummaryResponse;
import me.sogom.bridge.domain.schedule.service.ScheduleService;
import me.sogom.bridge.global.storage.PhotoUrlResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 9);
    private static final String YEAR_MONTH = "2026-06";

    @Mock
    private ParentRepository parentRepository;
    @Mock
    private ChildrenRepository childrenRepository;
    @Mock
    private TimePolicyRepository timePolicyRepository;
    @Mock
    private ScheduleService scheduleService;
    @Mock
    private FcmService fcmService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PhotoUrlResolver photoUrlResolver;

    @InjectMocks
    private ParentService parentService;

    @Test
    void childTimeSummaryIncludesParentBasePolicyMinutesBeforeChildPlan() {
        TimePolicy policy = stubParentChildAndPolicy();
        when(scheduleService.hasChildPlan(2L, YEAR_MONTH)).thenReturn(false);

        TimeSummaryResponse response = parentService.getChildTimeSummary(
                1L,
                2L,
                TODAY
        );

        assertThat(response.isParentPolicyExists()).isTrue();
        assertThat(response.isChildPlanExists()).isFalse();
        assertThat(response.getTodayScheduleStatus()).isEqualTo("waitingChildPlan");
        assertThat(response.getYearMonth()).isEqualTo(YEAR_MONTH);
        assertThat(response.getBasePolicyMinutes()).isEqualTo(policy.getBaseTime());
        assertThat(response.getRewardPoolMinutes()).isEqualTo(policy.getAccumulatedRewardTime());
        assertThat(response.getTodaySchedule()).isNull();
    }

    @Test
    void childTimeSummaryIncludesTodayScheduleWhenChildPlanIsAvailable() {
        TimePolicy policy = stubParentChildAndPolicy();
        DailyScheduleResponse dailySchedule = DailyScheduleResponse.preview(TODAY, 90, 15);
        when(scheduleService.hasChildPlan(2L, YEAR_MONTH)).thenReturn(true);
        when(scheduleService.findDailySchedulePreview(2L, TODAY)).thenReturn(Optional.of(dailySchedule));

        TimeSummaryResponse response = parentService.getChildTimeSummary(1L, 2L, TODAY);

        assertThat(response.isParentPolicyExists()).isTrue();
        assertThat(response.isChildPlanExists()).isTrue();
        assertThat(response.getTodayScheduleStatus()).isEqualTo("available");
        assertThat(response.getYearMonth()).isEqualTo(YEAR_MONTH);
        assertThat(response.getBasePolicyMinutes()).isEqualTo(policy.getBaseTime());
        assertThat(response.getRewardPoolMinutes()).isEqualTo(policy.getAccumulatedRewardTime());
        assertThat(response.getTodaySchedule()).isSameAs(dailySchedule);
        assertThat(response.getTodaySchedule().getBaseMinutes()).isEqualTo(90);
        assertThat(response.getTodaySchedule().getExtendedMinutes()).isEqualTo(15);
        assertThat(response.getTodaySchedule().getTotalAvailableMinutes()).isEqualTo(105);
    }

    @Test
    void childTimeSummaryMarksTemplateMissingWhenChildPlanExistsButTodayHasNoSchedule() {
        TimePolicy policy = stubParentChildAndPolicy();
        when(scheduleService.hasChildPlan(2L, YEAR_MONTH)).thenReturn(true);
        when(scheduleService.findDailySchedulePreview(2L, TODAY)).thenReturn(Optional.empty());

        TimeSummaryResponse response = parentService.getChildTimeSummary(1L, 2L, TODAY);

        assertThat(response.isParentPolicyExists()).isTrue();
        assertThat(response.isChildPlanExists()).isTrue();
        assertThat(response.getTodayScheduleStatus()).isEqualTo("templateMissing");
        assertThat(response.getYearMonth()).isEqualTo(YEAR_MONTH);
        assertThat(response.getBasePolicyMinutes()).isEqualTo(policy.getBaseTime());
        assertThat(response.getRewardPoolMinutes()).isEqualTo(policy.getAccumulatedRewardTime());
        assertThat(response.getTodaySchedule()).isNull();
    }

    private TimePolicy stubParentChildAndPolicy() {
        Parent parent = Parent.builder()
                .id(1L)
                .name("parent")
                .email("parent@test.com")
                .hash("hash")
                .build();
        Children child = Children.builder()
                .id(2L)
                .name("child")
                .email("child@test.com")
                .hash("hash")
                .parent(parent)
                .build();
        TimePolicy policy = TimePolicy.builder()
                .parent(parent)
                .child(child)
                .yearMonth(YEAR_MONTH)
                .baseTime(600)
                .accumulatedRewardTime(30)
                .build();

        when(parentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childrenRepository.findById(2L)).thenReturn(Optional.of(child));
        when(scheduleService.yearMonthOf(TODAY)).thenReturn(YEAR_MONTH);
        when(timePolicyRepository.findByChildIdAndYearMonth(2L, YEAR_MONTH))
                .thenReturn(Optional.of(policy));

        return policy;
    }
}
