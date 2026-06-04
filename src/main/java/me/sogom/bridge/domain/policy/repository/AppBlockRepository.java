package me.sogom.bridge.domain.policy.repository;

import me.sogom.bridge.domain.policy.entity.AppBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppBlockRepository extends JpaRepository<AppBlock, Long> {
    //특정 자녀에게 걸려있는 모든 차단 앱을 가져옴
    List<AppBlock> findAllByChildId(Long childId);
}
