package me.sogom.bridge.domain.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import me.sogom.bridge.domain.common.BaseEntity;
import me.sogom.bridge.domain.member.entity.Children;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "app_block")
public class AppBlock extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_block_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "children_id", nullable = false)
    private Children child; // 차단이 적용되는 자녀

    @Column(nullable = false, length = 100)
    private String appName; // 앱 이름 (예: "인스타그램")

    @Column(nullable = false, length = 255)
    private String packageName; // 안드로이드 패키지명 (예: "com.instagram.android")
}