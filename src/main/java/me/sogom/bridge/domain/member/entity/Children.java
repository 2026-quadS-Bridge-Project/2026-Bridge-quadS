package me.sogom.bridge.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "children")
public class Children {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "children_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 12)
    private String email;

    @Column(nullable = false)
    private String hash;

    @Column(length = 6)
    private String code; // 자녀 연동 코드

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}