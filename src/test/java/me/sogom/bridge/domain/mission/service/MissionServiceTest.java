package me.sogom.bridge.domain.mission.service;

import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import me.sogom.bridge.domain.mission.dto.req.MissionReqDTO;
import me.sogom.bridge.domain.mission.dto.res.MissionResDTO;
import me.sogom.bridge.domain.mission.entity.Mission;
import me.sogom.bridge.domain.mission.entity.MissionCategory;
import me.sogom.bridge.domain.mission.entity.MissionSetting;
import me.sogom.bridge.domain.mission.entity.ResetCycle;
import me.sogom.bridge.domain.mission.entity.VerificationType;
import me.sogom.bridge.domain.mission.exception.MissionErrorCode;
import me.sogom.bridge.domain.mission.exception.MissionException;
import me.sogom.bridge.domain.mission.repository.MissionRepository;
import me.sogom.bridge.domain.mission.repository.MissionSettingRepository;
import me.sogom.bridge.domain.notification.entity.NotificationType;
import me.sogom.bridge.domain.notification.service.NotificationService;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock
    private MissionRepository missionRepository;
    @Mock
    private MissionSettingRepository missionSettingRepository;
    @Mock
    private ParentRepository parentRepository;
    @Mock
    private ChildrenRepository childrenRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MissionService missionService;

    @Test
    void createMissionSavesSettingAndNotifiesChildWithMissionRoute() {
        Parent parent = parent(11L);
        Children child = child(22L, parent);
        MissionReqDTO.CreateMissionRequest request = createMissionRequest(22L);
        when(parentRepository.findById(11L)).thenReturn(Optional.of(parent));
        when(childrenRepository.findById(22L)).thenReturn(Optional.of(child));
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> {
            Mission saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        MissionResDTO.MissionResponse response = missionService.createMission(11L, request);

        assertThat(response.missionId()).isEqualTo(100L);
        assertThat(response.childId()).isEqualTo(22L);
        assertThat(response.title()).isEqualTo("방 정리");
        assertThat(response.category()).isEqualTo(MissionCategory.CLEANING);
        assertThat(response.resetCycle()).isEqualTo(ResetCycle.DAILY);
        assertThat(response.verificationType()).isEqualTo(VerificationType.PARENT);
        assertThat(response.reward()).isEqualTo(30);
        assertThat(response.description()).isEqualTo("책상 정리");

        ArgumentCaptor<MissionSetting> settingCaptor = ArgumentCaptor.forClass(MissionSetting.class);
        verify(missionSettingRepository).save(settingCaptor.capture());
        MissionSetting setting = settingCaptor.getValue();
        assertThat(setting.getMission().getId()).isEqualTo(100L);
        assertThat(setting.getCategory()).isEqualTo(MissionCategory.CLEANING);
        assertThat(setting.getResetCycle()).isEqualTo(ResetCycle.DAILY);
        assertThat(setting.getVerificationType()).isEqualTo(VerificationType.PARENT);
        assertThat(setting.getReward()).isEqualTo(30);
        assertThat(setting.getDescription()).isEqualTo("책상 정리");
        assertThat(setting.getLastResetAt()).isNotNull();

        verify(notificationService).createNotification(
                eq(22L),
                eq(MemberRole.CHILDREN),
                eq("새 미션이 생성되었습니다."),
                eq("방 정리"),
                eq(NotificationType.MISSION_CREATED),
                eq(22L),
                eq(100L),
                eq(null),
                eq("/child-home/mission/100")
        );
    }

    @Test
    void createMissionRejectsChildFromAnotherParent() {
        Parent parent = parent(11L);
        Children child = child(22L, parent(99L));
        when(parentRepository.findById(11L)).thenReturn(Optional.of(parent));
        when(childrenRepository.findById(22L)).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> missionService.createMission(11L, createMissionRequest(22L)))
                .isInstanceOf(MissionException.class);

        verify(missionRepository, never()).save(any(Mission.class));
        verify(missionSettingRepository, never()).save(any(MissionSetting.class));
        verify(notificationService, never()).createNotification(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void getMissionDetailAllowsOwningParent() {
        Parent parent = parent(11L);
        Children child = child(22L, parent);
        Mission mission = mission(100L, parent, child);
        MissionSetting setting = missionSetting(mission);
        when(missionRepository.findById(100L)).thenReturn(Optional.of(mission));
        when(missionSettingRepository.findByMissionId(100L)).thenReturn(Optional.of(setting));

        MissionResDTO.MissionResponse response = missionService.getMissionDetail(
                100L,
                MemberRole.PARENT,
                11L
        );

        assertThat(response.missionId()).isEqualTo(100L);
        assertThat(response.childId()).isEqualTo(22L);
        assertThat(response.title()).isEqualTo("방 정리");
    }

    @Test
    void getMissionDetailAllowsAssignedChild() {
        Parent parent = parent(11L);
        Children child = child(22L, parent);
        Mission mission = mission(100L, parent, child);
        MissionSetting setting = missionSetting(mission);
        when(missionRepository.findById(100L)).thenReturn(Optional.of(mission));
        when(missionSettingRepository.findByMissionId(100L)).thenReturn(Optional.of(setting));

        MissionResDTO.MissionResponse response = missionService.getMissionDetail(
                100L,
                MemberRole.CHILDREN,
                22L
        );

        assertThat(response.missionId()).isEqualTo(100L);
        assertThat(response.childId()).isEqualTo(22L);
        assertThat(response.title()).isEqualTo("방 정리");
    }

    @Test
    void getMissionDetailRejectsUnrelatedViewer() {
        Parent parent = parent(11L);
        Children child = child(22L, parent);
        Mission mission = mission(100L, parent, child);
        when(missionRepository.findById(100L)).thenReturn(Optional.of(mission));

        assertThatThrownBy(() -> missionService.getMissionDetail(
                100L,
                MemberRole.PARENT,
                99L
        ))
                .isInstanceOf(MissionException.class)
                .satisfies(exception ->
                        assertThat(((MissionException) exception).getErrorCode())
                                .isEqualTo(MissionErrorCode.UNAUTHORIZED_ACCESS));

        verify(missionSettingRepository, never()).findByMissionId(any());
    }

    private MissionReqDTO.CreateMissionRequest createMissionRequest(Long childId) {
        return new MissionReqDTO.CreateMissionRequest(
                childId,
                "방 정리",
                MissionCategory.CLEANING,
                ResetCycle.DAILY,
                VerificationType.PARENT,
                30,
                "책상 정리"
        );
    }

    private Parent parent(Long id) {
        return Parent.builder()
                .id(id)
                .name("parent")
                .email("parent" + id + "@test.com")
                .hash("hash")
                .build();
    }

    private Children child(Long id, Parent parent) {
        return Children.builder()
                .id(id)
                .name("하늘")
                .email("child@test.com")
                .hash("hash")
                .parent(parent)
                .build();
    }

    private Mission mission(Long id, Parent parent, Children child) {
        return Mission.builder()
                .id(id)
                .parent(parent)
                .child(child)
                .title("방 정리")
                .build();
    }

    private MissionSetting missionSetting(Mission mission) {
        return MissionSetting.builder()
                .mission(mission)
                .category(MissionCategory.CLEANING)
                .resetCycle(ResetCycle.DAILY)
                .verificationType(VerificationType.PARENT)
                .reward(30)
                .description("책상 정리")
                .build();
    }
}
