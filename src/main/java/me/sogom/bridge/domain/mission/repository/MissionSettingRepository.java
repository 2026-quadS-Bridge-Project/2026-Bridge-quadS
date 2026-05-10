package me.sogom.bridge.domain.mission.repository;

import me.sogom.bridge.domain.mission.entity.MissionSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionSettingRepository extends JpaRepository<MissionSetting, String> {
}
