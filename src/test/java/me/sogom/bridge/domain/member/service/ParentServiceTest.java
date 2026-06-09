package me.sogom.bridge.domain.member.service;

import me.sogom.bridge.domain.fcm.service.FcmService;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import me.sogom.bridge.domain.notification.service.NotificationService;
import me.sogom.bridge.domain.policy.entity.TimePolicy;
import me.sogom.bridge.domain.policy.repository.TimePolicyRepository;
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
                .yearMonth("2026-06")
                .baseTime(600)
                .accumulatedRewardTime(30)
                .build();

        when(parentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childrenRepository.findById(2L)).thenReturn(Optional.of(child));
        when(scheduleService.yearMonthOf(LocalDate.of(2026, 6, 9))).thenReturn("2026-06");
        when(timePolicyRepository.findByChildIdAndYearMonth(2L, "2026-06"))
                .thenReturn(Optional.of(policy));
        when(scheduleService.hasChildPlan(2L, "2026-06")).thenReturn(false);

        TimeSummaryResponse response = parentService.getChildTimeSummary(
                1L,
                2L,
                LocalDate.of(2026, 6, 9)
        );

        assertThat(response.isParentPolicyExists()).isTrue();
        assertThat(response.isChildPlanExists()).isFalse();
        assertThat(response.getTodayScheduleStatus()).isEqualTo("waitingChildPlan");
        assertThat(response.getBasePolicyMinutes()).isEqualTo(600);
        assertThat(response.getRewardPoolMinutes()).isEqualTo(30);
    }
}
