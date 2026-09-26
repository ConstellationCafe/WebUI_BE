package com.help.erpweb.domain.academy.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.help.global.discord.identity.DiscordUserRepository;
import com.help.erpweb.domain.academy.authorization.AcademyAuthorization;
import com.help.erpweb.domain.academy.dto.request.LessonRecordUpdateRequest;
import com.help.erpweb.domain.academy.entity.LessonRecord;
import com.help.erpweb.domain.academy.repository.AcademyClassRepository;
import com.help.erpweb.domain.academy.repository.AcademyMemberRepository;
import com.help.erpweb.domain.academy.repository.LessonRecordRepository;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
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
                .thenReturn(Optional.empty());

        LessonRecordService service = service(
                lessonRecordRepository,
                academyMemberRepository,
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

    @Test
    void rejectsUpdateWhenTeacherDoesNotOwnRecord() {
        LessonRecordRepository repository = mock(LessonRecordRepository.class);
        AcademyMemberRepository academyMembers =
                mock(AcademyMemberRepository.class);
        AcademyAuthorization authorization =
                mock(AcademyAuthorization.class);
        Authentication authentication = mock(Authentication.class);
        LessonRecord record = lessonRecord("another-teacher");

        when(authentication.getName()).thenReturn("teacher");
        when(authorization.hasGlobalAccess(authentication)).thenReturn(false);
        when(authorization.isOwner(authentication, 1)).thenReturn(false);
        when(academyMembers.findTeacherSk("teacher", 1))
                .thenReturn(Optional.of("teacher-sk"));
        when(repository.findById(1L)).thenReturn(Optional.of(record));

        LessonRecordService service = service(
                repository,
                academyMembers,
                authorization
        );

        assertThrows(
                AccessDeniedException.class,
                () -> service.updateLessonRecord(
                        authentication,
                        1L,
                        new LessonRecordUpdateRequest(
                                "Updated subject",
                                LocalDateTime.of(2026, 9, 23, 0, 0),
                                LocalTime.of(10, 0),
                                LocalTime.of(11, 0),
                                60,
                                "Updated description"
                        )
                )
        );
        assertEquals("Subject", record.getSubject());
        verify(repository, never()).delete(record);
    }

    @Test
    void allowsTeacherToUpdateTheirOwnRecord() {
        LessonRecordRepository repository = mock(LessonRecordRepository.class);
        AcademyMemberRepository academyMembers =
                mock(AcademyMemberRepository.class);
        AcademyAuthorization authorization =
                mock(AcademyAuthorization.class);
        Authentication authentication = mock(Authentication.class);
        LessonRecord record = lessonRecord("teacher-sk");

        when(authentication.getName()).thenReturn("teacher");
        when(authorization.hasGlobalAccess(authentication)).thenReturn(false);
        when(authorization.isOwner(authentication, 1)).thenReturn(false);
        when(academyMembers.findTeacherSk("teacher", 1))
                .thenReturn(Optional.of("teacher-sk"));
        when(repository.findById(1L)).thenReturn(Optional.of(record));

        LessonRecordService service = service(
                repository,
                academyMembers,
                authorization
        );

        service.updateLessonRecord(
                authentication,
                1L,
                new LessonRecordUpdateRequest(
                        "Updated subject",
                        LocalDateTime.of(2026, 9, 24, 0, 0),
                        LocalTime.of(12, 0),
                        LocalTime.of(13, 30),
                        90,
                        "Updated description"
                )
        );

        assertEquals("Updated subject", record.getSubject());
        assertEquals(LocalTime.of(12, 0), record.getStartTime());
        assertEquals(LocalTime.of(13, 30), record.getEndTime());
        assertEquals(90, record.getEducationDuration());
        assertEquals("Updated description", record.getDescription());
    }

    @Test
    void allowsTeacherToDeleteTheirOwnRecord() {
        LessonRecordRepository repository = mock(LessonRecordRepository.class);
        AcademyMemberRepository academyMembers =
                mock(AcademyMemberRepository.class);
        AcademyAuthorization authorization =
                mock(AcademyAuthorization.class);
        Authentication authentication = mock(Authentication.class);
        LessonRecord record = lessonRecord("teacher-sk");

        when(authentication.getName()).thenReturn("teacher");
        when(authorization.hasGlobalAccess(authentication)).thenReturn(false);
        when(authorization.isOwner(authentication, 1)).thenReturn(false);
        when(academyMembers.findTeacherSk("teacher", 1))
                .thenReturn(Optional.of("teacher-sk"));
        when(repository.findById(1L)).thenReturn(Optional.of(record));

        LessonRecordService service = service(
                repository,
                academyMembers,
                authorization
        );

        service.deleteLessonRecord(authentication, 1L);

        verify(repository).delete(record);
    }

    private LessonRecordService service(
            LessonRecordRepository repository,
            AcademyMemberRepository academyMembers,
            AcademyAuthorization authorization
    ) {
        return new LessonRecordService(
                repository,
                mock(AcademyClassRepository.class),
                mock(MembershipRepository.class),
                mock(DiscordUserRepository.class),
                academyMembers,
                new ObjectMapper(),
                authorization
        );
    }

    private LessonRecord lessonRecord(String mainTeacherId) {
        ObjectMapper objectMapper = new ObjectMapper();
        return new LessonRecord(
                1,
                "1",
                "Subject",
                LocalDateTime.of(2026, 9, 23, 0, 0),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                60,
                mainTeacherId,
                objectMapper.createArrayNode(),
                objectMapper.createArrayNode(),
                "Description"
        );
    }
}
