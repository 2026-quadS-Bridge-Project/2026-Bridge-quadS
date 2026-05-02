package me.sogom.bridge.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import me.sogom.bridge.domain.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "parent")
public class Parent extends BaseEntity implements Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parent_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(nullable = false)
    private String hash;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<Children> children = new ArrayList<>();
}