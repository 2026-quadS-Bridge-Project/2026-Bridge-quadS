package me.sogom.bridge.domain.mission.repository;

import me.sogom.bridge.domain.mission.entity.MissionPerformance;
import org.springframework.data.jpa.repository.JpaRepository;

// DB에 접근하여 저장/조회를 담당
public interface MissionPerformanceRepository extends JpaRepository<MissionPerformance, Long> {
}