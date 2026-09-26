package com.help.erpweb.domain.academy.repository;

import com.help.erpweb.domain.academy.entity.AcademyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 2026-09-26: 기존에는 discordId(원시 Discord ID)로 조회했다 — 이게 문제였던
 * 이유는 discordId는 봇(방)마다 별도의 sk(=(botId, discordId)로 결정되는
 * 신원)가 아니라 전역적으로 같은 값이기 때문이다. 즉 같은 사람이 서로 다른
 * 방(봇) 두 곳 모두에 Academy 데이터가 있으면, discordId 기준 조회는 두
 * 방의 데이터를 구분 없이 다 매치시켜버린다(방 경계를 넘는 데이터 노출).
 * <p>
 * AcademyMember(academy_member_view)/Student/Teacher는 이미 sk 컬럼을
 * 갖고 있으므로(스키마 변경 불필요), sk 기준 조회로 바꿔 방 경계를 지킨다.
 * sk는 {@code MembershipRepository.findSkByChatUser}(GuildContext의 현재
 * botId + ChatUser)로 구한다 — Membership/Content/Menu/Music/Learning
 * 도메인이 이미 쓰는 것과 동일한 경로다.
 */
public interface AcademyMemberRepository
        extends JpaRepository<AcademyMember, String> {

    boolean existsBySkAndAcademyId(
            String sk,
            Integer academyId
    );

    boolean existsBySkAndAcademyIdAndRoleName(
            String sk,
            Integer academyId,
            String roleName
    );

    boolean existsBySkAndAcademyIdAndClassId(
            String sk,
            Integer academyId,
            Integer classId
    );

    boolean existsBySkAndAcademyIdAndClassIdAndRoleName(
            String sk,
            Integer academyId,
            Integer classId,
            String roleName
    );

    @Query("""
        select distinct am.sk
        from AcademyMember am
        where am.sk = :sk
          and am.academyId = :academyId
          and am.roleName = 'TEACHER'
    """)
    Optional<String> findTeacherSk(
            @Param("sk") String sk,
            @Param("academyId") Integer academyId
    );

    @Query("""
        select distinct am.academyId
        from AcademyMember am
        where am.sk = :sk
    """)
    List<Integer> findAccessibleAcademyIds(
            @Param("sk") String sk
    );

    @Query("""
        select distinct am.classId
        from AcademyMember am
        where am.sk = :sk
          and am.academyId = :academyId
          and am.roleName = 'TEACHER'
          and am.classId is not null
    """)
    List<Integer> findAccessibleClassIds(
            @Param("sk") String sk,
            @Param("academyId") Integer academyId
    );

    List<AcademyMember> findAllBySk(
            String sk
    );

    boolean existsBySkAndRoleNameIn(
            String sk,
            Collection<String> roleNames
    );
}
