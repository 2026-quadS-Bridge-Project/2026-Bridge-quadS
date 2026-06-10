package me.sogom.bridge.domain.mission.repository;

import me.sogom.bridge.domain.mission.entity.MissionPerformance;
import me.sogom.bridge.domain.mission.entity.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// DB에 접근하여 저장/조회를 담당
public interface MissionPerformanceRepository extends JpaRepository<MissionPerformance, Long> {
    //미션 ID로 조회하여 가장 최근에 등록된 내역 1건만 가져오는 메서드
    Optional<MissionPerformance> findTopByMissionIdOrderByIdDesc(Long missionId);

    boolean existsByMissionIdAndStatus(Long missionId, MissionStatus status);
}
