package com.help.erpweb.domain.academy.authorization;

import com.help.erpweb.domain.academy.repository.AcademyMemberRepository;
import com.help.erpweb.domain.modules.erp.point.repository.MembershipRepository;
import com.help.global.chat.ChatIdentities;
import com.help.global.data.Authority;
import com.help.global.jwt.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 2026-09-26: 권한 판정을 discordId(전역, 봇/방 무관) 기준에서 sk(=(botId,
 * discordId)로 결정되는 방 스코프 신원) 기준으로 바꿨다 — discordId만으로는
 * 같은 사람이 다른 방에서 가진 Academy 멤버십까지 통과시켜버리는 문제가
 * 있었다. sk는 {@link MembershipRepository#findSkByChatUser}로 구하며,
 * 이 메서드가 내부적으로 {@code GuildContext.requireBotId()}(현재 요청의
 * botId)를 쓰므로 여기서 botId를 직접 다룰 필요는 없다.
 */
@Component("academyAuth")
@RequiredArgsConstructor
public class AcademyAuthorization {
    private static final String ROLE_ACADEMY_OWNER = "ACADEMY_OWNER";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_STUDENT = "STUDENT";

    private final AcademyMemberRepository academyMemberRepository;
    private final MembershipRepository membershipRepository;

    public boolean hasGlobalAccess(
            Authentication authentication
    ) {
        return isAuthenticated(authentication)
                && isAdmin(authentication);
    }

    /**
     * í´ë¹ Academyì êµ¬ì±ìì¸ì§ íì¸íë¤.
     *
     * ADMINì ëª¨ë  Academyì ì ê·¼ ê°ë¥íë¤.
     * ACADEMY_OWNER / TEACHER / STUDENT ëª¨ë trueê° ë  ì ìë¤.
     */
    public boolean isMember(
            Authentication authentication,
            Integer academyId
    ) {
        if (!isAuthenticated(authentication)) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        String sk = resolveSk(authentication);

        return academyMemberRepository
                .existsBySkAndAcademyId(
                        sk,
                        academyId
                );
    }

    /**
     * í´ë¹ Academyì íìì¥ì¸ì§ íì¸íë¤.
     *
     * ADMINì ëª¨ë  Academyìì íìì¥ ê¶íì ê°ì§ ê²ì¼ë¡ ì²ë¦¬íë¤.
     */
    public boolean isOwner(
            Authentication authentication,
            Integer academyId
    ) {
        if (!isAuthenticated(authentication)) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        String sk = resolveSk(authentication);

        return academyMemberRepository
                .existsBySkAndAcademyIdAndRoleName(
                        sk,
                        academyId,
                        ROLE_ACADEMY_OWNER
                );
    }

    /**
     * í¹ì  Classì ëí ê´ë¦¬ ê¶íì´ ìëì§ íì¸íë¤.
     *
     * ADMIN
     *      -> ëª¨ë  Academy / Class ì ê·¼ ê°ë¥
     *
     * ACADEMY_OWNER
     *      -> ìì ì´ íìì¥ì¸ Academyì ëª¨ë  Class ì ê·¼ ê°ë¥
     *
     * TEACHER
     *      -> ìì ì´ ë´ë¹íë Classë§ ì ê·¼ ê°ë¥
     *
     * STUDENT
     *      -> ê´ë¦¬ ê¶í ìì
     */
    public boolean canManageClass(
            Authentication authentication,
            Integer academyId,
            Integer classId
    ) {
        if (!isAuthenticated(authentication)) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        String sk = resolveSk(authentication);

        // í´ë¹ Academyì íìì¥ì´ë©´ ëª¨ë  Class ì ê·¼ ê°ë¥
        boolean isOwner =
                academyMemberRepository
                        .existsBySkAndAcademyIdAndRoleName(
                                sk,
                                academyId,
                                ROLE_ACADEMY_OWNER
                        );

        if (isOwner) {
            return true;
        }

        // ì¼ë° êµì¬ë ìì ì´ ë´ë¹íë Classë§ ì ê·¼ ê°ë¥
        return academyMemberRepository
                .existsBySkAndAcademyIdAndClassIdAndRoleName(
                        sk,
                        academyId,
                        classId,
                        ROLE_TEACHER
                );
    }

    /**
     * í´ë¹ Classì ììëì´ ìëì§ íì¸íë¤.
     *
     * ADMIN
     *      -> í­ì ì ê·¼ ê°ë¥
     *
     * ACADEMY_OWNER
     *      -> í´ë¹ Academyì ëª¨ë  Class ì ê·¼ ê°ë¥
     *
     * TEACHER / STUDENT
     *      -> ìì ì´ ìí Classë§ ì ê·¼ ê°ë¥
     */
    public boolean canAccessClass(
            Authentication authentication,
            Integer academyId,
            Integer classId
    ) {
        if (!isAuthenticated(authentication)) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        String sk = resolveSk(authentication);

        boolean isOwner =
                academyMemberRepository
                        .existsBySkAndAcademyIdAndRoleName(
                                sk,
                                academyId,
                                ROLE_ACADEMY_OWNER
                        );

        if (isOwner) {
            return true;
        }

        return academyMemberRepository
                .existsBySkAndAcademyIdAndClassId(
                        sk,
                        academyId,
                        classId
                );
    }

    /**
     * í´ë¹ Classì êµì¬ì¸ì§ íì¸íë¤.
     * ADMIN / ACADEMY_OWNERë êµì¬ ì´ì ê¶íì¼ë¡ ì·¨ê¸íë¤.
     */
    public boolean isTeacherOrAbove(
            Authentication authentication
    ) {
        if (!isAuthenticated(authentication)) {
            return false;
        }
        // ì ì­ ADMIN
        if (isAdmin(authentication)) {
            return true;
        }

        String sk = resolveSk(authentication);
        return academyMemberRepository
                .existsBySkAndRoleNameIn(
                        sk,
                        List.of(
                                "ACADEMY_OWNER",
                                "TEACHER"
                        )
                );
    }

    public boolean isTeacherOrAbove(
            Authentication authentication,
            Integer academyId,
            Integer classId
    ) {
        return canManageClass(
                authentication,
                academyId,
                classId
        );
    }

    private String resolveSk(
            Authentication authentication
    ) {
        CustomUser user = (CustomUser) authentication.getPrincipal();
        return membershipRepository.findSkByChatUser(
                ChatIdentities.fromPrincipal(user)
        );
    }

    private boolean isAdmin(
            Authentication authentication
    ) {
        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        Authority.ADMIN.equals(
                                authority.getAuthority()
                        )
                );
    }

    private boolean isAuthenticated(
            Authentication authentication
    ) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null;
    }

    public AcademyAccessScope getAccessScope(
            Authentication authentication,
            Integer academyId,
            Integer classId
    ) {
        if (!isAuthenticated(authentication)) {
            return AcademyAccessScope.NONE;
        }

        if (isAdmin(authentication)) {
            return AcademyAccessScope.ADMIN;
        }

        String sk = resolveSk(authentication);

        boolean isOwner = academyMemberRepository
                        .existsBySkAndAcademyIdAndRoleName(
                                sk,
                                academyId,
                                ROLE_ACADEMY_OWNER
                        );

        if (isOwner) {
            return AcademyAccessScope.OWNER;
        }

        if (classId == null) {
            return AcademyAccessScope.NONE;
        }

        boolean isTeacher =
                academyMemberRepository
                        .existsBySkAndAcademyIdAndClassIdAndRoleName(
                                sk,
                                academyId,
                                classId,
                                ROLE_TEACHER
                        );

        if (isTeacher) {
            return AcademyAccessScope.TEACHER;
        }

        boolean isStudent =
                academyMemberRepository
                        .existsBySkAndAcademyIdAndClassIdAndRoleName(
                                sk,
                                academyId,
                                classId,
                                ROLE_STUDENT
                        );

        if (isStudent) {
            return AcademyAccessScope.STUDENT;
        }

        return AcademyAccessScope.NONE;
    }
}
