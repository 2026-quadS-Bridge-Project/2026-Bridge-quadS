package me.sogom.bridge.domain.mission.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.sogom.bridge.domain.common.BaseEntity;
import me.sogom.bridge.domain.member.entity.Children;
//mission 수행 내역 entity
@Entity
@Getter
@Setter // 상태 업데이트를 위해 일시적 허용 (실무에선 update 메서드 사용 권장)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mission_performance")
public class MissionPerformance extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_performance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Children child;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) // 옵션: null 허용 안함, PostgreSQL 문법
    private MissionStatus status; // PENDING, ACCEPTED, REJECTED

    @Column(name = "proof_url", length = 500)
    private String proofUrl;
}
