package me.sogom.bridge.domain.mission.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.MemberException;
import me.sogom.bridge.domain.member.code.MemberErrorCode;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import me.sogom.bridge.domain.mission.dto.req.MissionReqDTO;
import me.sogom.bridge.domain.mission.dto.res.MissionResDTO;
import me.sogom.bridge.domain.mission.entity.Mission;
import me.sogom.bridge.domain.mission.entity.MissionSetting;
import me.sogom.bridge.domain.mission.exception.MissionErrorCode;
import me.sogom.bridge.domain.mission.exception.MissionException;
import me.sogom.bridge.domain.mission.repository.MissionRepository;
import me.sogom.bridge.domain.mission.repository.MissionSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final MissionSettingRepository missionSettingRepository;
    private final ParentRepository parentRepository;
    private final ChildrenRepository childrenRepository;

    @Transactional
    public MissionResDTO.CreateMissionResponse createMission(Long parentId, MissionReqDTO.CreateMissionRequest request) {

        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Children child = childrenRepository.findById(request.childId())
                .orElseThrow(() -> new MemberException(MemberErrorCode.CHILDREN_NOT_FOUND));

        // 부모-자녀 연관관계 검증
        if (child.getParent() == null || !child.getParent().getId().equals(parentId)) {
            throw new MissionException(MissionErrorCode.CHILD_PARENT_MISMATCH);
        }

        Mission mission = Mission.builder()
                .parent(parent)
                .child(child)
                .title(request.title())
                .build();
        missionRepository.save(mission);

        MissionSetting setting = MissionSetting.builder()
                .mission(mission)
                .category(request.category())
                .resetCycle(request.resetCycle())
                .verificationType(request.verificationType())
                .reward(request.reward())
                .description(request.description())
                .lastResetAt(LocalDateTime.now())
                .build();
        missionSettingRepository.save(setting);

        return MissionResDTO.CreateMissionResponse.of(mission, setting);
    }

    @Transactional(readOnly = true)
    public List<MissionResDTO.MissionSummaryResponse> getParentMissionSummaries(Long parentId, Long childId) {
        parentRepository.findById(parentId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Children child = childrenRepository.findById(childId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.CHILDREN_NOT_FOUND));

        if (child.getParent() == null || !child.getParent().getId().equals(parentId)) {
            throw new MissionException(MissionErrorCode.CHILD_PARENT_MISMATCH);
        }

        return missionSettingRepository.findMissionSummariesByParentIdAndChildId(parentId, childId);
    }
}
