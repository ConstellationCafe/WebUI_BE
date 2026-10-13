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
     * 해당 Academy의 구성원인지 확인한다.
     *
     * ADMIN은 모든 Academy에 접근 가능하다.
     * ACADEMY_OWNER / TEACHER / STUDENT 모두 true가 될 수 있다.
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
     * 해당 Academy의 학원장인지 확인한다.
     *
     * ADMIN은 모든 Academy에서 학원장 권한을 가진 것으로 처리한다.
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
     * 특정 Class에 대한 관리 권한이 있는지 확인한다.
     *
     * ADMIN
     *      -> 모든 Academy / Class 접근 가능
     *
     * ACADEMY_OWNER
     *      -> 자신이 학원장인 Academy의 모든 Class 접근 가능
     *
     * TEACHER
     *      -> 자신이 담당하는 Class만 접근 가능
     *
     * STUDENT
     *      -> 관리 권한 없음
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

        // 해당 Academy의 학원장이면 모든 Class 접근 가능
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

        // 일반 교사는 자신이 담당하는 Class만 접근 가능
        return academyMemberRepository
                .existsBySkAndAcademyIdAndClassIdAndRoleName(
                        sk,
                        academyId,
                        classId,
                        ROLE_TEACHER
                );
    }

    /**
     * 해당 Class에 소속되어 있는지 확인한다.
     *
     * ADMIN
     *      -> 항상 접근 가능
     *
     * ACADEMY_OWNER
     *      -> 해당 Academy의 모든 Class 접근 가능
     *
     * TEACHER / STUDENT
     *      -> 자신이 속한 Class만 접근 가능
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
     * 해당 Class의 교사인지 확인한다.
     * ADMIN / ACADEMY_OWNER도 교사 이상 권한으로 취급한다.
     */
    public boolean isTeacherOrAbove(
            Authentication authentication
    ) {
        if (!isAuthenticated(authentication)) {
            return false;
        }
        // 전역 ADMIN
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

    /**
     * 2026-09-26: 학생/교사 상태 목록 조회(getStudentStatuses/getTeacherStatuses)에
     * 인가를 추가하며 새로 만듦. academyId가 지정된 경우, 특정 class 단위가
     * 아니라 "이 Academy에서 TEACHER 이상인지"만 확인한다(어느 class를
     * 담당하는지는 안 따짐) — canManageClass/isTeacherOrAbove(3-arg)는 항상
     * classId까지 요구해서 이 용도에는 안 맞는다.
     */
    public boolean isTeacherOrAboveOfAcademy(
            Authentication authentication,
            Integer academyId
    ) {
        if (!isAuthenticated(authentication)) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        if (academyId == null) {
            return isTeacherOrAbove(authentication);
        }

        String sk = resolveSk(authentication);

        return academyMemberRepository
                .existsBySkAndAcademyIdAndRoleNameIn(
                        sk,
                        academyId,
                        List.of(
                                ROLE_ACADEMY_OWNER,
                                ROLE_TEACHER
                        )
                );
    }

    /**
     * 2026-09-26: getTeacherStatuses에서 academyId 없이(전체 조회) 호출되는
     * 경우용 — 특정 Academy가 아니라 "어느 Academy에서든 학원장(ACADEMY_OWNER)
     * 인지"만 확인한다. academyId가 있으면 기존 isOwner(authentication,
     * academyId)를 그대로 쓴다.
     */
    public boolean isOwnerOfAnyAcademy(
            Authentication authentication
    ) {
        if (!isAuthenticated(authentication)) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        String sk = resolveSk(authentication);

        return academyMemberRepository
                .existsBySkAndRoleNameIn(
                        sk,
                        List.of(ROLE_ACADEMY_OWNER)
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
