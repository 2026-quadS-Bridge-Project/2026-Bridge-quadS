package me.sogom.bridge.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import me.sogom.bridge.domain.common.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "children")
public class Children extends BaseEntity implements Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "children_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(nullable = false)
    private String hash;

    @Column
    private LocalDateTime passwordChangedAt;

    @Column(length = 8)
    private String code; // 자녀 연동 코드

    @Column(length = 100)
    private String profileImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    public void changePassword(String newHash, LocalDateTime changedAt) {
        this.hash = newHash;
        this.passwordChangedAt = changedAt;
    }
}