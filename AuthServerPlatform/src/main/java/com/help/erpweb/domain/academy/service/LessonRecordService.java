package com.help.erpweb.domain.academy.service;

import com.help.authserver.domain.user.entity.constellation.DiscordUser;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
import com.help.erpweb.domain.academy.dto.response.LessonRecordSummaryResponse;
import com.help.erpweb.domain.academy.repository.LessonRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonRecordService {

    private final LessonRecordRepository lessonRecordRepository;
    private final MembershipRepository membershipRepository;
    private final DiscordUserRepository discordUserRepository;

    public List<LessonRecordSummaryResponse> getLessonRecords(
            LocalDate date,
            String time,
            String subject,
            String teacherId
    ) {
        return lessonRecordRepository
                .findLessonRecordSummaries(
                        date,
                        normalize(time),
                        normalize(subject),
                        normalize(teacherId)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private LessonRecordSummaryResponse toResponse(
            LessonRecordRepository.LessonRecordSummaryProjection projection
    ) {
        String teacherName =
                findTeacherName(projection.getMainTeacherId());

        return new LessonRecordSummaryResponse(
                projection.getId(),
                projection.getAcademyName(),
                projection.getClassName(),
                projection.getSubject(),
                projection.getEducationDate(),
                projection.getEducationDuration(),
                teacherName,
                projection.getDescription(),
                projection.getMemberCount() != null
                        ? projection.getMemberCount()
                        : 0
        );
    }

    private String findTeacherName(String teacherSk) {

        if (teacherSk == null || teacherSk.isBlank()) {
            return null;
        }

        try {
            String discordId =
                    membershipRepository
                            .findDiscordIdBySk(teacherSk);

            return discordUserRepository
                    .findByDiscordID(discordId)
                    .map(DiscordUser::getUsername)
                    .orElse(null);

        } catch (Exception e) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}