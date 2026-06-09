package me.sogom.bridge.domain.member.service;

import me.sogom.bridge.domain.fcm.service.FcmService;
import me.sogom.bridge.domain.member.MemberException;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import me.sogom.bridge.domain.notification.entity.NotificationType;
import me.sogom.bridge.domain.notification.service.NotificationService;
import me.sogom.bridge.domain.policy.dto.PolicyReqDTO;
import me.sogom.bridge.domain.policy.entity.TimePolicy;
import me.sogom.bridge.domain.policy.repository.TimePolicyRepository;
import me.sogom.bridge.domain.schedule.dto.DailyScheduleResponse;
import me.sogom.bridge.domain.schedule.dto.TimeSummaryResponse;
import me.sogom.bridge.domain.schedule.service.ScheduleService;
import me.sogom.bridge.global.security.entity.MemberRole;
import me.sogom.bridge.global.storage.PhotoUrlResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void setTimePolicyCreatesMonthlyPolicyAndNotifiesChild() {
        Parent parent = stubParentChild();
        Children child = parent.getChildren().get(0);
        when(timePolicyRepository.findByChildIdAndYearMonth(2L, YEAR_MONTH))
                .thenReturn(Optional.empty());
        ArgumentCaptor<TimePolicy> policyCaptor = ArgumentCaptor.forClass(TimePolicy.class);

        parentService.setTimePolicy(
                1L,
                new PolicyReqDTO.SetTimePolicyRequest(2L, YEAR_MONTH, 600)
        );

        verify(timePolicyRepository).save(policyCaptor.capture());
        TimePolicy savedPolicy = policyCaptor.getValue();
        assertThat(savedPolicy.getParent()).isSameAs(parent);
        assertThat(savedPolicy.getChild()).isSameAs(child);
        assertThat(savedPolicy.getYearMonth()).isEqualTo(YEAR_MONTH);
        assertThat(savedPolicy.getBaseTime()).isEqualTo(600);
        assertThat(savedPolicy.getAccumulatedRewardTime()).isZero();
        verify(scheduleService, never()).clearChildPlan(any(), any());
        verify(fcmService).sendSilentPush(2L, MemberRole.CHILDREN, "TIME_POLICY_UPDATED");
        verify(notificationService).createNotification(
                eq(2L),
                eq(MemberRole.CHILDREN),
                eq("시간 설정 완료"),
                contains("이번 달 사용 시간을 설정"),
                eq(NotificationType.GENERAL),
                eq(2L),
                isNull(),
                isNull(),
                eq("/child-home/time-setup")
        );
    }

    @Test
    void setTimePolicyUpdatesExistingMonthlyPolicy() {
        Parent parent = stubParentChild();
        Children child = parent.getChildren().get(0);
        TimePolicy existingPolicy = TimePolicy.builder()
                .parent(parent)
                .child(child)
                .yearMonth(YEAR_MONTH)
                .baseTime(300)
                .accumulatedRewardTime(45)
                .build();
        when(timePolicyRepository.findByChildIdAndYearMonth(2L, YEAR_MONTH))
                .thenReturn(Optional.of(existingPolicy));

        parentService.setTimePolicy(
                1L,
                new PolicyReqDTO.SetTimePolicyRequest(2L, YEAR_MONTH, 600)
        );

        assertThat(existingPolicy.getBaseTime()).isEqualTo(600);
        assertThat(existingPolicy.getAccumulatedRewardTime()).isEqualTo(45);
        verify(timePolicyRepository, never()).save(any(TimePolicy.class));
        verify(scheduleService).clearChildPlan(2L, YEAR_MONTH);
        verify(fcmService).sendSilentPush(2L, MemberRole.CHILDREN, "TIME_POLICY_UPDATED");
    }

    @Test
    void setTimePolicyKeepsChildPlanWhenExistingBaseTimeIsUnchanged() {
        Parent parent = stubParentChild();
        Children child = parent.getChildren().get(0);
        TimePolicy existingPolicy = TimePolicy.builder()
                .parent(parent)
                .child(child)
                .yearMonth(YEAR_MONTH)
                .baseTime(600)
                .accumulatedRewardTime(45)
                .build();
        when(timePolicyRepository.findByChildIdAndYearMonth(2L, YEAR_MONTH))
                .thenReturn(Optional.of(existingPolicy));

        parentService.setTimePolicy(
                1L,
                new PolicyReqDTO.SetTimePolicyRequest(2L, YEAR_MONTH, 600)
        );

        assertThat(existingPolicy.getBaseTime()).isEqualTo(600);
        assertThat(existingPolicy.getAccumulatedRewardTime()).isEqualTo(45);
        verify(timePolicyRepository, never()).save(any(TimePolicy.class));
        verify(scheduleService, never()).clearChildPlan(any(), any());
        verify(fcmService).sendSilentPush(2L, MemberRole.CHILDREN, "TIME_POLICY_UPDATED");
    }

    @Test
    void setTimePolicyRejectsChildFromAnotherParent() {
        Parent parent = Parent.builder()
                .id(1L)
                .name("parent")
                .email("parent@test.com")
                .hash("hash")
                .build();
        Parent otherParent = Parent.builder()
                .id(99L)
                .name("other")
                .email("other@test.com")
                .hash("hash")
                .build();
        Children child = Children.builder()
                .id(2L)
                .name("child")
                .email("child@test.com")
                .hash("hash")
                .parent(otherParent)
                .build();
        when(parentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childrenRepository.findById(2L)).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> parentService.setTimePolicy(
                1L,
                new PolicyReqDTO.SetTimePolicyRequest(2L, YEAR_MONTH, 600)
        ))
                .isInstanceOf(MemberException.class);

        verify(timePolicyRepository, never()).findByChildIdAndYearMonth(any(), any());
        verify(timePolicyRepository, never()).save(any(TimePolicy.class));
        verify(fcmService, never()).sendSilentPush(any(), any(), any());
    }

    @Test
    void childTimeSummaryMarksNoParentPolicyWhenMonthlyPolicyIsMissing() {
        stubParentChild();
        when(scheduleService.yearMonthOf(TODAY)).thenReturn(YEAR_MONTH);
        when(timePolicyRepository.findByChildIdAndYearMonth(2L, YEAR_MONTH))
                .thenReturn(Optional.empty());

        TimeSummaryResponse response = parentService.getChildTimeSummary(1L, 2L, TODAY);

        assertThat(response.isParentPolicyExists()).isFalse();
        assertThat(response.isChildPlanExists()).isFalse();
        assertThat(response.getTodayScheduleStatus()).isEqualTo("noParentPolicy");
        assertThat(response.getYearMonth()).isEqualTo(YEAR_MONTH);
        assertThat(response.getBasePolicyMinutes()).isZero();
        assertThat(response.getRewardPoolMinutes()).isZero();
        assertThat(response.getTodaySchedule()).isNull();
    }

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
        Parent parent = stubParentChild();
        Children child = parent.getChildren().get(0);
        TimePolicy policy = TimePolicy.builder()
                .parent(parent)
                .child(child)
                .yearMonth(YEAR_MONTH)
                .baseTime(600)
                .accumulatedRewardTime(30)
                .build();

        when(scheduleService.yearMonthOf(TODAY)).thenReturn(YEAR_MONTH);
        when(timePolicyRepository.findByChildIdAndYearMonth(2L, YEAR_MONTH))
                .thenReturn(Optional.of(policy));

        return policy;
    }

    private Parent stubParentChild() {
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
        parent.getChildren().add(child);

        when(parentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childrenRepository.findById(2L)).thenReturn(Optional.of(child));

        return parent;
    }
}
