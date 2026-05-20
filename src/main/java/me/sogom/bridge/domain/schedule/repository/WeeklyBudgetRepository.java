package me.sogom.bridge.domain.schedule.repository;

import me.sogom.bridge.domain.schedule.entity.WeeklyBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeeklyBudgetRepository extends JpaRepository<WeeklyBudget, Long> {
    // 특정 월의 1~4주차 예산을 한 번에 조회
    List<WeeklyBudget> findAllByChildIdAndYearMonth(Long childId, String yearMonth);

    // 특정 월, 특정 주차의 예산 단건 조회
    Optional<WeeklyBudget> findByChildIdAndYearMonthAndWeekNumber(Long childId, String yearMonth, int weekNumber);
}