package me.sogom.bridge.domain.mission.repository;

import me.sogom.bridge.domain.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

// Mission 엔티티를 관리하는 리포지토리
public interface MissionRepository extends JpaRepository<Mission, Long> {
}