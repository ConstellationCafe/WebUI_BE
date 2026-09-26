package com.help.erpweb.domain.academy.authorization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.help.erpweb.domain.academy.repository.AcademyMemberRepository;
import com.help.erpweb.domain.modules.erp.point.repository.MembershipRepository;
import com.help.global.chat.ChatUser;
import com.help.global.jwt.CustomUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * 2026-09-26: 권한 판정 기준이 discordId(전역)에서 sk(=(botId, discordId)로
 * 결정되는 방 스코프 신원)로 바뀌었다 — principal은 이제 discordId 하나만
 * 아는 String이 아니라 CustomUser여야 하고, sk는
 * MembershipRepository.findSkByChatUser로 구한다(테스트에서는 stub).
 */
@ExtendWith(MockitoExtension.class)
class AcademyAuthorizationTest {

    @Mock
    private AcademyMemberRepository academyMemberRepository;

    @Mock
    private MembershipRepository membershipRepository;

    private AcademyAuthorization authorization;

    @BeforeEach
    void setUp() {
        authorization = new AcademyAuthorization(academyMemberRepository, membershipRepository);
    }

    @Test
    void unauthenticatedUserCannotAccessAcademy() {
        assertFalse(authorization.isMember(null, 1));
    }

    @Test
    void adminCanManageAnyClassWithoutRepositoryLookup() {
        Authentication admin = authentication("admin", "ROLE_ADMIN");

        assertTrue(authorization.canManageClass(admin, 1, 2));
        verify(academyMemberRepository, never())
                .existsBySkAndAcademyIdAndRoleName(
                        any(), any(), any()
                );
    }

    @Test
    void teacherCanManageOnlyAssignedClass() {
        Authentication teacher = authentication("teacher", "ROLE_USER");
        when(membershipRepository.findSkByChatUser(any(ChatUser.class)))
                .thenReturn("teacher-sk");
        when(academyMemberRepository
                .existsBySkAndAcademyIdAndRoleName(
                        "teacher-sk", 1, "ACADEMY_OWNER"
                ))
                .thenReturn(false);
        when(academyMemberRepository
                .existsBySkAndAcademyIdAndClassIdAndRoleName(
                        "teacher-sk", 1, 2, "TEACHER"
                ))
                .thenReturn(true);

        assertTrue(authorization.canManageClass(teacher, 1, 2));
        verify(academyMemberRepository)
                .existsBySkAndAcademyIdAndClassIdAndRoleName(
                        "teacher-sk", 1, 2, "TEACHER"
                );
    }

    @Test
    void studentCannotManageClass() {
        Authentication student = authentication("student", "ROLE_USER");
        lenient().when(membershipRepository.findSkByChatUser(any(ChatUser.class)))
                .thenReturn("student-sk");
        when(academyMemberRepository
                .existsBySkAndAcademyIdAndRoleName(
                        "student-sk", 1, "ACADEMY_OWNER"
                ))
                .thenReturn(false);
        when(academyMemberRepository
                .existsBySkAndAcademyIdAndClassIdAndRoleName(
                        "student-sk", 1, 2, "TEACHER"
                ))
                .thenReturn(false);

        assertFalse(authorization.canManageClass(student, 1, 2));
    }

    private Authentication authentication(String name, String authority) {
        CustomUser principal = CustomUser.of(name, "OAUTH_USER", authority, null);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
