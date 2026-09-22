package com.help.erpweb.domain.academy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.help.erpweb.domain.academy.authorization.AcademyAuthorization;
import com.help.erpweb.domain.academy.repository.AcademyClassRepository;
import com.help.erpweb.domain.academy.repository.AcademyMemberRepository;
import com.help.erpweb.domain.academy.repository.AcademyRepository;
import com.help.erpweb.domain.academy.repository.StudentRepository;
import com.help.erpweb.domain.academy.repository.TeacherRepository;
import com.help.authserver.domain.user.repository.config.ERPSubscriberRepository;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.erpweb.domain.config.repository.ModuleConfigRepository;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AcademyServiceRequirementsTest {

    private AcademyService academyService;

    @BeforeEach
    void setUp() {
        academyService = new AcademyService(
                mock(AcademyRepository.class),
                mock(AcademyClassRepository.class),
                mock(TeacherRepository.class),
                mock(StudentRepository.class),
                mock(ModuleConfigRepository.class),
                mock(ERPSubscriberRepository.class),
                mock(MembershipRepository.class),
                mock(DiscordUserRepository.class),
                mock(AcademyMemberRepository.class),
                mock(AcademyAuthorization.class)
        );
    }

    @Test
    void moonlightAcademyProvidesOnlyRotationSubject() {
        assertEquals(
                1,
                academyService.getSubjects(1).size()
        );
        assertEquals(
                "로테이션",
                academyService.getSubjects(1).get(0).name()
        );
    }

    @Test
    void starlightAcademyProvidesAllSubjects() {
        assertEquals(
                3,
                academyService.getSubjects(2).size()
        );
        assertEquals(
                "로테이션",
                academyService.getSubjects(2).get(0).name()
        );
        assertEquals(
                "언리미티드",
                academyService.getSubjects(2).get(1).name()
        );
        assertEquals(
                "스타터",
                academyService.getSubjects(2).get(2).name()
        );
    }
}
