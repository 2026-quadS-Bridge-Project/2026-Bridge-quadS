package me.sogom.bridge.domain.mission.repository;

import me.sogom.bridge.domain.mission.dto.res.MissionResDTO;
import me.sogom.bridge.domain.mission.entity.MissionSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MissionSettingRepository extends JpaRepository<MissionSetting, Long> {
    // 미션 ID로 세팅 정보(reward가 포함된)를 조회하는 메서드
    Optional<MissionSetting> findByMissionId(Long missionId);

    @Query("""
            select new me.sogom.bridge.domain.mission.dto.res.MissionResDTO$MissionSummaryResponse(
                m.id,
                m.title,
                s.category,
                s.reward
            )
            from MissionSetting s
            join s.mission m
            where m.parent.id = :parentId
              and m.child.id = :childId
            order by m.id desc
            """)
    List<MissionResDTO.MissionSummaryResponse> findMissionSummariesByParentIdAndChildId(Long parentId, Long childId);
}
