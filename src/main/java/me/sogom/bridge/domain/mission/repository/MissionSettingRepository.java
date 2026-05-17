package me.sogom.bridge.domain.mission.repository;

import me.sogom.bridge.domain.mission.entity.MissionSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MissionSettingRepository extends JpaRepository<MissionSetting, String> {
    // 미션 ID로 세팅 정보(reward가 포함된)를 조회하는 메서드
    Optional<MissionSetting> findByMissionId(Long missionId);
}