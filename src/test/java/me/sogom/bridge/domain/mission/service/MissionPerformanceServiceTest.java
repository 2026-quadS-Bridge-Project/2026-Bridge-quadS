package me.sogom.bridge.domain.mission.service;

import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.mission.dto.AiVerificationResponse;
import me.sogom.bridge.domain.mission.entity.Mission;
import me.sogom.bridge.domain.mission.entity.MissionCategory;
import me.sogom.bridge.domain.mission.entity.MissionPerformance;
import me.sogom.bridge.domain.mission.entity.MissionSetting;
import me.sogom.bridge.domain.mission.entity.MissionStatus;
import me.sogom.bridge.domain.mission.entity.ResetCycle;
import me.sogom.bridge.domain.mission.entity.VerificationType;
import me.sogom.bridge.domain.mission.exception.MissionErrorCode;
import me.sogom.bridge.domain.mission.exception.MissionException;
import me.sogom.bridge.domain.mission.repository.MissionPerformanceRepository;
import me.sogom.bridge.domain.mission.repository.MissionRepository;
import me.sogom.bridge.domain.mission.repository.MissionSettingRepository;
import me.sogom.bridge.domain.notification.entity.NotificationType;
import me.sogom.bridge.domain.notification.service.NotificationService;
import me.sogom.bridge.domain.policy.entity.TimePolicy;
import me.sogom.bridge.domain.policy.repository.TimePolicyRepository;
import me.sogom.bridge.global.security.entity.MemberRole;
import me.sogom.bridge.global.storage.PhotoUrlResolver;
import me.sogom.bridge.global.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionPerformanceServiceTest {

    @Mock
    private MissionPerformanceRepository performanceRepository;
    @Mock
    private MissionRepository missionRepository;
    @Mock
    private ChildrenRepository childrenRepository;
    @Mock
    private MissionVerificationAiService aiService;
    @Mock
    private MissionSettingRepository missionSettingRepository;
    @Mock
    private TimePolicyRepository timePolicyRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private StorageService storageService;
    @Mock
    private PhotoUrlResolver photoUrlResolver;

    @InjectMocks
    private MissionPerformanceService missionPerformanceService;

    @Test
    void verifyAndSaveMissionReturnsPendingStatusForParentVerification() throws Exception {
        Parent parent = parent();
        Children child = child(parent);
        Mission mission = mission(parent, child);
        MissionSetting setting = setting(mission, VerificationType.PARENT, 30);
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "proof.jpg",
                "image/jpeg",
                new byte[]{1}
        );

        when(missionRepository.findById(100L)).thenReturn(Optional.of(mission));
        when(childrenRepository.findById(22L)).thenReturn(Optional.of(child));
        when(performanceRepository.findTopByMissionIdOrderByIdDesc(100L))
                .thenReturn(Optional.empty());
        when(storageService.upload(any(), any())).thenReturn("mission/proof.jpg");
        when(missionSettingRepository.findByMissionId(100L)).thenReturn(Optional.of(setting));
        when(performanceRepository.save(any(MissionPerformance.class))).thenAnswer(invocation -> {
            MissionPerformance saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 201L);
            return saved;
        });

        AiVerificationResponse response = missionPerformanceService.verifyAndSaveMission(
                100L,
                22L,
                image
        );

        assertThat(response.isAccepted()).isFalse();
        assertThat(response.status()).isEqualTo(MissionStatus.PENDING);
        assertThat(response.performanceId()).isEqualTo(201L);
        verifyNoInteractions(aiService, timePolicyRepository);
        verify(notificationService).createNotification(
                eq(11L),
                eq(MemberRole.PARENT),
                eq("미션 확인 요청"),
                eq("하늘님이 미션 확인을 요청했습니다."),
                eq(NotificationType.MISSION_REQUESTED),
                eq(22L),
                eq(100L),
                eq(201L),
                eq("/today-mission?childrenId=22")
        );
    }

    @Test
    void verifyAndSaveMissionRewardsImmediatelyForChildVerification() throws Exception {
        Parent parent = parent();
        Children child = child(parent);
        Mission mission = mission(parent, child);
        MissionSetting setting = setting(mission, VerificationType.CHILD, 30);
        TimePolicy timePolicy = timePolicy(parent, child, 600, 5);
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "proof.jpg",
                "image/jpeg",
                new byte[]{1}
        );

        when(missionRepository.findById(100L)).thenReturn(Optional.of(mission));
        when(childrenRepository.findById(22L)).thenReturn(Optional.of(child));
        when(performanceRepository.findTopByMissionIdOrderByIdDesc(100L))
                .thenReturn(Optional.empty());
        when(storageService.upload(any(), any())).thenReturn("mission/proof.jpg");
        when(missionSettingRepository.findByMissionId(100L)).thenReturn(Optional.of(setting));
        when(timePolicyRepository.findByChildIdAndYearMonth(22L, YearMonth.now().toString()))
                .thenReturn(Optional.of(timePolicy));
        when(performanceRepository.save(any(MissionPerformance.class))).thenAnswer(invocation -> {
            MissionPerformance saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 201L);
            return saved;
        });

        AiVerificationResponse response = missionPerformanceService.verifyAndSaveMission(
                100L,
                22L,
                image
        );

        assertThat(response.isAccepted()).isTrue();
        assertThat(response.status()).isEqualTo(MissionStatus.ACCEPTED);
        assertThat(response.performanceId()).isEqualTo(201L);
        assertThat(timePolicy.getAccumulatedRewardTime()).isEqualTo(35);
        verifyNoInteractions(aiService);
        verify(timePolicyRepository).save(timePolicy);
        verify(notificationService).createNotification(
                eq(11L),
                eq(MemberRole.PARENT),
                eq("미션 완료"),
                eq("하늘님이 미션을 완료했습니다."),
                eq(NotificationType.MISSION_APPROVED),
                eq(22L),
                eq(100L),
                eq(201L),
                eq("/today-mission?childrenId=22")
        );
    }

    @Test
    void verifyAndSaveMissionRejectsAlreadyAcceptedMission() {
        Parent parent = parent();
        Children child = child(parent);
        Mission mission = mission(parent, child);
        MissionPerformance accepted = performance(mission, child, MissionStatus.ACCEPTED);
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "proof.jpg",
                "image/jpeg",
                new byte[]{1}
        );

        when(missionRepository.findById(100L)).thenReturn(Optional.of(mission));
        when(childrenRepository.findById(22L)).thenReturn(Optional.of(child));
        when(performanceRepository.findTopByMissionIdOrderByIdDesc(100L))
                .thenReturn(Optional.of(accepted));

        assertThatExceptionOfType(MissionException.class)
                .isThrownBy(() -> missionPerformanceService.verifyAndSaveMission(100L, 22L, image))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MissionErrorCode.MISSION_ALREADY_COMPLETED));
        verifyNoInteractions(storageService, missionSettingRepository, timePolicyRepository, notificationService);
        verify(performanceRepository, never()).save(any());
    }

    @Test
    void approveMissionRewardsOnlyParentVerificationPerformance() {
        Parent parent = parent();
        Children child = child(parent);
        Mission mission = mission(parent, child);
        MissionPerformance performance = performance(mission, child, MissionStatus.PENDING);
        MissionSetting setting = setting(mission, VerificationType.PARENT, 30);
        TimePolicy timePolicy = timePolicy(parent, child, 600, 5);

        when(performanceRepository.findById(200L)).thenReturn(Optional.of(performance));
        when(missionSettingRepository.findByMissionId(100L)).thenReturn(Optional.of(setting));
        when(timePolicyRepository.findByChildIdAndYearMonth(22L, YearMonth.now().toString()))
                .thenReturn(Optional.of(timePolicy));

        missionPerformanceService.approveMission(200L, 11L);

        assertThat(performance.getStatus()).isEqualTo(MissionStatus.ACCEPTED);
        assertThat(timePolicy.getAccumulatedRewardTime()).isEqualTo(35);
        verify(timePolicyRepository).save(timePolicy);
        verify(performanceRepository).save(performance);
        verify(notificationService).createNotification(
                eq(22L),
                eq(MemberRole.CHILDREN),
                eq("미션 승인 완료"),
                eq("부모님이 미션을 승인했습니다."),
                eq(NotificationType.MISSION_APPROVED),
                eq(22L),
                eq(100L),
                eq(200L),
                eq("/child-home/mission/100")
        );
    }

    @Test
    void approveMissionRejectsAlreadyAcceptedPerformanceWithoutReward() {
        Parent parent = parent();
        Children child = child(parent);
        Mission mission = mission(parent, child);
        MissionPerformance performance = performance(mission, child, MissionStatus.ACCEPTED);
        MissionSetting setting = setting(mission, VerificationType.PARENT, 30);

        when(performanceRepository.findById(200L)).thenReturn(Optional.of(performance));
        when(missionSettingRepository.findByMissionId(100L)).thenReturn(Optional.of(setting));

        assertThatExceptionOfType(MissionException.class)
                .isThrownBy(() -> missionPerformanceService.approveMission(200L, 11L))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MissionErrorCode.INVALID_MISSION_STATE));
        verifyNoInteractions(timePolicyRepository, notificationService);
        verify(performanceRepository, never()).save(any());
    }

    @Test
    void approveMissionRejectsAiVerificationPerformance() {
        Parent parent = parent();
        Children child = child(parent);
        Mission mission = mission(parent, child);
        MissionPerformance performance = performance(mission, child, MissionStatus.PENDING);
        MissionSetting setting = setting(mission, VerificationType.AI, 30);

        when(performanceRepository.findById(200L)).thenReturn(Optional.of(performance));
        when(missionSettingRepository.findByMissionId(100L)).thenReturn(Optional.of(setting));

        assertThatExceptionOfType(MissionException.class)
                .isThrownBy(() -> missionPerformanceService.approveMission(200L, 11L))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MissionErrorCode.INVALID_MISSION_STATE));
        assertThat(performance.getStatus()).isEqualTo(MissionStatus.PENDING);
        verifyNoInteractions(timePolicyRepository, notificationService);
        verify(performanceRepository, never()).save(any());
    }

    @Test
    void rejectMissionRejectsChildVerificationPerformance() {
        Parent parent = parent();
        Children child = child(parent);
        Mission mission = mission(parent, child);
        MissionPerformance performance = performance(mission, child, MissionStatus.PENDING);
        MissionSetting setting = setting(mission, VerificationType.CHILD, 30);

        when(performanceRepository.findById(200L)).thenReturn(Optional.of(performance));
        when(missionSettingRepository.findByMissionId(100L)).thenReturn(Optional.of(setting));

        assertThatExceptionOfType(MissionException.class)
                .isThrownBy(() -> missionPerformanceService.rejectMission(200L, 11L))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MissionErrorCode.INVALID_MISSION_STATE));
        assertThat(performance.getStatus()).isEqualTo(MissionStatus.PENDING);
        verifyNoInteractions(notificationService);
        verify(performanceRepository, never()).save(any());
    }

    private Parent parent() {
        return Parent.builder()
                .id(11L)
                .name("parent")
                .email("parent@test.com")
                .hash("hash")
                .build();
    }

    private Children child(Parent parent) {
        return Children.builder()
                .id(22L)
                .name("하늘")
                .email("child@test.com")
                .hash("hash")
                .parent(parent)
                .build();
    }

    private Mission mission(Parent parent, Children child) {
        return Mission.builder()
                .id(100L)
                .parent(parent)
                .child(child)
                .title("미션")
                .build();
    }

    private MissionPerformance performance(
            Mission mission,
            Children child,
            MissionStatus status
    ) {
        return MissionPerformance.builder()
                .id(200L)
                .mission(mission)
                .child(child)
                .status(status)
                .reason("대기")
                .build();
    }

    private MissionSetting setting(
            Mission mission,
            VerificationType verificationType,
            int reward
    ) {
        return MissionSetting.builder()
                .id(300L)
                .mission(mission)
                .category(MissionCategory.STUDY)
                .resetCycle(ResetCycle.DAILY)
                .verificationType(verificationType)
                .reward(reward)
                .description("설명")
                .lastResetAt(LocalDateTime.now())
                .build();
    }

    private TimePolicy timePolicy(
            Parent parent,
            Children child,
            int baseTime,
            int rewardTime
    ) {
        return TimePolicy.builder()
                .id(400L)
                .parent(parent)
                .child(child)
                .yearMonth(YearMonth.now().toString())
                .baseTime(baseTime)
                .accumulatedRewardTime(rewardTime)
                .build();
    }
}
