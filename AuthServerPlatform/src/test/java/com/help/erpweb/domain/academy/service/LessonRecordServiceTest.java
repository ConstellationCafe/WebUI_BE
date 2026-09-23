package com.help.erpweb.domain.academy.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.erpweb.domain.academy.authorization.AcademyAuthorization;
import com.help.erpweb.domain.academy.repository.AcademyClassRepository;
import com.help.erpweb.domain.academy.repository.AcademyMemberRepository;
import com.help.erpweb.domain.academy.repository.LessonRecordRepository;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class LessonRecordServiceTest {

    @Test
    void returnsEmptyListWhenTeacherHasNoLessonRecord() {
        LessonRecordRepository lessonRecordRepository =
                mock(LessonRecordRepository.class);
        AcademyMemberRepository academyMemberRepository =
                mock(AcademyMemberRepository.class);
        AcademyAuthorization academyAuthorization =
                mock(AcademyAuthorization.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn("teacher");
        when(academyAuthorization.hasGlobalAccess(authentication))
                .thenReturn(false);
        when(academyAuthorization.isOwner(authentication, 1))
                .thenReturn(false);
        when(academyMemberRepository.findTeacherSk("teacher", 1))
                .thenReturn(java.util.Optional.empty());

        LessonRecordService service = new LessonRecordService(
                lessonRecordRepository,
                mock(AcademyClassRepository.class),
                mock(MembershipRepository.class),
                mock(DiscordUserRepository.class),
                academyMemberRepository,
                new ObjectMapper(),
                academyAuthorization
        );

        List<?> records = assertDoesNotThrow(() ->
                service.getLessonRecords(
                        authentication,
                        null,
                        null,
                        null,
                        null,
                        1,
                        null
                )
        );

        assertTrue(records.isEmpty());
    }
}
