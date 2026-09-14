package com.help.erpweb.domain.academy.repository;

import com.help.erpweb.domain.academy.entity.AcademyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AcademyMemberRepository
        extends JpaRepository<AcademyMember, String> {

    boolean existsByDiscordIdAndAcademyId(
            String discordId,
            Integer academyId
    );

    boolean existsByDiscordIdAndAcademyIdAndRoleName(
            String discordId,
            Integer academyId,
            String roleName
    );

    boolean existsByDiscordIdAndAcademyIdAndClassId(
            String discordId,
            Integer academyId,
            Integer classId
    );

    boolean existsByDiscordIdAndAcademyIdAndClassIdAndRoleName(
            String discordId,
            Integer academyId,
            Integer classId,
            String roleName
    );

    @Query("""
        select distinct am.sk
        from AcademyMember am
        where am.discordId = :discordId
          and am.academyId = :academyId
          and am.roleName = 'TEACHER'
    """)
    Optional<String> findTeacherSk(
            @Param("discordId") String discordId,
            @Param("academyId") Integer academyId
    );

    @Query("""
        select distinct am.academyId
        from AcademyMember am
        where am.discordId = :discordId
          and am.roleName in ('ACADEMY_OWNER', 'TEACHER')
    """)
    List<Integer> findAccessibleAcademyIds(
            @Param("discordId") String discordId
    );

    @Query("""
        select distinct am.classId
        from AcademyMember am
        where am.discordId = :discordId
          and am.academyId = :academyId
          and am.roleName = 'TEACHER'
          and am.classId is not null
    """)
    List<Integer> findAccessibleClassIds(
            @Param("discordId") String discordId,
            @Param("academyId") Integer academyId
    );

    List<AcademyMember> findAllByDiscordId(
            String discordId
    );

    boolean existsByDiscordIdAndRoleNameIn(
            String discordId,
            Collection<String> roleNames
    );
}