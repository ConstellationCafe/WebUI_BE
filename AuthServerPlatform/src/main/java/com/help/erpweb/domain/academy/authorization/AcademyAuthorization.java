package com.help.erpweb.domain.academy.authorization;

import com.help.erpweb.domain.academy.repository.AcademyMemberRepository;
import com.help.global.data.Authority;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("academyAuth")
@RequiredArgsConstructor
public class AcademyAuthorization {
    private static final String ROLE_ACADEMY_OWNER = "ACADEMY_OWNER";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_STUDENT = "STUDENT";

    private final AcademyMemberRepository academyMemberRepository;

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

        String discordId = authentication.getName();

        return academyMemberRepository
                .existsByDiscordIdAndAcademyId(
                        discordId,
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

        String discordId = authentication.getName();

        return academyMemberRepository
                .existsByDiscordIdAndAcademyIdAndRoleName(
                        discordId,
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

        String discordId = authentication.getName();

        // 해당 Academy의 학원장이면 모든 Class 접근 가능
        boolean isOwner =
                academyMemberRepository
                        .existsByDiscordIdAndAcademyIdAndRoleName(
                                discordId,
                                academyId,
                                ROLE_ACADEMY_OWNER
                        );

        if (isOwner) {
            return true;
        }

        // 일반 교사는 자신이 담당하는 Class만 접근 가능
        return academyMemberRepository
                .existsByDiscordIdAndAcademyIdAndClassIdAndRoleName(
                        discordId,
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

        String discordId = authentication.getName();

        boolean isOwner =
                academyMemberRepository
                        .existsByDiscordIdAndAcademyIdAndRoleName(
                                discordId,
                                academyId,
                                ROLE_ACADEMY_OWNER
                        );

        if (isOwner) {
            return true;
        }

        return academyMemberRepository
                .existsByDiscordIdAndAcademyIdAndClassId(
                        discordId,
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

        String discordId = authentication.getName();
        return academyMemberRepository
                .existsByDiscordIdAndRoleNameIn(
                        discordId,
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

        String discordId = authentication.getName();

        boolean isOwner = academyMemberRepository
                        .existsByDiscordIdAndAcademyIdAndRoleName(
                                discordId,
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
                        .existsByDiscordIdAndAcademyIdAndClassIdAndRoleName(
                                discordId,
                                academyId,
                                classId,
                                ROLE_TEACHER
                        );

        if (isTeacher) {
            return AcademyAccessScope.TEACHER;
        }

        boolean isStudent =
                academyMemberRepository
                        .existsByDiscordIdAndAcademyIdAndClassIdAndRoleName(
                                discordId,
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