package me.sogom.bridge.domain.schedule.repository;

import me.sogom.bridge.domain.schedule.entity.WeeklyTimeDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyTimeDistributionRepository extends JpaRepository<WeeklyTimeDistribution, Long> {

    // 특정 자녀의 '특정 요일' 템플릿 단건 조회 (오늘 요일 템플릿 꺼내올 때 사용)
    Optional<WeeklyTimeDistribution> findByChildIdAndDayOfWeek(Long childId, DayOfWeek dayOfWeek);

    // 특정 자녀의 '일주일치 전체' 템플릿 조회 (자녀가 주간 계획표 화면을 열었을 때 사용)
    List<WeeklyTimeDistribution> findAllByChildId(Long childId);
}