package com.help.erpweb.domain.academy.authorization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.help.erpweb.domain.academy.repository.AcademyMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AcademyAuthorizationTest {

    @Mock
    private AcademyMemberRepository academyMemberRepository;

    private AcademyAuthorization authorization;

    @BeforeEach
    void setUp() {
        authorization = new AcademyAuthorization(academyMemberRepository);
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
                .existsByDiscordIdAndAcademyIdAndRoleName(
                        "admin", 1, "ACADEMY_OWNER"
                );
    }

    @Test
    void teacherCanManageOnlyAssignedClass() {
        Authentication teacher = authentication("teacher", "ROLE_USER");
        when(academyMemberRepository
                .existsByDiscordIdAndAcademyIdAndRoleName(
                        "teacher", 1, "ACADEMY_OWNER"
                ))
                .thenReturn(false);
        when(academyMemberRepository
                .existsByDiscordIdAndAcademyIdAndClassIdAndRoleName(
                        "teacher", 1, 2, "TEACHER"
                ))
                .thenReturn(true);

        assertTrue(authorization.canManageClass(teacher, 1, 2));
        verify(academyMemberRepository)
                .existsByDiscordIdAndAcademyIdAndClassIdAndRoleName(
                        "teacher", 1, 2, "TEACHER"
                );
    }

    @Test
    void studentCannotManageClass() {
        Authentication student = authentication("student", "ROLE_USER");
        when(academyMemberRepository
                .existsByDiscordIdAndAcademyIdAndRoleName(
                        "student", 1, "ACADEMY_OWNER"
                ))
                .thenReturn(false);
        when(academyMemberRepository
                .existsByDiscordIdAndAcademyIdAndClassIdAndRoleName(
                        "student", 1, 2, "TEACHER"
                ))
                .thenReturn(false);

        assertFalse(authorization.canManageClass(student, 1, 2));
    }

    private Authentication authentication(String name, String authority) {
        return new UsernamePasswordAuthenticationToken(
                name,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
