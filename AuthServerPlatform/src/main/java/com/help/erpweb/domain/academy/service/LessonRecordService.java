package com.help.erpweb.domain.academy.service;

import com.help.authserver.domain.user.entity.constellation.DiscordUser;
import com.help.authserver.domain.user.repository.constellation.DiscordUserRepository;
import com.help.erpweb.domain.academy.dto.response.LessonRecordSummaryResponse;
import com.help.erpweb.domain.academy.entity.AcademyClass;
import com.help.erpweb.domain.academy.repository.AcademyClassRepository;
import com.help.erpweb.domain.academy.repository.LessonRecordRepository;
import com.help.erpweb.domain.membership.repository.MembershipRepository;
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
    private final AcademyClassRepository academyClassRepository;
    private final MembershipRepository membershipRepository;
    private final DiscordUserRepository discordUserRepository;

    public List<LessonRecordSummaryResponse> getLessonRecords(
            LocalDate date,
            String time,
            String subject,
            String teacherId,
            Integer academyId,
            Integer classId
    ) {
        String className = null;

        if (classId != null) {
            AcademyClass academyClass =
                    academyClassRepository
                            .findById(classId)
                            .orElse(null);

            if (academyClass != null) {
                className = String.valueOf(
                        academyClass.getClassNumber()
                );
            }
        }

        String subjectName = convertSubjectIdToName(subject);

        return lessonRecordRepository
                .findLessonRecordSummaries(
                        academyId,
                        className,
                        date,
                        normalize(time),
                        subjectName,
                        normalize(teacherId)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String convertSubjectIdToName(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            return null;
        }

        return switch (subjectId.trim()) {
            case "1" -> "로테이션";
            case "2" -> "언리미티드";
            case "3" -> "스타터";
            default -> subjectId.trim();
        };
    }

    private LessonRecordSummaryResponse toResponse(
            LessonRecordRepository.LessonRecordSummaryProjection projection
    ) {
        String teacherName =
                findTeacherName(
                        projection.getMainTeacherId()
                );

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