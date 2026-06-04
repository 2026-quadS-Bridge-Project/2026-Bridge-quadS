package me.sogom.bridge.domain.mission.entity;

import jakarta.persistence.*;
import lombok.*;
import me.sogom.bridge.domain.common.BaseEntity;
import me.sogom.bridge.domain.member.entity.Children;
//mission 수행 내역 entity
@Entity
@Getter
@Builder
@AllArgsConstructor //@Builder를 쓰기 위한 필수 세트
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

    @Column(name = "reason", columnDefinition = "TEXT") //AI의 분석 근거를 저장할 컬럼 추가
    private String reason;

    // AI 판독 결과나 부모 수동 확인에 의해 상태와 판독 이유를 변경하는 비즈니스 메서드
    public void updateStatusAndReason(MissionStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }
}
