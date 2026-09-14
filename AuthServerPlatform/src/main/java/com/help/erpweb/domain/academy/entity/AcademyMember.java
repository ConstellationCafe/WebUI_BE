package com.help.erpweb.domain.academy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Getter
@Entity
@Immutable
@Table(
        name = "academy_member_view",
        catalog = "Academy"
)
public class AcademyMember {

    @Id
    @Column(name = "member_id")
    private String memberId;

    @Column(name = "academy_id", nullable = false)
    private Integer academyId;

    @Column(name = "class_id")
    private Integer classId;

    @Column(name = "sk", length = 36)
    private String sk;

    @Column(name = "discord_id", length = 32)
    private String discordId;

    @Column(name = "role_name", nullable = false)
    private String roleName;
}