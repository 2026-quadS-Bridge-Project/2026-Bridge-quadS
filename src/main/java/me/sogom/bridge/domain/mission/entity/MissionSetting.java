package me.sogom.bridge.domain.mission.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
//mission setting entity
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mission_setting")
public class MissionSetting {
    @Id
    @Column(name = "mission_setting_id")
    private String id; // SQL에 BIGINT가 아닌 VARCHAR(255)로 되어 있음

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Enumerated(EnumType.STRING)
    private MissionCategory category; // ENUM 클래스

    private int reward; // 보상 시간

    @Column(length = 100)
    private String prompt; // 부모가 작성한 AI 검증 기준
}
