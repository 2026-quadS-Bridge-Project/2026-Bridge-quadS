package me.sogom.bridge.domain.schedule.repository;

import me.sogom.bridge.domain.schedule.entity.DailyTimeAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyTimeAllocationRepository extends JpaRepository<DailyTimeAllocation, Long> {

    // 특정 자녀의 '특정 날짜(오늘)' 데이터 단건 조회 (오늘 앱을 켤 때 가장 많이 쓰임!)
    Optional<DailyTimeAllocation> findByChildIdAndTargetDate(Long childId, LocalDate targetDate);

    // 특정 자녀의 '특정 기간(시작일~종료일)' 데이터 조회 (과거 사용 리포트나 주간 달력 뷰 그릴 때 유용)
    List<DailyTimeAllocation> findAllByChildIdAndTargetDateBetween(Long childId, LocalDate startDate, LocalDate endDate);
}