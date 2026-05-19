package me.sogom.bridge.domain.schedule.repository;

import me.sogom.bridge.domain.schedule.entity.WeeklyRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WeeklyRoutineRepository extends JpaRepository<WeeklyRoutine, Long> {
    // 특정 자녀의 일주일 전체 학원/고정 일정 조회
    List<WeeklyRoutine> findAllByChild_Id(Long childId);
}