package me.sogom.bridge.domain.member.repository;

import me.sogom.bridge.domain.member.entity.Children;
import org.springframework.data.jpa.repository.JpaRepository;

// Children 엔티티를 관리하는 리포지토리
import java.util.Optional;

public interface ChildrenRepository extends JpaRepository<Children, Long> {
    Optional<Children> findByEmail(String email);
}