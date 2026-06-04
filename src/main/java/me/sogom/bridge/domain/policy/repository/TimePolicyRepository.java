package me.sogom.bridge.domain.policy.repository;

import me.sogom.bridge.domain.policy.entity.TimePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TimePolicyRepository extends JpaRepository<TimePolicy, Long> {
    //특정 자녀의 특정 연월 정책을 하나 찾아옴
    // 예: childId=1, yearMonth="2026-04"
    Optional<TimePolicy> findByChildIdAndYearMonth(Long childId, String yearMonth);
}
